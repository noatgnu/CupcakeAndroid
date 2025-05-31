package info.proteo.cupcake.ui.contact

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.proteo.cupcake.data.remote.model.instrument.ExternalContact
import info.proteo.cupcake.data.remote.model.instrument.ExternalContactDetails
import info.proteo.cupcake.data.repository.SupportInformationRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditContactDialogViewModel @Inject constructor(
    private val supportInformationRepository: SupportInformationRepository
) : ViewModel() {

    private val _contactName = MutableLiveData<String>()
    val contactName: LiveData<String> = _contactName

    private val _contactDetails = MutableLiveData<List<ExternalContactDetails>>(emptyList())
    val contactDetails: LiveData<List<ExternalContactDetails>> = _contactDetails

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<Result<String>?>(null)
    val operationResult: LiveData<Result<String>?> = _operationResult

    private var supportInfoId: Int = -1
    private var contactId: Int? = null
    private var contactRole: String = ""
    private var isEditingMode: Boolean = false
    // private var originalContact: ExternalContact? = null // Not strictly needed if we don't diff against it for saving

    private val TAG = "AddEditContactDialogVM"

    fun initialize(supportInfoId: Int, contactId: Int?, role: String, isEditing: Boolean) {
        this.supportInfoId = supportInfoId
        this.contactId = contactId
        this.contactRole = role
        this.isEditingMode = isEditing

        // Reset state for new initialization
        _contactName.value = ""
        _contactDetails.value = emptyList()
        _isLoading.value = false
        _operationResult.value = null


        if (isEditingMode && contactId != null) {
            loadContactData(contactId)
        }
    }

    private fun loadContactData(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Loading contact data for ID: $id")
            supportInformationRepository.getExternalContactById(id).collect { result ->
                result.fold(
                    onSuccess = { contact ->
                        // originalContact = contact // Store if needed for complex diffing, otherwise not essential for current save logic
                        _contactName.value = contact.contactName ?: ""
                        _contactDetails.value = contact.contactDetails?.map { it.copy() } ?: emptyList() // Use .copy() for mutable list safety if needed downstream
                        Log.d(TAG, "Contact data loaded: ${contact.contactName}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to load contact data for ID: $id", error)
                        _operationResult.value = Result.failure(Exception("Failed to load contact: ${error.message}"))
                    }
                )
                _isLoading.value = false
            }
        }
    }

    fun updateContactName(name: String) {
        if (_contactName.value != name) {
            _contactName.value = name
        }
    }

    fun addContactDetail() {
        val currentDetails = _contactDetails.value?.toMutableList() ?: mutableListOf()
        currentDetails.add(
            ExternalContactDetails(
                id = null,
                contactMethodAltName = "",
                contactType = "",
                contactValue = ""
            )
        )
        _contactDetails.value = currentDetails
    }

    fun updateContactDetail(position: Int, updatedDetail: ExternalContactDetails) {
        val currentDetails = _contactDetails.value?.toMutableList() ?: return
        if (position < 0 || position >= currentDetails.size) {
            Log.w(TAG, "updateContactDetail: Invalid position $position")
            return
        }
        currentDetails[position] = updatedDetail
        _contactDetails.value = currentDetails
    }

    fun removeContactDetail(position: Int) {
        val currentDetails = _contactDetails.value?.toMutableList() ?: return
        if (position < 0 || position >= currentDetails.size) {
            Log.w(TAG, "removeContactDetail: Invalid position $position")
            return
        }

        val detailToRemove = currentDetails[position]
        if (detailToRemove.id != null && isEditingMode) {
            Log.d(TAG, "Detail to remove has ID ${detailToRemove.id}, marking for server deletion if necessary during save, or deleting explicitly.")
            deleteContactDetailFromServer(detailToRemove.id)
        }

        currentDetails.removeAt(position)
        _contactDetails.value = currentDetails
    }

    private fun deleteContactDetailFromServer(detailId: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting to delete contact detail with ID: $detailId from server.")
                supportInformationRepository.deleteContactDetail(detailId)
                Log.d(TAG, "Successfully called delete for contact detail ID: $detailId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting contact detail ID: $detailId from server", e)
            }
        }
    }

    fun saveContact(contactNameFromUi: String) {
        val currentContactName = contactNameFromUi.trim() // Use the latest from UI
        if (currentContactName.isBlank()) {
            _operationResult.value = Result.failure(Exception("Contact name cannot be empty"))
            return
        }

        val currentDetails = _contactDetails.value ?: emptyList()
        for (detail in currentDetails) {
            if (detail.contactType.isNullOrBlank() || detail.contactValue.isNullOrBlank()) {
                _operationResult.value = Result.failure(Exception("All contact details must have a type and a value."))
                return
            }
        }


        val contactToSave = ExternalContact(
            id = if (isEditingMode) this.contactId else null,
            contactName = currentContactName,
            contactDetails = currentDetails
        )

        val capturedContactId = this.contactId
        val capturedIsEditingMode = isEditingMode
        val capturedContactRole = contactRole

        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Saving contact. IsEditing: $capturedIsEditingMode, ContactID: $capturedContactId, Role: $capturedContactRole")
            try {
                if (capturedIsEditingMode && capturedContactId != null) {
                    updateExistingContact(capturedContactId, contactToSave)
                } else {
                    createNewContact(contactToSave)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during saveContact dispatch", e)
                _isLoading.value = false
                _operationResult.value = Result.failure(e)
            }
        }
    }

    private suspend fun createNewContact(contact: ExternalContact) {
        Log.d(TAG, "Creating new contact for role '$contactRole' with $supportInfoId: $contact")
        val result = when (contactRole.lowercase()) {
            "vendor" -> supportInformationRepository.addVendorContact(supportInfoId, contact)
            "manufacturer" -> supportInformationRepository.addManufacturerContact(supportInfoId, contact)
            else -> {
                Log.e(TAG, "Unknown contact role: $contactRole")
                _isLoading.value = false
                _operationResult.value = Result.failure(Exception("Unknown contact role: $contactRole"))
                return
            }
        }
        _isLoading.value = false
        _operationResult.value = Result.success("Contact added successfully")

    }

    private suspend fun updateExistingContact(id: Int, contact: ExternalContact) {
        Log.d(TAG, "Updating contact with ID $id: $contact")
        val updateResult = supportInformationRepository.updateExternalContact(id, contact)

        updateResult.fold(
            onSuccess = { _ ->
                Log.d(TAG, "Contact ID $id updated successfully.")
                _operationResult.value = Result.success("Contact updated successfully")
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to update contact ID $id.", error)
                _operationResult.value = Result.failure(error)
            }
        )
        _isLoading.value = false
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }
}