package com.parqueame.util

import com.parqueame.models.ReservationStatus

/**
 * Centraliza la definición de "estados activos" y helpers de estado.
 * Evita reventar si el enum no tiene ciertas constantes.
 */
object ReservationStatusUtil {

    /** Intenta resolver el enum por nombre; si no existe, devuelve null. */
    fun statusOrNull(name: String): ReservationStatus? =
        runCatching { ReservationStatus.valueOf(name) }.getOrNull()

    /**
     * Conjunto de estados que cuentan como "activos".
     * Agrega/ajusta nombres según tu dominio; sólo se incluirán los que existan.
     */
    fun activeStatuses(): Set<ReservationStatus> = setOfNotNull(
        statusOrNull("ACTIVE"),
        statusOrNull("CONFIRMED"),
        statusOrNull("IN_PROGRESS"),
        statusOrNull("RESERVED"),
        statusOrNull("PAID")
        // Ejemplos alternativos: statusOrNull("ONGOING"), statusOrNull("APPROVED"), etc.
    )

    /** ¿Este status es activo? (null → false) */
    fun isActive(status: ReservationStatus?): Boolean =
        status != null && status in activeStatuses()

    /** Nombres (String) para consultar en Mongo cuando guardas el enum como texto. */
    fun activeStatusNames(): Set<String> = activeStatuses().mapTo(mutableSetOf()) { it.name }

    /**
     * Nombre del estado de "cancelado" que realmente exista en tu enum.
     * Orden de preferencia común: CANCELED → CANCELLED → CANCEL → VOID → ANULADA.
     * Si ninguno existe, devuelve "CANCELED" como fallback (no romperá compilación).
     */
    fun cancelStatusName(): String =
        statusOrNull("CANCELED")?.name
            ?: statusOrNull("CANCELLED")?.name
            ?: statusOrNull("CANCEL")?.name
            ?: statusOrNull("VOID")?.name
            ?: statusOrNull("ANULADA")?.name
            ?: "CANCELED"
}