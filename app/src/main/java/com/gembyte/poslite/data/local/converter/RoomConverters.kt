package com.gembyte.poslite.data.local.converter

import androidx.room.TypeConverter
import com.gembyte.poslite.data.model.LedgerType
import com.gembyte.poslite.data.model.PaymentType
import com.gembyte.poslite.data.model.WeightUnit

class RoomConverters {

    @TypeConverter
    fun fromWeightUnit(value: WeightUnit): String {
        return value.name
    }

    @TypeConverter
    fun toWeightUnit(value: String): WeightUnit {
        return WeightUnit.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentType(
        value: PaymentType
    ): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentType(
        value: String
    ): PaymentType {
        return PaymentType.valueOf(value)
    }
}