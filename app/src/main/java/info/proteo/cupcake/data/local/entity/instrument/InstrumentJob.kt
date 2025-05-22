package info.proteo.cupcake.data.local.entity.instrument

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instrument_job")
data class InstrumentJobEntity(
    @PrimaryKey val id: Int,
    val instrument: Int,
    @ColumnInfo(name = "user_id") val userId: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "project_id") val projectId: Int?,
    @ColumnInfo(name = "session_id") val sessionId: Int?,
    @ColumnInfo(name = "protocol_id") val protocolId: Int?,
    @ColumnInfo(name = "job_type") val jobType: String?,
    val assigned: Boolean?,
    @ColumnInfo(name = "instrument_usage_id") val instrumentUsageId: Int?,
    @ColumnInfo(name = "job_name") val jobName: String?,
    @ColumnInfo(name = "sample_number") val sampleNumber: Int?,
    @ColumnInfo(name = "sample_type") val sampleType: String?,
    val funder: String?,
    @ColumnInfo(name = "cost_center") val costCenter: String?,
    @ColumnInfo(name = "stored_reagent_id") val storedReagentId: Int?,
    @ColumnInfo(name = "injection_volume") val injectionVolume: Float?,
    @ColumnInfo(name = "injection_unit") val injectionUnit: String?,
    @ColumnInfo(name = "search_engine") val searchEngine: String?,
    @ColumnInfo(name = "search_engine_version") val searchEngineVersion: String?,
    @ColumnInfo(name = "search_details") val searchDetails: String?,
    val location: String?,
    val status: String?,
    @ColumnInfo(name = "service_lab_group_id") val serviceLabGroupId: Int?,
    @ColumnInfo(name = "selected_template_id") val selectedTemplateId: Int?,
    @ColumnInfo(name = "submitted_at") val submittedAt: String?,
    @ColumnInfo(name = "completed_at") val completedAt: String?
)