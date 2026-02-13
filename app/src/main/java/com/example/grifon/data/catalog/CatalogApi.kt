package com.example.grifon.data.catalog

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {
    @GET("v1/shops")
    suspend fun getShops(): List<ShopDto>

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
    ): ProductsResponseDto
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
data class CategoryDto(
    val id: Int,
    val name: String? = null,
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
)
