package com.example.grifon.domain.usecase

import com.example.grifon.data.repository.CatalogRepository
import com.example.grifon.data.repository.CartRepository
import com.example.grifon.data.repository.FavoriteRepository
import com.example.grifon.data.repository.OrderRepository
import com.example.grifon.data.repository.ProductRepository
import com.example.grifon.data.repository.RecentProductRepository
import com.example.grifon.data.repository.ShopRepository
import com.example.grifon.data.repository.CustomerRepository
import com.example.grifon.data.repository.WholesaleCustomerRepository
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.LocalOrder
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption

class GetActiveShopUseCase(private val shopRepository: ShopRepository) {
    operator fun invoke() = shopRepository.getActiveShopId()
}

class SetActiveShopUseCase(private val shopRepository: ShopRepository) {
    suspend operator fun invoke(shopId: String) = shopRepository.setActiveShopId(shopId)
}

class GetCategoryTreeUseCase(private val catalogRepository: CatalogRepository) {
    operator fun invoke(shopId: String) = catalogRepository.getCategoryTree(shopId)
}

class GetCategoryFiltersUseCase(private val catalogRepository: CatalogRepository) {
    operator fun invoke(shopId: String, categoryId: String) =
        catalogRepository.getCategoryFilters(shopId, categoryId)
}

class SearchProductsUseCase(private val catalogRepository: CatalogRepository) {
    operator fun invoke(shopId: String, query: String, filters: FilterState, sortOption: SortOption) =
        catalogRepository.searchProducts(shopId, query, filters, sortOption)
}

class GetProductsByCategoryUseCase(private val catalogRepository: CatalogRepository) {
    operator fun invoke(
        shopId: String,
        categoryId: String,
        filters: FilterState,
        sortOption: SortOption,
    ) = catalogRepository.getProductsByCategory(shopId, categoryId, filters, sortOption)
}

class GetProductByIdUseCase(private val catalogRepository: CatalogRepository) {
    operator fun invoke(shopId: String, productId: String) =
        catalogRepository.getProductById(shopId, productId)
}

class ApplyFiltersUseCase {
    operator fun invoke(filters: FilterState) = filters
}

class AddToCartUseCase(private val cartRepository: CartRepository) {
    suspend operator fun invoke(shopId: String, item: CartItem) = cartRepository.addToCart(shopId, item)
}

class RemoveFromCartUseCase(private val cartRepository: CartRepository) {
    suspend operator fun invoke(shopId: String, productId: String) =
        cartRepository.removeFromCart(shopId, productId)
}

class GetCartUseCase(private val cartRepository: CartRepository) {
    operator fun invoke(shopId: String) = cartRepository.observeCart(shopId)
}

class ClearCartUseCase(private val cartRepository: CartRepository) {
    suspend operator fun invoke(shopId: String) = cartRepository.clearCart(shopId)
}

class ObserveOrdersUseCase(private val orderRepository: OrderRepository) {
    operator fun invoke(shopId: String, customerId: Int?) =
        orderRepository.observeOrders(shopId, customerId)
}

class SaveOrderUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(order: LocalOrder) = orderRepository.saveOrder(order)
}

class ObserveFavoritesUseCase(private val favoriteRepository: FavoriteRepository) {
    operator fun invoke(shopId: String) = favoriteRepository.observeFavorites(shopId)
}

class ObserveFavoriteStatusUseCase(private val favoriteRepository: FavoriteRepository) {
    operator fun invoke(shopId: String, productId: String) =
        favoriteRepository.observeIsFavorite(shopId, productId)
}

class ToggleFavoriteUseCase(private val favoriteRepository: FavoriteRepository) {
    suspend operator fun invoke(shopId: String, product: Product) =
        favoriteRepository.toggleFavorite(shopId, product)
}

class ObserveRecentProductsUseCase(private val recentProductRepository: RecentProductRepository) {
    operator fun invoke(shopId: String, limit: Int = 10) =
        recentProductRepository.observeRecentProducts(shopId, limit)
}

class RecordRecentProductVisitUseCase(private val recentProductRepository: RecentProductRepository) {
    suspend operator fun invoke(shopId: String, product: Product) =
        recentProductRepository.recordVisit(shopId, product)
}

class SyncWholesaleCustomersUseCase(
    private val wholesaleCustomerRepository: WholesaleCustomerRepository
) {
    suspend operator fun invoke(shopId: String) =
        wholesaleCustomerRepository.syncWholesaleCustomers(shopId)
}

class SyncCustomersUseCase(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(shopId: String) = customerRepository.syncCustomers(shopId)
}

class SyncProductsUseCase(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(shopId: String) = productRepository.syncProducts(shopId)
}
