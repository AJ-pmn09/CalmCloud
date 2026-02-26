package com.mindaigle.data.remote

import com.mindaigle.data.remote.dto.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Mock data for test profiles (use app without server).
 * One week of dynamic data (last 7 days from today) so check-ins, moods, stress,
 * messages, appointments, PHQ-9/GAD-7 all display correctly.
 */
object TestMockData {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    /** Last 7 days (today first, then older). Dynamic: always relative to current date. */
    fun last7Days(): List<String> {
        val cal = Calendar.getInstance()
        return (0 until 7).map {
            val d = dateFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            d
        }.reversed() // oldest first: [today-6, ..., today]
    }

    fun today(): String = dateFormat.format(Calendar.getInstance().time)

    fun nowIso(): String = dateTimeFormat.format(Calendar.getInstance().time)

    // --- Student / check-ins / trends ---

    fun mockStudentDataResponse(email: String, name: String): StudentDataResponse {
        val dates = last7Days()
        val fhir = StudentFHIRData(observations = emptyList())
        val studentInfo = StudentInfo(id = 0, name = name, firstName = name, lastName = "", email = email, profilePictureUrl = null)
        val latestDate = dates.last()
        val latestCheckin = LatestCheckin(
            stressLevel = 3,
            moodRating = 4,
            mood = 4,
            date = latestDate,
            stressSource = "Schoolwork",
            additionalNotes = "Feeling okay today."
        )
        val activityData = ActivityData(
            heartRate = 72,
            steps = 6500,
            sleepHours = 7.5,
            hydrationPercent = 75,
            nutritionPercent = 80,
            mood = "good",
            stressLevel = 3
        )
        return StudentDataResponse(
            success = true,
            student = studentInfo,
            fhirData = fhir,
            latestCheckin = latestCheckin,
            activityData = activityData,
            isParentView = false
        )
    }

    fun mockTrendsDataResponse(): TrendsDataResponse {
        val dates = last7Days()
        val checkins = dates.mapIndexed { i, date ->
            TrendCheckin(
                date = date,
                stressLevel = listOf(4, 3, 5, 2, 4, 3, 3)[i.coerceAtMost(6)],
                moodRating = listOf(4, 5, 3, 5, 4, 4, 4)[i.coerceAtMost(6)],
                mood = listOf(4, 5, 3, 5, 4, 4, 4)[i.coerceAtMost(6)],
                stressSource = listOf("Schoolwork", "Sleep", "Peers", null, "Schoolwork", null, null)[i.coerceAtMost(6)],
                additionalNotes = if (i % 2 == 0) "Check-in completed." else null
            )
        }
        val activities = dates.mapIndexed { i, date ->
            TrendActivity(
                date = date,
                heartRate = 68 + (i % 5),
                steps = 5000 + (i * 500),
                sleepHours = 7.0 + (i % 3) * 0.5,
                hydrationPercent = 70 + (i % 20),
                nutritionPercent = 75 + (i % 15),
                mood = listOf("good", "okay", "great", "good", "okay", "good", "great")[i.coerceAtMost(6)],
                stressLevel = listOf(3, 4, 2, 3, 4, 3, 3)[i.coerceAtMost(6)]
            )
        }
        return TrendsDataResponse(success = true, days = 7, checkins = checkins, activities = activities)
    }

    fun mockStudentsList(): List<Student> {
        val dates = last7Days()
        return listOf(
            Student(1, "Alex Rivera", "alex.rivera@test.mindaigle", 9, null, LastCheckin("calm", 3, 2), 7),
            Student(2, "Jordan Lee", "jordan.lee@test.mindaigle", 10, null, LastCheckin("happy", 4, 1), 5),
            Student(3, "Sam Taylor", "sam.taylor@test.mindaigle", 9, null, LastCheckin("anxious", 4, 4), 4)
        )
    }

    fun mockChildrenList(): List<Child> {
        val dates = last7Days()
        return listOf(
            Child(1, "Alex Rivera", "alex.rivera@test.mindaigle", 9, null, "calm", 2, 7, dates.last()),
            Child(2, "Jordan Lee", "jordan.lee@test.mindaigle", 10, null, "happy", 1, 5, dates.last())
        )
    }

    // --- Messages / communications ---

    fun mockCommunicationsResponse(): CommunicationsResponse {
        val dates = last7Days()
        val baseTime = "T10:00:00"
        val comms = listOf(
            Communication(1, 101, "Counselor Smith", "counselor@school.edu", "associate", "student", "Wellness check", "Hope you're doing well this week. Reach out if you need to talk.", "normal", true, false, "delivered", null, dates[4] + baseTime),
            Communication(2, 102, "Nurse Jones", "nurse@school.edu", "associate", "student", "Reminder", "Don't forget to log your mood when you can.", "normal", true, false, "read", dates[3] + baseTime, dates[2] + baseTime),
            Communication(3, 103, "MindAIgle Team", "support@mindaigle.com", "associate", "student", "Welcome", "You're all set. Use check-ins to track how you feel.", "low", true, false, "delivered", null, dates[0] + baseTime)
        )
        return CommunicationsResponse(communications = comms)
    }

    // --- Appointments ---

    fun mockStaffList(): List<StaffMember> = listOf(
        StaffMember(10, "Counselor Smith", "counselor@school.edu", "counselor"),
        StaffMember(11, "Nurse Jones", "nurse@school.edu", "nurse")
    )

    fun mockSentMessagesResponse(): Pair<List<SentMessage>, Int> {
        val dates = last7Days()
        val comms = listOf(
            SentMessage(1, "student", 1, "Alex Rivera", null, "Wellness check-in", "Hope you're doing well. Reach out if you need to talk.", "normal", true, false, "delivered", dates[2] + "T10:00:00"),
            SentMessage(2, "student", 2, "Jordan Lee", null, "Reminder", "Don't forget to log your mood today.", "normal", true, false, "sent", dates[1] + "T09:00:00")
        )
        return Pair(comms, 2)
    }

    fun mockAppointmentsResponse(): AppointmentsResponse {
        val dates = last7Days()
        val appointments = listOf(
            Appointment(1, 1, 10, "Counselor Smith", "counselor@school.edu", "counselor", dates.last() + "T14:00:00", 30, "check-in", "Weekly wellness check-in.", "scheduled", nowIso(), null),
            Appointment(2, 1, 11, "Nurse Jones", "nurse@school.edu", "nurse", dates[5] + "T09:30:00", 15, "follow-up", null, "completed", nowIso(), null)
        )
        return AppointmentsResponse(appointments = appointments)
    }

    // --- Activity logs (for achievements) ---

    fun mockActivityLogsResponse(): ActivityLogsResponse {
        val dates = last7Days()
        val logs = dates.mapIndexed { i, date ->
            ActivityLog(
                date = date,
                steps = 4000 + (i * 800),
                sleepHours = 6.5 + (i % 3) * 0.5,
                hydrationPercent = 65 + (i % 25),
                nutritionPercent = 70 + (i % 20)
            )
        }
        val meta = ActivityLogsMeta(requestedDays = 30, returnedCount = 7, totalInDb = 7, dateRange = DateRange(dates.first(), dates.last()))
        return ActivityLogsResponse(logs = logs, meta = meta)
    }

    // --- Counselor notes ---

    fun mockCounselorNotesResponse(): CounselorNotesResponse {
        val dates = last7Days()
        val notes = listOf(
            CounselorNote(1, 1, 10, "Counselor Smith", "counselor@school.edu", "counselor", "Student engaged well in check-in. Will follow up next week.", dates[3] + "T11:00:00", null),
            CounselorNote(2, 1, 10, "Counselor Smith", "counselor@school.edu", "counselor", "Positive progress on stress management strategies.", dates[0] + "T14:30:00", null)
        )
        return CounselorNotesResponse(notes = notes)
    }

    // --- Analytics (stress/activity trends for charts) ---

    fun mockAnalyticsTrendsResponse(): AnalyticsTrendsResponse {
        val dates = last7Days()
        val stressTrends = listOf(3f, 4f, 2f, 3f, 4f, 3f, 3f)
        val activityTrends = listOf(0.7f, 0.8f, 0.6f, 0.75f, 0.85f, 0.7f, 0.8f)
        val meta = AnalyticsTrendsMeta(stressDataPoints = 7, activityDataPoints = 7)
        val studentTrends = listOf(
            StudentTrendSeries(1, "Alex Rivera", listOf(4f, 3f, 5f, 2f, 4f, 3f, 3f), listOf(1f, 1f, 0f, 1f, 1f, 1f, 1f)),
            StudentTrendSeries(2, "Jordan Lee", listOf(3f, 4f, 2f, 4f, 3f, 4f, 3f), listOf(1f, 0f, 1f, 1f, 0f, 1f, 1f)),
            StudentTrendSeries(3, "Sam Taylor", listOf(2f, 3f, 4f, 3f, 2f, 3f, 4f), listOf(0f, 1f, 1f, 0f, 1f, 1f, 1f))
        )
        val peerComparison = studentTrends.map { s ->
            PeerComparisonItem(
                studentId = s.studentId,
                studentName = s.studentName,
                lastStress = s.stressByDay.lastOrNull()?.toInt(),
                lastMood = 4,
                checkinCount = s.activityByDay.sum().toInt(),
                latestStressAny = s.stressByDay.maxOrNull()?.toInt(),
                latestMoodAny = 4,
                latestCheckinDate = dates.lastOrNull()
            )
        }
        return AnalyticsTrendsResponse(
            success = true,
            days = 7,
            stressTrends = stressTrends,
            activityTrends = activityTrends,
            dates = dates,
            meta = meta,
            startDate = dates.first(),
            endDate = dates.last(),
            peerComparison = peerComparison,
            studentTrends = studentTrends
        )
    }

    fun mockStudentRequestsResponse(): StudentRequestsResponse = StudentRequestsResponse(requests = emptyList())

    // --- Reminders ---

    fun mockReminderConfigResponse(studentId: Int = 0): ReminderConfigResponse = ReminderConfigResponse(
        studentId = studentId,
        reminderEnabled = true,
        reminderIntervalHours = 24,
        lastMissedCheckinAt = null,
        lastReminderSentAt = null,
        lastCheckinDate = today(),
        smartScheduling = true,
        quietHoursStart = "22:00:00",
        quietHoursEnd = "07:00:00",
        maxPerDay = 3
    )

    fun mockReminderLogsResponse(): ReminderLogsResponse {
        val dates = last7Days()
        val logs = listOf(
            ReminderLog(1, 1, "Alex Rivera", 9, "checkin", "level_1", 24, 0, dates[2] + "T08:00:00", "push", "sent"),
            ReminderLog(2, 2, "Jordan Lee", 10, "checkin", "level_1", 24, 1, dates[1] + "T09:00:00", "push", "sent")
        )
        return ReminderLogsResponse(logs = logs)
    }

    // --- Alerts (optional, can stay empty or add one) ---

    fun mockEmergencyAlertsResponse(): EmergencyAlertsResponse = EmergencyAlertsResponse(alerts = emptyList())

    // --- Assistance requests ---

    fun mockAssistanceRequestsResponse(): AssistanceRequestsResponse {
        val dates = last7Days()
        val requests = listOf(
            AssistanceRequest(1, 1, "Alex Rivera", 9, null, null, 1, "Parent One", "Would like to discuss support options for stress.", "normal", "resolved", 10, "Counselor Smith", dates[4] + "T10:00:00", "Referred to weekly check-ins.", dates[4] + "T10:00:00"),
            AssistanceRequest(2, 2, "Jordan Lee", 10, null, null, 2, "Parent Two", "Question about mood tracking.", "low", "pending", null, null, null, null, dates[2] + "T15:30:00")
        )
        return AssistanceRequestsResponse(requests = requests)
    }

    // --- PHQ-9 and GAD-7 screeners ---

    private val phq9Questions = listOf(
        "Little interest or pleasure in doing things",
        "Feeling down, depressed, or hopeless",
        "Trouble falling or staying asleep, or sleeping too much",
        "Feeling tired or having little energy",
        "Poor appetite or overeating",
        "Feeling bad about yourself — or that you are a failure or have let yourself or your family down",
        "Trouble concentrating on things, such as reading the newspaper or watching television",
        "Moving or speaking so slowly that other people could have noticed? Or the opposite — being so fidgety or restless that you have been moving around a lot more than usual",
        "Thoughts that you would be better off dead or of hurting yourself in some way"
    )

    private val gad7Questions = listOf(
        "Feeling nervous, anxious, or on edge",
        "Not being able to stop or control worrying",
        "Worrying too much about different things",
        "Trouble relaxing",
        "Being so restless that it's hard to sit still",
        "Becoming easily annoyed or irritable",
        "Feeling afraid as if something awful might happen"
    )

    fun mockScreenerCatalogResponse(): ScreenerCatalogResponse {
        val catalog = listOf(
            ScreenerCatalogItem(screenerType = "phq9", name = "PHQ-9", questions = phq9Questions, answerScale = listOf(0, 1, 2, 3)),
            ScreenerCatalogItem(screenerType = "gad7", name = "GAD-7", questions = gad7Questions, answerScale = listOf(0, 1, 2, 3))
        )
        return ScreenerCatalogResponse(success = true, data = catalog)
    }

    fun mockListScreenerInstancesResponse(): ListScreenerInstancesResponse {
        val dates = last7Days()
        val instances = listOf(
            ScreenerInstance(1, 0, "phq9", "completed", "self", null, dates[3] + "T12:00:00", dates[2] + "T12:00:00"),
            ScreenerInstance(2, 0, "gad7", "completed", "self", null, dates[1] + "T11:00:00", dates[0] + "T11:00:00"),
            ScreenerInstance(3, 0, "phq9", "assigned", "manual", 10, null, today() + "T09:00:00")
        )
        return ListScreenerInstancesResponse(success = true, data = instances)
    }

    fun mockScreenerStudentReportData(studentId: Int = 0): ScreenerStudentReportData {
        val dates = last7Days()
        val phq9Instance = ScreenerInstanceWithScore(1, studentId, "phq9", "completed", dates[3] + "T12:00:00", 8, "mild", true)
        val gad7Instance = ScreenerInstanceWithScore(2, studentId, "gad7", "completed", dates[1] + "T11:00:00", 5, "minimal", false)
        val latestByType = mapOf(
            "phq9" to phq9Instance,
            "gad7" to gad7Instance
        )
        val history = listOf(phq9Instance, gad7Instance)
        return ScreenerStudentReportData(studentId = studentId, latestByType = latestByType, history = history)
    }

    fun mockScreenerSchoolReportData(): ScreenerSchoolReportData {
        val byType = mapOf(
            "phq9" to mapOf("minimal" to 2, "mild" to 3, "moderate" to 1),
            "gad7" to mapOf("minimal" to 4, "mild" to 2)
        )
        return ScreenerSchoolReportData(byType = byType)
    }

    /** Single instance for getInstance(instanceId) in test mode. */
    fun mockScreenerInstance(instanceId: Int, studentId: Int = 0, screenerType: String = "phq9", status: String = "assigned"): ScreenerInstance =
        ScreenerInstance(instanceId, studentId, screenerType, status, "manual", 10, null, today() + "T09:00:00")

    /** After "submitting" a screener in test mode. */
    fun mockSubmitScreenerData(instanceId: Int, total: Int = 6, severity: String = "minimal", positive: Boolean = false): SubmitScreenerData =
        SubmitScreenerData(instanceId = instanceId, status = "completed", total = total, severity = severity, positive = positive)
}
