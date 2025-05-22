package info.proteo.cupcake.data.model.api.generic

data class LimitOffsetResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)
