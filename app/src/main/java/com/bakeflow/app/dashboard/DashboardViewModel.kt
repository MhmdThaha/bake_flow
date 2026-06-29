package com.bakeflow.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.BakeFlowPreferences
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.ProductionRepository
import com.bakeflow.app.domain.repository.SaleRepository
import com.bakeflow.app.domain.repository.StockAdjustmentRepository
import com.bakeflow.app.domain.repository.WasteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val bakeryName: String = "",
    val todaySalesCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val todayProductionCount: Int = 0,
    val todayWasteCount: Int = 0,
    val todayWasteQuantity: Double = 0.0,
    val todayWasteLoss: Double = 0.0,
    val todayAdjustmentCount: Int = 0,
    val todayAdjustmentNetChange: Double = 0.0,
    val lowStockCount: Int = 0,
    val lowStockNames: List<String> = emptyList(),
    val bestSellingProduct: String = "—",
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val saleRepository: SaleRepository,
    private val productionRepository: ProductionRepository,
    private val ingredientRepository: IngredientRepository,
    private val wasteRepository: WasteRepository,
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val preferences: BakeFlowPreferences
) : ViewModel() {

    private val bakeryName = preferences.getBakeryName()

    private val _uiState = MutableStateFlow(DashboardUiState(bakeryName = bakeryName))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                saleRepository.observeSalesSummary(),
                productionRepository.observeDashboardStats(),
                ingredientRepository.observeIngredients(),
                wasteRepository.observeWasteSummary(),
                stockAdjustmentRepository.observeAdjustmentSummary()
            ) { salesSummary, productionStats, ingredients, wasteSummary, adjustmentSummary ->
                val lowStock = ingredients.filter { it.isLowStock }
                DashboardUiState(
                    bakeryName = bakeryName,
                    todaySalesCount = salesSummary.todayCount,
                    todayRevenue = salesSummary.todayRevenue,
                    todayProductionCount = productionStats.todayBatchCount,
                    todayWasteCount = wasteSummary.todayCount,
                    todayWasteQuantity = wasteSummary.todayQuantity,
                    todayWasteLoss = wasteSummary.todayEstimatedLoss,
                    todayAdjustmentCount = adjustmentSummary.todayCount,
                    todayAdjustmentNetChange = adjustmentSummary.todayDifferenceTotal,
                    lowStockCount = lowStock.size,
                    lowStockNames = lowStock.take(5).map { it.name },
                    bestSellingProduct = salesSummary.bestSellingProductName,
                    isLoading = false
                )
            }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .catch {
                    _uiState.update { state ->
                        state.copy(isLoading = false, bakeryName = bakeryName)
                    }
                }
                .collect { dashboard ->
                    _uiState.value = dashboard
                }
        }
    }
}

class DashboardViewModelFactory(
    private val saleRepository: SaleRepository,
    private val productionRepository: ProductionRepository,
    private val ingredientRepository: IngredientRepository,
    private val wasteRepository: WasteRepository,
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val preferences: BakeFlowPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(
                saleRepository,
                productionRepository,
                ingredientRepository,
                wasteRepository,
                stockAdjustmentRepository,
                preferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
