package com.mindaigle.data.remote.dto

import com.google.gson.annotations.SerializedName

data class Appointment(
    @SerializedName("id")
    val id: Int,
    @SerializedName("studentId")
    val studentId: Int,
    @SerializedName("staffId")
    val staffId: Int?,
    @SerializedName("staffName")
    val staffName: String?,
    @SerializedName("staffEmail")
    val staffEmail: String?,
    @SerializedName("staffRole")
    val staffRole: String?,
    @SerializedName("appointmentDate")
    val appointmentDate: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("type")
    val type: String,
    @SerializedName("notes")
    val notes: String?,
    @SerializedName("status")
    val status: String,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("updatedAt")
    val updatedAt: String?
)

data class AppointmentsResponse(
    @SerializedName("appointments")
    val appointments: List<Appointment>
)

data class CreateAppointmentRequest(
    @SerializedName("staffId")
    val staffId: Int? = null,
    @SerializedName("appointmentDate")
    val appointmentDate: String,
    @SerializedName("duration")
    val duration: Int = 30,
    @SerializedName("type")
    val type: String = "general",
    @SerializedName("notes")
    val notes: String? = null
)

data class CreateAppointmentResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("appointment")
    val appointment: Appointment
)

data class UpdateAppointmentRequest(
    @SerializedName("appointmentDate")
    val appointmentDate: String? = null,
    @SerializedName("duration")
    val duration: Int? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("notes")
    val notes: String? = null
)

data class StaffMember(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("role")
    val role: String
)

data class StaffAvailabilityResponse(
    @SerializedName("staff")
    val staff: List<StaffMember>
)
