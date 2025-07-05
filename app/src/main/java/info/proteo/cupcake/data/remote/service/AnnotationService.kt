package info.proteo.cupcake.data.remote.service

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.data.local.dao.annotation.AnnotationDao
import info.proteo.cupcake.data.local.dao.annotation.AnnotationFolderPathDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.entity.annotation.AnnotationEntity
import info.proteo.cupcake.data.local.entity.annotation.AnnotationFolderPathEntity
import info.proteo.cupcake.data.local.entity.user.UserEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import javax.inject.Inject
import javax.inject.Singleton

import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.annotation.AnnotationFolderPath
import info.proteo.cupcake.shared.data.model.user.UserBasic
import kotlinx.coroutines.flow.firstOrNull

@JsonClass(generateAdapter = true)
data class SignedTokenResponse(@Json(name="signed_token") val signedToken: String)

@JsonClass(generateAdapter = true)
data class RenameAnnotationRequest( @Json(name = "annotation_name") val annotationName: String)

@JsonClass(generateAdapter = true)
data class MoveToFolderRequest(val folder: Int)

@JsonClass(generateAdapter = true)
data class RetranscribeRequest(val language: String?)

@JsonClass(generateAdapter = true)
data class BindUploadedFileRequest(
    @Json(name = "upload_id") val uploadId: String,
    @Json(name = "file_name") val fileName: String,
    @Json(name = "annotation_name") val annotationName: String,
    @Json(name = "folder") val folder: Int? = null,
    val step: Int? = null,
    val session: String? = null,
    @Json(name = "stored_reagent") val storedReagent: Int? = null
)

@JsonClass(generateAdapter = true)
data class CreateAnnotationRequest(
    @Json(name = "annotation")
    val annotation: String,

    @Json(name = "annotation_type")
    val annotationType: String,

    @Json(name = "stored_reagent")
    val storedReagent: Int? = null,

    @Json(name = "step")
    val step: Int? = null,

    @Json(name = "session")
    val session: String? = null, // session unique_id

    @Json(name = "maintenance")
    val maintenance: Boolean? = null,

    @Json(name = "instrument")
    val instrument: Int? = null,

    @Json(name = "time_started")
    val timeStarted: String? = null, // ISO 8601 datetime string

    @Json(name = "time_ended")
    val timeEnded: String? = null, // ISO 8601 datetime string

    @Json(name = "instrument_job")
    val instrumentJob: Int? = null,

    @Json(name = "instrument_user_type")
    val instrumentUserType: String? = null // e.g., "staff_annotation" or "user_annotation"
)

@JsonClass(generateAdapter = true)
data class UpdateAnnotationRequest(
    @Json(name = "annotation") val annotation: String? = null,
    @Json(name = "translation") val translation: String? = null,
    @Json(name = "transcription") val transcription: String? = null
)

interface AnnotationApiService {

    @GET("api/annotation/get_annotation_in_folder/")
    suspend fun getAnnotationsInFolder(
        @Query("folder") folderId: Int,
        @Query("search_term") searchTerm: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): LimitOffsetResponse<Annotation>

    @GET("api/annotation/")
    suspend fun getAnnotations(
        @Query("step") stepId: Int?,
        @Query("session__unique_id") sessionUniqueId: String?,
        @Query("search") search: String?,
        @Query("ordering") ordering: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("folder") folderId: Int? = null
    ): LimitOffsetResponse<Annotation>

    @GET("api/annotation/{id}/")
    suspend fun getAnnotationById(@Path("id") id: Int): Annotation

    @Multipart
    @POST("api/annotation/")
    suspend fun createAnnotation(
        @PartMap partMap: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Annotation

    @Multipart
    @PATCH("api/annotation/{id}/")
    suspend fun updateAnnotation(
        @Path("id") id: Int,
        @PartMap partMap: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Annotation

    @DELETE("api/annotation/{id}/")
    suspend fun deleteAnnotation(@Path("id") id: Int): retrofit2.Response<Unit>

    @GET("api/annotation/{id}/download_file/")
    @Streaming
    suspend fun downloadFile(@Path("id") id: Int): ResponseBody

    @POST("api/annotation/{id}/get_signed_url/")
    suspend fun getSignedUrl(@Path("id") id: Int): SignedTokenResponse

    @GET("api/annotation/download_signed/")
    @Streaming
    suspend fun downloadSignedFile(@Query("token") token: String): ResponseBody

    @POST("api/annotation/{id}/retranscribe/")
    suspend fun retranscribe(
        @Path("id") id: Int,
        @Body body: RetranscribeRequest
    ): retrofit2.Response<Unit>

    @POST("api/annotation/{id}/ocr/")
    suspend fun ocr(@Path("id") id: Int): retrofit2.Response<Unit>

    @POST("api/annotation/{id}/scratch/")
    suspend fun scratch(@Path("id") id: Int): Annotation

    @POST("api/annotation/{id}/rename/")
    suspend fun renameAnnotation(
        @Path("id") id: Int,
        @Body body: RenameAnnotationRequest
    ): Annotation

    @POST("api/annotation/{id}/move_to_folder/")
    suspend fun moveToFolder(
        @Path("id") id: Int,
        @Body body: MoveToFolderRequest
    ): Annotation

    @POST("api/annotation/bind_uploaded_file/")
    suspend fun bindUploadedFile(@Body body: BindUploadedFileRequest): Annotation
}

interface AnnotationService {
    suspend fun getAnnotationsInFolder(
        folderId: Int,
        searchTerm: String?,
        limit: Int,
        offset: Int
    ): Result<LimitOffsetResponse<Annotation>>

    suspend fun getAnnotations(
        stepId: Int?,
        sessionUniqueId: String?,
        search: String?,
        ordering: String?,
        limit: Int?,
        offset: Int?,
        folderId: Int?
    ): Result<LimitOffsetResponse<Annotation>>

    suspend fun getAnnotationById(id: Int): Result<Annotation>

    suspend fun createAnnotation(
        partMap: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation>

    suspend fun updateAnnotation(
        id: Int,
        partMap: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation>

    suspend fun deleteAnnotation(id: Int): Result<Unit>
    suspend fun downloadFile(id: Int): Result<ResponseBody>
    suspend fun getSignedUrl(id: Int): Result<SignedTokenResponse>
    suspend fun downloadSignedFile(token: String): Result<ResponseBody>
    suspend fun retranscribe(id: Int, language: String?): Result<Unit>
    suspend fun ocr(id: Int): Result<Unit>
    suspend fun scratch(id: Int): Result<Annotation>
    suspend fun renameAnnotation(id: Int, newName: String): Result<Annotation>
    suspend fun moveToFolder(id: Int, folderId: Int): Result<Annotation>
    suspend fun bindUploadedFile(request: BindUploadedFileRequest): Result<Annotation>
}

@Singleton
class AnnotationServiceImpl @Inject constructor(
    private val apiService: AnnotationApiService,
    private val annotationDao: AnnotationDao,
    private val userDao: UserDao,
    private val annotationFolderPathDao: AnnotationFolderPathDao

) : AnnotationService {

    override suspend fun getAnnotationsInFolder(
        folderId: Int,
        searchTerm: String?,
        limit: Int,
        offset: Int
    ): Result<LimitOffsetResponse<Annotation>> {
        return try {
            Result.success(apiService.getAnnotationsInFolder(folderId, searchTerm, limit, offset))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAnnotations(
        stepId: Int?, sessionUniqueId: String?, search: String?, ordering: String?,
        limit: Int?, offset: Int?, folderId: Int?
    ): Result<LimitOffsetResponse<Annotation>> {
        return try {
            Log.d("AnnotationService", "Fetching annotations with params: stepId=$stepId, sessionUniqueId=$sessionUniqueId, search=$search, ordering=$ordering, limit=$limit, offset=$offset, folderId=$folderId")
            val response = apiService.getAnnotations(stepId, sessionUniqueId, search, ordering, limit ?: 20, offset ?: 0, folderId)
            Log.d("AnnotationService", "Fetched ${response.results.size} annotations")
            response.results.forEach { cacheAnnotationWithRelations(it) }

            Result.success(response)
        } catch (e: Exception) {
            try {
                val count = annotationDao.countAll().firstOrNull()
                if (count == null) {
                    return Result.success(LimitOffsetResponse(0, null, null, emptyList()))
                }
                val cachedEntities = annotationDao.getAllPaginated(limit ?: 20, offset ?: 0).firstOrNull() ?: emptyList()
                val domainObjects = cachedEntities.map { loadAnnotationWithRelations(it) }
                Result.success(LimitOffsetResponse(count, null, null, domainObjects))
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }


    override suspend fun getAnnotationById(id: Int): Result<Annotation> {
        return try {
            val annotation = apiService.getAnnotationById(id)
            cacheAnnotationWithRelations(annotation)
            Result.success(annotation)
        } catch (e: Exception) {
            try {
                val cachedEntity = annotationDao.getById(id)
                if (cachedEntity != null) {
                    Result.success(loadAnnotationWithRelations(cachedEntity))
                } else {
                    Result.failure(e)
                }
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createAnnotation(
        partMap: Map<String, @JvmSuppressWildcards RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation> {
        return try {
            val createdAnnotation = apiService.createAnnotation(partMap, file)

            //val annotationEntity = createdAnnotation.toEntity() // Convert Annotation to AnnotationEntity
            //annotationDao.insert(annotationEntity)
            //createdAnnotation.folder?.forEach { folderPath ->
            //    annotationFolderPathDao.insert(folderPath.toEntity(createdAnnotation.id))
            //}
             //createdAnnotation.user?.let { user ->
             //    userDao.insert(user.toEntity())
            // }

            Result.success(createdAnnotation)
        } catch (e: Exception) {
            Log.e("AnnotationService", "Error creating annotation", e)
            Result.failure(e)
        }
    }

    override suspend fun updateAnnotation(
        id: Int,
        partMap: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation> {
        return try {
            val updatedAnnotation = apiService.updateAnnotation(id, partMap, file)
            cacheAnnotationWithRelations(updatedAnnotation)
            Result.success(updatedAnnotation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAnnotation(id: Int): Result<Unit> {
        return try {
            apiService.deleteAnnotation(id)
            annotationDao.getById(id)?.let { annotationDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(id: Int): Result<ResponseBody> {
        return try {
            Result.success(apiService.downloadFile(id))
        } catch (e: Exception)
        {
            Result.failure(e)
        }
    }

    override suspend fun getSignedUrl(id: Int): Result<SignedTokenResponse> {
        return try {
            Result.success(apiService.getSignedUrl(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadSignedFile(token: String): Result<ResponseBody> {
        return try {
            Result.success(apiService.downloadSignedFile(token))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun retranscribe(id: Int, language: String?): Result<Unit> {
        return try {
            apiService.retranscribe(id, RetranscribeRequest(language))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ocr(id: Int): Result<Unit> {
        return try {
            apiService.ocr(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun scratch(id: Int): Result<Annotation> {
        return try {
            val updatedAnnotation = apiService.scratch(id)
            cacheAnnotationWithRelations(updatedAnnotation)
            Result.success(updatedAnnotation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameAnnotation(id: Int, newName: String): Result<Annotation> {
        return try {
            val updatedAnnotation = apiService.renameAnnotation(id, RenameAnnotationRequest(newName))
            cacheAnnotationWithRelations(updatedAnnotation)
            Result.success(updatedAnnotation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveToFolder(id: Int, folderId: Int): Result<Annotation> {
        return try {
            val updatedAnnotation = apiService.moveToFolder(id, MoveToFolderRequest(folderId))
            cacheAnnotationWithRelations(updatedAnnotation)
            Result.success(updatedAnnotation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bindUploadedFile(request: BindUploadedFileRequest): Result<Annotation> {
        return try {
            val annotation = apiService.bindUploadedFile(request)
            cacheAnnotationWithRelations(annotation)
            Result.success(annotation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun Annotation.toEntity(): AnnotationEntity {
        return AnnotationEntity(
            id = id,
            step = step,
            session = session,
            annotation = annotation,
            file = file,
            createdAt = createdAt,
            updatedAt = updatedAt,
            annotationType = annotationType,
            transcribed = transcribed,
            transcription = transcription,
            language = language,
            translation = translation,
            scratched = scratched,
            annotationName = annotationName,
            summary = summary,
            fixed = fixed,
            userId = user?.id,
            storedReagent = storedReagent,
            folderId = folder?.firstOrNull()?.id
        )
    }

    fun AnnotationEntity.toDomain(user: UserBasic?, folderPathList: List<AnnotationFolderPath>?): Annotation {
        return Annotation(
            id = id,
            step = step,
            session = session,
            annotation = annotation,
            file = file,
            createdAt = createdAt,
            updatedAt = updatedAt,
            annotationType = annotationType,
            transcribed = transcribed,
            transcription = transcription,
            language = language,
            translation = translation,
            scratched = scratched,
            annotationName = annotationName,
            folder = folderPathList ?: emptyList(),
            summary = summary,
            instrumentUsage = null,
            metadataColumns = null,
            fixed = fixed,
            user = user,
            storedReagent = storedReagent
        )
    }

    fun AnnotationFolderPath.toEntity(): AnnotationFolderPathEntity {
        return AnnotationFolderPathEntity(
            id = this.id,
            folderName = this.folderName
        )
    }


    fun AnnotationFolderPathEntity.toDomain(): AnnotationFolderPath {
        return AnnotationFolderPath(
            id = id,
            folderName = folderName
        )
    }

    fun UserBasic.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            username = username,
            email = null,
            firstName = firstName ?: "",
            lastName = lastName ?: "",
            isStaff = false
        )
    }

    fun UserEntity.toUserBasic(): UserBasic {
        return UserBasic(
            id = id,
            username = username,
            firstName = firstName,
            lastName = lastName
        )
    }

    private suspend fun cacheAnnotationWithRelations(annotation: Annotation) {
        annotation.user?.let { userDao.insert(it.toEntity()) }
        annotation.folder?.forEach { folderPath ->
            annotationFolderPathDao.insert(folderPath.toEntity())
        }
        annotationDao.insert(annotation.toEntity())
    }

    private suspend fun loadAnnotationWithRelations(entity: AnnotationEntity): Annotation {
        val user = entity.userId?.let { userDao.getById(it)?.toUserBasic() }
        val folderPathList = entity.folderId?.let { fId ->
            annotationFolderPathDao.getById(fId)?.let { listOf(it.toDomain()) }
        }
        return entity.toDomain(user = user, folderPathList = folderPathList)
    }


}