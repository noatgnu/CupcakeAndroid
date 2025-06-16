package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.data.local.dao.annotation.AnnotationDao
import info.proteo.cupcake.data.local.dao.annotation.AnnotationFolderDao
import info.proteo.cupcake.data.local.dao.annotation.AnnotationFolderPathDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.entity.annotation.AnnotationEntity
import info.proteo.cupcake.data.local.entity.annotation.AnnotationFolderPathEntity
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.annotation.AnnotationFolderPath
import info.proteo.cupcake.shared.data.model.user.UserBasic
import kotlinx.coroutines.flow.first
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadTokenResponse(
    val token: String
)

data class AnnotationFolderDetails(
    val id: Int,
    val name: String
)


interface ReagentDocumentApiService {
    @GET("api/reagent_documents/")
    suspend fun getReagentDocuments(
        @Query("reagent_id") reagentId: Int,
        @Query("folder_name") folderName: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): LimitOffsetResponse<Annotation>

    @GET("api/reagent_documents/folder_list/")
    suspend fun getReagentDocumentFolders(
        @Query("reagent_id") reagentId: Int
    ): List<AnnotationFolderDetails>

    @GET("api/reagent_documents/get_download_token/")
    suspend fun getDocumentDownloadToken(
        @Query("annotation_id") annotationId: Int
    ): DownloadTokenResponse
}

interface ReagentDocumentService {
    suspend fun getReagentDocuments(
        reagentId: Int,
        folderName: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<LimitOffsetResponse<Annotation>>

    suspend fun getReagentDocumentFolders(reagentId: Int): Result<List<AnnotationFolderDetails>>

    suspend fun getDocumentDownloadToken(annotationId: Int): Result<DownloadTokenResponse>
}

@Singleton
class ReagentDocumentServiceImpl @Inject constructor(
    private val reagentDocumentApiService: ReagentDocumentApiService,
    private val annotationDao: AnnotationDao,
    private val annotationFolderDao: AnnotationFolderDao,
    private val userDao: UserDao,
    private val annotationFolderPathDao: AnnotationFolderPathDao
) : ReagentDocumentService {

    override suspend fun getReagentDocuments(
        reagentId: Int,
        folderName: String?,
        limit: Int,
        offset: Int
    ): Result<LimitOffsetResponse<Annotation>> {
        return try {
            val response = reagentDocumentApiService.getReagentDocuments(
                reagentId = reagentId,
                folderName = folderName,
                limit = limit,
                offset = offset
            )
            response.results.forEach {
                cacheAnnotation(it)
            }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedAnnotations = annotationDao.getByReagent(reagentId).first()
                val filteredAnnotations = if (folderName != null) {
                    cachedAnnotations.filter {
                        it.annotationName?.contains(folderName, ignoreCase = true) == true
                    }
                } else {
                    cachedAnnotations
                }

                val domainObjects = filteredAnnotations.map { annotationEntity ->
                    val user = annotationEntity.userId?.let { userId ->
                        val userData = userDao.getById(userId)
                        if (userData == null) {
                            null
                        } else {
                            UserBasic(
                                id = userData.id,
                                username = userData.username,
                                firstName = userData.firstName,
                                lastName = userData.lastName
                            )
                        }

                    }
                    val folderPathList = annotationEntity.folderId?.let { folderId ->
                        annotationFolderPathDao.getById(folderId)?.let { folderPathEntity ->
                            listOf(folderPathEntity.toDomain())
                        }
                    }
                    annotationEntity.toDomain(user = user, folderPathList = folderPathList)
                }
                val typedResponse = LimitOffsetResponse<Annotation>(
                    count = domainObjects.size,
                    next = null,
                    previous = null,
                    results = domainObjects
                )
                Result.success(typedResponse)
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getReagentDocumentFolders(reagentId: Int): Result<List<AnnotationFolderDetails>> {
        return try {
            val response = reagentDocumentApiService.getReagentDocumentFolders(reagentId)
            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedFolders = annotationFolderDao.getByReagent(reagentId).first()
                val folderDetails = cachedFolders.map { folder ->
                    AnnotationFolderDetails(
                        id = folder.id,
                        name = folder.folderName
                    )
                }
                Result.success(folderDetails)
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getDocumentDownloadToken(annotationId: Int): Result<DownloadTokenResponse> {
        return try {
            val response = reagentDocumentApiService.getDocumentDownloadToken(annotationId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheAnnotation(annotation: Annotation) {
        annotationDao.insert(toEntity(annotation))
    }

    private fun toEntity(annotation: Annotation): AnnotationEntity {
        return AnnotationEntity(
            id = annotation.id,
            step = annotation.step,
            session = annotation.session,
            annotation = annotation.annotation,
            file = annotation.file,
            createdAt = annotation.createdAt,
            updatedAt = annotation.updatedAt,
            annotationType = annotation.annotationType,
            transcribed = annotation.transcribed,
            transcription = annotation.transcription,
            language = annotation.language,
            translation = annotation.translation,
            scratched = annotation.scratched,
            annotationName = annotation.annotationName,
            summary = annotation.summary,
            fixed = annotation.fixed,
            userId = annotation.user?.id,
            storedReagent = annotation.storedReagent,
            folderId = annotation.folder.first().id
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

}