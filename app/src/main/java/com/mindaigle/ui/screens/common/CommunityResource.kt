package com.mindaigle.ui.screens.common

/**
 * Community resource types for mapping (food pantries, shelters, hospitals, crisis lines).
 */
enum class ResourceType(val displayName: String, val iconLabel: String) {
    FOOD_PANTRY("Food pantries & meal programs", "Food"),
    SHELTER("Shelters & housing", "Shelter"),
    HOSPITAL("Hospitals & clinics", "Health"),
    MENTAL_HEALTH("Mental health & counseling", "Mental health"),
    CRISIS("Crisis & hotlines", "Crisis"),
    OTHER("Other resources", "Other")
}

data class CommunityResource(
    val id: String,
    val name: String,
    val type: ResourceType,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val hours: String?,
    val description: String?
) {
    val fullAddress: String get() = "$address, $city, $state $zip"
    val geoUri: String get() = "geo:$latitude,$longitude?q=$latitude,$longitude(${name.replace(" ", "+")})"
}
