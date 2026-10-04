package com.example.data.model

import java.util.UUID

data class SubjectMarks(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val totalMarks: Double,
    val obtainedMarks: Double
) {
    val percentage: Double
        get() = if (totalMarks > 0) (obtainedMarks / totalMarks) * 100.0 else 0.0

    val isPassed: Boolean
        get() = obtainedMarks >= (totalMarks * 0.33)

    val grade: String
        get() = when {
            percentage >= 80.0 -> "A+"
            percentage >= 70.0 -> "A"
            percentage >= 60.0 -> "B"
            percentage >= 50.0 -> "C"
            percentage >= 40.0 -> "D"
            percentage >= 33.0 -> "E"
            else -> "F"
        }
}

data class StudentResult(
    val id: String = UUID.randomUUID().toString(),
    val rollNo: String = "",
    val studentName: String,
    val className: String,
    val subjects: List<SubjectMarks> = emptyList(),
    val remarks: String = ""
) {
    val totalMarks: Double
        get() = subjects.sumOf { it.totalMarks }

    val obtainedMarks: Double
        get() = subjects.sumOf { it.obtainedMarks }

    val percentage: Double
        get() = if (totalMarks > 0) (obtainedMarks / totalMarks) * 100.0 else 0.0

    val grade: String
        get() = when {
            percentage >= 80.0 -> "A+"
            percentage >= 70.0 -> "A"
            percentage >= 60.0 -> "B"
            percentage >= 50.0 -> "C"
            percentage >= 40.0 -> "D"
            percentage >= 33.0 -> "E"
            else -> "F"
        }

    val result: String
        get() = if (percentage >= 33.0 && subjects.all { it.isPassed }) {
            "PASS"
        } else if (percentage >= 33.0) {
            "PASS"
        } else {
            "FAIL"
        }

    val defaultRemarks: String
        get() = if (remarks.isNotBlank()) {
            remarks
        } else {
            when {
                percentage >= 85.0 -> "Outstanding academic performance. Keep it up!"
                percentage >= 75.0 -> "Excellent achievement. Commendable hard work."
                percentage >= 65.0 -> "Very good result. Shows consistent progress."
                percentage >= 50.0 -> "Good effort. Regular study will improve grades."
                percentage >= 33.0 -> "Satisfactory. Needs focused attention in weak subjects."
                else -> "Needs serious attention and hard work."
            }
        }
}
