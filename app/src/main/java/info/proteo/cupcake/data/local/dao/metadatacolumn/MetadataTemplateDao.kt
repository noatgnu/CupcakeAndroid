package info.proteo.cupcake.data.local.dao.metadatacolumn

import androidx.room.*
import info.proteo.cupcake.data.local.entity.metadatacolumn.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataTemplateDao {
    // Metadata Table Template User Column operations
    @Query("SELECT columnId FROM metadata_table_template_user_column WHERE templateId = :templateId")
    fun getUserColumnsForTemplate(templateId: Int): Flow<List<Int>>

    @Query("SELECT templateId FROM metadata_table_template_user_column WHERE columnId = :columnId")
    fun getTemplatesForUserColumn(columnId: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateUserColumn(crossRef: MetadataTableTemplateUserColumnCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateUserColumns(crossRefs: List<MetadataTableTemplateUserColumnCrossRef>)

    @Delete
    suspend fun deleteTemplateUserColumn(crossRef: MetadataTableTemplateUserColumnCrossRef)

    @Query("DELETE FROM metadata_table_template_user_column WHERE templateId = :templateId")
    suspend fun deleteAllUserColumnsForTemplate(templateId: Int)

    // Metadata Table Template Staff Column operations
    @Query("SELECT columnId FROM metadata_table_template_staff_column WHERE templateId = :templateId")
    fun getStaffColumnsForTemplate(templateId: Int): Flow<List<Int>>

    @Query("SELECT templateId FROM metadata_table_template_staff_column WHERE columnId = :columnId")
    fun getTemplatesForStaffColumn(columnId: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateStaffColumn(crossRef: MetadataTableTemplateStaffColumnCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateStaffColumns(crossRefs: List<MetadataTableTemplateStaffColumnCrossRef>)

    @Delete
    suspend fun deleteTemplateStaffColumn(crossRef: MetadataTableTemplateStaffColumnCrossRef)

    @Query("DELETE FROM metadata_table_template_staff_column WHERE templateId = :templateId")
    suspend fun deleteAllStaffColumnsForTemplate(templateId: Int)
}