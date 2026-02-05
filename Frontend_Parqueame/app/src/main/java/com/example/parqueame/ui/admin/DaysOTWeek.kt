package com.example.parqueame.ui.admin

import com.example.parqueame.R

enum class DaysOTWeek(val labelRes: Int, val short: String, val order: Int) {
    MON(R.string.monday_label, "Lu", 1),
    TUE(R.string.tuesday_label, "Ma", 2),
    WED(R.string.wednesday_label, "Mi", 3),
    THU(R.string.thursday_label, "Ju", 4),
    FRI(R.string.friday_label, "Vi", 5),
    SAT(R.string.saturday_label, "Sa", 6),
    SUN(R.string.sunday_label, "Do", 7)
}