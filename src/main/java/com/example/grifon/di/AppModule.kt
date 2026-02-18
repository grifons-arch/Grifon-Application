package com.example.grifon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.grifon.BuildConfig
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.local.*
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build()

    @Provides
    @Singleton
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "grifon_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    @Singleton
    fun provideShopPreferences(@ApplicationContext context: Context): ShopPreferences =
        ShopPreferences(context.dataStore)

    @Provides
    @Singleton
    fun provideCatalogRepository(
        catalogApi: CatalogApi,
        productDao: ProductDao,
        categoryDao: CategoryDao
    ): CatalogRepository = ApiCatalogRepository(catalogApi, productDao, categoryDao)

    @Provides
    @Singleton
    fun provideShopRepository(
        preferences: ShopPreferences,
        catalogApi: CatalogApi
    ): ShopRepository = ApiShopRepository(preferences, catalogApi)

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = FakeUserRepository()

    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository = FakeCartRepository()

    @Provides
    @Singleton
    fun provideBarcodeScannerService(): BarcodeScannerService = FakeBarcodeScannerService()

    // ΠΡΟΣΘΗΚΗ ΟΛΩΝ ΤΩΝ USE CASES ΠΟΥ ΛΕΙΠΟΥΝ
    @Provides
    fun provideGetActiveShopUseCase(repo: ShopRepository) = GetActiveShopUseCase(repo)

    @Provides
    fun provideSetActiveShopUseCase(repo: ShopRepository) = SetActiveShopUseCase(repo)

    @Provides
    fun provideGetCategoryTreeUseCase(repo: CatalogRepository) = GetCategoryTreeUseCase(repo)

    @Provides
    fun provideSearchProductsUseCase(repo: CatalogRepository) = SearchProductsUseCase(repo)

    @Provides
    fun provideGetProductsByCategoryUseCase(repo: CatalogRepository) = GetProductsByCategoryUseCase(repo)

    @Provides
    fun provideGetProductByIdUseCase(repo: CatalogRepository) = GetProductByIdUseCase(repo)

    @Provides
    fun provideGetCartUseCase(repo: CartRepository) = GetCartUseCase(repo)

    @Provides
    fun provideAddToCartUseCase(repo: CartRepository) = AddToCartUseCase(repo)

    @Provides
    fun provideRemoveFromCartUseCase(repo: CartRepository) = RemoveFromCartUseCase(repo)

    @Provides
    fun provideApplyFiltersUseCase() = ApplyFiltersUseCase()
}
