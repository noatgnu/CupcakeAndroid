package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.data.remote.service.AnnotationFolderDetails
import info.proteo.cupcake.data.remote.service.DownloadTokenResponse
import info.proteo.cupcake.data.remote.service.ReagentDocumentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReagentDocumentRepository @Inject constructor(
    private val reagentDocumentService: ReagentDocumentService
) {
    fun getReagentDocuments(
        reagentId: Int,
        folderName: String? = null,
        limit: Int = 10,
        offset: Int = 0
    ): Flow<Result<LimitOffsetResponse<Annotation>>> = flow {
        emit(reagentDocumentService.getReagentDocuments(reagentId, folderName, limit, offset))
    }

    fun getReagentDocumentFolders(reagentId: Int): Flow<Result<List<AnnotationFolderDetails>>> = flow {
        emit(reagentDocumentService.getReagentDocumentFolders(reagentId))
    }

    fun getDocumentDownloadToken(annotationId: Int): Flow<Result<DownloadTokenResponse>> = flow {
        emit(reagentDocumentService.getDocumentDownloadToken(annotationId))
    }
}