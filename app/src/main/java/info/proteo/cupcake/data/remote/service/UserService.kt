package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.StoredReagentPermission
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.shared.data.model.user.UserBasic
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class StoredReagentPermissionRequest(
    @Json(name = "stored_reagents") val storedReagentIds: List<Int>
)

data class SessionPermissionRequest(
    val session: String
)


data class ProtocolPermissionRequest(
    val protocol: Int
)

data class AnnotationsPermissionRequest(
    val annotations: List<Int>
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "first_name") val firstName: String?,
    @Json(name = "last_name") val lastName: String?,
    val email: String?
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "old_password") val oldPassword: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SummarizePromptRequest(
    val prompt: String,
    val target: String
)

@JsonClass(generateAdapter = true)
data class SummarizeStepsRequest(
    val steps: List<Int>,
    @Json(name = "current_step") val currentStep: Int
)

@JsonClass(generateAdapter = true)
data class SummarizeAudioTranscriptRequest(
    val target: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class ExportDataRequest(
    @Json(name = "protocol_ids") val protocolIds: List<Int>?,
    @Json(name = "session_ids") val sessionIds: List<Int>?,
    val format: String?
)

@JsonClass(generateAdapter = true)
data class ImportUserDataRequest(
    @Json(name = "upload_id") val uploadId: String,
    @Json(name = "import_options") val importOptions: Map<String, Any>?
)

@JsonClass(generateAdapter = true)
data class DryRunImportRequest(
    @Json(name = "upload_id") val uploadId: String,
    @Json(name = "import_options") val importOptions: Map<String, Any>?
)

@JsonClass(generateAdapter = true)
data class CheckUserInLabGroupRequest(
    @Json(name = "lab_group") val labGroup: Int
)

@JsonClass(generateAdapter = true)
data class SignupRequest(
    val token: String
)

@JsonClass(generateAdapter = true)
data class TurnCredentials(
    val username: String,
    val password: String,
    @Json(name = "turn_server") val turnServer: String,
    @Json(name = "turn_port") val turnPort: Int
)

@JsonClass(generateAdapter = true)
data class IsStaffResponse(
    @Json(name = "is_staff") val isStaff: Boolean
)

@JsonClass(generateAdapter = true)
data class DryRunImportResponse(
    val message: String,
    @Json(name = "instance_id") val instanceId: String?
)

@JsonClass(generateAdapter = true)
data class ServerSettings(
    @Json(name = "allow_overlap_bookings") val allowOverlapBookings: Boolean,
    @Json(name = "use_coturn") val useCoturn: Boolean,
    @Json(name = "use_llm") val useLlm: Boolean,
    @Json(name = "use_ocr") val useOcr: Boolean,
    @Json(name = "use_whisper") val useWhisper: Boolean,
    @Json(name = "default_service_lab_group") val defaultServiceLabGroup: String,
    @Json(name = "can_send_email") val canSendEmail: Boolean
)

@JsonClass(generateAdapter = true)
data class UserPermissionResponse(
    val edit: Boolean = false,
    val view: Boolean = false,
    val delete: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class AnnotationsPermissionResponse(
    val permission: UserPermissionResponse,
    val annotation: Int
)

interface UserApiService {
    @GET("api/user/current/")
    suspend fun getCurrentUser(): User

    @GET("api/user/")
    suspend fun searchUsers(@Query("search") query: String): LimitOffsetResponse<UserBasic>

    @GET("api/user/{id}/")
    suspend fun getUserById(@Path("id") id: Int): User

    @GET("api/user/")
    suspend fun getUsersInLabGroup(@Query("lab_group") labGroupId: Int): LimitOffsetResponse<UserBasic>

    @GET("api/user/")
    suspend fun getAccessibleUsers(@Query("stored_reagent") storedReagentId: Int): LimitOffsetResponse<UserBasic>

    @POST("api/user/check_stored_reagent_permission/")
    suspend fun checkStoredReagentPermission(@Body request: StoredReagentPermissionRequest): List<StoredReagentPermission>

    @POST("api/user/check_session_permission/")
    suspend fun checkSessionPermission(@Body request: SessionPermissionRequest): UserPermissionResponse

    @POST("api/user/check_protocol_permission/")
    suspend fun checkProtocolPermission(@Body request: ProtocolPermissionRequest): UserPermissionResponse

    @POST("api/user/check_annotation_permission/")
    suspend fun checkAnnotationsPermission(@Body request: AnnotationsPermissionRequest): List<AnnotationsPermissionResponse>

    @GET("api/user/get_server_settings/")
    suspend fun getServerSettings(): ServerSettings

    @PUT("api/user/update_profile/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): User

    @POST("api/user/change_password/")
    suspend fun changePassword(@Body request: ChangePasswordRequest)

    @POST("api/user/summarize_prompt/")
    suspend fun summarizePrompt(@Body request: SummarizePromptRequest)

    @POST("api/user/summarize_steps/")
    suspend fun summarizeSteps(@Body request: SummarizeStepsRequest)

    @POST("api/user/summarize_audio_transcript/")
    suspend fun summarizeAudioTranscript(@Body request: SummarizeAudioTranscriptRequest)

    @GET("api/user/generate_turn_credential/")
    suspend fun generateTurnCredential(): TurnCredentials

    @POST("api/user/export_data/")
    suspend fun exportData(@Body request: ExportDataRequest)

    @POST("api/user/import_user_data/")
    suspend fun importUserData(@Body request: ImportUserDataRequest)

    @POST("api/user/dry_run_import_user_data/")
    suspend fun dryRunImportUserData(@Body request: DryRunImportRequest): DryRunImportResponse

    @GET("api/user/is_staff/")
    suspend fun isStaff(): IsStaffResponse

    @GET("api/user/get_user_lab_groups/")
    suspend fun getUserLabGroups(@Query("is_professional") isProfessional: Boolean?): LimitOffsetResponse<info.proteo.cupcake.shared.data.model.user.LabGroup>

    @POST("api/user/check_user_in_lab_group/")
    suspend fun checkUserInLabGroup(@Body request: CheckUserInLabGroupRequest)

    @POST("api/user/signup/")
    suspend fun signup(@Body request: SignupRequest): User
}

interface UserService {
    suspend fun getCurrentUser(): User
    suspend fun searchUsers(query: String): LimitOffsetResponse<UserBasic>
    suspend fun getUserById(id: Int): User
    suspend fun getUsersInLabGroup(labGroupId: Int): LimitOffsetResponse<UserBasic>
    suspend fun getAccessibleUsers(storedReagentId: Int): LimitOffsetResponse<UserBasic>
    suspend fun checkStoredReagentPermission(request: StoredReagentPermissionRequest): List<StoredReagentPermission>
    suspend fun checkSessionPermission(request: SessionPermissionRequest): UserPermissionResponse
    suspend fun checkProtocolPermission(request: ProtocolPermissionRequest): UserPermissionResponse
    suspend fun checkAnnotationsPermission(request: AnnotationsPermissionRequest): List<AnnotationsPermissionResponse>
    suspend fun getServerSettings(): ServerSettings
    suspend fun updateProfile(request: UpdateProfileRequest): User
    suspend fun changePassword(request: ChangePasswordRequest)
    suspend fun summarizePrompt(request: SummarizePromptRequest)
    suspend fun summarizeSteps(request: SummarizeStepsRequest)
    suspend fun summarizeAudioTranscript(request: SummarizeAudioTranscriptRequest)
    suspend fun generateTurnCredential(): TurnCredentials
    suspend fun exportData(request: ExportDataRequest)
    suspend fun importUserData(request: ImportUserDataRequest)
    suspend fun dryRunImportUserData(request: DryRunImportRequest): DryRunImportResponse
    suspend fun isStaff(): IsStaffResponse
    suspend fun getUserLabGroups(isProfessional: Boolean?): LimitOffsetResponse<info.proteo.cupcake.shared.data.model.user.LabGroup>
    suspend fun checkUserInLabGroup(request: CheckUserInLabGroupRequest)
    suspend fun signup(request: SignupRequest): User
}

@Singleton
class UserServiceImpl @Inject constructor(
    private val userApiService: UserApiService
) : UserService {

    override suspend fun getCurrentUser(): User {
        return userApiService.getCurrentUser()
    }

    override suspend fun searchUsers(query: String): LimitOffsetResponse<UserBasic> {
        return userApiService.searchUsers(query)
    }

    override suspend fun getUserById(id: Int): User {
        return userApiService.getUserById(id)
    }

    override suspend fun getUsersInLabGroup(labGroupId: Int): LimitOffsetResponse<UserBasic> {
        return userApiService.getUsersInLabGroup(labGroupId)
    }

    override suspend fun getAccessibleUsers(storedReagentId: Int): LimitOffsetResponse<UserBasic> {
        return userApiService.getAccessibleUsers(storedReagentId)
    }

    override suspend fun checkStoredReagentPermission(request: StoredReagentPermissionRequest): List<StoredReagentPermission> {
        return userApiService.checkStoredReagentPermission(request)
    }

    override suspend fun checkSessionPermission(request: SessionPermissionRequest): UserPermissionResponse {
        return userApiService.checkSessionPermission(request)
    }

    override suspend fun checkProtocolPermission(request: ProtocolPermissionRequest): UserPermissionResponse {
        return userApiService.checkProtocolPermission(request)
    }

    override suspend fun checkAnnotationsPermission(request: AnnotationsPermissionRequest): List<AnnotationsPermissionResponse> {
        return userApiService.checkAnnotationsPermission(request)
    }

    override suspend fun getServerSettings(): ServerSettings {
        return userApiService.getServerSettings()
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): User {
        return userApiService.updateProfile(request)
    }

    override suspend fun changePassword(request: ChangePasswordRequest) {
        userApiService.changePassword(request)
    }

    override suspend fun summarizePrompt(request: SummarizePromptRequest) {
        userApiService.summarizePrompt(request)
    }

    override suspend fun summarizeSteps(request: SummarizeStepsRequest) {
        userApiService.summarizeSteps(request)
    }

    override suspend fun summarizeAudioTranscript(request: SummarizeAudioTranscriptRequest) {
        userApiService.summarizeAudioTranscript(request)
    }

    override suspend fun generateTurnCredential(): TurnCredentials {
        return userApiService.generateTurnCredential()
    }

    override suspend fun exportData(request: ExportDataRequest) {
        userApiService.exportData(request)
    }

    override suspend fun importUserData(request: ImportUserDataRequest) {
        userApiService.importUserData(request)
    }

    override suspend fun dryRunImportUserData(request: DryRunImportRequest): DryRunImportResponse {
        return userApiService.dryRunImportUserData(request)
    }

    override suspend fun isStaff(): IsStaffResponse {
        return userApiService.isStaff()
    }

    override suspend fun getUserLabGroups(isProfessional: Boolean?): LimitOffsetResponse<info.proteo.cupcake.shared.data.model.user.LabGroup> {
        return userApiService.getUserLabGroups(isProfessional)
    }

    override suspend fun checkUserInLabGroup(request: CheckUserInLabGroupRequest) {
        userApiService.checkUserInLabGroup(request)
    }

    override suspend fun signup(request: SignupRequest): User {
        return userApiService.signup(request)
    }
}