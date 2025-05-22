package info.proteo.cupcake.ui.reagent

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.reagent.StoredReagent
import info.proteo.cupcake.data.remote.service.BarcodeGenerator
import info.proteo.cupcake.data.repository.StoredReagentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoredReagentDetailViewModel @Inject constructor(
    private val reagentRepository: StoredReagentRepository,
    private val barcodeGenerator: BarcodeGenerator
) : ViewModel() {

    private val _storedReagent = MutableStateFlow<StoredReagent?>(null)
    val storedReagent: StateFlow<StoredReagent?> = _storedReagent

    private val _barcodeBitmap = MutableStateFlow<Bitmap?>(null)
    val barcodeBitmap: StateFlow<Bitmap?> = _barcodeBitmap

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedBarcodeFormat = MutableStateFlow("CODE128")
    val selectedBarcodeFormat: StateFlow<String> = _selectedBarcodeFormat

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
                    }.onFailure { exception ->

                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun generateBarcode(content: String) {
        currentBarcodeContent = content

        // Try formats in priority order until one works
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
                    // Try next format
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
}