package info.proteo.cupcake.ui.maintenance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.MaintenanceLogRepository
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLog
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLogRequest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MaintenanceLogDetailViewModel @Inject constructor(
    private val maintenanceLogRepository: MaintenanceLogRepository,
    private val annotationRepository: AnnotationRepository
) : ViewModel() {

    private val _maintenanceLog = MutableLiveData<Result<MaintenanceLog>>()
    val maintenanceLog: LiveData<Result<MaintenanceLog>> = _maintenanceLog

    private val _annotations = MutableLiveData<Result<List<Annotation>>>()
    val annotations: LiveData<Result<List<Annotation>>> = _annotations

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateResult = MutableLiveData<Result<MaintenanceLog>?>()
    val updateResult: LiveData<Result<MaintenanceLog>?> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>?>()
    val deleteResult: LiveData<Result<Unit>?> = _deleteResult

    private var currentMaintenanceLogId: Long? = null

    fun loadMaintenanceLog(id: Long) {
        currentMaintenanceLogId = id
        _isLoading.value = true

        maintenanceLogRepository.getMaintenanceLog(id)
            .onEach { result ->
                _isLoading.value = false
                _maintenanceLog.value = result
                
                // Load annotations if maintenance log has annotation folder
                result.onSuccess { maintenanceLog ->
                    maintenanceLog.annotationFolder?.let { folderId ->
                        loadAnnotations(folderId)
                    }
                }
            }
            .catch { exception ->
                _isLoading.value = false
                _maintenanceLog.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    private fun loadAnnotations(folderId: Long) {
        annotationRepository.getAnnotationsInFolder(folderId.toInt(), null, 50, 0)
            .onEach { result ->
                _annotations.value = result.map { it.results }
            }
            .catch { exception ->
                _annotations.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    fun updateMaintenanceLog(request: MaintenanceLogRequest) {
        val id = currentMaintenanceLogId ?: return
        
        maintenanceLogRepository.updateMaintenanceLog(id, request)
            .onEach { result ->
                _updateResult.value = result
                if (result.isSuccess) {
                    // Reload the maintenance log to show updated data
                    loadMaintenanceLog(id)
                }
            }
            .catch { exception ->
                _updateResult.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    fun updateMaintenanceLogStatus(status: String) {
        val id = currentMaintenanceLogId ?: return
        
        maintenanceLogRepository.updateMaintenanceLogStatus(id, status)
            .onEach { result ->
                _updateResult.value = result
                if (result.isSuccess) {
                    // Reload the maintenance log to show updated data
                    loadMaintenanceLog(id)
                }
            }
            .catch { exception ->
                _updateResult.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    fun deleteMaintenanceLog() {
        val id = currentMaintenanceLogId ?: return
        
        maintenanceLogRepository.deleteMaintenanceLog(id)
            .onEach { result ->
                _deleteResult.value = result
            }
            .catch { exception ->
                _deleteResult.value = Result.failure(exception)
            }
            .launchIn(viewModelScope)
    }

    fun refreshData() {
        currentMaintenanceLogId?.let { id ->
            loadMaintenanceLog(id)
        }
    }

    fun clearResults() {
        _updateResult.value = null
        _deleteResult.value = null
    }
}