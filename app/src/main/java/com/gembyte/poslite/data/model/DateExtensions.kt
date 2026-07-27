package com.gembyte.poslite.data.model

import java.text.SimpleDateFormat
import java.util.*

fun Long.toDateString(): String {
    return SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.getDefault()
    ).format(Date(this))
}