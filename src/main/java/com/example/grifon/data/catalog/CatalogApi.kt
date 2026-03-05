package com.example.grifon.data.catalog

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {
    @GET("v1/shops")
    suspend fun getShops(): List<ShopDto>

    @GET("v1/products")
    suspend fun getProducts(
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("sort") sort: String = "[id_DESC]",
        @Query("search") search: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("inStockOnly") inStockOnly: Boolean? = null,
    ): ProductsResponseDto

    @GET("v1/categories")
    suspend fun getCategories(
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
    ): CategoriesResponseDto

    @GET("v1/categories/{categoryId}/products")
    suspend fun getCategoryProducts(
        @Path("categoryId") categoryId: Int,
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
        @Query("sort") sort: String = "[id_DESC]",
        @Query("search") search: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("inStockOnly") inStockOnly: Boolean? = null,
    ): ProductsResponseDto

    @GET("v1/products/{productId}")
    suspend fun getProductById(
        @Path("productId") productId: Int,
        @Query("shopId") shopId: Int,
        @Query("lang") lang: Int = 1,
    ): ProductDto

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto
}

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    val token: String? = null,
    val customerId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)

@JsonClass(generateAdapter = true)
data class ErrorResponseDto(
    val error: ErrorDetailDto
)

@JsonClass(generateAdapter = true)
data class ErrorDetailDto(
    val code: String,
    val message: String,
    val details: Any? = null
)

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
data class CategoryDto(
    val id: Int,
    val name: String? = null,
    val parentId: Int? = null,
    val childrenCount: Int? = null,
)

@JsonClass(generateAdapter = true)
data class ProductsResponseDto(
    val items: List<ProductDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    val id: Int,
    val name: String? = null,
    val price: Double? = null,
    val reference: String? = null,
    val brand: String? = null,
    val quantity: Int? = null,
    val inStock: Boolean? = null,
    val defaultImage: ImageDto? = null,
    val images: List<ImageDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ImageDto(
    val id: Int,
    val url: String? = null,
)
