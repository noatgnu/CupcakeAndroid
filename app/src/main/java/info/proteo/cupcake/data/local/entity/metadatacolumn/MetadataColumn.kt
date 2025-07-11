package info.proteo.cupcake.data.local.entity.metadatacolumn

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata_column")
data class MetadataColumnEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String?,
    @ColumnInfo(name = "column_position") val columnPosition: Int?,
    val value: String?,
    @ColumnInfo(name = "stored_reagent") val storedReagent: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "not_applicable") val notApplicable: Boolean,
    val mandatory: Boolean,
    val modifiers: String?, // JSON string
    val readonly: Boolean,
    val hidden: Boolean,
    @ColumnInfo(name = "auto_generated") val autoGenerated: Boolean
)

@Entity(tableName = "subcellular_location")
data class SubcellularLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "location_identifier") val locationIdentifier: String?,
    @ColumnInfo(name = "topology_identifier") val topologyIdentifier: String?,
    @ColumnInfo(name = "orientation_identifier") val orientationIdentifier: String?,
    val accession: String?,
    val definition: String?,
    val synonyms: String?,
    val content: String?,
    @ColumnInfo(name = "is_a") val isA: String?,
    @ColumnInfo(name = "part_of") val partOf: String?,
    val keyword: String?,
    @ColumnInfo(name = "gene_ontology") val geneOntology: String?,
    val annotation: String?,
    val references: String?,
    val links: String?
)

@Entity(tableName = "tissue")
data class TissueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val identifier: String?,
    val accession: String?,
    val synonyms: String?,
    @ColumnInfo(name = "cross_references") val crossReferences: String?
)

@Entity(tableName = "human_disease")
data class HumanDiseaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val identifier: String?,
    val acronym: String?,
    val accession: String?,
    val synonyms: String?,
    @ColumnInfo(name = "cross_references") val crossReferences: String?,
    val definition: String?,
    val keywords: String?
)

@Entity(tableName = "ms_unique_vocabularies")
data class MSUniqueVocabulariesEntity(
    @PrimaryKey val accession: String,
    val name: String?,
    val definition: String?,
    @ColumnInfo(name = "term_type") val termType: String?
)

@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: Int,
    val code: String?,
    val taxon: String?,
    @ColumnInfo(name = "common_name") val commonName: String?,
    @ColumnInfo(name = "official_name") val officialName: String?,
    val synonym: String?
)

@Entity(tableName = "unimod")
data class UnimodEntity(
    @PrimaryKey val accession: String,
    val name: String?,
    val definition: String?,
    @ColumnInfo(name = "additional_data") val additionalData: String?
)

@Entity(tableName = "favourite_metadata_option")
data class FavouriteMetadataOptionEntity(
    @PrimaryKey val id: Int,
    val user: Int,
    val name: String?,
    val type: String?,
    val value: String?,
    @ColumnInfo(name = "display_value") val displayValue: String?,
    @ColumnInfo(name = "service_lab_group") val serviceLabGroup: Int?,
    @ColumnInfo(name = "lab_group") val labGroup: Int?,
    val preset: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "is_global") val isGlobal: Boolean
)

@Entity(tableName = "preset")
data class PresetEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val user: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "metadata_table_template")
data class MetadataTableTemplateEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val user: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "hidden_user_columns") val hiddenUserColumns: Int?,
    @ColumnInfo(name = "hidden_staff_columns") val hiddenStaffColumns: Int?,
    @ColumnInfo(name = "service_lab_group") val serviceLabGroup: Int?,
    @ColumnInfo(name = "lab_group") val labGroup: Int?,
    @ColumnInfo(name = "field_mask_mapping") val fieldMaskMapping: String?, // JSON string
    val enabled: Boolean
)

@Entity(
    tableName = "metadata_table_template_user_column",
    primaryKeys = ["templateId", "columnId"]
)
data class MetadataTableTemplateUserColumnCrossRef(
    val templateId: Int,
    val columnId: Int
)

@Entity(
    tableName = "metadata_table_template_staff_column",
    primaryKeys = ["templateId", "columnId"]
)
data class MetadataTableTemplateStaffColumnCrossRef(
    val templateId: Int,
    val columnId: Int
)