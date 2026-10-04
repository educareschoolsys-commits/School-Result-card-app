package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.excel.ExcelParser
import com.example.data.model.StudentResult
import com.example.data.model.SubjectMarks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Educare School System Chagmalai", appName)
    }

    @Test
    fun testStudentCalculations() {
        val student = StudentResult(
            rollNo = "101",
            studentName = "Ali Raza",
            className = "Class 10",
            subjects = listOf(
                SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 85.0),
                SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 95.0),
                SubjectMarks(name = "Science", totalMarks = 100.0, obtainedMarks = 90.0)
            )
        )

        assertEquals(300.0, student.totalMarks, 0.001)
        assertEquals(270.0, student.obtainedMarks, 0.001)
        assertEquals(90.0, student.percentage, 0.001)
        assertEquals("A+", student.grade)
        assertEquals("PASS", student.result)
    }

    @Test
    fun testFailGradeCalculation() {
        val student = StudentResult(
            rollNo = "102",
            studentName = "Failed Student",
            className = "Class 10",
            subjects = listOf(
                SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 25.0),
                SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 20.0)
            )
        )

        assertEquals(200.0, student.totalMarks, 0.001)
        assertEquals(45.0, student.obtainedMarks, 0.001)
        assertEquals(22.5, student.percentage, 0.001)
        assertEquals("F", student.grade)
        assertEquals("FAIL", student.result)
    }

    @Test
    fun testCsvSpreadsheetImport() {
        val csv = """
            Roll No,Student Name,Class,English,Mathematics,Urdu
            101,Hamza Tariq,10th,80,90,75
            102,Sara Khan,10th,85,95,88
        """.trimIndent()

        val parseResult = ExcelParser.parseBytes(csv.toByteArray(StandardCharsets.UTF_8))
        assertTrue(parseResult.isSuccess)
        assertEquals(2, parseResult.students.size)

        val firstStudent = parseResult.students[0]
        assertEquals("Hamza Tariq", firstStudent.studentName)
        assertEquals("10th", firstStudent.className)
        assertEquals(3, firstStudent.subjects.size)
        assertEquals(245.0, firstStudent.obtainedMarks, 0.001)
        assertEquals(300.0, firstStudent.totalMarks, 0.001)
    }

    @Test
    fun testSampleDataGenerator() {
        val samples = ExcelParser.getSampleStudents()
        assertTrue(samples.isNotEmpty())
        assertEquals(5, samples.size)
        assertNotNull(samples.first().studentName)
    }
}
