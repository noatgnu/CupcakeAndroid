package info.proteo.cupcake.data.remote

import android.util.Log
import com.squareup.moshi.*
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class LimitOffsetResponseAdapterFactory : JsonAdapter.Factory {
    private val TAG = "LimitOffsetMoshiDebug"

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {

        if (annotations.isNotEmpty()) {
            Log.d(TAG, "Skipping type with annotations: $annotations")
            return null
        }

        val rawType = Types.getRawType(type)
        Log.d(TAG, "Checking type: $type, raw type: $rawType")

        if (rawType != LimitOffsetResponse::class.java) {
            return null
        }

        if (type !is ParameterizedType) {
            Log.e(TAG, "LimitOffsetResponse type is not parameterized: $type")
            return null
        }

        // Get inner type parameter
        val itemType = (type as ParameterizedType).actualTypeArguments[0]
        Log.d(TAG, "Creating adapter for LimitOffsetResponse with item type: $itemType")

        // Create list type adapter with proper type parameter
        val listType = Types.newParameterizedType(List::class.java, itemType)

        try {
            val listAdapter = moshi.adapter<List<Any>>(listType)
            Log.d(TAG, "Successfully created list adapter for type: $listType")

            return object : JsonAdapter<LimitOffsetResponse<Any>>() {
                override fun fromJson(reader: JsonReader): LimitOffsetResponse<Any>? {
                    Log.d(TAG, "Starting to parse LimitOffsetResponse JSON")

                    try {
                        reader.beginObject()
                        Log.d(TAG, "Successfully began JSON object")

                        var count = 0
                        var next: String? = null
                        var previous: String? = null
                        var results = emptyList<Any>()

                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            Log.d(TAG, "Processing field: $name")

                            when (name) {
                                "count" -> {
                                    try {
                                        count = reader.nextInt()
                                        Log.d(TAG, "Successfully parsed count: $count")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to parse count field", e)
                                        reader.skipValue()
                                    }
                                }
                                "next" -> {
                                    try {
                                        next = readNullableString(reader)
                                        Log.d(TAG, "Successfully parsed next: $next")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to parse next field", e)
                                        reader.skipValue()
                                    }
                                }
                                "previous" -> {
                                    try {
                                        previous = readNullableString(reader)
                                        Log.d(TAG, "Successfully parsed previous: $previous")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to parse previous field", e)
                                        reader.skipValue()
                                    }
                                }
                                "results" -> {
                                    Log.d(TAG, "Starting to parse results array")
                                    try {
                                        val parsedResults = listAdapter.fromJson(reader)
                                        results = parsedResults ?: emptyList()
                                        Log.d(TAG, "Successfully parsed ${results.size} results")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to parse results array", e)
                                        Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                                        Log.e(TAG, "Exception message: ${e.message}")
                                        e.printStackTrace()
                                        
                                        // Try to safely skip the value, but handle the case where reader is in inconsistent state
                                        try {
                                            reader.skipValue()
                                        } catch (skipException: Exception) {
                                            Log.e(TAG, "Failed to skip value after parsing error", skipException)
                                            // If we can't skip, we need to abort the entire parsing
                                            throw e
                                        }
                                        results = emptyList()
                                    }
                                }
                                else -> {
                                    Log.d(TAG, "Skipping unknown field: $name")
                                    reader.skipValue()
                                }
                            }
                        }
                        reader.endObject()
                        Log.d(TAG, "Successfully completed parsing LimitOffsetResponse")

                        val response = LimitOffsetResponse(count, next, previous, results)
                        Log.d(TAG, "Created LimitOffsetResponse with count=$count, next=$next, previous=$previous, results size=${results.size}")
                        return response

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse LimitOffsetResponse", e)
                        Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                        Log.e(TAG, "Exception message: ${e.message}")
                        Log.e(TAG, "Stack trace:", e)
                        return null
                    }
                }

                override fun toJson(writer: JsonWriter, value: LimitOffsetResponse<Any>?) {
                    // Implementation not needed for debugging
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
            Log.e(TAG, "Failed to create LimitOffsetResponse adapter", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            return null
        }
    }
}