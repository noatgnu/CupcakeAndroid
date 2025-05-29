package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.remote.service.AnnotationService
import info.proteo.cupcake.data.remote.service.BindUploadedFileRequest
import info.proteo.cupcake.data.remote.service.SignedTokenResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationRepository @Inject constructor(
    private val annotationService: AnnotationService
) {

    fun getAnnotationsInFolder(
        folderId: Int,
        searchTerm: String?,
        limit: Int,
        offset: Int
    ): Flow<Result<LimitOffsetResponse<Annotation>>> = flow {
        emit(annotationService.getAnnotationsInFolder(folderId, searchTerm, limit, offset))
    }

    fun getAnnotations(
        stepId: Int? = null,
        sessionUniqueId: String? = null,
        search: String? = null,
        ordering: String? = null,
        limit: Int,
        offset: Int,
        folderId: Int? = null
    ): Flow<Result<LimitOffsetResponse<Annotation>>> = flow {
        emit(annotationService.getAnnotations(stepId, sessionUniqueId, search, ordering, limit, offset, folderId))
    }

    fun getAnnotationById(id: Int): Flow<Result<Annotation>> = flow {
        emit(annotationService.getAnnotationById(id))
    }

    suspend fun createAnnotation(
        partMap: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation> {
        return annotationService.createAnnotation(partMap, file)
    }

    suspend fun updateAnnotation(
        id: Int,
        partMap: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation> {
        return annotationService.updateAnnotation(id, partMap, file)
    }

    suspend fun deleteAnnotation(id: Int): Result<Unit> {
        return annotationService.deleteAnnotation(id)
    }

    fun downloadFile(id: Int): Flow<Result<ResponseBody>> = flow {
        emit(annotationService.downloadFile(id))
    }

    suspend fun getSignedUrl(id: Int): Result<SignedTokenResponse> {
        return annotationService.getSignedUrl(id)
    }

    fun downloadSignedFile(token: String): Flow<Result<ResponseBody>> = flow {
        emit(annotationService.downloadSignedFile(token))
    }

    suspend fun retranscribe(id: Int, language: String?): Result<Unit> {
        return annotationService.retranscribe(id, language)
    }

    suspend fun ocr(id: Int): Result<Unit> {
        return annotationService.ocr(id)
    }

    suspend fun scratch(id: Int): Result<Annotation> {
        return annotationService.scratch(id)
    }

    suspend fun renameAnnotation(id: Int, newName: String): Result<Annotation> {
        return annotationService.renameAnnotation(id, newName)
    }

    suspend fun moveToFolder(id: Int, folderId: Int): Result<Annotation> {
        return annotationService.moveToFolder(id, folderId)
    }

    suspend fun bindUploadedFile(request: BindUploadedFileRequest): Result<Annotation> {
        return annotationService.bindUploadedFile(request)
    }
}