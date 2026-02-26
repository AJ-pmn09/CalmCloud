package com.mindaigle.ui.screens.common

/**
 * Sample community resources for immediate use. In production, replace with
 * 211 API (https://apiportal.211.org/) or your backend.
 */
object CommunityResourcesData {

    fun getAllResources(): List<CommunityResource> = listOf(
        // Food pantries & meal programs
        CommunityResource(
            id = "fp1",
            name = "Community Food Bank",
            type = ResourceType.FOOD_PANTRY,
            address = "123 Main St",
            city = "Sample City",
            state = "CA",
            zip = "90001",
            latitude = 34.0522,
            longitude = -118.2437,
            phone = "555-0100",
            hours = "Mon–Fri 9am–4pm",
            description = "Emergency food pantry; bring ID."
        ),
        CommunityResource(
            id = "fp2",
            name = "School Pantry Program",
            type = ResourceType.FOOD_PANTRY,
            address = "456 Oak Ave",
            city = "Sample City",
            state = "CA",
            zip = "90002",
            latitude = 34.0622,
            longitude = -118.2537,
            phone = "555-0101",
            hours = "Tue/Thu 3–6pm",
            description = "Free groceries for families with students."
        ),
        // Shelters
        CommunityResource(
            id = "sh1",
            name = "Family Shelter & Services",
            type = ResourceType.SHELTER,
            address = "789 Shelter Way",
            city = "Sample City",
            state = "CA",
            zip = "90003",
            latitude = 34.0722,
            longitude = -118.2637,
            phone = "555-0200",
            hours = "24/7 intake",
            description = "Emergency shelter; call for availability."
        ),
        // Hospitals & clinics
        CommunityResource(
            id = "h1",
            name = "Community Health Center",
            type = ResourceType.HOSPITAL,
            address = "100 Health Blvd",
            city = "Sample City",
            state = "CA",
            zip = "90004",
            latitude = 34.0822,
            longitude = -118.2737,
            phone = "555-0300",
            hours = "Mon–Fri 8am–8pm",
            description = "Sliding-scale medical and behavioral health."
        ),
        CommunityResource(
            id = "h2",
            name = "County General Hospital",
            type = ResourceType.HOSPITAL,
            address = "200 Hospital Dr",
            city = "Sample City",
            state = "CA",
            zip = "90005",
            latitude = 34.0922,
            longitude = -118.2837,
            phone = "555-0301",
            hours = "24/7 ER",
            description = "Emergency room and inpatient care."
        ),
        // Mental health
        CommunityResource(
            id = "mh1",
            name = "Youth Counseling Center",
            type = ResourceType.MENTAL_HEALTH,
            address = "300 Care Lane",
            city = "Sample City",
            state = "CA",
            zip = "90006",
            latitude = 34.1022,
            longitude = -118.2937,
            phone = "555-0400",
            hours = "Mon–Fri 9am–5pm",
            description = "Free/sliding-scale counseling for youth."
        ),
        // Crisis & hotlines
        CommunityResource(
            id = "cr1",
            name = "988 Suicide & Crisis Lifeline",
            type = ResourceType.CRISIS,
            address = "Call or text 988",
            city = "",
            state = "",
            zip = "",
            latitude = 0.0,
            longitude = 0.0,
            phone = "988",
            hours = "24/7",
            description = "Free, confidential support for people in distress."
        ),
        CommunityResource(
            id = "cr2",
            name = "Crisis Text Line",
            type = ResourceType.CRISIS,
            address = "Text HOME to 741741",
            city = "",
            state = "",
            zip = "",
            latitude = 0.0,
            longitude = 0.0,
            phone = "741741",
            hours = "24/7",
            description = "Free crisis support via text."
        ),
        CommunityResource(
            id = "cr3",
            name = "National Domestic Violence Hotline",
            type = ResourceType.CRISIS,
            address = "1-800-799-7233",
            city = "",
            state = "",
            zip = "",
            latitude = 0.0,
            longitude = 0.0,
            phone = "1-800-799-7233",
            hours = "24/7",
            description = "Support for domestic violence."
        )
    )

    fun getResourcesByType(type: ResourceType): List<CommunityResource> =
        getAllResources().filter { it.type == type }
}
