package info.proteo.cupcake.data.remote.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class DataPermission {
    val edit: Boolean = false
    val delete: Boolean = false
    val view: Boolean = false
}