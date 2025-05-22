package info.proteo.cupcake.data.local.entity.generic

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "limit_offset_cache")
data class LimitOffsetCacheEntity(
    @PrimaryKey val cacheKey: String,
    val count: Int,
    val next: String?,
    val previous: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
