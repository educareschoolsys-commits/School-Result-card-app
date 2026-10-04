package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentResult
import com.example.data.model.SubjectMarks
import com.example.ui.theme.PassGreen
import com.example.ui.theme.SchoolGold
import com.example.ui.theme.SchoolGoldDark
import com.example.ui.theme.SchoolNavy
import java.io.File

@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, className: String, rollNo: String, copySubjects: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("10th - Grade A") }
    var rollNo by remember { mutableStateOf("") }
    var copySubjects by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Student",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SchoolNavy
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        hasError = false
                    },
                    label = { Text("Student Name *") },
                    placeholder = { Text("e.g. Muhammad Bilal") },
                    isError = hasError && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_name")
                )

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class / Grade") },
                    placeholder = { Text("e.g. 10th - Grade A") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_class")
                )

                OutlinedTextField(
                    value = rollNo,
                    onValueChange = { rollNo = it },
                    label = { Text("Roll Number") },
                    placeholder = { Text("e.g. 106") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_roll")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = copySubjects,
                        onCheckedChange = { copySubjects = it },
                        colors = CheckboxDefaults.colors(checkedColor = SchoolNavy)
                    )
                    Text(
                        text = "Auto-add standard class subjects",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), className.trim(), rollNo.trim(), copySubjects)
                    } else {
                        hasError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy),
                modifier = Modifier.testTag("dialog_confirm_add_student")
            ) {
                Text("Add Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SchoolNavy)
            }
        }
    )
}

@Composable
fun EditStudentDialog(
    student: StudentResult,
    onDismiss: () -> Unit,
    onConfirm: (StudentResult) -> Unit
) {
    var name by remember { mutableStateOf(student.studentName) }
    var className by remember { mutableStateOf(student.className) }
    var rollNo by remember { mutableStateOf(student.rollNo) }
    var remarks by remember { mutableStateOf(student.remarks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Student Information",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SchoolNavy
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rollNo,
                    onValueChange = { rollNo = it },
                    label = { Text("Roll Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Teacher Remarks (Optional)") },
                    placeholder = { Text("Leave blank for auto-generated remarks") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        student.copy(
                            studentName = name.trim().ifBlank { student.studentName },
                            className = className.trim().ifBlank { student.className },
                            rollNo = rollNo.trim(),
                            remarks = remarks.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSubjectDialog(
    studentName: String?,
    onDismiss: () -> Unit,
    onConfirm: (subjectName: String, totalMarks: Double, obtainedMarks: Double, applyToAll: Boolean) -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    var totalMarksStr by remember { mutableStateOf("100") }
    var obtainedMarksStr by remember { mutableStateOf("0") }
    var applyToAll by remember { mutableStateOf(studentName == null) }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (studentName != null) "Add Subject for $studentName" else "Add Subject to Class",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SchoolNavy
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = {
                        subjectName = it
                        hasError = false
                    },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Computer Science, Physics") },
                    isError = hasError && subjectName.isBlank(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_subject_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalMarksStr,
                        onValueChange = { totalMarksStr = it },
                        label = { Text("Total Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_subject_total_marks")
                    )

                    if (studentName != null && !applyToAll) {
                        OutlinedTextField(
                            value = obtainedMarksStr,
                            onValueChange = { obtainedMarksStr = it },
                            label = { Text("Obtained Marks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_subject_obtained_marks")
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it },
                        colors = CheckboxDefaults.colors(checkedColor = SchoolNavy)
                    )
                    Text(
                        text = "Add to all students in the class",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.isNotBlank()) {
                        val total = totalMarksStr.toDoubleOrNull() ?: 100.0
                        val obt = obtainedMarksStr.toDoubleOrNull() ?: 0.0
                        onConfirm(subjectName.trim(), total, obt, applyToAll)
                    } else {
                        hasError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy),
                modifier = Modifier.testTag("dialog_confirm_add_subject")
            ) {
                Text("Add Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditSubjectDialog(
    subject: SubjectMarks,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newTotal: Double, newObtained: Double) -> Unit
) {
    var subjectName by remember { mutableStateOf(subject.name) }
    var totalMarksStr by remember { mutableStateOf(if (subject.totalMarks % 1.0 == 0.0) subject.totalMarks.toInt().toString() else subject.totalMarks.toString()) }
    var obtainedMarksStr by remember { mutableStateOf(if (subject.obtainedMarks % 1.0 == 0.0) subject.obtainedMarks.toInt().toString() else subject.obtainedMarks.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Subject Marks",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SchoolNavy
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalMarksStr,
                        onValueChange = { totalMarksStr = it },
                        label = { Text("Total Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = obtainedMarksStr,
                        onValueChange = { obtainedMarksStr = it },
                        label = { Text("Obtained Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Show calculated preview percentage & grade
                val total = totalMarksStr.toDoubleOrNull() ?: 1.0
                val obt = obtainedMarksStr.toDoubleOrNull() ?: 0.0
                val perc = if (total > 0) (obt / total) * 100.0 else 0.0
                val grade = when {
                    perc >= 80.0 -> "A+"
                    perc >= 70.0 -> "A"
                    perc >= 60.0 -> "B"
                    perc >= 50.0 -> "C"
                    perc >= 40.0 -> "D"
                    perc >= 33.0 -> "E"
                    else -> "F"
                }

                Text(
                    text = "Live Calculation: ${String.format("%.1f", perc)}% • Grade: $grade",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (perc >= 33.0) PassGreen else Color.Red
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = totalMarksStr.toDoubleOrNull() ?: subject.totalMarks
                    val obt = obtainedMarksStr.toDoubleOrNull() ?: subject.obtainedMarks
                    onConfirm(subjectName.trim(), total, obt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy)
            ) {
                Text("Update Marks")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditSessionDialog(
    currentSession: String,
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (session: String, date: String) -> Unit
) {
    var session by remember { mutableStateOf(currentSession) }
    var date by remember { mutableStateOf(currentDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Exam Session & Issue Date",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SchoolNavy
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = session,
                    onValueChange = { session = it },
                    label = { Text("Examination Session") },
                    placeholder = { Text("e.g. Annual Examination 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date of Issue") },
                    placeholder = { Text("e.g. 05 October 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(session.trim(), date.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PdfSuccessDialog(
    file: File,
    title: String,
    onDismiss: () -> Unit,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PassGreen,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "PDF Generated Successfully!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SchoolNavy
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "A4 print-ready PDF saved at: ${file.name}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onShare(file) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }

                Button(
                    onClick = { onOpen(file) },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolNavy)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
