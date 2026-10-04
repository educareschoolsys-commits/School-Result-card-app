package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.excel.ExcelParser
import com.example.data.model.StudentResult
import com.example.data.model.SubjectMarks
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScreenTab {
    TABLE,
    PREVIEW
}

class ResultCardViewModel(application: Application) : AndroidViewModel(application) {

    private val _students = MutableStateFlow<List<StudentResult>>(emptyList())
    val students: StateFlow<List<StudentResult>> = _students.asStateFlow()

    private val _currentStudentIndex = MutableStateFlow(0)
    val currentStudentIndex: StateFlow<Int> = _currentStudentIndex.asStateFlow()

    private val _sessionName = MutableStateFlow("Annual Examination 2026")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    private val _issueDate = MutableStateFlow(
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
    )
    val issueDate: StateFlow<String> = _issueDate.asStateFlow()

    private val _activeTab = MutableStateFlow(ScreenTab.TABLE)
    val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterClass = MutableStateFlow("All")
    val filterClass: StateFlow<String> = _filterClass.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Filtered students for table list
    val filteredStudents: StateFlow<List<StudentResult>> = combine(
        _students,
        _searchQuery,
        _filterClass
    ) { list, query, selectedClass ->
        list.filter { student ->
            val matchesQuery = query.isBlank() ||
                student.studentName.contains(query, ignoreCase = true) ||
                student.rollNo.contains(query, ignoreCase = true) ||
                student.className.contains(query, ignoreCase = true)

            val matchesClass = selectedClass == "All" || student.className.equals(selectedClass, ignoreCase = true)
            matchesQuery && matchesClass
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Unique classes for filter chips
    val availableClasses: StateFlow<List<String>> = _students.combine(_students) { list, _ ->
        val classes = list.map { it.className.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All") + classes
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("All"))

    // Active student currently shown in Preview
    val currentStudent: StateFlow<StudentResult?> = combine(
        _students,
        _currentStudentIndex
    ) { list, idx ->
        if (list.isNotEmpty() && idx in list.indices) {
            list[idx]
        } else if (list.isNotEmpty()) {
            list.first()
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val saveFile = File(application.filesDir, "educare_students_cache.json")

    init {
        loadPersistedData()
    }

    private fun loadPersistedData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (saveFile.exists()) {
                    val json = saveFile.readText()
                    val type = Types.newParameterizedType(List::class.java, StudentResult::class.java)
                    val adapter = moshi.adapter<List<StudentResult>>(type)
                    val loaded = adapter.fromJson(json)
                    if (!loaded.isNullOrEmpty()) {
                        _students.value = loaded
                        return@launch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // If no stored data, load sample school data
            _students.value = ExcelParser.getSampleStudents()
        }
    }

    private fun persistData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val type = Types.newParameterizedType(List::class.java, StudentResult::class.java)
                val adapter = moshi.adapter<List<StudentResult>>(type)
                val json = adapter.toJson(_students.value)
                saveFile.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importExcelFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                ExcelParser.parseSpreadsheet(getApplication(), uri)
            }
            _isLoading.value = false
            _statusMessage.value = result.message

            if (result.isSuccess && result.students.isNotEmpty()) {
                _students.value = result.students
                _currentStudentIndex.value = 0
                _activeTab.value = ScreenTab.TABLE
                persistData()
            }
        }
    }

    fun loadSampleData() {
        _students.value = ExcelParser.getSampleStudents()
        _currentStudentIndex.value = 0
        _statusMessage.value = "Loaded 5 sample students from Educare School System Chagmalai"
        persistData()
    }

    fun clearAllStudents() {
        _students.value = emptyList()
        _currentStudentIndex.value = 0
        _statusMessage.value = "All students cleared"
        persistData()
    }

    fun addStudent(
        name: String,
        className: String,
        rollNo: String,
        copySubjects: Boolean = true
    ) {
        val currentList = _students.value
        val defaultSubjects = if (copySubjects && currentList.isNotEmpty()) {
            // Copy subject names & total marks from the first student in class
            val templateStudent = currentList.firstOrNull { it.className.equals(className, ignoreCase = true) } ?: currentList.first()
            templateStudent.subjects.map {
                SubjectMarks(name = it.name, totalMarks = it.totalMarks, obtainedMarks = 0.0)
            }
        } else {
            listOf(
                SubjectMarks(name = "English", totalMarks = 100.0, obtainedMarks = 0.0),
                SubjectMarks(name = "Urdu", totalMarks = 100.0, obtainedMarks = 0.0),
                SubjectMarks(name = "Mathematics", totalMarks = 100.0, obtainedMarks = 0.0),
                SubjectMarks(name = "Science", totalMarks = 100.0, obtainedMarks = 0.0),
                SubjectMarks(name = "Islamiyat", totalMarks = 50.0, obtainedMarks = 0.0)
            )
        }

        val newStudent = StudentResult(
            rollNo = rollNo.ifBlank { "${currentList.size + 1}" },
            studentName = name,
            className = className.ifBlank { "Class 10" },
            subjects = defaultSubjects
        )

        _students.value = currentList + newStudent
        _currentStudentIndex.value = _students.value.lastIndex
        _statusMessage.value = "Student '$name' added successfully"
        persistData()
    }

    fun updateStudent(updated: StudentResult) {
        _students.value = _students.value.map {
            if (it.id == updated.id) updated else it
        }
        persistData()
    }

    fun deleteStudent(studentId: String) {
        val currentList = _students.value
        val student = currentList.find { it.id == studentId }
        val updated = currentList.filter { it.id != studentId }
        _students.value = updated

        if (_currentStudentIndex.value >= updated.size) {
            _currentStudentIndex.value = (updated.size - 1).coerceAtLeast(0)
        }
        _statusMessage.value = "Deleted student ${student?.studentName ?: ""}"
        persistData()
    }

    fun addSubjectToStudent(
        studentId: String,
        subjectName: String,
        totalMarks: Double,
        obtainedMarks: Double = 0.0
    ) {
        _students.value = _students.value.map { student ->
            if (student.id == studentId) {
                val newSubject = SubjectMarks(
                    name = subjectName.trim(),
                    totalMarks = totalMarks,
                    obtainedMarks = obtainedMarks
                )
                student.copy(subjects = student.subjects + newSubject)
            } else {
                student
            }
        }
        _statusMessage.value = "Added subject '$subjectName'"
        persistData()
    }

    fun addSubjectToAll(subjectName: String, totalMarks: Double) {
        val cleanName = subjectName.trim()
        if (cleanName.isBlank()) return

        _students.value = _students.value.map { student ->
            val exists = student.subjects.any { it.name.equals(cleanName, ignoreCase = true) }
            if (!exists) {
                student.copy(
                    subjects = student.subjects + SubjectMarks(
                        name = cleanName,
                        totalMarks = totalMarks,
                        obtainedMarks = 0.0
                    )
                )
            } else {
                student
            }
        }
        _statusMessage.value = "Added '$cleanName' ($totalMarks marks) to all students"
        persistData()
    }

    fun deleteSubject(studentId: String, subjectId: String) {
        _students.value = _students.value.map { student ->
            if (student.id == studentId) {
                student.copy(subjects = student.subjects.filter { it.id != subjectId })
            } else {
                student
            }
        }
        _statusMessage.value = "Subject removed"
        persistData()
    }

    fun deleteSubjectFromAll(subjectName: String) {
        _students.value = _students.value.map { student ->
            student.copy(subjects = student.subjects.filter { !it.name.equals(subjectName, ignoreCase = true) })
        }
        _statusMessage.value = "Removed '$subjectName' from all students"
        persistData()
    }

    fun updateSubject(
        studentId: String,
        subjectId: String,
        newName: String,
        newTotal: Double,
        newObtained: Double
    ) {
        _students.value = _students.value.map { student ->
            if (student.id == studentId) {
                student.copy(
                    subjects = student.subjects.map { sub ->
                        if (sub.id == subjectId) {
                            sub.copy(
                                name = newName.trim().ifBlank { sub.name },
                                totalMarks = newTotal.coerceAtLeast(1.0),
                                obtainedMarks = newObtained.coerceIn(0.0, newTotal)
                            )
                        } else {
                            sub
                        }
                    }
                )
            } else {
                student
            }
        }
        persistData()
    }

    fun selectStudent(index: Int) {
        if (index in _students.value.indices) {
            _currentStudentIndex.value = index
        }
    }

    fun selectStudentById(id: String) {
        val idx = _students.value.indexOfFirst { it.id == id }
        if (idx != -1) {
            _currentStudentIndex.value = idx
            _activeTab.value = ScreenTab.PREVIEW
        }
    }

    fun nextStudent() {
        val max = _students.value.size
        if (max > 0) {
            _currentStudentIndex.value = (_currentStudentIndex.value + 1) % max
        }
    }

    fun previousStudent() {
        val max = _students.value.size
        if (max > 0) {
            _currentStudentIndex.value = (_currentStudentIndex.value - 1 + max) % max
        }
    }

    fun setSessionName(name: String) {
        _sessionName.value = name
    }

    fun setIssueDate(date: String) {
        _issueDate.value = date
    }

    fun setActiveTab(tab: ScreenTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterClass(className: String) {
        _filterClass.value = className
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
