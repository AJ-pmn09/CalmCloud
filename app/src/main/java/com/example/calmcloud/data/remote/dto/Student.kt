package com.example.calmcloud.data.remote.dto

import com.google.gson.annotations.SerializedName

data class Student(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("grade")
    val grade: Int?,
    @SerializedName("lastCheckin")
    val lastCheckin: LastCheckin? = null,
    @SerializedName("observationCount")
    val observationCount: Int = 0
)

data class LastCheckin(
    @SerializedName("emotion")
    val emotion: String? = null,
    @SerializedName("emotion_intensity")
    val emotionIntensity: Int? = null,
    @SerializedName("stress_level")
    val stressLevel: Int? = null
)

data class StudentsResponse(
    @SerializedName("students")
    val students: List<Student>
)

data class Child(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("grade")
    val grade: Int?,
    @SerializedName("recentEmotion")
    val recentEmotion: String? = null,
    @SerializedName("recentStress")
    val recentStress: Int? = null,
    @SerializedName("checkinCount")
    val checkinCount: Int = 0,
    @SerializedName("lastCheckinDate")
    val lastCheckinDate: String? = null
)

data class ChildrenResponse(
    @SerializedName("children")
    val children: List<Child>
)

data class SaveStudentDataRequest(
    @SerializedName("studentId")
    val studentId: Int? = null,
    @SerializedName("fhirData")
    val fhirData: StudentFHIRData
)

data class SaveStudentDataResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: StudentFHIRData
)

