package com.finition.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finition.app.data.DashboardState
import com.finition.app.data.FinanceRepository
import com.finition.app.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(repository: FinanceRepository) : ViewModel() {

    private val now = MutableStateFlow(Format.startOfToday())

    val state: StateFlow<DashboardState> = repository.observeDashboard(now)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}
