package com.gembyte.poslite.data.local.converter

import androidx.room.TypeConverter
import com.gembyte.poslite.data.model.ProductSaleUnit

class AppConverters {

    @TypeConverter
    fun fromSaleUnit(value: ProductSaleUnit): String {
        return value.name
    }

    @TypeConverter
    fun toSaleUnit(value: String): ProductSaleUnit {
        return ProductSaleUnit.valueOf(value)
    }
}