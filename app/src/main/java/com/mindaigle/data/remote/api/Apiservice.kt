package com.mindaigle.data.remote.api

import com.mindaigle.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Health check
    @GET("hello")
    suspend fun hello(): Response<Map<String, String>>

    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @GET("auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: Map<String, String?>
    ): Response<ProfileUpdateResponse>

    @POST("auth/profile/photo")
    suspend fun uploadProfilePhoto(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<ProfilePhotoResponse>

    // Student Data
    @POST("student-data")
    suspend fun saveStudentData(
        @Header("Authorization") token: String,
        @Body request: SaveStudentDataRequest
    ): Response<SaveStudentDataResponse>

    @GET("student-data/me")
    suspend fun getMyStudentData(
        @Header("Authorization") token: String
    ): Response<StudentDataResponse>

    @GET("student-data/{studentId}")
    suspend fun getStudentData(
        @Header("Authorization") token: String,
        @Path("studentId") studentId: Int
    ): Response<StudentDataResponse>

    @GET("student-data/trends")
    suspend fun getTrendsData(
        @Header("Authorization") token: String,
        @Query("days") days: Int = 7
    ): Response<TrendsDataResponse>

    @GET("student-data/fhir-export")
    suspend fun exportFHIRData(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<FHIRExportResponse>

    @GET("student-data/{studentId}/fhir-export")
    suspend fun exportFHIRDataForStudent(
        @Header("Authorization") token: String,
        @Path("studentId") studentId: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<FHIRExportResponse>

    @GET("students")
    suspend fun getStudents(
        @Header("Authorization") token: String
    ): Response<StudentsResponse>

    // Parent endpoints
    @GET("parent/children")
    suspend fun getChildren(
        @Header("Authorization") token: String
    ): Response<ChildrenResponse>

    // Assistance Requests
    @POST("assistance-request")
    suspend fun createAssistanceRequest(
        @Header("Authorization") token: String,
        @Body request: CreateAssistanceRequest
    ): Response<CreateAssistanceResponse>

    @GET("assistance-requests")
    suspend fun getAssistanceRequests(
        @Header("Authorization") token: String
    ): Response<AssistanceRequestsResponse>

    @PUT("assistance-request/{requestId}")
    suspend fun updateAssistanceRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: Int,
        @Body request: UpdateAssistanceRequest
    ): Response<UpdateAssistanceResponse>

    // Reminder Configuration
    @GET("student-reminder-config")
    suspend fun getReminderConfig(
        @Header("Authorization") token: String
    ): Response<ReminderConfigResponse>

    @PUT("student-reminder-config")
    suspend fun updateReminderConfig(
        @Header("Authorization") token: String,
        @Body request: UpdateReminderConfigRequest
    ): Response<UpdateReminderConfigResponse>

    // Emergency Alerts
    @POST("emergency-alert")
    suspend fun createEmergencyAlert(
        @Header("Authorization") token: String,
        @Body request: CreateEmergencyAlertRequest
    ): Response<CreateEmergencyAlertResponse>

    @GET("emergency-alerts")
    suspend fun getEmergencyAlerts(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null
    ): Response<EmergencyAlertsResponse>

    @PUT("emergency-alert/{alertId}/acknowledge")
    suspend fun acknowledgeAlert(
        @Header("Authorization") token: String,
        @Path("alertId") alertId: Int
    ): Response<AlertActionResponse>

    @PUT("emergency-alert/{alertId}/resolve")
    suspend fun resolveAlert(
        @Header("Authorization") token: String,
        @Path("alertId") alertId: Int,
        @Body request: ResolveAlertRequest
    ): Response<AlertActionResponse>

    @GET("emergency-alert/{alertId}/screening")
    suspend fun getSuicideRiskScreening(
        @Header("Authorization") token: String,
        @Path("alertId") alertId: Int
    ): Response<SuicideRiskScreeningResponse>

    // Communications
    @POST("communications/send-message")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    @GET("communications/my-messages")
    suspend fun getMyMessages(
        @Header("Authorization") token: String
    ): Response<CommunicationsResponse>

    @PUT("communications/message/{messageId}/read")
    suspend fun markMessageAsRead(
        @Header("Authorization") token: String,
        @Path("messageId") messageId: Int
    ): Response<MessageActionResponse>

    @GET("communications/sent-messages")
    suspend fun getSentMessages(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<SentMessagesResponse>

    @GET("communications/parent-visible-messages/{studentId}")
    suspend fun getParentVisibleMessages(
        @Header("Authorization") token: String,
        @Path("studentId") studentId: Int
    ): Response<CommunicationsResponse>

    // Reminder Logs (for staff)
    @GET("reminder-logs")
    suspend fun getReminderLogs(
        @Header("Authorization") token: String,
        @Query("studentId") studentId: Int? = null,
        @Query("escalationLevel") escalationLevel: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ReminderLogsResponse>

    // Appointments
    @GET("appointments")
    suspend fun getAppointments(
        @Header("Authorization") token: String
    ): Response<AppointmentsResponse>

    @POST("appointments")
    suspend fun createAppointment(
        @Header("Authorization") token: String,
        @Body request: CreateAppointmentRequest
    ): Response<CreateAppointmentResponse>

    @PUT("appointments/{id}")
    suspend fun updateAppointment(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateAppointmentRequest
    ): Response<CreateAppointmentResponse>

    @DELETE("appointments/{id}")
    suspend fun cancelAppointment(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Map<String, Any>>

    @GET("appointments/staff-availability")
    suspend fun getStaffAvailability(
        @Header("Authorization") token: String
    ): Response<StaffAvailabilityResponse>

    // Achievements
    @GET("achievements")
    suspend fun getAchievements(
        @Header("Authorization") token: String
    ): Response<AchievementsResponse>

    @GET("achievements/my-achievements")
    suspend fun getMyAchievements(
        @Header("Authorization") token: String
    ): Response<AchievementsResponse>

    @POST("achievements/unlock")
    suspend fun unlockAchievement(
        @Header("Authorization") token: String,
        @Body request: UnlockAchievementRequest
    ): Response<UnlockAchievementResponse>

    // Counselor Notes
    @GET("counselor-notes")
    suspend fun getCounselorNotes(
        @Header("Authorization") token: String
    ): Response<CounselorNotesResponse>

    // Activity Logs (for achievements calculation)
    @GET("activity-logs")
    suspend fun getActivityLogs(
        @Header("Authorization") token: String,
        @Query("days") days: Int = 30
    ): Response<ActivityLogsResponse>

    @POST("activity-logs/sync")
    suspend fun syncActivityLogs(
        @Header("Authorization") token: String,
        @Body request: ActivitySyncRequest
    ): Response<ActivitySyncResponse>

    // Staff Replies (for staff to reply to student requests)
    @POST("communications/{communicationId}/reply")
    suspend fun replyToStudentRequest(
        @Header("Authorization") token: String,
        @Path("communicationId") communicationId: Int,
        @Body request: StaffReplyRequest
    ): Response<StaffReplyResponse>

    @GET("student-requests")
    suspend fun getStudentRequests(
        @Header("Authorization") token: String
    ): Response<StudentRequestsResponse>

    // Analytics Trends (for staff/associate; optional studentIds for peer comparison)
    @GET("analytics/trends")
    suspend fun getAnalyticsTrends(
        @Header("Authorization") token: String,
        @Query("days") days: Int = 7,
        @Query("studentIds") studentIds: String? = null
    ): Response<AnalyticsTrendsResponse>

    // Clinical Screeners (PHQ-9 / GAD-7)
    @GET("screeners/catalog")
    suspend fun getScreenerCatalog(
        @Header("Authorization") token: String
    ): Response<ScreenerCatalogResponse>

    @POST("screeners/instances")
    suspend fun createScreenerInstance(
        @Header("Authorization") token: String,
        @Body request: CreateScreenerInstanceRequest
    ): Response<ScreenerInstanceResponse>

    @GET("screeners/instances")
    suspend fun listScreenerInstances(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("student_id") studentId: Int? = null
    ): Response<ListScreenerInstancesResponse>

    @GET("screeners/instances/{id}")
    suspend fun getScreenerInstance(
        @Header("Authorization") token: String,
        @Path("id") instanceId: Int,
        @Query("student_id") studentId: Int? = null
    ): Response<GetScreenerInstanceResponse>

    @POST("screeners/instances/{id}/submit")
    suspend fun submitScreener(
        @Header("Authorization") token: String,
        @Path("id") instanceId: Int,
        @Body request: SubmitScreenerRequest
    ): Response<SubmitScreenerResponse>

    @GET("screeners/reports/student/{studentId}")
    suspend fun getScreenerStudentReport(
        @Header("Authorization") token: String,
        @Path("studentId") studentId: Int
    ): Response<ScreenerStudentReportResponse>

    @GET("screeners/reports/school")
    suspend fun getScreenerSchoolReport(
        @Header("Authorization") token: String
    ): Response<ScreenerSchoolReportResponse>
}

