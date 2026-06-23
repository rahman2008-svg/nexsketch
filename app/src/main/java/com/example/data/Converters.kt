package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val layersListType = Types.newParameterizedType(List::class.java, CanvasLayer::class.java)
    private val layersAdapter = moshi.adapter<List<CanvasLayer>>(layersListType)

    @TypeConverter
    fun fromLayersJson(value: String): List<CanvasLayer> {
        if (value.isBlank()) return emptyList()
        return try {
            layersAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toLayersJson(list: List<CanvasLayer>): String {
        return try {
            layersAdapter.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }
}
