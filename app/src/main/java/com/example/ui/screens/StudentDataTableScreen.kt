package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.excel.ExcelParser
import com.example.data.model.StudentResult
import com.example.data.model.SubjectMarks
import com.example.ui.components.AddStudentDialog
import com.example.ui.components.AddSubjectDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EditStudentDialog
import com.example.ui.components.EditSubjectDialog
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.FailRed
import com.example.ui.theme.FailRedBg
import com.example.ui.theme.PaperWhite
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PassGreenBg
import com.example.ui.theme.SchoolGold
import com.example.ui.theme.SchoolGoldDark
import com.example.ui.theme.SchoolGoldLight
import com.example.ui.theme.SchoolNavy
import com.example.ui.theme.SchoolNavyDark
import com.example.ui.theme.SchoolNavyLight
import com.example.ui.theme.SurfaceCream
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun StudentDataTableScreen(
    students: List<StudentResult>,
    filteredStudents: List<StudentResult>,
    searchQuery: String,
    filterClass: String,
    availableClasses: List<String>,
    onSearchQueryChanged: (String) -> Unit,
    onFilterClassSelected: (String) -> Unit,
    onUploadExcelClicked: () -> Unit,
    onLoadSampleClicked: () -> Unit,
    onAddStudent: (name: String, className: String, rollNo: String, copySubjects: Boolean) -> Unit,
    onUpdateStudent: (StudentResult) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onAddSubjectToStudent: (studentId: String, name: String, total: Double, obt: Double) -> Unit,
    onAddSubjectToAll: (name: String, total: Double) -> Unit,
    onDeleteSubject: (studentId: String, subjectId: String) -> Unit,
    onUpdateSubject: (studentId: String, subjectId: String, name: String, total: Double, obt: Double) -> Unit,
    onViewResultCard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAddSubjectClassDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<StudentResult?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentResult?>(null) }
    var subjectToAddForStudent by remember { mutableStateOf<StudentResult?>(null) }
    var subjectToEditData by remember { mutableStateOf<Pair<String, SubjectMarks>?>(null) }

    // Expanded states for inline student subject tables
    val expandedStudents = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCream)
    ) {
        // Top Action Bar
        Surface(
            color = PaperWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Student Records & Marks",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SchoolNavy
                            )
                        )
                        Text(
                            text = "${students.size} students enrolled • Auto-calculating marks & grades",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onUploadExcelClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("upload_excel_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = SchoolGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upload Excel Sheet",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { showAddStudentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolNavyLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_student_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Student")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Actions Row: Add Subject to Class, Sample Template, Load Demo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showAddSubjectClassDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_subject_class_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SchoolNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subject to Class", color = SchoolNavy, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            exportSampleTemplate(context)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("download_template_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SchoolNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel/CSV Template", color = SchoolNavy, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onLoadSampleClicked,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("load_demo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SchoolNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Sample Class", color = SchoolNavy, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar & Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search by student name or roll number...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SchoolNavy,
                            unfocusedBorderColor = BorderSlate
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("search_student_input")
                    )
                }

                if (availableClasses.size > 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableClasses) { cls ->
                            FilterChip(
                                selected = filterClass == cls,
                                onClick = { onFilterClassSelected(cls) },
                                label = { Text(cls, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SchoolNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Student List / Table
        if (students.isEmpty()) {
            EmptyStudentState(
                onUploadExcel = onUploadExcelClicked,
                onAddStudent = { showAddStudentDialog = true },
                onLoadSample = onLoadSampleClicked
            )
        } else if (filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students match your search: '$searchQuery'",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredStudents, key = { it.id }) { student ->
                    val isExpanded = expandedStudents[student.id] ?: false

                    StudentRowCard(
                        student = student,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedStudents[student.id] = !isExpanded
                        },
                        onViewResultCard = { onViewResultCard(student.id) },
                        onEditStudent = { studentToEdit = student },
                        onDeleteStudent = { studentToDelete = student },
                        onAddSubject = { subjectToAddForStudent = student },
                        onEditSubject = { sub -> subjectToEditData = student.id to sub },
                        onDeleteSubject = { subId -> onDeleteSubject(student.id, subId) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }

    // Dialogs
    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { name, cls, roll, copy ->
                onAddStudent(name, cls, roll, copy)
                showAddStudentDialog = false
            }
        )
    }

    if (showAddSubjectClassDialog) {
        AddSubjectDialog(
            studentName = null,
            onDismiss = { showAddSubjectClassDialog = false },
            onConfirm = { name, total, _, applyToAll ->
                if (applyToAll) {
                    onAddSubjectToAll(name, total)
                }
                showAddSubjectClassDialog = false
            }
        )
    }

    subjectToAddForStudent?.let { targetStudent ->
        AddSubjectDialog(
            studentName = targetStudent.studentName,
            onDismiss = { subjectToAddForStudent = null },
            onConfirm = { name, total, obt, applyToAll ->
                if (applyToAll) {
                    onAddSubjectToAll(name, total)
                } else {
                    onAddSubjectToStudent(targetStudent.id, name, total, obt)
                }
                subjectToAddForStudent = null
            }
        )
    }

    studentToEdit?.let { student ->
        EditStudentDialog(
            student = student,
            onDismiss = { studentToEdit = null },
            onConfirm = { updated ->
                onUpdateStudent(updated)
                studentToEdit = null
            }
        )
    }

    studentToDelete?.let { student ->
        ConfirmDeleteDialog(
            title = "Delete Student Record?",
            message = "Are you sure you want to delete ${student.studentName} (Roll No: ${student.rollNo})? This cannot be undone.",
            onDismiss = { studentToDelete = null },
            onConfirm = {
                onDeleteStudent(student.id)
                studentToDelete = null
            }
        )
    }

    subjectToEditData?.let { (studentId, subject) ->
        EditSubjectDialog(
            subject = subject,
            onDismiss = { subjectToEditData = null },
            onConfirm = { newName, newTotal, newObt ->
                onUpdateSubject(studentId, subject.id, newName, newTotal, newObt)
                subjectToEditData = null
            }
        )
    }
}

@Composable
fun StudentRowCard(
    student: StudentResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onViewResultCard: () -> Unit,
    onEditStudent: () -> Unit,
    onDeleteStudent: () -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (SubjectMarks) -> Unit,
    onDeleteSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPass = student.result.contains("PASS", ignoreCase = true)

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PaperWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .animateContentSize()
            .testTag("student_card_${student.rollNo}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Roll No, Name, Class, Grade Badge, Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Roll No Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SchoolNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.rollNo.ifBlank { "—" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Class
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.studentName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = student.className,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                // Grade Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isPass) PassGreenBg else FailRedBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${student.grade} (${String.format(Locale.US, "%.1f", student.percentage)}%)",
                        color = if (isPass) PassGreen else FailRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // View Result Card Button
                IconButton(
                    onClick = onViewResultCard,
                    modifier = Modifier.testTag("view_result_card_${student.rollNo}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview Result Card",
                        tint = SchoolNavy
                    )
                }

                // Edit Student Info
                IconButton(onClick = onEditStudent) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Student Info",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Student
                IconButton(onClick = onDeleteStudent) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Student",
                        tint = FailRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Summary Bar: Total, Obtained, Result
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCream)
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Total: ${if (student.totalMarks % 1.0 == 0.0) student.totalMarks.toInt() else student.totalMarks}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Obtained: ${if (student.obtainedMarks % 1.0 == 0.0) student.obtainedMarks.toInt() else student.obtainedMarks}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SchoolNavy
                    )
                    Text(
                        text = "Status: ${student.result}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPass) PassGreen else FailRed
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Hide Subjects" else "${student.subjects.size} Subjects",
                        fontSize = 12.sp,
                        color = SchoolNavyDark,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = SchoolNavyDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanded Subjects Table
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Subjects Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SchoolNavyDark.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subject", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.8f))
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        Text("Obt.", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        Text("Grade", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text("Actions", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                    }

                    student.subjects.forEachIndexed { sIdx, sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1.8f)
                            )
                            Text(
                                text = if (sub.totalMarks % 1.0 == 0.0) sub.totalMarks.toInt().toString() else sub.totalMarks.toString(),
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (sub.obtainedMarks % 1.0 == 0.0) sub.obtainedMarks.toInt().toString() else sub.obtainedMarks.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sub.isPassed) TextDark else FailRed,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = sub.grade,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sub.isPassed) SchoolNavy else FailRed,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                modifier = Modifier.weight(1.2f),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { onEditSubject(sub) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Marks",
                                        tint = SchoolNavy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSubject(sub.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Subject",
                                        tint = FailRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (sIdx < student.subjects.lastIndex) {
                            HorizontalDivider(color = BorderSlate.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add Subject to this student button
                    TextButton(
                        onClick = onAddSubject,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SchoolNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Subject to ${student.studentName}",
                            color = SchoolNavy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStudentState(
    onUploadExcel: () -> Unit,
    onAddStudent: () -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(SchoolNavy.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = SchoolNavy,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "No Student Data Yet",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SchoolNavy
                    )
                )

                Text(
                    text = "Upload an Excel sheet (.xlsx / .xls), add students manually, or load the Educare Chagmalai demo class.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onUploadExcel,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = SchoolGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Excel Sheet (.xlsx / .xls)")
                }

                OutlinedButton(
                    onClick = onAddStudent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = SchoolNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Student Manually", color = SchoolNavy)
                }

                TextButton(onClick = onLoadSample) {
                    Text("Load Sample School Class (5 Students)", color = SchoolGoldDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Saves a sample CSV template to cache and opens sharing intent
 */
private fun exportSampleTemplate(context: Context) {
    try {
        val sampleCsv = ExcelParser.generateSampleCsv()
        val file = File(context.cacheDir, "Educare_Chagmalai_Result_Template.csv")
        FileOutputStream(file).use { it.write(sampleCsv.toByteArray()) }

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Educare Result Card Excel Template")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Excel Template"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not export template: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
