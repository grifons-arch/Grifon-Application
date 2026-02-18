package com.example.grifon.domain.usecase

import com.example.grifon.data.repository.CatalogRepository
import com.example.grifon.data.repository.CartRepository
import com.example.grifon.data.repository.ShopRepository
import com.example.grifon.domain.model.CartItem
import com.example.grifon.domain.model.FilterState
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
