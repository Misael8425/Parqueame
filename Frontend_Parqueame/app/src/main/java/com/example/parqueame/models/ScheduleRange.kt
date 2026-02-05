package com.example.parqueame.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ScheduleRange(
    val open: String,   // "08:00"
    val close: String   // "18:00"
) : Parcelable