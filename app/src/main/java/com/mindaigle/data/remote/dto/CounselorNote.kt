package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CounselorNote(
    @SerializedName("id")
    val id: Int,
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("counselorId")
    val counselorId: Int?,
    @SerializedName("counselorName")
    val counselorName: String?,
    @SerializedName("counselorEmail")
    val counselorEmail: String?,
    @SerializedName("counselorRole")
    val counselorRole: String?,
    @SerializedName("noteText")
    val noteText: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String?
)

data class CounselorNotesResponse(
    @SerializedName("notes")
    val notes: List<CounselorNote>
)
