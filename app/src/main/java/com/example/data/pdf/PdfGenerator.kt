package com.example.data.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.StudentResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    // Standard A4 dimensions in points (72 points per inch)
    // 210mm x 297mm -> 595.28 x 841.89 points
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    private val COLOR_NAVY = Color.rgb(13, 35, 66)       // #0D2342
    private val COLOR_NAVY_LIGHT = Color.rgb(27, 59, 111) // #1B3B6F
    private val COLOR_GOLD = Color.rgb(212, 175, 55)     // #D4AF37
    private val COLOR_GOLD_LIGHT = Color.rgb(243, 229, 171)
    private val COLOR_BG_TINT = Color.rgb(248, 250, 252)
    private val COLOR_BORDER = Color.rgb(203, 213, 225)
    private val COLOR_TEXT_DARK = Color.rgb(30, 41, 59)
    private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139)
    private val COLOR_PASS = Color.rgb(21, 128, 61)
    private val COLOR_PASS_BG = Color.rgb(220, 252, 231)
    private val COLOR_FAIL = Color.rgb(185, 28, 28)
    private val COLOR_FAIL_BG = Color.rgb(254, 226, 226)

    /**
     * Generates a single A4 PDF for one student and saves to cache/documents
     */
    fun generateSingleStudentPdf(
        context: Context,
        student: StudentResult,
        sessionName: String,
        issueDate: String
    ): File? {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val crestBitmap = loadCrestBitmap(context)

            drawResultCard(
                canvas = page.canvas,
                student = student,
                sessionName = sessionName,
                issueDate = issueDate,
                crestBitmap = crestBitmap
            )

            document.finishPage(page)

            val dir = File(context.cacheDir, "result_cards").apply { mkdirs() }
            val cleanName = student.studentName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val file = File(dir, "ResultCard_${student.rollNo}_$cleanName.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
        }
    }

    /**
     * Generates a single combined multi-page PDF containing all students in the class
     */
    fun generateAllStudentsPdf(
        context: Context,
        students: List<StudentResult>,
        sessionName: String,
        issueDate: String
    ): File? {
        if (students.isEmpty()) return null
        val document = PdfDocument()
        try {
            val crestBitmap = loadCrestBitmap(context)

            for ((index, student) in students.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)

                drawResultCard(
                    canvas = page.canvas,
                    student = student,
                    sessionName = sessionName,
                    issueDate = issueDate,
                    crestBitmap = crestBitmap
                )

                document.finishPage(page)
            }

            val dir = File(context.cacheDir, "result_cards").apply { mkdirs() }
            val file = File(dir, "Educare_Chagmalai_All_Result_Cards_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
        }
    }

    /**
     * Native Android Print Integration
     */
    fun printResultCard(
        context: Context,
        student: StudentResult,
        sessionName: String,
        issueDate: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            private var tempPdfFile: File? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                tempPdfFile = generateSingleStudentPdf(context, student, sessionName, issueDate)
                if (tempPdfFile == null) {
                    callback?.onLayoutFailed("Could not generate result card for printing")
                    return
                }

                val info = PrintDocumentInfo.Builder("Educare_Result_Card_${student.rollNo}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val file = tempPdfFile
                if (file == null || destination == null) {
                    callback?.onWriteFailed("File is missing")
                    return
                }

                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val jobName = "Educare Result Card - ${student.studentName}"
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    /**
     * Native Android Print Integration for ALL students
     */
    fun printAllStudents(
        context: Context,
        students: List<StudentResult>,
        sessionName: String,
        issueDate: String
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            private var tempPdfFile: File? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                tempPdfFile = generateAllStudentsPdf(context, students, sessionName, issueDate)
                if (tempPdfFile == null) {
                    callback?.onLayoutFailed("Could not generate result cards for printing")
                    return
                }

                val info = PrintDocumentInfo.Builder("Educare_All_Result_Cards.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(students.size)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val file = tempPdfFile
                if (file == null || destination == null) {
                    callback?.onWriteFailed("File is missing")
                    return
                }

                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val jobName = "Educare All Result Cards (${students.size} Students)"
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    /**
     * Shares a PDF file via standard Android ACTION_SEND Intent
     */
    fun sharePdf(context: Context, file: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Result Card PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens a PDF file via standard Android ACTION_VIEW Intent
     */
    fun openPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open Result Card"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No PDF viewer found on device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCrestBitmap(context: Context): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_school_crest)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Master Canvas Rendering of the Official A4 Result Card
     */
    fun drawResultCard(
        canvas: Canvas,
        student: StudentResult,
        sessionName: String,
        issueDate: String,
        crestBitmap: Bitmap?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Clean Paper Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // 2. Ornate Academic Borders (Navy & Gold)
        val outerMargin = 22f
        val innerMargin = 26f

        // Outer Dark Navy Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = COLOR_NAVY
        canvas.drawRect(outerMargin, outerMargin, PAGE_WIDTH - outerMargin, PAGE_HEIGHT - outerMargin, paint)

        // Inner Gold Border
        paint.strokeWidth = 1.0f
        paint.color = COLOR_GOLD
        canvas.drawRect(innerMargin, innerMargin, PAGE_WIDTH - innerMargin, PAGE_HEIGHT - innerMargin, paint)

        // Corner Ornaments
        drawCornerOrnaments(canvas, innerMargin, PAGE_WIDTH - innerMargin, innerMargin, PAGE_HEIGHT - innerMargin)

        // 3. School Header Section
        val headerTop = 40f
        var currentY = headerTop

        // School Crest Emblem
        val crestSize = 58f
        val crestLeft = 40f
        if (crestBitmap != null) {
            val src = Rect(0, 0, crestBitmap.width, crestBitmap.height)
            val dst = RectF(crestLeft, currentY, crestLeft + crestSize, currentY + crestSize)
            canvas.drawBitmap(crestBitmap, src, dst, paint)
        }

        // School Name: EDUCARE SCHOOL SYSTEM CHAGMALAI
        paint.style = Paint.Style.FILL
        paint.color = COLOR_NAVY
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 21f
        paint.textAlign = Paint.Align.CENTER
        val centerX = PAGE_WIDTH / 2f
        canvas.drawText("EDUCARE SCHOOL SYSTEM CHAGMALAI", centerX, currentY + 22f, paint)

        // Motto / Subtitle
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("Striving for Excellence in Education • Chagmalai Campus", centerX, currentY + 36f, paint)

        // Title Ribbon / Banner: "STUDENT PROGRESS REPORT & RESULT CARD"
        currentY += 56f
        val bannerRect = RectF(outerMargin + 10f, currentY, PAGE_WIDTH - outerMargin - 10f, currentY + 24f)
        paint.color = COLOR_NAVY
        canvas.drawRoundRect(bannerRect, 4f, 4f, paint)

        paint.color = COLOR_GOLD
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(bannerRect, 4f, 4f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 11.5f
        paint.textAlign = Paint.Align.CENTER
        val examTitle = if (sessionName.isNotBlank()) "RESULT CARD • ${sessionName.uppercase()}" else "STUDENT RESULT CARD"
        canvas.drawText(examTitle, centerX, currentY + 16.5f, paint)

        // 4. Student Information Box
        currentY += 34f
        val infoBoxTop = currentY
        val infoBoxBottom = currentY + 54f
        val infoBoxLeft = outerMargin + 12f
        val infoBoxRight = PAGE_WIDTH - outerMargin - 12f

        paint.color = COLOR_BG_TINT
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(infoBoxLeft, infoBoxTop, infoBoxRight, infoBoxBottom), 6f, 6f, paint)

        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(infoBoxLeft, infoBoxTop, infoBoxRight, infoBoxBottom), 6f, 6f, paint)

        // Draw Student Data Grid inside Info Box
        paint.style = Paint.Style.FILL
        val col1X = infoBoxLeft + 14f
        val col2X = centerX + 10f
        val row1Y = infoBoxTop + 20f
        val row2Y = infoBoxTop + 42f

        drawLabelValue(canvas, paint, "Student Name:", student.studentName.ifBlank { "—" }, col1X, row1Y, isBold = true)
        drawLabelValue(canvas, paint, "Class / Grade:", student.className.ifBlank { "—" }, col2X, row1Y, isBold = true)
        drawLabelValue(canvas, paint, "Roll Number:", student.rollNo.ifBlank { "—" }, col1X, row2Y, isBold = false)
        val dateDisplay = if (issueDate.isNotBlank()) issueDate else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        drawLabelValue(canvas, paint, "Date of Issue:", dateDisplay, col2X, row2Y, isBold = false)

        // 5. Subject Marks Table
        currentY = infoBoxBottom + 16f
        val tableLeft = outerMargin + 12f
        val tableRight = PAGE_WIDTH - outerMargin - 12f
        val tableWidth = tableRight - tableLeft

        val colWidthSr = 34f
        val colWidthGrade = 60f
        val colWidthPerc = 70f
        val colWidthObt = 86f
        val colWidthTotal = 80f
        val colWidthSubject = tableWidth - colWidthSr - colWidthGrade - colWidthPerc - colWidthObt - colWidthTotal

        val xSr = tableLeft
        val xSubject = xSr + colWidthSr
        val xTotal = xSubject + colWidthSubject
        val xObt = xTotal + colWidthTotal
        val xPerc = xObt + colWidthObt
        val xGrade = xPerc + colWidthPerc

        // Table Header Row
        val headerHeight = 24f
        paint.color = COLOR_NAVY
        paint.style = Paint.Style.FILL
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + headerHeight, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 9.5f

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Sr.", xSr + colWidthSr / 2f, currentY + 16f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Subject", xSubject + 8f, currentY + 16f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total Marks", xTotal + colWidthTotal - 8f, currentY + 16f, paint)
        canvas.drawText("Obtained Marks", xObt + colWidthObt - 8f, currentY + 16f, paint)
        canvas.drawText("Percentage", xPerc + colWidthPerc - 8f, currentY + 16f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Grade", xGrade + colWidthGrade / 2f, currentY + 16f, paint)

        currentY += headerHeight

        // Subject Rows
        val rowHeight = 21f
        paint.style = Paint.Style.FILL

        for ((idx, sub) in student.subjects.withIndex()) {
            val isEven = idx % 2 == 0
            paint.color = if (isEven) Color.WHITE else COLOR_BG_TINT
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paint)

            // Bottom row divider line
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.6f
            canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, paint)

            // Sr No
            paint.color = COLOR_TEXT_MUTED
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textSize = 9f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("${idx + 1}", xSr + colWidthSr / 2f, currentY + 14.5f, paint)

            // Subject Name
            paint.color = COLOR_TEXT_DARK
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(sub.name, xSubject + 8f, currentY + 14.5f, paint)

            // Total Marks
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textAlign = Paint.Align.RIGHT
            val totalStr = if (sub.totalMarks % 1.0 == 0.0) sub.totalMarks.toInt().toString() else String.format(Locale.US, "%.1f", sub.totalMarks)
            canvas.drawText(totalStr, xTotal + colWidthTotal - 8f, currentY + 14.5f, paint)

            // Obtained Marks
            paint.color = if (sub.isPassed) COLOR_TEXT_DARK else COLOR_FAIL
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val obtStr = if (sub.obtainedMarks % 1.0 == 0.0) sub.obtainedMarks.toInt().toString() else String.format(Locale.US, "%.1f", sub.obtainedMarks)
            canvas.drawText(obtStr, xObt + colWidthObt - 8f, currentY + 14.5f, paint)

            // Percentage
            paint.color = COLOR_TEXT_DARK
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val percStr = String.format(Locale.US, "%.1f%%", sub.percentage)
            canvas.drawText(percStr, xPerc + colWidthPerc - 8f, currentY + 14.5f, paint)

            // Grade
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.color = if (sub.isPassed) COLOR_NAVY_LIGHT else COLOR_FAIL
            canvas.drawText(sub.grade, xGrade + colWidthGrade / 2f, currentY + 14.5f, paint)

            currentY += rowHeight
        }

        // Table Total / Aggregate Row
        val totalRowHeight = 23f
        paint.color = Color.rgb(241, 245, 249)
        paint.style = Paint.Style.FILL
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + totalRowHeight, paint)

        // Borders around table
        paint.color = COLOR_NAVY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRect(tableLeft, infoBoxBottom + 16f, tableRight, currentY + totalRowHeight, paint)

        // Total Row Texts
        paint.style = Paint.Style.FILL
        paint.color = COLOR_NAVY
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 9.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("GRAND TOTAL", xSubject + 8f, currentY + 16f, paint)

        paint.textAlign = Paint.Align.RIGHT
        val grandTotalStr = if (student.totalMarks % 1.0 == 0.0) student.totalMarks.toInt().toString() else String.format(Locale.US, "%.1f", student.totalMarks)
        canvas.drawText(grandTotalStr, xTotal + colWidthTotal - 8f, currentY + 16f, paint)

        val grandObtStr = if (student.obtainedMarks % 1.0 == 0.0) student.obtainedMarks.toInt().toString() else String.format(Locale.US, "%.1f", student.obtainedMarks)
        canvas.drawText(grandObtStr, xObt + colWidthObt - 8f, currentY + 16f, paint)

        val grandPercStr = String.format(Locale.US, "%.2f%%", student.percentage)
        canvas.drawText(grandPercStr, xPerc + colWidthPerc - 8f, currentY + 16f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(student.grade, xGrade + colWidthGrade / 2f, currentY + 16f, paint)

        currentY += totalRowHeight + 14f

        // 6. Summary Highlight Scorecard (5 Pillars: Total, Obtained, Percentage, Grade, Result)
        val scoreCardHeight = 48f
        val cardRect = RectF(tableLeft, currentY, tableRight, currentY + scoreCardHeight)
        paint.style = Paint.Style.FILL
        paint.color = COLOR_BG_TINT
        canvas.drawRoundRect(cardRect, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        canvas.drawRoundRect(cardRect, 6f, 6f, paint)

        // Draw 5 Metric Columns
        val metricWidth = tableWidth / 5f
        val metrics = listOf(
            "TOTAL MARKS" to grandTotalStr,
            "OBTAINED" to grandObtStr,
            "PERCENTAGE" to String.format(Locale.US, "%.1f%%", student.percentage),
            "GRADE" to student.grade,
            "FINAL RESULT" to student.result
        )

        for ((mIdx, metric) in metrics.withIndex()) {
            val colCenterX = tableLeft + (mIdx * metricWidth) + (metricWidth / 2f)

            // Divider vertical line
            if (mIdx > 0) {
                paint.color = COLOR_BORDER
                paint.strokeWidth = 1f
                canvas.drawLine(tableLeft + (mIdx * metricWidth), currentY + 8f, tableLeft + (mIdx * metricWidth), currentY + scoreCardHeight - 8f, paint)
            }

            // Metric Label
            paint.style = Paint.Style.FILL
            paint.color = COLOR_TEXT_MUTED
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textSize = 7.5f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(metric.first, colCenterX, currentY + 18f, paint)

            // Metric Value
            if (metric.first == "FINAL RESULT") {
                val isPass = student.result.contains("PASS", ignoreCase = true)
                // Draw badge background
                val badgeWidth = 62f
                val badgeHeight = 18f
                val badgeRect = RectF(colCenterX - badgeWidth / 2f, currentY + 24f, colCenterX + badgeWidth / 2f, currentY + 24f + badgeHeight)
                paint.color = if (isPass) COLOR_PASS_BG else COLOR_FAIL_BG
                canvas.drawRoundRect(badgeRect, 4f, 4f, paint)

                paint.color = if (isPass) COLOR_PASS else COLOR_FAIL
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText(metric.second, colCenterX, currentY + 37f, paint)
            } else {
                paint.color = COLOR_NAVY
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                paint.textSize = 13f
                canvas.drawText(metric.second, colCenterX, currentY + 37f, paint)
            }
        }

        currentY += scoreCardHeight + 12f

        // 7. Teacher's Remarks Box
        val remarksHeight = 36f
        val remarksRect = RectF(tableLeft, currentY, tableRight, currentY + remarksHeight)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRoundRect(remarksRect, 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = COLOR_BORDER
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(remarksRect, 4f, 4f, paint)

        paint.style = Paint.Style.FILL
        paint.color = COLOR_NAVY
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Class Teacher's Remarks:", tableLeft + 10f, currentY + 15f, paint)

        paint.color = COLOR_TEXT_DARK
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        paint.textSize = 9f
        canvas.drawText(student.defaultRemarks, tableLeft + 10f, currentY + 28f, paint)

        // 8. Grading Scale Guide at Bottom
        currentY += remarksHeight + 14f
        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 7f
        paint.textAlign = Paint.Align.CENTER
        val scaleLegend = "Grading Scale: A+ (≥80%) Outstanding  |  A (70-79%) Excellent  |  B (60-69%) Very Good  |  C (50-59%) Good  |  D (40-49%) Satisfactory  |  E (33-39%) Pass  |  F (<33%) Fail"
        canvas.drawText(scaleLegend, centerX, currentY, paint)

        // 9. Signatures and Official Stamp Section
        val sigAreaY = PAGE_HEIGHT - outerMargin - 52f
        val sigLineLength = 105f
        val sig1X = tableLeft + 15f
        val sig2X = centerX - (sigLineLength / 2f)
        val sig3X = tableRight - sigLineLength - 15f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = COLOR_TEXT_DARK

        // Teacher signature line
        canvas.drawLine(sig1X, sigAreaY, sig1X + sigLineLength, sigAreaY, paint)
        // Controller signature line
        canvas.drawLine(sig2X, sigAreaY, sig2X + sigLineLength, sigAreaY, paint)
        // Principal signature line
        canvas.drawLine(sig3X, sigAreaY, sig3X + sigLineLength, sigAreaY, paint)

        // Signature Labels
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText("Class Teacher", sig1X + sigLineLength / 2f, sigAreaY + 13f, paint)
        canvas.drawText("Controller of Exams", sig2X + sigLineLength / 2f, sigAreaY + 13f, paint)
        canvas.drawText("Principal", sig3X + sigLineLength / 2f, sigAreaY + 13f, paint)

        // Official Seal Circle Watermark/Stamp
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(203, 213, 225)
        val sealRadius = 24f
        val sealX = centerX
        val sealY = sigAreaY - 8f
        canvas.drawCircle(sealX, sealY, sealRadius, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 6f
        canvas.drawText("OFFICIAL SEAL", sealX, sealY - 2f, paint)
        canvas.drawText("EDUCAR E", sealX, sealY + 7f, paint)
    }

    private fun drawLabelValue(
        canvas: Canvas,
        paint: Paint,
        label: String,
        value: String,
        x: Float,
        y: Float,
        isBold: Boolean
    ) {
        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(label, x, y, paint)

        val labelWidth = paint.measureText(label) + 6f
        paint.color = COLOR_NAVY
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        paint.textSize = 10f
        canvas.drawText(value, x + labelWidth, y, paint)
    }

    private fun drawCornerOrnaments(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = COLOR_GOLD
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        val size = 12f

        // Top Left
        canvas.drawLine(left, top, left + size, top, paint)
        canvas.drawLine(left, top, left, top + size, paint)

        // Top Right
        canvas.drawLine(right, top, right - size, top, paint)
        canvas.drawLine(right, top, right, top + size, paint)

        // Bottom Left
        canvas.drawLine(left, bottom, left + size, bottom, paint)
        canvas.drawLine(left, bottom, left, bottom - size, paint)

        // Bottom Right
        canvas.drawLine(right, bottom, right - size, bottom, paint)
        canvas.drawLine(right, bottom, right, bottom - size, paint)
    }
}
