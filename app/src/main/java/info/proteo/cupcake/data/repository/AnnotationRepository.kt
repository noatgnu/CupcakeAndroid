package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.data.remote.service.AnnotationService
import info.proteo.cupcake.data.remote.service.BindUploadedFileRequest
import info.proteo.cupcake.data.remote.service.CreateAnnotationRequest
import info.proteo.cupcake.data.remote.service.SignedTokenResponse
import info.proteo.cupcake.data.remote.service.UpdateAnnotationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

fun String.toTextRequestBody(): RequestBody = this.toRequestBody("text/plain".toMediaTypeOrNull())


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
        limit: Int? = null,
        offset: Int? = null,
        folderId: Int? = null
    ): Flow<Result<LimitOffsetResponse<Annotation>>> = flow {
        emit(annotationService.getAnnotations(
            stepId = stepId,
            sessionUniqueId = sessionUniqueId,
            search = search,
            ordering = ordering,
            limit = limit,
            offset = offset,
            folderId = folderId
            ))
    }

    fun getAnnotationById(id: Int): Flow<Result<Annotation>> = flow {
        emit(annotationService.getAnnotationById(id))
    }



    suspend fun createAnnotationInRepository(
        requestData: CreateAnnotationRequest,
        filePart: MultipartBody.Part?
    ): Result<Annotation> {
        val params = mutableMapOf<String, RequestBody>()
        params["annotation"] = requestData.annotation.toTextRequestBody()
        params["annotation_type"] = requestData.annotationType.toTextRequestBody()
        requestData.storedReagent?.let { params["stored_reagent"] = it.toString().toTextRequestBody() }
        requestData.step?.let { params["step"] = it.toString().toTextRequestBody() }
        requestData.session?.let { params["session"] = it.toTextRequestBody() }
        requestData.maintenance?.let { params["maintenance"] = it.toString().toTextRequestBody() }
        requestData.instrument?.let { params["instrument"] = it.toString().toTextRequestBody() }
        requestData.timeStarted?.let { params["time_started"] = it.toTextRequestBody() }
        requestData.timeEnded?.let { params["time_ended"] = it.toTextRequestBody() }
        requestData.instrumentJob?.let { params["instrument_job"] = it.toString().toTextRequestBody() }
        requestData.instrumentUserType?.let { params["instrument_user_type"] = it.toTextRequestBody() }

        return annotationService.createAnnotation(params, filePart)
    }

    suspend fun updateAnnotationInRepository(
        id: Int,
        requestData: UpdateAnnotationRequest,
        filePart: MultipartBody.Part?
    ): Result<Annotation> {
        val params = mutableMapOf<String, RequestBody>()
        requestData.annotation?.let { params["annotation"] = it.toTextRequestBody() }
        requestData.translation?.let { params["translation"] = it.toTextRequestBody() }
        requestData.transcription?.let { params["transcription"] = it.toTextRequestBody() }

        return annotationService.updateAnnotation(id, params, filePart)
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