package com.example.parqueame.models

data class BranchDto(
    val id: String,
    val name: String,
    val location: String,
    val rating: Double
)

data class ChartDto(
    val labels: List<String>,
    val values: List<Float>,
    val topLabel: String // ej: "≈ 1K"
)

data class MetricItemDto(
    val value: String,        // "3,456" | "$30,980"
    val label: String,        // "Total clientes"
    val deltaText: String,    // "+2.5%" | "-5%" | "0%"
    val trendUp: Boolean,     // true/false
    val highlight: Boolean = false,
    val neutral: Boolean = false
)

data class DashboardDto(
    val chart: ChartDto,
    val metricsTop: List<MetricItemDto>,
    val metricsBottom: List<MetricItemDto>
)

enum class PeriodDto { WEEKLY, MONTHLY, YEARLY }

data class DashboardResponse(
    val branches: List<BranchDto>,
    val selectedBranchId: String,
    val period: PeriodDto,
    val dashboard: DashboardDto
)
