package com.example.grifon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.local.UserPreferences
import com.example.grifon.data.repository.*
import com.example.grifon.data.fake.*
import com.example.grifon.domain.usecase.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.example.grifon.BuildConfig

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideShopPreferences(dataStore: DataStore<Preferences>): ShopPreferences =
        ShopPreferences(dataStore)

    @Provides
    @Singleton
    fun provideUserPreferences(dataStore: DataStore<Preferences>): UserPreferences =
        UserPreferences(dataStore)

    @Provides
    @Singleton
    fun provideShopRepository(
        preferences: ShopPreferences,
        catalogApi: CatalogApi,
    ): ShopRepository = ApiShopRepository(preferences, catalogApi)

    @Provides
    @Singleton
    fun provideCatalogRepository(catalogApi: CatalogApi): CatalogRepository = 
        ApiCatalogRepository(catalogApi)

    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository = FakeCartRepository()

    @Provides
    @Singleton
    fun provideUserRepository(
        catalogApi: CatalogApi,
        userPreferences: UserPreferences
    ): UserRepository = ApiUserRepository(catalogApi, userPreferences)

    @Provides
    @Singleton
    fun provideBarcodeScannerService(): BarcodeScannerService = FakeBarcodeScannerService()

    @Provides
    fun provideGetActiveShopUseCase(repo: ShopRepository) = GetActiveShopUseCase(repo)

    @Provides
    fun provideSetActiveShopUseCase(repo: ShopRepository) = SetActiveShopUseCase(repo)

    @Provides
    fun provideGetCategoryTreeUseCase(repo: CatalogRepository) = GetCategoryTreeUseCase(repo)

    @Provides
    fun provideSearchProductsUseCase(repo: CatalogRepository) = SearchProductsUseCase(repo)

    @Provides
    fun provideGetProductsByCategoryUseCase(repo: CatalogRepository) =
        GetProductsByCategoryUseCase(repo)

    @Provides
    fun provideGetProductByIdUseCase(repo: CatalogRepository) = GetProductByIdUseCase(repo)

    @Provides
    fun provideApplyFiltersUseCase() = ApplyFiltersUseCase()

    @Provides
    fun provideAddToCartUseCase(repo: CartRepository) = AddToCartUseCase(repo)

    @Provides
    fun provideRemoveFromCartUseCase(repo: CartRepository) = RemoveFromCartUseCase(repo)

    @Provides
    fun provideGetCartUseCase(repo: CartRepository) = GetCartUseCase(repo)
}
