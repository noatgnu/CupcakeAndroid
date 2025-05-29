package info.proteo.cupcake.ui.instrument

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.service.SignedTokenResponse
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.InstrumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstrumentDetailViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository,
    private val annotationRepository: AnnotationRepository
) : ViewModel() {

    private val _instrument = MutableLiveData<Result<Instrument>>()
    val instrument: LiveData<Result<Instrument>> = _instrument

    private val _isLoadingInstrument = MutableLiveData<Boolean>()
    val isLoadingInstrument: LiveData<Boolean> = _isLoadingInstrument

    private val _folderAnnotations = MutableLiveData<Result<LimitOffsetResponse<Annotation>>>()
    val folderAnnotations: LiveData<Result<LimitOffsetResponse<Annotation>>> = _folderAnnotations

    private val _isLoadingFolderAnnotations = MutableLiveData<Boolean>()
    val isLoadingFolderAnnotations: LiveData<Boolean> = _isLoadingFolderAnnotations

    fun loadInstrumentDetails(instrumentId: Int) {
        viewModelScope.launch {
            instrumentRepository.getInstrument(instrumentId)
                .onStart {
                    _isLoadingInstrument.value = true
                }
                .catch { e ->
                    _instrument.value = Result.failure(e)
                    _isLoadingInstrument.value = false
                }
                .collect { result ->
                    _instrument.value = result
                    _isLoadingInstrument.value = false
                }
        }
    }

    fun loadAnnotationsForFolder(
        folderId: Int,
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ) {
        viewModelScope.launch {
            annotationRepository.getAnnotationsInFolder(folderId, search, limit, offset)
                .onStart {
                    _isLoadingFolderAnnotations.value = true
                }
                .catch { e ->
                    _folderAnnotations.value = Result.failure(e)
                    _isLoadingFolderAnnotations.value = false
                }
                .collect { result ->
                    _folderAnnotations.value = result
                    _isLoadingFolderAnnotations.value = false
                }
        }
    }

    suspend fun getAnnotationDownloadToken(annotationId: Int): Flow<Result<SignedTokenResponse>> {
        return flow {
            emit(annotationRepository.getSignedUrl(annotationId))
        }
    }
}