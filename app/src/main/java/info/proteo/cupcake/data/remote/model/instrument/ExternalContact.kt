package info.proteo.cupcake.data.remote.model.instrument

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExternalContactDetails(
    val id: Int?,
    @Json(name = "contact_method_alt_name") val contactMethodAltName: String?,
    @Json(name = "contact_type") val contactType: String?,
    @Json(name = "contact_value") val contactValue: String?
) {
    companion object {
        fun fromIdValue(id: Int?): Int? {
            return if (id == 0) null else id
        }
    }
}


@JsonClass(generateAdapter = true)
data class ExternalContact(
    val id: Int?,
    @Json(name = "contact_name") val contactName: String?,
    @Json(name = "contact_details") val contactDetails: List<ExternalContactDetails>?
) {
    companion object {
        fun fromIdValue(id: Int?): Int? {
            return if (id == 0) null else id
        }
    }
}