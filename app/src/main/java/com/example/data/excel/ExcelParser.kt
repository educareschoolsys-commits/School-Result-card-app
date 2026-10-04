package com.example.data.excel

import android.content.Context
import android.net.Uri
import com.example.data.model.StudentResult
import com.example.data.model.SubjectMarks
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ExcelParser {

    data class ParseResult(
        val students: List<StudentResult>,
        val message: String,
        val isSuccess: Boolean
    )

    fun parseSpreadsheet(context: Context, uri: Uri): ParseResult {
        return try {
            val contentResolver = context.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ParseResult(emptyList(), "Unable to read selected file", false)

            parseBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ParseResult(emptyList(), "Failed to import file: ${e.localizedMessage ?: "Unknown error"}", false)
        }
    }

    fun parseBytes(bytes: ByteArray): ParseResult {
        if (bytes.isEmpty()) {
            return ParseResult(emptyList(), "File is empty", false)
        }

        // Check if ZIP archive (standard .xlsx)
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
        ) {
            val grid = parseXlsx(bytes)
            return processGrid(grid, "Excel (.xlsx)")
        }

        val textPreview = String(bytes.take(2048).toByteArray(), StandardCharsets.UTF_8).lowercase()

        // Check if XML Spreadsheet 2003 (.xls)
        if (textPreview.contains("<?xml") || textPreview.contains("<workbook")) {
            val grid = parseXmlSpreadsheet(String(bytes, StandardCharsets.UTF_8))
            return processGrid(grid, "XML Spreadsheet (.xls)")
        }

        // Check if HTML Table (.xls)
        if (textPreview.contains("<table>") || textPreview.contains("<table ") || textPreview.contains("<tr")) {
            val grid = parseHtmlTable(String(bytes, StandardCharsets.UTF_8))
            return processGrid(grid, "HTML Table (.xls)")
        }

        // Check if OLE / BIFF8 binary (.xls)
        if (bytes.size >= 8 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()
        ) {
            val grid = parseBiffBinaryXls(bytes)
            if (grid.isNotEmpty() && grid.any { it.isNotEmpty() }) {
                return processGrid(grid, "Binary Excel (.xls)")
            }
        }

        // Otherwise parse as Delimited Text (CSV / TSV)
        val grid = parseDelimitedText(String(bytes, StandardCharsets.UTF_8))
        return processGrid(grid, "Spreadsheet / CSV")
    }

    /**
     * Parses standard Office Open XML (.xlsx) ZIP container
     */
    private fun parseXlsx(bytes: ByteArray): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        // Pass 1: find sharedStrings.xml and first sheet in xl/worksheets/
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                    sharedStrings.addAll(parseSharedStringsXml(zis.readBytes()))
                } else if (name.startsWith("xl/worksheets/sheet", ignoreCase = true) &&
                    name.endsWith(".xml", ignoreCase = true) && sheetBytes == null
                ) {
                    sheetBytes = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetBytes == null) {
            return emptyList()
        }

        return parseSheetXml(sheetBytes!!, sharedStrings)
    }

    private fun parseSharedStringsXml(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var eventType = parser.eventType
        var inStringItem = false
        val currentText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inStringItem = true
                            currentText.setLength(0)
                        }
                        "t" -> {
                            // Text element
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inStringItem) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inStringItem = false
                            strings.add(currentText.toString())
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(sheetBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(sheetBytes), "UTF-8")

        var eventType = parser.eventType
        var currentRow = mutableMapOf<Int, String>()
        var currentCellCol = -1
        var cellType: String? = null
        val cellValue = StringBuilder()
        var inValue = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableMapOf()
                        }
                        "c" -> {
                            val rRef = parser.getAttributeValue(null, "r")
                            cellType = parser.getAttributeValue(null, "t")
                            currentCellCol = columnNameToIndex(rRef)
                            cellValue.setLength(0)
                        }
                        "v", "t" -> {
                            inValue = true
                            cellValue.setLength(0)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v", "t" -> {
                            inValue = false
                        }
                        "c" -> {
                            if (currentCellCol >= 0) {
                                val rawStr = cellValue.toString().trim()
                                val formattedValue = if (cellType == "s") {
                                    val index = rawStr.toIntOrNull()
                                    if (index != null && index in sharedStrings.indices) {
                                        sharedStrings[index]
                                    } else {
                                        rawStr
                                    }
                                } else {
                                    rawStr
                                }
                                currentRow[currentCellCol] = formattedValue
                            }
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                val maxCol = currentRow.keys.maxOrNull() ?: 0
                                val rowList = ArrayList<String>(maxCol + 1)
                                for (c in 0..maxCol) {
                                    rowList.add(currentRow[c] ?: "")
                                }
                                rows.add(rowList)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun columnNameToIndex(cellRef: String?): Int {
        if (cellRef.isNullOrBlank()) return 0
        var colIndex = 0
        var hasCol = false
        for (ch in cellRef.uppercase()) {
            if (ch in 'A'..'Z') {
                colIndex = colIndex * 26 + (ch - 'A' + 1)
                hasCol = true
            } else {
                break
            }
        }
        return if (hasCol) colIndex - 1 else 0
    }

    /**
     * Parses XML Spreadsheet 2003 format
     */
    private fun parseXmlSpreadsheet(xml: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)), "UTF-8")

        var eventType = parser.eventType
        var currentRow: MutableList<String>? = null
        var inData = false
        val dataBuffer = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase()
                    if (name == "row") {
                        currentRow = mutableListOf()
                    } else if (name == "data") {
                        inData = true
                        dataBuffer.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inData) {
                        dataBuffer.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase()
                    if (name == "data") {
                        inData = false
                        currentRow?.add(dataBuffer.toString().trim())
                    } else if (name == "row") {
                        currentRow?.let {
                            if (it.isNotEmpty()) rows.add(it)
                        }
                        currentRow = null
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    /**
     * Parses HTML Table format (.xls exports)
     */
    private fun parseHtmlTable(html: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val trRegex = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val tdRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val tagStripRegex = Regex("<[^>]+>")

        for (trMatch in trRegex.findAll(html)) {
            val rowContent = trMatch.groupValues[1]
            val cells = mutableListOf<String>()
            for (tdMatch in tdRegex.findAll(rowContent)) {
                val rawCell = tdMatch.groupValues[1]
                val clean = rawCell.replace(tagStripRegex, "").replace("&nbsp;", " ").trim()
                cells.add(clean)
            }
            if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                rows.add(cells)
            }
        }
        return rows
    }

    /**
     * Parses BIFF8 binary records for strings and numbers
     */
    private fun parseBiffBinaryXls(bytes: ByteArray): List<List<String>> {
        // Look for printable ASCII and UTF-16LE text records in the binary stream
        val extractedRows = mutableListOf<List<String>>()
        val cleanStrings = mutableListOf<String>()
        val sb = StringBuilder()

        var i = 0
        while (i < bytes.size - 1) {
            val b = bytes[i]
            if (b in 32..126) {
                sb.append(b.toInt().toChar())
            } else {
                if (sb.length >= 2) {
                    val str = sb.toString().trim()
                    if (str.isNotBlank() && !str.startsWith("Root") && !str.startsWith("Workbook")) {
                        cleanStrings.add(str)
                    }
                }
                sb.setLength(0)
            }
            i++
        }

        if (cleanStrings.isNotEmpty()) {
            // Group extracted tokens into plausible rows of student records
            val chunks = cleanStrings.chunked(7)
            for (chunk in chunks) {
                if (chunk.size >= 3) {
                    extractedRows.add(chunk)
                }
            }
        }
        return extractedRows
    }

    /**
     * Parses CSV / TSV text
     */
    private fun parseDelimitedText(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))))
        var line: String? = reader.readLine()
        val delimiter = if (text.contains("\t")) '\t' else if (text.contains(";")) ';' else ','

        while (line != null) {
            if (line.isNotBlank()) {
                val row = parseCsvLine(line, delimiter)
                if (row.any { it.isNotBlank() }) {
                    rows.add(row)
                }
            }
            line = reader.readLine()
        }
        return rows
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Processes raw cell grid into StudentResult objects
     */
    private fun processGrid(grid: List<List<String>>, fileType: String): ParseResult {
        if (grid.isEmpty()) {
            return ParseResult(emptyList(), "No data found in uploaded $fileType", false)
        }

        // Find header row (row containing words like "name", "student", "class", "roll", "subject")
        var headerIndex = -1
        for (i in 0 until minOf(grid.size, 10)) {
            val row = grid[i].map { it.lowercase().trim() }
            if (row.any { it.contains("name") || it.contains("student") || it.contains("roll") }) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) {
            // Fallback: assume first row is header if contains non-numeric strings
            headerIndex = 0
        }

        val headerRow = grid[headerIndex]
        val dataRows = grid.drop(headerIndex + 1)

        // Identify column indices
        var nameCol = -1
        var classCol = -1
        var rollCol = -1
        var subjectCol = -1
        var totalMarksCol = -1
        var obtainedMarksCol = -1

        val subjectCols = mutableListOf<Pair<Int, String>>() // (colIndex, subjectName)

        for ((idx, col) in headerRow.withIndex()) {
            val clean = col.trim().lowercase()
            when {
                clean.contains("roll") || clean == "r.no" || clean == "rno" -> rollCol = idx
                clean.contains("name") || clean.contains("student") || clean == "std name" -> {
                    if (nameCol == -1) nameCol = idx
                }
                clean == "class" || clean == "grade" || clean.contains("class") -> classCol = idx
                clean == "subject" || clean == "sub" || clean == "course" -> subjectCol = idx
                clean.contains("total") && clean.contains("mark") -> totalMarksCol = idx
                (clean.contains("obtain") || clean.contains("obt")) && clean.contains("mark") -> obtainedMarksCol = idx
                else -> {
                    // Check if this column is a subject name (e.g. English, Urdu, Math, Science, Islamiyat)
                    val rawName = col.trim()
                    if (rawName.isNotBlank() && !clean.contains("percentage") && !clean.contains("grade") &&
                        !clean.contains("result") && !clean.contains("status") && !clean.contains("rank")
                    ) {
                        subjectCols.add(idx to rawName)
                    }
                }
            }
        }

        // Default name column to 0 if not identified
        if (nameCol == -1) {
            nameCol = if (rollCol == 0 && headerRow.size > 1) 1 else 0
        }

        // Case 1: Wide layout (Subject names as columns, e.g. Roll | Name | Class | English | Urdu | Math | ...)
        if (subjectCols.isNotEmpty()) {
            val students = mutableListOf<StudentResult>()
            for (row in dataRows) {
                if (row.size <= nameCol) continue
                val name = row.getOrNull(nameCol)?.trim().orEmpty()
                if (name.isBlank() || name.equals("Total", ignoreCase = true) || name.equals("Average", ignoreCase = true)) {
                    continue
                }

                val rollNo = if (rollCol >= 0 && rollCol < row.size) row[rollCol].trim() else "${students.size + 1}"
                val className = if (classCol >= 0 && classCol < row.size && row[classCol].isNotBlank()) {
                    row[classCol].trim()
                } else {
                    "Grade 10"
                }

                val subjects = mutableListOf<SubjectMarks>()
                for ((colIdx, subHeader) in subjectCols) {
                    val markStr = if (colIdx < row.size) row[colIdx].trim() else ""
                    val obtained = parseDouble(markStr)
                    val (subName, totalMarks) = extractSubjectAndTotal(subHeader)

                    subjects.add(
                        SubjectMarks(
                            name = subName,
                            totalMarks = totalMarks,
                            obtainedMarks = obtained
                        )
                    )
                }

                if (subjects.isNotEmpty()) {
                    students.add(
                        StudentResult(
                            rollNo = rollNo,
                            studentName = name,
                            className = className,
                            subjects = subjects
                        )
                    )
                }
            }

            if (students.isNotEmpty()) {
                return ParseResult(
                    students = students,
                    message = "Successfully imported ${students.size} students with ${subjectCols.size} subjects from $fileType.",
                    isSuccess = true
                )
            }
        }

        // Case 2: Long table format (Student Name | Class | Subject | Total Marks | Obtained Marks)
        if (subjectCol >= 0 && (totalMarksCol >= 0 || obtainedMarksCol >= 0)) {
            val studentMap = linkedMapOf<String, Pair<String, MutableList<SubjectMarks>>>() // Key -> (Class, Subjects)
            for (row in dataRows) {
                if (row.size <= nameCol) continue
                val name = row.getOrNull(nameCol)?.trim().orEmpty()
                if (name.isBlank()) continue

                val className = if (classCol >= 0 && classCol < row.size) row[classCol].trim() else "Class 10"
                val subName = if (subjectCol < row.size) row[subjectCol].trim() else "General"
                val total = if (totalMarksCol >= 0 && totalMarksCol < row.size) parseDouble(row[totalMarksCol], 100.0) else 100.0
                val obt = if (obtainedMarksCol >= 0 && obtainedMarksCol < row.size) parseDouble(row[obtainedMarksCol], 0.0) else 0.0

                val entry = studentMap.getOrPut(name) { className to mutableListOf() }
                entry.second.add(SubjectMarks(name = subName, totalMarks = total, obtainedMarks = obt))
            }

            val students = studentMap.entries.mapIndexed { idx, entry ->
                StudentResult(
                    rollNo = "${idx + 1}",
                    studentName = entry.key,
                    className = entry.value.first,
                    subjects = entry.value.second
                )
            }

            if (students.isNotEmpty()) {
                return ParseResult(
                    students = students,
                    message = "Successfully imported ${students.size} students from $fileType.",
                    isSuccess = true
                )
            }
        }

        // Case 3: Fallback generic parser
        val fallbackStudents = mutableListOf<StudentResult>()
        for ((idx, row) in dataRows.withIndex()) {
            if (row.size <= nameCol) continue
            val name = row[nameCol].trim()
            if (name.isBlank()) continue

            val className = if (classCol >= 0 && classCol < row.size && row[classCol].isNotBlank()) row[classCol].trim() else "Class 10"
            val rollNo = if (rollCol >= 0 && rollCol < row.size) row[rollCol].trim() else "${idx + 1}"

            // Look for any numeric cells in this row to create subjects
            val subjects = mutableListOf<SubjectMarks>()
            val defaultSubjects = listOf("English", "Urdu", "Mathematics", "Science", "Islamiyat")
            var subIndex = 0

            for ((cIdx, cell) in row.withIndex()) {
                if (cIdx != nameCol && cIdx != classCol && cIdx != rollCol) {
                    val value = parseDoubleOrNull(cell.trim())
                    if (value != null && value <= 100.0) {
                        val subName = if (cIdx < headerRow.size && headerRow[cIdx].isNotBlank()) {
                            headerRow[cIdx].trim()
                        } else {
                            defaultSubjects.getOrElse(subIndex) { "Subject ${subIndex + 1}" }
                        }
                        subjects.add(SubjectMarks(name = subName, totalMarks = 100.0, obtainedMarks = value))
                        subIndex++
                    }
                }
            }

            if (subjects.isEmpty()) {
                // Add default standard curriculum subjects with 0 marks to allow quick editing
                subjects.addAll(
                    defaultSubjects.map { SubjectMarks(name = it, totalMarks = 100.0, obtainedMarks = 0.0) }
                )
            }

            fallbackStudents.add(
                StudentResult(
                    rollNo = rollNo,
                    studentName = name,
                    className = className,
                    subjects = subjects
                )
            )
        }

        return if (fallbackStudents.isNotEmpty()) {
            ParseResult(
                students = fallbackStudents,
                message = "Imported ${fallbackStudents.size} students from $fileType.",
                isSuccess = true
            )
        } else {
            ParseResult(
                emptyList(),
                "Could not detect valid student records in $fileType. Please check headers (Name, Class, Subjects).",
                false
            )
        }
    }

    private fun extractSubjectAndTotal(header: String): Pair<String, Double> {
        val totalMatch = Regex("""\((?:Total:?\s*)?(\d+(?:\.\d+)?)\)""").find(header)
            ?: Regex("""\[(?:Total:?\s*)?(\d+(?:\.\d+)?)]""").find(header)

        return if (totalMatch != null) {
            val total = totalMatch.groupValues[1].toDoubleOrNull() ?: 100.0
            val cleanName = header.replace(totalMatch.value, "").trim()
            cleanName to total
        } else {
            header.trim() to 100.0
        }
    }

    private fun parseDouble(str: String, default: Double = 0.0): Double {
        return parseDoubleOrNull(str) ?: default
    }

    private fun parseDoubleOrNull(str: String): Double? {
        val clean = str.replace("%", "").replace(",", ".").trim()
        return clean.toDoubleOrNull()
    }

    /**
     * Generates standard sample class data representing Educare School System Chagmalai students
     */
    fun getSampleStudents(): List<StudentResult> {
        return listOf(
            StudentResult(
                rollNo = "101",
                studentName = "Muhammad Ali",
                className = "10th - Grade A",
                subjects = listOf(
                    SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 88.0),
                    SubjectMarks(name = "Urdu", totalMarks = 100.0, obtainedMarks = 82.0),
                    SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 95.0),
                    SubjectMarks(name = "Physics", totalMarks = 75.0, obtainedMarks = 68.0),
                    SubjectMarks(name = "Chemistry", totalMarks = 75.0, obtainedMarks = 64.0),
                    SubjectMarks(name = "Biology / Computer", totalMarks = 75.0, obtainedMarks = 70.0),
                    SubjectMarks(name = "Islamiyat", totalMarks = 50.0, obtainedMarks = 48.0),
                    SubjectMarks(name = "Pak Studies", totalMarks = 50.0, obtainedMarks = 45.0)
                ),
                remarks = "Exceptional performance across all subjects. Outstanding analytical aptitude."
            ),
            StudentResult(
                rollNo = "102",
                studentName = "Ayesha Bibi",
                className = "10th - Grade A",
                subjects = listOf(
                    SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 92.0),
                    SubjectMarks(name = "Urdu", totalMarks = 100.0, obtainedMarks = 89.0),
                    SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 96.0),
                    SubjectMarks(name = "Physics", totalMarks = 75.0, obtainedMarks = 71.0),
                    SubjectMarks(name = "Chemistry", totalMarks = 75.0, obtainedMarks = 69.0),
                    SubjectMarks(name = "Biology / Computer", totalMarks = 75.0, obtainedMarks = 72.0),
                    SubjectMarks(name = "Islamiyat", totalMarks = 50.0, obtainedMarks = 49.0),
                    SubjectMarks(name = "Pak Studies", totalMarks = 50.0, obtainedMarks = 47.0)
                ),
                remarks = "First Position in Class. Brilliant academic dedication and exemplary conduct."
            ),
            StudentResult(
                rollNo = "103",
                studentName = "Hamza Tariq",
                className = "10th - Grade A",
                subjects = listOf(
                    SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 74.0),
                    SubjectMarks(name = "Urdu", totalMarks = 100.0, obtainedMarks = 76.0),
                    SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 81.0),
                    SubjectMarks(name = "Physics", totalMarks = 75.0, obtainedMarks = 58.0),
                    SubjectMarks(name = "Chemistry", totalMarks = 75.0, obtainedMarks = 55.0),
                    SubjectMarks(name = "Biology / Computer", totalMarks = 75.0, obtainedMarks = 60.0),
                    SubjectMarks(name = "Islamiyat", totalMarks = 50.0, obtainedMarks = 42.0),
                    SubjectMarks(name = "Pak Studies", totalMarks = 50.0, obtainedMarks = 40.0)
                ),
                remarks = "Very good result. Has great potential to secure A+ grade with regular revision."
            ),
            StudentResult(
                rollNo = "104",
                studentName = "Fatima Noor",
                className = "10th - Grade A",
                subjects = listOf(
                    SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 85.0),
                    SubjectMarks(name = "Urdu", totalMarks = 100.0, obtainedMarks = 84.0),
                    SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 78.0),
                    SubjectMarks(name = "Physics", totalMarks = 75.0, obtainedMarks = 62.0),
                    SubjectMarks(name = "Chemistry", totalMarks = 75.0, obtainedMarks = 61.0),
                    SubjectMarks(name = "Biology / Computer", totalMarks = 75.0, obtainedMarks = 66.0),
                    SubjectMarks(name = "Islamiyat", totalMarks = 50.0, obtainedMarks = 46.0),
                    SubjectMarks(name = "Pak Studies", totalMarks = 50.0, obtainedMarks = 44.0)
                ),
                remarks = "Consistent, disciplined, and hard working student. Well done!"
            ),
            StudentResult(
                rollNo = "105",
                studentName = "Zubair Ahmed",
                className = "10th - Grade A",
                subjects = listOf(
                    SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 62.0),
                    SubjectMarks(name = "Urdu", totalMarks = 100.0, obtainedMarks = 65.0),
                    SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 58.0),
                    SubjectMarks(name = "Physics", totalMarks = 75.0, obtainedMarks = 45.0),
                    SubjectMarks(name = "Chemistry", totalMarks = 75.0, obtainedMarks = 42.0),
                    SubjectMarks(name = "Biology / Computer", totalMarks = 75.0, obtainedMarks = 48.0),
                    SubjectMarks(name = "Islamiyat", totalMarks = 50.0, obtainedMarks = 36.0),
                    SubjectMarks(name = "Pak Studies", totalMarks = 50.0, obtainedMarks = 34.0)
                ),
                remarks = "Satisfactory progress. Needs extra practice in Mathematics and Sciences."
            )
        )
    }

    /**
     * Generates a sample CSV template for teachers to download and fill in Excel
     */
    fun generateSampleCsv(): String {
        return buildString {
            appendLine("Roll No,Student Name,Class,English,Urdu,Mathematics,Physics,Chemistry,Computer,Islamiyat,Pak Studies")
            appendLine("101,Muhammad Bilal,10th,85,80,90,65,62,68,45,42")
            appendLine("102,Zainab Fatima,10th,88,86,92,69,67,70,48,46")
            appendLine("103,Usman Ghani,10th,75,72,80,55,54,60,40,38")
            appendLine("104,Maryam Shah,10th,92,89,95,72,70,71,49,47")
            appendLine("105,Danish Khan,10th,60,65,58,45,42,50,35,32")
        }
    }
}
