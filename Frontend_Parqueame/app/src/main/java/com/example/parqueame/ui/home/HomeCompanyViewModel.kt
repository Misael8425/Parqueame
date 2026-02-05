package com.example.parqueame.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parqueame.api.RetrofitInstance            // 👈 usa tu singleton
import com.example.parqueame.api.ApiService
import com.example.parqueame.models.BranchDto
import com.example.parqueame.models.DashboardDto
import com.example.parqueame.models.DashboardResponse
import com.example.parqueame.models.PeriodDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class HomeCompanyUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val period: PeriodDto = PeriodDto.MONTHLY,
    val branches: List<BranchDto> = emptyList(),
    val selectedBranch: BranchDto? = null,
    val dashboard: DashboardDto? = null
)

class HomeCompanyViewModel : ViewModel() {                 // 👈 constructor vacío
    private val api: ApiService = RetrofitInstance.apiService  // 👈 usa TU retrofit

    private val _ui = MutableStateFlow(HomeCompanyUiState())
    val ui: StateFlow<HomeCompanyUiState> = _ui.asStateFlow()

    fun initIfNeeded() {
        if (_ui.value.branches.isEmpty()) {
            loadDashboard(_ui.value.period, _ui.value.selectedBranch?.id)
        }
    }

    fun setPeriod(period: PeriodDto) {
        if (_ui.value.period == period) return
        _ui.value = _ui.value.copy(period = period)
        loadDashboard(period, _ui.value.selectedBranch?.id)
    }

    fun setBranch(branchId: String) {
        loadDashboard(_ui.value.period, branchId)
    }

    fun retry() {
        loadDashboard(_ui.value.period, _ui.value.selectedBranch?.id)
    }

    private fun loadDashboard(period: PeriodDto, branchId: String?) {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                // Llama directamente al endpoint (no hay Response wrapper)
                val body: DashboardResponse = api.getDashboard(period = period, branchId = branchId)

                // Procesa el resultado normalmente
                val selected = body.branches.firstOrNull { it.id == body.selectedBranchId }
                    ?: body.branches.firstOrNull()

                _ui.value = HomeCompanyUiState(
                    loading = false,
                    error = null,
                    period = body.period,
                    branches = body.branches,
                    selectedBranch = selected,
                    dashboard = body.dashboard
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }


}
