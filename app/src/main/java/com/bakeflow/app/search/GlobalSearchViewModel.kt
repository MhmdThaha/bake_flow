package com.bakeflow.app.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.PurchaseRepository
import com.bakeflow.app.domain.repository.SaleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchResultType(val label: String) {
    PRODUCT("Product"),
    INGREDIENT("Ingredient"),
    SALE("Sale"),
    PURCHASE("Purchase")
}

data class GlobalSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: SearchResultType,
    val navigateRoute: String
)

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<GlobalSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

private data class SearchIndexSnapshot(
    val products: List<GlobalSearchResult>,
    val ingredients: List<GlobalSearchResult>,
    val sales: List<GlobalSearchResult>,
    val purchases: List<GlobalSearchResult>
)

class GlobalSearchViewModel(
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private var products: List<GlobalSearchResult> = emptyList()
    private var ingredients: List<GlobalSearchResult> = emptyList()
    private var sales: List<GlobalSearchResult> = emptyList()
    private var purchases: List<GlobalSearchResult> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                productRepository.observeProducts(),
                ingredientRepository.observeIngredients(),
                saleRepository.observeSales(),
                purchaseRepository.observePurchases()
            ) { productList, ingredientList, saleList, purchaseList ->
                SearchIndexSnapshot(
                    products = productList.map {
                        GlobalSearchResult(
                            id = it.id,
                            title = it.name,
                            subtitle = "${it.category} · ${formatMoney(it.sellingPrice)}",
                            type = SearchResultType.PRODUCT,
                            navigateRoute = "products"
                        )
                    },
                    ingredients = ingredientList.map {
                        GlobalSearchResult(
                            id = it.id,
                            title = it.name,
                            subtitle = "Stock: ${formatQty(it.currentStock)} ${it.unit.displayName}",
                            type = SearchResultType.INGREDIENT,
                            navigateRoute = "inventory"
                        )
                    },
                    sales = saleList.map {
                        GlobalSearchResult(
                            id = it.saleId,
                            title = it.productName,
                            subtitle = "Sale · ${formatQty(it.quantity)} · ${formatMoney(it.totalAmount)}",
                            type = SearchResultType.SALE,
                            navigateRoute = "sales"
                        )
                    },
                    purchases = purchaseList.map {
                        GlobalSearchResult(
                            id = it.purchaseId,
                            title = it.ingredientName,
                            subtitle = "Purchase · ${it.supplierName.ifBlank { "—" }} · ${formatMoney(it.totalCost)}",
                            type = SearchResultType.PURCHASE,
                            navigateRoute = "inventory"
                        )
                    }
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { snapshot ->
                    products = snapshot.products
                    ingredients = snapshot.ingredients
                    sales = snapshot.sales
                    purchases = snapshot.purchases
                    applySearch(_uiState.value.query)
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        applySearch(query)
    }

    private fun applySearch(query: String) {
        val normalized = query.trim().lowercase()
        val all = products + ingredients + sales + purchases
        val filtered = if (normalized.isBlank()) {
            emptyList()
        } else {
            all.filter {
                it.title.lowercase().contains(normalized) ||
                    it.subtitle.lowercase().contains(normalized)
            }.take(20)
        }
        _uiState.update { it.copy(results = filtered, isSearching = normalized.isNotBlank()) }
    }

    private fun formatMoney(value: Double): String = String.format("$%.2f", value)

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.1f", value)
}

class GlobalSearchViewModelFactory(
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlobalSearchViewModel::class.java)) {
            return GlobalSearchViewModel(
                productRepository,
                ingredientRepository,
                saleRepository,
                purchaseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
