package com.example.grifon.data.catalog

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface CatalogApi {
    @GET("v1/shops")
    suspend fun getShops(): List<ShopDto>

    @GET("v1/wholesale-customers")
    suspend fun getWholesaleCustomers(
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
    ): WholesaleCustomersResponseDto

    @GET("v1/customers")
    suspend fun getCustomers(
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
    ): CustomersResponseDto

    @GET("v1/products")
    suspend fun getProducts(
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 1000,
        @Query("sort") sort: String = "[id_DESC]",
        @Query("search") search: String? = null,
        @Query("priceMin") priceMin: Double? = null,
        @Query("priceMax") priceMax: Double? = null,
        @Query("colors") colors: String? = null,
        @Query("attributes") attributes: String? = null,
        @Query("customerId") customerId: Int? = null,
    ): ProductsResponseDto

    @GET("v1/categories")
    suspend fun getCategories(
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 500,
    ): CategoriesResponseDto

    @GET("v1/categories/{categoryId}/products")
    suspend fun getCategoryProducts(
        @Path("categoryId") categoryId: Int,
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 1000,
        @Query("sort") sort: String = "[id_DESC]",
        @Query("search") search: String? = null,
        @Query("priceMin") priceMin: Double? = null,
        @Query("priceMax") priceMax: Double? = null,
        @Query("colors") colors: String? = null,
        @Query("attributes") attributes: String? = null,
        @Query("customerId") customerId: Int? = null,
    ): ProductsResponseDto

    @GET("v1/categories/{categoryId}/filters")
    suspend fun getCategoryFilters(
        @Path("categoryId") categoryId: Int,
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("customerId") customerId: Int? = null,
    ): CatalogFacetsResponseDto

    @GET("v1/products/{productId}")
    suspend fun getProduct(
        @Path("productId") productId: Int,
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("customerId") customerId: Int? = null,
    ): ProductDto

    @POST("v1/customer-activity/favorites")
    suspend fun syncFavoriteProduct(
        @Body request: ProductActivityRequestDto,
    ): ActivityResponseDto

    @POST("v1/customer-activity/recent-products")
    suspend fun syncRecentProduct(
        @Body request: ProductActivityRequestDto,
    ): ActivityResponseDto

    @GET("v1/customer-activity/favorites")
    suspend fun getFavoriteProducts(
        @Query("customerId") customerId: Int,
        @Query("shopId") shopId: Int,
    ): ProductActivityItemsResponseDto

    @GET("v1/customer-activity/recent-products")
    suspend fun getRecentProducts(
        @Query("customerId") customerId: Int,
        @Query("shopId") shopId: Int,
        @Query("limit") limit: Int = 20,
    ): ProductActivityItemsResponseDto

    @GET("v1/checkout/session")
    suspend fun getCheckoutSession(
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("customerId") customerId: Int? = null,
    ): CheckoutSessionDto

    @POST("v1/checkout/orders")
    suspend fun createCheckoutOrder(
        @Body request: CheckoutOrderRequestDto,
    ): CheckoutOrderResponseDto

    @GET("v1/checkout/orders/{orderReference}")
    suspend fun getCheckoutOrder(
        @Path("orderReference") orderReference: String,
    ): CheckoutOrderResponseDto

    @POST("v1/checkout/orders/{orderReference}/capture")
    suspend fun captureCheckoutOrder(
        @Path("orderReference") orderReference: String,
    ): CheckoutOrderResponseDto
}

@JsonClass(generateAdapter = true)
data class ShopDto(
    val id: Int,
    val code: String? = null,
)

@JsonClass(generateAdapter = true)
data class CategoriesResponseDto(
    val items: List<CategoryDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CatalogFacetsResponseDto(
    val items: List<CatalogFacetDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CatalogFacetDto(
    val key: String,
    val title: String,
    val type: String,
    val options: List<CatalogFacetOptionDto> = emptyList(),
    val minValue: Double? = null,
    val maxValue: Double? = null,
)

@JsonClass(generateAdapter = true)
data class CatalogFacetOptionDto(
    val key: String,
    val label: String,
    val count: Int,
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: Int,
    val name: String? = null,
    val parentId: Int? = null,
    val position: Int? = null,
    val active: Int? = null,
    val slug: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProductsResponseDto(
    val items: List<ProductDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WholesaleCustomersResponseDto(
    val items: List<WholesaleCustomerDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WholesaleCustomerDto(
    val customerId: Int,
    val shopId: Int,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val company: String? = null,
    val active: Boolean = false,
    val defaultGroupId: Int? = null,
    val defaultGroupName: String? = null,
    val wholesaleGroupIds: List<Int> = emptyList(),
    val wholesaleGroupNames: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CustomersResponseDto(
    val items: List<CustomerDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CustomerDto(
    val customerId: Int,
    val shopId: Int,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val company: String? = null,
    val active: Boolean = false,
    val defaultGroupId: Int? = null,
    val defaultGroupName: String? = null,
    val groupIds: List<Int> = emptyList(),
    val groupNames: List<String> = emptyList(),
    val wholesaleGroupIds: List<Int> = emptyList(),
    val wholesaleGroupNames: List<String> = emptyList(),
    val isWholesale: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    val id: Int,
    val name: String? = null,
    val price: Double? = null,
    val reference: String? = null,
    val attributes: Map<String, List<String>> = emptyMap(),
    val defaultImage: ImageDto? = null,
    val categories: List<CategoryItemDto>? = null,
)

@JsonClass(generateAdapter = true)
data class CategoryItemDto(
    val id: Int,
)

@JsonClass(generateAdapter = true)
data class ImageDto(
    val id: Int,
    val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProductActivityRequestDto(
    val customerId: Int,
    val shopId: Int,
    val productId: Int,
    val isFavorite: Boolean? = null,
    val product: ProductSnapshotDto? = null,
)

@JsonClass(generateAdapter = true)
data class ProductSnapshotDto(
    val title: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val imageUrl: String? = null,
    val brand: String? = null,
)

@JsonClass(generateAdapter = true)
data class ActivityResponseDto(
    val ok: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ProductActivityItemsResponseDto(
    val ok: Boolean = false,
    val items: List<ProductActivityItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CheckoutSessionDto(
    val shopId: Int,
    val customerId: Int? = null,
    val isLoggedIn: Boolean = false,
    val canViewPrices: Boolean = false,
    val canCheckout: Boolean = false,
    val mode: String? = null,
    val storefrontBaseUrl: String? = null,
    val cartUrl: String? = null,
    val checkoutUrl: String? = null,
    val loginUrl: String? = null,
    val paymentMethods: List<CheckoutMethodDto> = emptyList(),
    val shippingMethods: List<CheckoutMethodDto> = emptyList(),
    val notes: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CheckoutMethodDto(
    val code: String,
    val title: String,
    val type: String,
    val available: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class CheckoutOrderRequestDto(
    val shopId: Int,
    val lang: Int = 1,
    val customerId: Int,
    val paymentMethodCode: String,
    val shippingMethodCode: String,
    val address: CheckoutAddressDto,
    val items: List<CheckoutOrderItemDto>,
)

@JsonClass(generateAdapter = true)
data class CheckoutAddressDto(
    val recipient: String,
    val email: String,
    val phone: String,
    val company: String? = null,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
)

@JsonClass(generateAdapter = true)
data class CheckoutOrderItemDto(
    val productId: Int,
    val title: String,
    val qty: Int,
    val unitPrice: Double,
    val currency: String,
)

@JsonClass(generateAdapter = true)
data class CheckoutOrderResponseDto(
    val orderReference: String,
    val shopId: Int,
    val customerId: Int,
    val items: List<CheckoutOrderItemDto> = emptyList(),
    val totalAmount: Double,
    val currency: String,
    val paymentMethodCode: String,
    val shippingMethodCode: String,
    val paymentStatus: String,
    val orderStatus: String,
    val paymentProvider: String,
    val paymentSessionStatus: String,
    val paymentSessionMessage: String,
    val paymentSessionUrl: String? = null,
    val paymentSessionId: String? = null,
    val paymentCaptureId: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProductActivityItemDto(
    val productId: Int,
    val shopId: Int,
    val title: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val imageUrl: String? = null,
    val brand: String? = null,
    val updatedAt: Long? = null,
    val visitedAt: Long? = null,
)
