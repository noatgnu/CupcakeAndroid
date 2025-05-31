package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.data.local.dao.instrument.ExternalContactDao
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.instrument.ExternalContact
import info.proteo.cupcake.data.remote.model.instrument.SupportInformation
import info.proteo.cupcake.data.local.dao.instrument.SupportInformationDao
import info.proteo.cupcake.data.local.entity.instrument.ExternalContactEntity
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationEntity
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationManufacturerContactCrossRef
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationVendorContactCrossRef
import info.proteo.cupcake.data.remote.model.instrument.ExternalContactDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class RemoveContactRequest(
    @Json(name = "contact_id") val contactId: Int,
    @Json(name = "contact_type") val contactType: String = "vendor"
)

interface SupportInformationApiService {
    @GET("api/support_information/")
    suspend fun getSupportInformation(
        @Query("search") search: String? = null,
        @Query("vendor_name") vendorName: String? = null,
        @Query("manufacturer_name") manufacturerName: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<SupportInformation>

    @GET("api/support_information/{id}/")
    suspend fun getSupportInformationById(@Path("id") id: Int): SupportInformation

    @POST("api/support_information/")
    suspend fun createSupportInformation(@Body supportInfo: SupportInformation): SupportInformation

    @PUT("api/support_information/{id}/")
    suspend fun updateSupportInformation(
        @Path("id") id: Int,
        @Body supportInfo: SupportInformation
    ): SupportInformation

    @DELETE("api/support_information/{id}/")
    suspend fun deleteSupportInformation(@Path("id") id: Int)

    @POST("api/support_information/{id}/add_vendor_contact/")
    suspend fun addVendorContact(
        @Path("id") id: Int,
        @Body contact: ExternalContact
    ): ExternalContact

    @POST("api/support_information/{id}/add_manufacturer_contact/")
    suspend fun addManufacturerContact(
        @Path("id") id: Int,
        @Body contact: ExternalContact
    ): ExternalContact

    @HTTP(method = "DELETE", path = "api/support_information/{id}/remove_contact/", hasBody = true)
    suspend fun removeContact(
        @Path("id") id: Int,
        @Body request: RemoveContactRequest
    ) : Response<Unit>

    @GET("api/external-contacts/")
    suspend fun getExternalContacts(
        @Query("support_information_id") supportInfoId: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<ExternalContact>

    @GET("api/external-contacts/{id}/")
    suspend fun getExternalContactById(@Path("id") id: Int): ExternalContact

    @POST("api/external-contacts/")
    suspend fun createExternalContact(@Body contact: ExternalContact): ExternalContact

    @PUT("api/external-contacts/{id}/")
    suspend fun updateExternalContact(
        @Path("id") id: Int,
        @Body contact: ExternalContact
    ): ExternalContact

    @DELETE("api/external-contacts/{id}/")
    suspend fun deleteExternalContact(@Path("id") id: Int) : Response<Unit>

    // External Contact Details endpoints
    @GET("api/contact-details/")
    suspend fun getContactDetails(
        @Query("contact_id") contactId: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<ExternalContactDetails>

    @GET("api/contact-details/{id}/")
    suspend fun getContactDetailById(@Path("id") id: Int): ExternalContactDetails

    @POST("api/contact-details/")
    suspend fun createContactDetail(@Body detail: ExternalContactDetails): ExternalContactDetails

    @PUT("api/contact-details/{id}/")
    suspend fun updateContactDetail(
        @Path("id") id: Int,
        @Body detail: ExternalContactDetails
    ): ExternalContactDetails

    @DELETE("api/contact-details/{id}/")
    suspend fun deleteContactDetail(@Path("id") id: Int): Response<Unit>
}

interface SupportInformationService {
    suspend fun getSupportInformation(
        search: String? = null,
        vendorName: String? = null,
        manufacturerName: String? = null,
        ordering: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<SupportInformation>>

    suspend fun getSupportInformationById(id: Int): Result<SupportInformation>
    suspend fun createSupportInformation(supportInfo: SupportInformation): Result<SupportInformation>
    suspend fun updateSupportInformation(id: Int, supportInfo: SupportInformation): Result<SupportInformation>
    suspend fun deleteSupportInformation(id: Int): Result<Unit>
    suspend fun addVendorContact(id: Int, contact: ExternalContact): Result<ExternalContact>
    suspend fun addManufacturerContact(id: Int, contact: ExternalContact): Result<ExternalContact>
    suspend fun removeContact(id: Int, contactId: Int, contactType: String = "vendor"): Result<Unit>
    suspend fun getExternalContacts(supportInfoId: Int? = null): Result<LimitOffsetResponse<ExternalContact>>
    suspend fun getExternalContactById(id: Int): Result<ExternalContact>
    suspend fun createExternalContact(contact: ExternalContact): Result<ExternalContact>
    suspend fun updateExternalContact(id: Int, contact: ExternalContact): Result<ExternalContact>
    suspend fun deleteExternalContact(id: Int): Result<Unit>
    suspend fun getContactDetails(contactId: Int? = null): Result<LimitOffsetResponse<ExternalContactDetails>>
    suspend fun getContactDetailById(id: Int): Result<ExternalContactDetails>
    suspend fun createContactDetail(detail: ExternalContactDetails): Result<ExternalContactDetails>
    suspend fun updateContactDetail(id: Int, detail: ExternalContactDetails): Result<ExternalContactDetails>
    suspend fun deleteContactDetail(id: Int): Result<Unit>
}

@Singleton
class SupportInformationServiceImpl @Inject constructor(
    private val apiService: SupportInformationApiService,
    private val supportInformationDao: SupportInformationDao,
    private val externalContactDao: ExternalContactDao
) : SupportInformationService {

    override suspend fun getSupportInformation(
        search: String?,
        vendorName: String?,
        manufacturerName: String?,
        ordering: String?,
        limit: Int?,
        offset: Int?
    ): Result<LimitOffsetResponse<SupportInformation>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSupportInformation(
                    search, vendorName, manufacturerName, ordering, limit, offset
                )
                response.results.forEach { supportInfo ->
                    cacheSupportInformation(supportInfo)
                }
                Result.success(response)
            } catch (e: Exception) {
                // No caching implementation here since we don't have a method to get all
                // support information from the DAO
                Result.failure(e)
            }
        }
    }

    override suspend fun getSupportInformationById(id: Int): Result<SupportInformation> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSupportInformationById(id)
                cacheSupportInformation(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedInfo = supportInformationDao.getById(id)
                    if (cachedInfo != null) {
                        Result.success(loadSupportInformation(cachedInfo))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createSupportInformation(supportInfo: SupportInformation): Result<SupportInformation> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createSupportInformation(supportInfo)
                cacheSupportInformation(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateSupportInformation(
        id: Int,
        supportInfo: SupportInformation
    ): Result<SupportInformation> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateSupportInformation(id, supportInfo)
                cacheSupportInformation(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteSupportInformation(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteSupportInformation(id)
                // Remove from cache
                supportInformationDao.getById(id)?.let { supportInformationDao.delete(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addVendorContact(
        id: Int,
        contact: ExternalContact
    ): Result<ExternalContact> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.addVendorContact(id, contact)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addManufacturerContact(
        id: Int,
        contact: ExternalContact
    ): Result<ExternalContact> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.addManufacturerContact(id, contact)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun removeContact(
        id: Int,
        contactId: Int,
        contactType: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RemoveContactRequest(contactId, contactType)
                apiService.removeContact(id, request)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheSupportInformation(supportInfo: SupportInformation) {
        val entity = SupportInformationEntity(
            id = supportInfo.id,
            vendorName = supportInfo.vendorName,
            manufacturerName = supportInfo.manufacturerName,
            serialNumber = supportInfo.serialNumber,
            maintenanceFrequencyDays = supportInfo.maintenanceFrequencyDays,
            locationId = supportInfo.locationId,
            warrantyStartDate = supportInfo.warrantyStartDate,
            warrantyEndDate = supportInfo.warrantyEndDate,
            createdAt = supportInfo.createdAt,
            updatedAt = supportInfo.updatedAt
        )
        supportInformationDao.insert(entity)

        supportInfo.vendorContacts?.forEach { contact ->
            val contactEntity = contact.id?.let {
                ExternalContactEntity(
                    id = it,
                    contactName = contact.contactName
                )
            }
            if (contactEntity == null) return@forEach
            externalContactDao.insert(contactEntity)

            supportInformationDao.insertVendorContactRef(
                SupportInformationVendorContactCrossRef(supportInfo.id, contact.id)
            )
        }

        supportInfo.manufacturerContacts?.forEach { contact ->
            val contactEntity = contact.id?.let {
                ExternalContactEntity(
                    id = it,
                    contactName = contact.contactName
                )
            }
            if (contactEntity == null) return@forEach
            externalContactDao.insert(contactEntity)

            supportInformationDao.insertManufacturerContactRef(
                SupportInformationManufacturerContactCrossRef(supportInfo.id, contact.id)
            )
        }
    }

    private suspend fun loadSupportInformation(entity: SupportInformationEntity): SupportInformation {
        val withContacts = supportInformationDao.getSupportInformationWithContacts(entity.id)

        return SupportInformation(
            id = entity.id,
            vendorName = entity.vendorName,
            vendorContacts = withContacts?.vendorContacts?.map { contactEntity ->
                ExternalContact(
                    id = contactEntity.id,
                    contactName = contactEntity.contactName,
                    contactDetails = null
                )
            },
            manufacturerName = entity.manufacturerName,
            manufacturerContacts = withContacts?.manufacturerContacts?.map { contactEntity ->
                ExternalContact(
                    id = contactEntity.id,
                    contactName = contactEntity.contactName,
                    contactDetails = null
                )
            },
            serialNumber = entity.serialNumber,
            maintenanceFrequencyDays = entity.maintenanceFrequencyDays,
            location = null,
            locationId = entity.locationId,
            warrantyStartDate = entity.warrantyStartDate,
            warrantyEndDate = entity.warrantyEndDate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun getExternalContacts(supportInfoId: Int?): Result<LimitOffsetResponse<ExternalContact>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getExternalContacts(supportInfoId)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getExternalContactById(id: Int): Result<ExternalContact> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getExternalContactById(id)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedContact = externalContactDao.getById(id)
                    if (cachedContact != null) {
                        // This needs a proper implementation to fetch contact details
                        Result.success(ExternalContact(
                            id = cachedContact.id,
                            contactName = cachedContact.contactName,
                            contactDetails = emptyList()
                        ))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createExternalContact(contact: ExternalContact): Result<ExternalContact> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createExternalContact(contact)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateExternalContact(id: Int, contact: ExternalContact): Result<ExternalContact> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateExternalContact(id, contact)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteExternalContact(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteExternalContact(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // External Contact Details implementations
    override suspend fun getContactDetails(contactId: Int?): Result<LimitOffsetResponse<ExternalContactDetails>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getContactDetails(contactId)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getContactDetailById(id: Int): Result<ExternalContactDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getContactDetailById(id)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createContactDetail(detail: ExternalContactDetails): Result<ExternalContactDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createContactDetail(detail)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateContactDetail(id: Int, detail: ExternalContactDetails): Result<ExternalContactDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateContactDetail(id, detail)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteContactDetail(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteContactDetail(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}