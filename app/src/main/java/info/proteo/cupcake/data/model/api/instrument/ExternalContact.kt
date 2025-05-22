package info.proteo.cupcake.data.model.api.instrument

import com.squareup.moshi.Json

data class ExternalContactDetails(
    val id: Int,
    @Json(name = "contact_method_alt_name") val contactMethodAltName: String?,
    @Json(name = "contact_type") val contactType: String?,
    @Json(name = "contact_value") val contactValue: String?
)

data class ExternalContact(
    val id: Int,
    val user: Int,
    @Json(name = "contact_name") val contactName: String?,
    @Json(name = "contact_details") val contactDetails: List<ExternalContactDetails>?
)