//package com.example.parqueame.ui.parqueos
//
//import android.os.Parcelable
//import com.example.parqueame.models.ParkingLotDto
//import kotlinx.parcelize.Parcelize
//
//@Parcelize
//data class ReservationSummaryUi(
//    val parqueo: ParkingLotDto,
//    val startEpochMinutes: Long,
//    val endEpochMinutes: Long,
//    val timezone: String,
//    // Datos del backend:
//    val hoursBilled: Int,
//    val pricePerHour: Int,
//    val totalAmount: Int,
//    // Opcionales para UI:
//    val distanceMeters: Double? = null,
//    val paymentMethodLabel: String? = null
//) : Parcelable {
//
//    val subtotalDop: Double get() = hoursBilled.toDouble() * pricePerHour.toDouble()
//    val discountsDop: Double get() = 0.0
//    val taxesDop: Double get() = 0.0
//    val totalDop: Double get() = totalAmount.toDouble()
//}
