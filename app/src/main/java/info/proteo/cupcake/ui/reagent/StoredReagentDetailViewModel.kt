package info.proteo.cupcake.ui.reagent

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel

import info.proteo.cupcake.shared.data.model.reagent.ReagentAction
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent
import info.proteo.cupcake.shared.data.model.reagent.StoredReagentPermission
import info.proteo.cupcake.data.remote.service.BarcodeGenerator
import info.proteo.cupcake.data.remote.service.StorageObjectService
import info.proteo.cupcake.data.remote.service.StoredReagentPermissionRequest
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.data.remote.service.AnnotationFolderDetails
import info.proteo.cupcake.data.remote.service.DownloadTokenResponse
import info.proteo.cupcake.data.repository.ReagentActionRepository
import info.proteo.cupcake.data.repository.ReagentDocumentRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoredReagentDetailViewModel @Inject constructor(
    private val reagentRepository: StoredReagentRepository,
    private val barcodeGenerator: BarcodeGenerator,
    private val storageObjectService: StorageObjectService,
    private val reagentActionRepository: ReagentActionRepository,
    private val userRepository: UserRepository,
    private val reagentDocumentRepository: ReagentDocumentRepository
) : ViewModel() {

    private val _reagentActions = MutableStateFlow<List<ReagentAction>>(emptyList())
    val reagentActions: StateFlow<List<ReagentAction>> = _reagentActions

    private val _isLoadingActions = MutableStateFlow(false)
    val isLoadingActions: StateFlow<Boolean> = _isLoadingActions

    private val _actionLoadError = MutableStateFlow<String?>(null)
    val actionLoadError: StateFlow<String?> = _actionLoadError

    private var currentPage = 0
    private val pageSize = 20
    private var hasMoreActions = true
    private var reagentUnit: String? = null

    private val _storedReagent = MutableStateFlow<StoredReagent?>(null)
    val storedReagent: StateFlow<StoredReagent?> = _storedReagent

    private val _barcodeBitmap = MutableStateFlow<Bitmap?>(null)
    val barcodeBitmap: StateFlow<Bitmap?> = _barcodeBitmap

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedBarcodeFormat = MutableStateFlow("CODE128")
    val selectedBarcodeFormat: StateFlow<String> = _selectedBarcodeFormat

    private val _locationPath = MutableStateFlow<String?>(null)
    val locationPath: StateFlow<String?> = _locationPath

    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit

    private val _canView = MutableStateFlow(true)
    val canView: StateFlow<Boolean> = _canView

    private val _canDelete = MutableStateFlow(false)
    val canDelete: StateFlow<Boolean> = _canDelete

    private val _canUse = MutableStateFlow(false)
    val canUse: StateFlow<Boolean> = _canUse

    private val _barcodeScanResult = MutableStateFlow<String?>(null)
    val barcodeScanResult: StateFlow<String?> = _barcodeScanResult

    private val _documents = MutableStateFlow<List<Annotation>>(emptyList())
    val documents: StateFlow<List<Annotation>> = _documents

    private val _documentFolders = MutableStateFlow<List<AnnotationFolderDetails>>(emptyList())
    val documentFolders: StateFlow<List<AnnotationFolderDetails>> = _documentFolders

    private val _isLoadingDocuments = MutableStateFlow(false)
    val isLoadingDocuments: StateFlow<Boolean> = _isLoadingDocuments

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder



    private val _isBarcodeUpdating = MutableStateFlow(false)
    val isBarcodeUpdating: StateFlow<Boolean> = _isBarcodeUpdating

    private val _barcodeUpdateStatus = MutableStateFlow<Result<String>?>(null)
    val barcodeUpdateStatus: StateFlow<Result<String>?> = _barcodeUpdateStatus

    private val formatPriorityOrder = listOf(
        "CODE128", "EAN13", "UPC", "CODE39", "ITF14", "QR_CODE"
    )

    private var currentBarcodeContent = ""

    fun loadStoredReagent(reagentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                reagentRepository.getStoredReagentById(reagentId).collect { result ->
                    result.onSuccess { storedReagent ->
                        _storedReagent.value = storedReagent
                        loadPermissions(reagentId)
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun loadDocumentFolders(reagentId: Int) {
        viewModelScope.launch {
            reagentDocumentRepository.getReagentDocumentFolders(reagentId).collect { result ->
                result.onSuccess { folders ->
                    Log.d("StoredReagentDetailViewModel", "Loaded document folders: ${folders.size} for reagent ID: $reagentId")
                    _documentFolders.value = folders
                    if (folders.isNotEmpty() && _selectedFolder.value == null) {
                        _selectedFolder.value = folders.firstOrNull()?.name
                        loadDocuments(reagentId, _selectedFolder.value)
                    }
                }.onFailure { error ->
                    Log.e("StoredReagentDetailViewModel", "Error loading document folders: ${error.message}", error)
                }
            }
        }
    }

    fun loadDocuments(reagentId: Int, folderName: String? = null) {
        viewModelScope.launch {
            _isLoadingDocuments.value = true
            _selectedFolder.value = folderName

            reagentDocumentRepository.getReagentDocuments(
                reagentId = reagentId,
                folderName = folderName,
                limit = 50,
                offset = 0
            ).collect { result ->
                result.onSuccess { response ->
                    _documents.value = response.results
                }.onFailure { error ->
                    Log.e("StoredReagentDetailViewModel", "Error loading documents: ${error.message}", error)
                }
                _isLoadingDocuments.value = false
            }
        }
    }

    private fun loadPermissions(reagentId: Int) {
        Log.d("StoredReagentDetailViewModel", "Starting permission loading for reagent ID: $reagentId")
        viewModelScope.launch {
            try {
                val result = userRepository.checkStoredReagentPermissionDirectly(reagentId)

                result.onSuccess { permission ->
                    permission?.let {
                        _canEdit.value = it.permission.edit
                        _canView.value = it.permission.view
                        _canDelete.value = it.permission.delete
                        _canUse.value = it.permission.use
                    } ?: run {
                        Log.w("StoredReagentDetailViewModel", "Permission object was null, using defaults")
                        _canEdit.value = false
                        _canView.value = true
                        _canDelete.value = false
                        _canUse.value = false
                    }
                }.onFailure { error ->
                    Log.e("StoredReagentDetailViewModel", "Failed to load permissions: ${error.message}", error)
                    Log.e("StoredReagentDetailViewModel", "Error stack trace:", error)
                    _canEdit.value = false
                    _canView.value = true
                    _canDelete.value = false
                    _canUse.value = false
                }
            } catch (e: Exception) {
                Log.e("StoredReagentDetailViewModel", "Unexpected error in loadPermissions: ${e.message}", e)
                _canEdit.value = false
                _canView.value = true
                _canDelete.value = false
                _canUse.value = false
            }
        }
    }

    fun loadLocationPath(locationId: Int) {
        viewModelScope.launch {
            storageObjectService.getPathToRoot(locationId)
                .onSuccess { path ->
                    _locationPath.value = path.joinToString(" / ") { it.name }
                }
                .onFailure { error ->
                    Log.e("StoredReagentDetailViewModel", "Failed to get path: ${error.message}", error)
                }
        }
    }

    fun getDocumentDownloadToken(annotationId: Int): Flow<Result<DownloadTokenResponse>> = flow {
        emit(reagentDocumentRepository.getDocumentDownloadToken(annotationId).first())
    }

    fun setBarcodeResult(barcode: String?) {
        _barcodeScanResult.value = barcode
        if (barcode != null) {
            processBarcodeResult()
        }
    }

    fun processBarcodeResult() {
        val barcode = _barcodeScanResult.value ?: return
        Log.d("StoredReagentViewModel", "Processing barcode: $barcode")

        viewModelScope.launch {
            _isBarcodeUpdating.value = true
            storedReagent.value?.let { reagent ->
                val updatedReagent = reagent.copy(barcode = barcode)
                Log.d("StoredReagentViewModel", "Updating reagent with barcode: $barcode")
                saveStoredReagent(updatedReagent).collect { result ->
                    result.onSuccess {
                        generateBarcode(barcode)
                        _barcodeUpdateStatus.value = Result.success("Barcode updated successfully")
                        Log.d("StoredReagentViewModel", "Barcode updated successfully")
                    }.onFailure { exception ->
                        _barcodeUpdateStatus.value = Result.failure(exception)
                        Log.e("StoredReagentViewModel", "Barcode update failed", exception)
                    }
                    _isBarcodeUpdating.value = false
                    _barcodeScanResult.value = null
                }
            } ?: run {
                _barcodeUpdateStatus.value = Result.failure(IllegalStateException("No reagent loaded"))
                _isBarcodeUpdating.value = false
                _barcodeScanResult.value = null
            }
        }
    }

    fun clearBarcodeUpdateStatus() {
        _barcodeUpdateStatus.value = null
    }

    fun generateBarcode(content: String) {
        currentBarcodeContent = content

        viewModelScope.launch {
            for (format in formatPriorityOrder) {
                try {
                    val bitmap = barcodeGenerator.generateBarcode(content, format)
                    if (bitmap != null) {
                        _barcodeBitmap.value = bitmap
                        _selectedBarcodeFormat.value = format
                        break
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
    }

    fun updateBarcodeFormat(format: String) {
        viewModelScope.launch {
            try {
                val bitmap = barcodeGenerator.generateBarcode(currentBarcodeContent, format)
                _barcodeBitmap.value = bitmap
                _selectedBarcodeFormat.value = format
            } catch (e: Exception) {
                _barcodeBitmap.value = null
            }
        }
    }

    fun setReagentUnit(unit: String) {
        reagentUnit = unit
    }

    fun getReagentUnit(): String? {
        return reagentUnit
    }

    fun loadReagentActions(reagentId: Int, refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            _reagentActions.value = emptyList()
            hasMoreActions = true
        }

        if (!hasMoreActions || _isLoadingActions.value) return

        viewModelScope.launch {
            _isLoadingActions.value = true
            _actionLoadError.value = null

            reagentActionRepository.getReagentActions(
                reagentId = reagentId,
                offset = currentPage * pageSize,
                limit = pageSize
            ).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        Log.d("StoredReagentDetailViewModel", "Loaded ${response.results} actions for reagent ID: $reagentId")
                        val newItems = response.results
                        _reagentActions.value = _reagentActions.value + newItems
                        currentPage++
                        hasMoreActions = newItems.size == pageSize
                        _isLoadingActions.value = false
                    },
                    onFailure = { exception ->
                        _actionLoadError.value = exception.message ?: "Error loading actions"
                        _isLoadingActions.value = false
                    }
                )
            }
        }
    }

    fun loadMoreActions(reagentId: Int) {
        loadReagentActions(reagentId, false)
    }

    fun addReagent(quantity: Float, notes: String?): Flow<Result<ReagentAction>> = flow {
        _storedReagent.value?.let { reagent ->
            emit(reagentActionRepository.createReagentAction(
                reagentId = reagent.id,
                actionType = "add",
                quantity = quantity,
                notes = notes
            ))
        } ?: emit(Result.failure(IllegalStateException("Reagent not loaded")))
    }

    fun reserveReagent(quantity: Float, notes: String?): Flow<Result<ReagentAction>> = flow {
        _storedReagent.value?.let { reagent ->
            emit(reagentActionRepository.createReagentAction(
                reagentId = reagent.id,
                actionType = "reserve",
                quantity = quantity,
                notes = notes
            ))
        } ?: emit(Result.failure(IllegalStateException("Reagent not loaded")))
    }

    fun saveStoredReagent(storedReagent: StoredReagent): Flow<Result<StoredReagent>> = flow {
        emit(reagentRepository.updateStoredReagent(storedReagent.id, storedReagent))
    }
}