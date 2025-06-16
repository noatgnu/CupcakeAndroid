package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.instrument.ExternalContact
import info.proteo.cupcake.shared.data.model.instrument.ExternalContactDetails
import info.proteo.cupcake.shared.data.model.instrument.SupportInformation
import info.proteo.cupcake.data.remote.service.SupportInformationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportInformationRepository @Inject constructor(
    private val supportInformationService: SupportInformationService
) {
    fun getSupportInformation(
        search: String? = null,
        vendorName: String? = null,
        manufacturerName: String? = null,
        ordering: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<SupportInformation>>> = flow {
        emit(supportInformationService.getSupportInformation(search, vendorName, manufacturerName, ordering, limit, offset))
    }

    fun getSupportInformationById(id: Int): Flow<Result<SupportInformation>> = flow {
        emit(supportInformationService.getSupportInformationById(id))
    }

    suspend fun createSupportInformation(supportInfo: SupportInformation): Result<SupportInformation> {
        return supportInformationService.createSupportInformation(supportInfo)
    }

    suspend fun updateSupportInformation(id: Int, supportInfo: SupportInformation): Result<SupportInformation> {
        return supportInformationService.updateSupportInformation(id, supportInfo)
    }

    suspend fun deleteSupportInformation(id: Int): Result<Unit> {
        return supportInformationService.deleteSupportInformation(id)
    }

    suspend fun addVendorContact(id: Int, contact: ExternalContact): Result<ExternalContact> {
        return supportInformationService.addVendorContact(id, contact)
    }

    suspend fun addManufacturerContact(id: Int, contact: ExternalContact): Result<ExternalContact> {
        return supportInformationService.addManufacturerContact(id, contact)
    }

    suspend fun removeContact(id: Int, contactId: Int, contactType: String = "vendor"): Result<Unit> {
        return supportInformationService.removeContact(id, contactId, contactType)
    }

    suspend fun saveSupportInformation(supportInfo: SupportInformation): Result<SupportInformation> {
        return if (supportInfo.id > 0) {
            updateSupportInformation(supportInfo.id, supportInfo)
        } else {
            createSupportInformation(supportInfo)
        }
    }

    fun getExternalContacts(supportInfoId: Int? = null): Flow<Result<LimitOffsetResponse<ExternalContact>>> = flow {
        emit(supportInformationService.getExternalContacts(supportInfoId))
    }

    fun getExternalContactById(id: Int): Flow<Result<ExternalContact>> = flow {
        emit(supportInformationService.getExternalContactById(id))
    }

    suspend fun createExternalContact(contact: ExternalContact): Result<ExternalContact> {
        return supportInformationService.createExternalContact(contact)
    }

    suspend fun updateExternalContact(id: Int, contact: ExternalContact): Result<ExternalContact> {
        return supportInformationService.updateExternalContact(id, contact)
    }

    suspend fun deleteExternalContact(id: Int): Result<Unit> {
        return supportInformationService.deleteExternalContact(id)
    }

    // External Contact Details methods
    fun getContactDetails(contactId: Int? = null): Flow<Result<LimitOffsetResponse<ExternalContactDetails>>> = flow {
        emit(supportInformationService.getContactDetails(contactId))
    }

    fun getContactDetailById(id: Int): Flow<Result<ExternalContactDetails>> = flow {
        emit(supportInformationService.getContactDetailById(id))
    }

    suspend fun createContactDetail(detail: ExternalContactDetails): Result<ExternalContactDetails> {
        return supportInformationService.createContactDetail(detail)
    }

    suspend fun updateContactDetail(id: Int, detail: ExternalContactDetails): Result<ExternalContactDetails> {
        return supportInformationService.updateContactDetail(id, detail)
    }

    suspend fun deleteContactDetail(id: Int): Result<Unit> {
        return supportInformationService.deleteContactDetail(id)
    }
}