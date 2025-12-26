package com.example.calmcloud.data.remote.dto

import com.google.gson.annotations.SerializedName

// FHIR Observation DTO
data class FHIRObservation(
    @SerializedName("id")
    val id: String,
    @SerializedName("resourceType")
    val resourceType: String = "Observation",
    @SerializedName("status")
    val status: String = "final",
    @SerializedName("code")
    val code: FHIRCode,
    @SerializedName("valueQuantity")
    val valueQuantity: FHIRValueQuantity? = null,
    @SerializedName("valueString")
    val valueString: String? = null,
    @SerializedName("effectiveDateTime")
    val effectiveDateTime: String,
    @SerializedName("subject")
    val subject: FHIRSubject
)

data class FHIRCode(
    @SerializedName("coding")
    val coding: List<FHIRCoding>
)

data class FHIRCoding(
    @SerializedName("system")
    val system: String = "http://loinc.org",
    @SerializedName("code")
    val code: String,
    @SerializedName("display")
    val display: String
)

data class FHIRValueQuantity(
    @SerializedName("value")
    val value: Double,
    @SerializedName("unit")
    val unit: String? = null
)

data class FHIRSubject(
    @SerializedName("reference")
    val reference: String
)

data class StudentFHIRData(
    @SerializedName("observations")
    val observations: List<FHIRObservation> = emptyList()
)

data class StudentDataResponse(
    @SerializedName("student")
    val student: StudentInfo,
    @SerializedName("fhirData")
    val fhirData: StudentFHIRData,
    @SerializedName("isParentView")
    val isParentView: Boolean = false
)

data class StudentInfo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String
)

