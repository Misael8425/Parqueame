package com.example.parqueame.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
enum class WeekDay : Parcelable {
    MON, TUE, WED, THU, FRI, SAT, SUN
}
