package info.proteo.cupcake.data.remote

import android.util.Log
import com.squareup.moshi.*
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class LimitOffsetResponseAdapterFactory : JsonAdapter.Factory {
    private val TAG = "MoshiDebug"

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {

        if (annotations.isNotEmpty()) {
            return null
        }

        val rawType = Types.getRawType(type)
        Log.d(TAG, "Raw type: $rawType")

        if (rawType != LimitOffsetResponse::class.java) {
            return null
        }

        if (type !is ParameterizedType) {
            return null
        }

        // Get inner type parameter
        val itemType = (type as ParameterizedType).actualTypeArguments[0]

        // Create list type adapter with proper type parameter
        val listType = Types.newParameterizedType(List::class.java, itemType)

        try {
            val listAdapter = moshi.adapter<List<Any>>(listType)

            return object : JsonAdapter<LimitOffsetResponse<Any>>() {
                override fun fromJson(reader: JsonReader): LimitOffsetResponse<Any>? {
                    try {
                        reader.beginObject()
                        var count = 0
                        var next: String? = null
                        var previous: String? = null
                        var results = emptyList<Any>()

                        while (reader.hasNext()) {
                            val name = reader.nextName()

                            when (name) {
                                "count" -> count = reader.nextInt()
                                "next" -> next = readNullableString(reader)
                                "previous" -> previous = readNullableString(reader)
                                "results" -> {
                                    try {
                                        results = listAdapter.fromJson(reader) ?: emptyList()
                                    } catch (e: Exception) {
                                        reader.skipValue()
                                    }
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        return LimitOffsetResponse(count, next, previous, results)
                    } catch (e: Exception) {
                        return null
                    }
                }

                override fun toJson(writer: JsonWriter, value: LimitOffsetResponse<Any>?) {
                }

                private fun readNullableString(reader: JsonReader): String? {
                    return if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<String?>()
                        null
                    } else {
                        reader.nextString()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create adapter", e)
            return null
        }
    }
}