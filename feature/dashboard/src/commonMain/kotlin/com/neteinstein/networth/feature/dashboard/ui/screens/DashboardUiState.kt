package com.neteinstein.networth.feature.dashboard.ui.screens

import com.neteinstein.networth.feature.dashboard.domain.model.NetWorthSummary

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: NetWorthSummary = NetWorthSummary.empty(),
) {
    val isEmpty: Boolean get() = !isLoading && summary.assets.isEmpty() && summary.liabilities.isEmpty()
}
