package info.proteo.cupcake.data.network

import com.squareup.moshi.*
import info.proteo.cupcake.shared.data.model.protocol.ProtocolModel
import java.lang.reflect.Type

/**
 * Custom Moshi adapter to handle complex protocol JSON parsing issues
 */
class ProtocolModelAdapter : JsonAdapter<ProtocolModel>() {
    
    private val delegate = Moshi.Builder().build().adapter(ProtocolModel::class.java)
    
    @FromJson
    override fun fromJson(reader: JsonReader): ProtocolModel? {
        return try {
            // Use lenient mode to handle malformed JSON more gracefully
            reader.isLenient = true
            delegate.fromJson(reader)
        } catch (e: Exception) {
            // If parsing fails, try to parse just the basic fields
            parseBasicProtocol(reader)
        }
    }
    
    @ToJson
    override fun toJson(writer: JsonWriter, value: ProtocolModel?) {
        delegate.toJson(writer, value)
    }
    
    private fun parseBasicProtocol(reader: JsonReader): ProtocolModel? {
        return try {
            reader.beginObject()
            var id = 0
            var protocolId: Long? = null
            var protocolTitle: String? = null
            var protocolDescription: String? = null
            var protocolUrl: String? = null
            var protocolVersionUri: String? = null
            var enabled = false
            var complexityRating = 0f
            var durationRating = 0f
            
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextInt()
                    "protocol_id" -> protocolId = reader.nextLong()
                    "protocol_title" -> protocolTitle = reader.nextString()
                    "protocol_description" -> protocolDescription = reader.nextString()
                    "protocol_url" -> protocolUrl = reader.nextString()
                    "protocol_version_uri" -> protocolVersionUri = reader.nextString()
                    "enabled" -> enabled = reader.nextBoolean()
                    "complexity_rating" -> complexityRating = reader.nextDouble().toFloat()
                    "duration_rating" -> durationRating = reader.nextDouble().toFloat()
                    else -> reader.skipValue() // Skip complex nested fields
                }
            }
            reader.endObject()
            
            ProtocolModel(
                id = id,
                protocolId = protocolId,
                protocolCreatedOn = null,
                protocolDoi = null,
                protocolTitle = protocolTitle,
                protocolDescription = protocolDescription,
                protocolUrl = protocolUrl,
                protocolVersionUri = protocolVersionUri,
                steps = null,
                sections = null,
                enabled = enabled,
                complexityRating = complexityRating,
                durationRating = durationRating,
                reagents = null,
                tags = null,
                metadataColumns = null
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Factory to create the custom protocol adapter
 */
class ProtocolAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        return if (type == ProtocolModel::class.java) {
            ProtocolModelAdapter()
        } else null
    }
}