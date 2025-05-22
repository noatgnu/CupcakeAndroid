package info.proteo.cupcake.data.local.dao.metadatacolumn

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.metadatacolumn.FavouriteMetadataOptionEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.HumanDiseaseEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.MSUniqueVocabulariesEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.MetadataColumnEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.MetadataTableTemplateEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.PresetEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.SpeciesEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.SubcellularLocationEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.TissueEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.UnimodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataColumnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(column: MetadataColumnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(columns: List<MetadataColumnEntity>)

    @Update
    suspend fun update(column: MetadataColumnEntity)

    @Delete
    suspend fun delete(column: MetadataColumnEntity)

    @Query("SELECT * FROM metadata_column WHERE id = :id")
    suspend fun getById(id: Int): MetadataColumnEntity?

    @Query("SELECT * FROM metadata_column WHERE stored_reagent = :reagentId")
    fun getByReagent(reagentId: Int): Flow<List<MetadataColumnEntity>>

    @Query("DELETE FROM metadata_column")
    suspend fun deleteAll()
}

@Dao
interface MSUniqueVocabulariesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vocabulary: MSUniqueVocabulariesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vocabularies: List<MSUniqueVocabulariesEntity>)
    @Update
    suspend fun update(vocabulary: MSUniqueVocabulariesEntity)
    @Delete
    suspend fun delete(vocabulary: MSUniqueVocabulariesEntity)
    @Query("SELECT * FROM ms_unique_vocabularies WHERE accession = :accession")
    suspend fun getByAccession(accession: String): MSUniqueVocabulariesEntity?
    @Query("SELECT * FROM ms_unique_vocabularies WHERE name = :name")
    suspend fun getByName(name: String): MSUniqueVocabulariesEntity?
    @Query("SELECT * FROM ms_unique_vocabularies")
    fun getAllVocabularies(): Flow<List<MSUniqueVocabulariesEntity>>
    @Query("DELETE FROM ms_unique_vocabularies")
    suspend fun deleteAll()
}

@Dao
interface HumanDiseaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(disease: HumanDiseaseEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(diseases: List<HumanDiseaseEntity>)
    @Update
    suspend fun update(disease: HumanDiseaseEntity)
    @Delete
    suspend fun delete(disease: HumanDiseaseEntity)
    @Query("SELECT * FROM human_disease WHERE id = :id")
    suspend fun getById(id: Int): HumanDiseaseEntity?
    @Query("SELECT * FROM human_disease WHERE identifier = :identifier")
    suspend fun getByIdentifier(identifier: String): HumanDiseaseEntity?
    @Query("SELECT * FROM human_disease")
    fun getAllDiseases(): Flow<List<HumanDiseaseEntity>>
    @Query("DELETE FROM human_disease")
    suspend fun deleteAll()

}


@Dao
interface TissueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tissue: TissueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tissues: List<TissueEntity>)

    @Update
    suspend fun update(tissue: TissueEntity)

    @Delete
    suspend fun delete(tissue: TissueEntity)

    @Query("SELECT * FROM tissue WHERE id = :id")
    suspend fun getById(id: Int): TissueEntity?

    @Query("SELECT * FROM tissue WHERE identifier = :identifier")
    suspend fun getByIdentifier(identifier: String): TissueEntity?

    @Query("SELECT * FROM tissue")
    fun getAllTissues(): Flow<List<TissueEntity>>

    @Query("DELETE FROM tissue")
    suspend fun deleteAll()
}

@Dao
interface SubcellularLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: SubcellularLocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<SubcellularLocationEntity>)

    @Update
    suspend fun update(location: SubcellularLocationEntity)

    @Delete
    suspend fun delete(location: SubcellularLocationEntity)

    @Query("SELECT * FROM subcellular_location WHERE id = :id")
    suspend fun getById(id: Int): SubcellularLocationEntity?

    @Query("SELECT * FROM subcellular_location WHERE location_identifier = :identifier")
    suspend fun getByIdentifier(identifier: String): SubcellularLocationEntity?

    @Query("SELECT * FROM subcellular_location")
    fun getAllLocations(): Flow<List<SubcellularLocationEntity>>

    @Query("DELETE FROM subcellular_location")
    suspend fun deleteAll()
}

@Dao
interface SpeciesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(species: SpeciesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(species: List<SpeciesEntity>)

    @Update
    suspend fun update(species: SpeciesEntity)

    @Delete
    suspend fun delete(species: SpeciesEntity)

    @Query("SELECT * FROM species WHERE id = :id")
    suspend fun getById(id: Int): SpeciesEntity?

    @Query("SELECT * FROM species WHERE code = :code")
    suspend fun getByCode(code: String): SpeciesEntity?

    @Query("SELECT * FROM species")
    fun getAllSpecies(): Flow<List<SpeciesEntity>>

    @Query("DELETE FROM species")
    suspend fun deleteAll()
}

@Dao
interface UnimodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unimod: UnimodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(unimods: List<UnimodEntity>)

    @Update
    suspend fun update(unimod: UnimodEntity)

    @Delete
    suspend fun delete(unimod: UnimodEntity)

    @Query("SELECT * FROM unimod WHERE accession = :accession")
    suspend fun getByAccession(accession: String): UnimodEntity?

    @Query("SELECT * FROM unimod WHERE name LIKE :searchQuery")
    fun searchUnimods(searchQuery: String): Flow<List<UnimodEntity>>

    @Query("SELECT * FROM unimod")
    fun getAllUnimods(): Flow<List<UnimodEntity>>

    @Query("DELETE FROM unimod")
    suspend fun deleteAll()
}

@Dao
interface FavouriteMetadataOptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: FavouriteMetadataOptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<FavouriteMetadataOptionEntity>)

    @Update
    suspend fun update(option: FavouriteMetadataOptionEntity)

    @Delete
    suspend fun delete(option: FavouriteMetadataOptionEntity)

    @Query("SELECT * FROM favourite_metadata_option WHERE id = :id")
    suspend fun getById(id: Int): FavouriteMetadataOptionEntity?

    @Query("SELECT * FROM favourite_metadata_option WHERE user = :userId")
    fun getByUser(userId: Int): Flow<List<FavouriteMetadataOptionEntity>>

    @Query("SELECT * FROM favourite_metadata_option WHERE type = :type")
    fun getByType(type: String): Flow<List<FavouriteMetadataOptionEntity>>

    @Query("DELETE FROM favourite_metadata_option")
    suspend fun deleteAll()
}

@Dao
interface PresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<PresetEntity>)

    @Update
    suspend fun update(preset: PresetEntity)

    @Delete
    suspend fun delete(preset: PresetEntity)

    @Query("SELECT * FROM preset WHERE id = :id")
    suspend fun getById(id: Int): PresetEntity?

    @Query("SELECT * FROM preset WHERE user = :userId")
    fun getByUser(userId: Int): Flow<List<PresetEntity>>

    @Query("DELETE FROM preset")
    suspend fun deleteAll()
}

@Dao
interface MetadataTableTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: MetadataTableTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<MetadataTableTemplateEntity>)

    @Update
    suspend fun update(template: MetadataTableTemplateEntity)

    @Delete
    suspend fun delete(template: MetadataTableTemplateEntity)

    @Query("SELECT * FROM metadata_table_template WHERE id = :id")
    suspend fun getById(id: Int): MetadataTableTemplateEntity?

    @Query("SELECT * FROM metadata_table_template WHERE user = :userId")
    fun getByUser(userId: Int): Flow<List<MetadataTableTemplateEntity>>

    @Query("SELECT * FROM metadata_table_template WHERE service_lab_group = :labGroupId")
    fun getByServiceLabGroup(labGroupId: Int): Flow<List<MetadataTableTemplateEntity>>

    @Query("SELECT * FROM metadata_table_template WHERE lab_group = :labGroupId")
    fun getByLabGroup(labGroupId: Int): Flow<List<MetadataTableTemplateEntity>>

    @Query("DELETE FROM metadata_table_template")
    suspend fun deleteAll()
}
