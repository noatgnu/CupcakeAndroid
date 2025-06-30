package info.proteo.cupcake.ui.instrument

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.instrument.Instrument
import info.proteo.cupcake.data.repository.InstrumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class InstrumentViewModel @Inject constructor(
    private val repository: InstrumentRepository
) : ViewModel() {

    private val _instruments = MutableStateFlow<Result<LimitOffsetResponse<Instrument>>>(
        Result.success(LimitOffsetResponse(count = 0, next = null, previous = null, results = emptyList()))
    )
    val instruments: StateFlow<Result<LimitOffsetResponse<Instrument>>> = _instruments
    private var serialNumber: String? = null
    private var currentOffset = 0
    private val pageSize = 20
    private var _hasMoreData = true
    val hasMoreData: Boolean get() = _hasMoreData
    private var searchQuery: String? = null
    private var ordering: String? = null
    private var acceptsBookings: Boolean? = null

    init {
        loadInitialInstruments()
    }

    fun search(query: String?, isSerialNumber: Boolean = false) {
        if (isSerialNumber) {
            searchQuery = null
            serialNumber = query
        } else {
            searchQuery = query
            serialNumber = null
        }
        loadInitialInstruments()
    }

    fun loadInitialInstruments() {
        currentOffset = 0
        _hasMoreData = true
        loadInstruments(refresh = true)
    }

    fun loadMoreInstruments() {
        if (_hasMoreData) {
            loadInstruments(refresh = false)
        }
    }

    fun search(query: String?) {
        searchQuery = query
        loadInitialInstruments()
    }

    fun setBookingFilter(acceptsBookings: Boolean?) {
        this.acceptsBookings = acceptsBookings
        loadInitialInstruments()
    }

    private fun loadInstruments(refresh: Boolean) {
        if (refresh) {
            currentOffset = 0
        }
        Log.d("InstrumentViewModel", "Loading instruments with query: $searchQuery, serialNumber: $serialNumber, offset: $currentOffset")

        repository.getInstruments(
            search = searchQuery,
            ordering = ordering,
            limit = pageSize,
            offset = currentOffset,
            serialNumber = serialNumber,
            acceptsBookings = acceptsBookings
        ).onEach { result ->
            result.onSuccess { response ->
                Log.d("InstrumentViewModel", "Fetched instruments: ${response.results.size} items")
                if (!refresh && _instruments.value.isSuccess) {
                    val currentList = _instruments.value.getOrNull()?.results ?: emptyList()
                    val combinedList = currentList + response.results
                    _instruments.value = Result.success(
                        response.copy(results = combinedList)
                    )
                } else {
                    _instruments.value = result
                }

                _hasMoreData = response.next != null
                if (_hasMoreData) {
                    currentOffset += pageSize
                }
            }.onFailure {
                _instruments.value = result
            }
        }.catch { exception ->
            _instruments.value = Result.failure(exception)
        }.launchIn(viewModelScope)
    }
}