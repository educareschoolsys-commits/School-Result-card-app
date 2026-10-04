package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StudentResult
import com.example.data.pdf.PdfGenerator
import com.example.ui.components.EditStudentDialog
import com.example.ui.components.PdfSuccessDialog
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.FailRed
import com.example.ui.theme.FailRedBg
import com.example.ui.theme.PaperWhite
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PassGreenBg
import com.example.ui.theme.SchoolGold
import com.example.ui.theme.SchoolGoldDark
import com.example.ui.theme.SchoolNavy
import com.example.ui.theme.SchoolNavyDark
import com.example.ui.theme.SchoolNavyLight
import com.example.ui.theme.SurfaceCream
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import java.io.File
import java.util.Locale

@Composable
fun ResultCardPreviewScreen(
    students: List<StudentResult>,
    currentStudent: StudentResult?,
    currentIndex: Int,
    sessionName: String,
    issueDate: String,
    onSelectStudent: (Int) -> Unit,
    onNextStudent: () -> Unit,
    onPreviousStudent: () -> Unit,
    onUpdateStudent: (StudentResult) -> Unit,
    onGoToTable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showStudentDropdown by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf(false) }
    var exportedPdfFile by remember { mutableStateOf<File?>(null) }
    var exportTitle by remember { mutableStateOf("") }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    if (currentStudent == null || students.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SurfaceCream)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "No Student Selected",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SchoolNavy
                    )
                )
                Text(
                    text = "Import student data or add students in the Student Data Table.",
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onGoToTable,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy)
                ) {
                    Text("Go to Student Table")
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCream)
    ) {
        // Top Toolbar: Student Navigation & Action Buttons
        Surface(
            color = PaperWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Row 1: Student Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous Student Button
                    IconButton(
                        onClick = onPreviousStudent,
                        enabled = students.size > 1,
                        modifier = Modifier.testTag("prev_student_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Student",
                            tint = if (students.size > 1) SchoolNavy else TextMuted
                        )
                    }

                    // Student Dropdown Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showStudentDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .testTag("student_selector_dropdown_button")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${currentStudent.studentName} (Roll: ${currentStudent.rollNo.ifBlank { "${currentIndex + 1}" }})",
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolNavy,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentStudent.className} • Student ${currentIndex + 1} of ${students.size}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = SchoolNavy
                            )
                        }

                        DropdownMenu(
                            expanded = showStudentDropdown,
                            onDismissRequest = { showStudentDropdown = false },
                            modifier = Modifier.widthIn(min = 280.dp)
                        ) {
                            students.forEachIndexed { idx, s ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${s.studentName} (${s.rollNo})",
                                                    fontWeight = if (idx == currentIndex) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (idx == currentIndex) SchoolNavy else TextDark
                                                )
                                                Text(
                                                    text = "${s.className} • ${s.grade} (${String.format(Locale.US, "%.1f", s.percentage)}%)",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            Text(
                                                text = s.result,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (s.result.contains("PASS", ignoreCase = true)) PassGreen else FailRed
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectStudent(idx)
                                        showStudentDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Next Student Button
                    IconButton(
                        onClick = onNextStudent,
                        enabled = students.size > 1,
                        modifier = Modifier.testTag("next_student_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Student",
                            tint = if (students.size > 1) SchoolNavy else TextMuted
                        )
                    }

                    // Quick Edit Student Info
                    IconButton(
                        onClick = { showEditStudentDialog = true },
                        modifier = Modifier.testTag("quick_edit_student_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Student Info",
                            tint = SchoolGoldDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Action Buttons (Print, Download PDF, Download All as One PDF)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Print Single Result Card
                    Button(
                        onClick = {
                            PdfGenerator.printResultCard(
                                context = context,
                                student = currentStudent,
                                sessionName = sessionName,
                                issueDate = issueDate
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("print_result_card_button")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = SchoolGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Result Card", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Download PDF (Single Student)
                    Button(
                        onClick = {
                            isGeneratingPdf = true
                            val file = PdfGenerator.generateSingleStudentPdf(
                                context = context,
                                student = currentStudent,
                                sessionName = sessionName,
                                issueDate = issueDate
                            )
                            isGeneratingPdf = false
                            if (file != null) {
                                exportedPdfFile = file
                                exportTitle = "Result Card for ${currentStudent.studentName} (Class: ${currentStudent.className})"
                            } else {
                                Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolNavyLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("download_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontSize = 13.sp)
                    }

                    // Download ALL as ONE PDF
                    Button(
                        onClick = {
                            isGeneratingPdf = true
                            val file = PdfGenerator.generateAllStudentsPdf(
                                context = context,
                                students = students,
                                sessionName = sessionName,
                                issueDate = issueDate
                            )
                            isGeneratingPdf = false
                            if (file != null) {
                                exportedPdfFile = file
                                exportTitle = "All Result Cards (${students.size} Students Combined)"
                            } else {
                                Toast.makeText(context, "Failed to create batch PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolGoldDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("download_all_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download All Cards as One PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Print All Students
                    OutlinedButton(
                        onClick = {
                            PdfGenerator.printAllStudents(
                                context = context,
                                students = students,
                                sessionName = sessionName,
                                issueDate = issueDate
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("print_all_button")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = SchoolNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print All (${students.size})", color = SchoolNavy, fontSize = 13.sp)
                    }
                }
            }
        }

        // Main Result Card Paper Preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Print Ready Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PassGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A4 Print-Ready Preview • Educare School System Chagmalai",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // The Realistic A4 Result Card Paper
                ResultCardPaper(
                    student = currentStudent,
                    sessionName = sessionName,
                    issueDate = issueDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Dialogs
    if (showEditStudentDialog) {
        EditStudentDialog(
            student = currentStudent,
            onDismiss = { showEditStudentDialog = false },
            onConfirm = { updated ->
                onUpdateStudent(updated)
                showEditStudentDialog = false
            }
        )
    }

    exportedPdfFile?.let { file ->
        PdfSuccessDialog(
            file = file,
            title = exportTitle,
            onDismiss = { exportedPdfFile = null },
            onOpen = { PdfGenerator.openPdf(context, it) },
            onShare = { PdfGenerator.sharePdf(context, it, exportTitle) }
        )
    }
}

/**
 * Authentic Academic A4 Result Card Paper View
 */
@Composable
fun ResultCardPaper(
    student: StudentResult,
    sessionName: String,
    issueDate: String,
    modifier: Modifier = Modifier
) {
    val isPass = student.result.contains("PASS", ignoreCase = true)

    Surface(
        color = PaperWhite,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(2.dp),
        modifier = modifier
            .border(2.dp, SchoolNavy, RoundedCornerShape(2.dp))
    ) {
        // Inner Ornate Gold Border
        Box(
            modifier = Modifier
                .padding(6.dp)
                .border(1.dp, SchoolGold, RoundedCornerShape(1.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: School Crest, Name, Tagline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_school_crest),
                        contentDescription = "Educare Emblem",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EDUCARE SCHOOL SYSTEM CHAGMALAI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = SchoolNavy,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 0.5.sp,
                                fontSize = 18.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Striving for Excellence in Education • Chagmalai Campus",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title Ribbon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SchoolNavy, RoundedCornerShape(4.dp))
                        .border(1.dp, SchoolGold, RoundedCornerShape(4.dp))
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val cardTitle = if (sessionName.isNotBlank()) {
                        "RESULT CARD • ${sessionName.uppercase()}"
                    } else {
                        "STUDENT PROGRESS REPORT & RESULT CARD"
                    }
                    Text(
                        text = cardTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Student Information Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCream, RoundedCornerShape(6.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Student Name: ",
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.width(95.dp)
                            )
                            Text(
                                text = student.studentName.ifBlank { "—" },
                                fontWeight = FontWeight.Bold,
                                color = SchoolNavy,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Class: ",
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.width(55.dp)
                            )
                            Text(
                                text = student.className.ifBlank { "—" },
                                fontWeight = FontWeight.Bold,
                                color = SchoolNavy,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Roll Number: ",
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.width(95.dp)
                            )
                            Text(
                                text = student.rollNo.ifBlank { "—" },
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Date: ",
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.width(55.dp)
                            )
                            Text(
                                text = issueDate,
                                fontWeight = FontWeight.Normal,
                                color = TextDark,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subject Marks Table
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SchoolNavy, RoundedCornerShape(4.dp))
                ) {
                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SchoolNavy)
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sr.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                            Text("Subject", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                            Text("Total Marks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("Obtained Marks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            Text("Percentage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("Grade", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                        }

                        // Table Rows
                        student.subjects.forEachIndexed { sIdx, sub ->
                            val isEven = sIdx % 2 == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isEven) Color.White else SurfaceCream)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${sIdx + 1}", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                                Text(sub.name, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                val totalStr = if (sub.totalMarks % 1.0 == 0.0) sub.totalMarks.toInt().toString() else String.format(Locale.US, "%.1f", sub.totalMarks)
                                Text(totalStr, color = TextDark, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                val obtStr = if (sub.obtainedMarks % 1.0 == 0.0) sub.obtainedMarks.toInt().toString() else String.format(Locale.US, "%.1f", sub.obtainedMarks)
                                Text(obtStr, fontWeight = FontWeight.Bold, color = if (sub.isPassed) TextDark else FailRed, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                Text(String.format(Locale.US, "%.1f%%", sub.percentage), color = TextDark, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                Text(sub.grade, fontWeight = FontWeight.Bold, color = if (sub.isPassed) SchoolNavyLight else FailRed, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                            }
                            HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }

                        // Table Grand Total Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(28.dp))
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold, color = SchoolNavy, fontSize = 12.sp, modifier = Modifier.weight(2f))
                            val gTotalStr = if (student.totalMarks % 1.0 == 0.0) student.totalMarks.toInt().toString() else String.format(Locale.US, "%.1f", student.totalMarks)
                            Text(gTotalStr, fontWeight = FontWeight.Bold, color = SchoolNavy, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            val gObtStr = if (student.obtainedMarks % 1.0 == 0.0) student.obtainedMarks.toInt().toString() else String.format(Locale.US, "%.1f", student.obtainedMarks)
                            Text(gObtStr, fontWeight = FontWeight.Bold, color = SchoolNavy, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            Text(String.format(Locale.US, "%.2f%%", student.percentage), fontWeight = FontWeight.Bold, color = SchoolNavy, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text(student.grade, fontWeight = FontWeight.Bold, color = SchoolNavy, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5-Pillar Scorecard Grid
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCream),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, RoundedCornerShape(6.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScorecardMetric(
                            label = "TOTAL MARKS",
                            value = if (student.totalMarks % 1.0 == 0.0) student.totalMarks.toInt().toString() else student.totalMarks.toString()
                        )
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderSlate))
                        ScorecardMetric(
                            label = "OBTAINED",
                            value = if (student.obtainedMarks % 1.0 == 0.0) student.obtainedMarks.toInt().toString() else student.obtainedMarks.toString()
                        )
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderSlate))
                        ScorecardMetric(
                            label = "PERCENTAGE",
                            value = String.format(Locale.US, "%.1f%%", student.percentage)
                        )
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderSlate))
                        ScorecardMetric(
                            label = "GRADE",
                            value = student.grade,
                            valueColor = SchoolNavy
                        )
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderSlate))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RESULT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isPass) PassGreenBg else FailRedBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = student.result,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPass) PassGreen else FailRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Teacher Remarks
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.8.dp, BorderSlate, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "Teacher's Remarks:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = SchoolNavy
                        )
                        Text(
                            text = student.defaultRemarks,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grading Scale Legend
                Text(
                    text = "Grading Scale: A+ (≥80%) | A (70-79%) | B (60-69%) | C (50-59%) | D (40-49%) | E (33-39%) | F (<33%)",
                    fontSize = 8.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Signatures & Official Stamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    SignatureColumn(title = "Class Teacher")

                    // Official Seal Circle Stamp
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.dp, BorderSlate, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("OFFICIAL SEAL", fontSize = 6.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text("EDUCARE", fontSize = 7.sp, color = SchoolNavy, fontWeight = FontWeight.Bold)
                            Text("CHAGMALAI", fontSize = 6.sp, color = TextMuted)
                        }
                    }

                    SignatureColumn(title = "Principal")
                }
            }
        }
    }
}

@Composable
private fun ScorecardMetric(
    label: String,
    value: String,
    valueColor: Color = SchoolNavy
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun SignatureColumn(title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(110.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextDark)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}
