package com.example.parqueame.repository

import com.example.parqueame.api.ApiService
import com.example.parqueame.models.DashboardResponse
import com.example.parqueame.models.PeriodDto

class CompanyRepository(private val api: ApiService) {
    suspend fun fetchDashboard(period: PeriodDto, branchId: String?): DashboardResponse {
        return api.getDashboard(period, branchId)
    }
}
