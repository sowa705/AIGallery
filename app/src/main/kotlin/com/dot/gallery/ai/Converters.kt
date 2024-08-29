package com.dot.gallery.ai

import androidx.room.TypeConverter
import java.lang.reflect.Type
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    @TypeConverter
    fun fromString(value: String?): FloatArray {
        val listType: Type = object : TypeToken<FloatArray>(){}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromFloatArray(array: FloatArray): String {
        val gson = Gson()
        return gson.toJson(array)
    }
}