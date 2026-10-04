package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EducareTopAppBar
import com.example.ui.components.EditSessionDialog
import com.example.ui.screens.ResultCardPreviewScreen
import com.example.ui.screens.StudentDataTableScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SchoolNavy
import com.example.ui.viewmodel.ResultCardViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    private val viewModel: ResultCardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EducareResultCardApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun EducareResultCardApp(viewModel: ResultCardViewModel) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val filteredStudents by viewModel.filteredStudents.collectAsStateWithLifecycle()
    val currentStudent by viewModel.currentStudent.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentStudentIndex.collectAsStateWithLifecycle()
    val sessionName by viewModel.sessionName.collectAsStateWithLifecycle()
    val issueDate by viewModel.issueDate.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterClass by viewModel.filterClass.collectAsStateWithLifecycle()
    val availableClasses by viewModel.availableClasses.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditSessionDialog by remember { mutableStateOf(false) }

    // Excel / Spreadsheet file picker launcher
    val excelFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importExcelFile(uri)
        }
    }

    // Show status messages in Snackbar
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Handle back button: if in Preview tab, return to Table tab
    BackHandler(enabled = activeTab == ScreenTab.PREVIEW) {
        viewModel.setActiveTab(ScreenTab.TABLE)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EducareTopAppBar(
                activeTab = activeTab,
                onTabSelected = { viewModel.setActiveTab(it) },
                onUploadExcelClicked = {
                    excelFilePickerLauncher.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel",
                            "text/comma-separated-values",
                            "text/csv",
                            "text/plain",
                            "*/*"
                        )
                    )
                },
                onEditSessionClicked = { showEditSessionDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                ScreenTab.TABLE -> {
                    StudentDataTableScreen(
                        students = students,
                        filteredStudents = filteredStudents,
                        searchQuery = searchQuery,
                        filterClass = filterClass,
                        availableClasses = availableClasses,
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onFilterClassSelected = { viewModel.setFilterClass(it) },
                        onUploadExcelClicked = {
                            excelFilePickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/comma-separated-values",
                                    "text/csv",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        onLoadSampleClicked = { viewModel.loadSampleData() },
                        onAddStudent = { name, cls, roll, copy ->
                            viewModel.addStudent(name, cls, roll, copy)
                        },
                        onUpdateStudent = { viewModel.updateStudent(it) },
                        onDeleteStudent = { viewModel.deleteStudent(it) },
                        onAddSubjectToStudent = { sId, name, total, obt ->
                            viewModel.addSubjectToStudent(sId, name, total, obt)
                        },
                        onAddSubjectToAll = { name, total ->
                            viewModel.addSubjectToAll(name, total)
                        },
                        onDeleteSubject = { sId, subId ->
                            viewModel.deleteSubject(sId, subId)
                        },
                        onUpdateSubject = { sId, subId, name, total, obt ->
                            viewModel.updateSubject(sId, subId, name, total, obt)
                        },
                        onViewResultCard = { studentId ->
                            viewModel.selectStudentById(studentId)
                        }
                    )
                }

                ScreenTab.PREVIEW -> {
                    ResultCardPreviewScreen(
                        students = students,
                        currentStudent = currentStudent,
                        currentIndex = currentIndex,
                        sessionName = sessionName,
                        issueDate = issueDate,
                        onSelectStudent = { viewModel.selectStudent(it) },
                        onNextStudent = { viewModel.nextStudent() },
                        onPreviousStudent = { viewModel.previousStudent() },
                        onUpdateStudent = { viewModel.updateStudent(it) },
                        onGoToTable = { viewModel.setActiveTab(ScreenTab.TABLE) }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SchoolNavy)
                }
            }
        }
    }

    if (showEditSessionDialog) {
        EditSessionDialog(
            currentSession = sessionName,
            currentDate = issueDate,
            onDismiss = { showEditSessionDialog = false },
            onConfirm = { newSession, newDate ->
                viewModel.setSessionName(newSession)
                viewModel.setIssueDate(newDate)
                showEditSessionDialog = false
            }
        )
    }
}
