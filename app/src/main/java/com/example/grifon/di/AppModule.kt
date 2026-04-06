package com.example.grifon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.grifon.data.catalog.CatalogApi
import com.example.grifon.data.auth.AuthApi
import com.example.grifon.data.auth.UserRepositoryImpl
import com.example.grifon.data.auth.WholesaleApplicationRepositoryImpl
import com.example.grifon.data.local.AppDatabase
import com.example.grifon.data.local.CustomerDao
import com.example.grifon.data.local.FavoriteDao
import com.example.grifon.data.local.ProductDao
import com.example.grifon.data.local.RecentProductDao
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.local.WholesaleCustomerDao
import com.example.grifon.data.repository.*
import com.example.grifon.data.fake.*
import com.example.grifon.data.sync.LoginCustomerActivitySyncService
import com.example.grifon.domain.auth.RegisterRepository
import com.example.grifon.domain.auth.RegisterUseCase
import com.example.grifon.domain.auth.WholesaleApplicationRepository
import com.example.grifon.domain.auth.SubmitWholesaleApplicationUseCase
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
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "grifon.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideFavoriteDao(appDatabase: AppDatabase): FavoriteDao = appDatabase.favoriteDao()

    @Provides
    @Singleton
    fun provideRecentProductDao(appDatabase: AppDatabase): RecentProductDao = appDatabase.recentProductDao()

    @Provides
    @Singleton
    fun provideWholesaleCustomerDao(appDatabase: AppDatabase): WholesaleCustomerDao =
        appDatabase.wholesaleCustomerDao()

    @Provides
    @Singleton
    fun provideCustomerDao(appDatabase: AppDatabase): CustomerDao = appDatabase.customerDao()

    @Provides
    @Singleton
    fun provideProductDao(appDatabase: AppDatabase): ProductDao = appDatabase.productDao()

    @Provides
    @Singleton
    fun provideShopPreferences(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>
    ): ShopPreferences = ShopPreferences(context, dataStore)

    @Provides
    @Singleton
    fun provideShopRepository(
        preferences: ShopPreferences,
        catalogApi: CatalogApi,
    ): ShopRepository = ApiShopRepository(preferences, catalogApi)

    @Provides
    @Singleton
    fun provideCatalogRepository(
        catalogApi: CatalogApi,
        preferences: ShopPreferences,
        localPriceAccessService: com.example.grifon.data.local.LocalPriceAccessService,
    ): CatalogRepository =
        ApiCatalogRepository(catalogApi, preferences, localPriceAccessService)

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        favoriteDao: FavoriteDao,
        preferences: ShopPreferences,
        catalogApi: CatalogApi,
    ): FavoriteRepository = LocalFavoriteRepository(favoriteDao, preferences, catalogApi)

    @Provides
    @Singleton
    fun provideRecentProductRepository(
        recentProductDao: RecentProductDao,
        preferences: ShopPreferences,
        catalogApi: CatalogApi,
    ): RecentProductRepository = LocalRecentProductRepository(recentProductDao, preferences, catalogApi)

    @Provides
    @Singleton
    fun provideWholesaleCustomerRepository(
        appDatabase: AppDatabase,
        catalogApi: CatalogApi,
    ): WholesaleCustomerRepository = ApiWholesaleCustomerRepository(appDatabase, catalogApi)

    @Provides
    @Singleton
    fun provideCustomerRepository(
        appDatabase: AppDatabase,
        catalogApi: CatalogApi,
    ): CustomerRepository = ApiCustomerRepository(appDatabase, catalogApi)

    @Provides
    @Singleton
    fun provideProductRepository(
        appDatabase: AppDatabase,
        catalogApi: CatalogApi,
    ): ProductRepository = ApiProductRepository(appDatabase, catalogApi)

    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository = FakeCartRepository()

    // ΕΔΩ ΕΠΙΒΑΛΛΟΥΜΕ ΤΗΝ ΠΡΑΓΜΑΤΙΚΗ ΥΛΟΠΟΙΗΣΗ ΤΟΥ USER
    @Provides
    @Singleton
    fun provideUserRepository(
        authApi: AuthApi,
        preferences: ShopPreferences,
        loginCustomerActivitySyncService: LoginCustomerActivitySyncService,
        @ApplicationContext context: Context,
    ): UserRepository = UserRepositoryImpl(
        authApi,
        preferences,
        loginCustomerActivitySyncService,
        context,
    )

    @Provides
    @Singleton
    fun provideBarcodeScannerService(): BarcodeScannerService = FakeBarcodeScannerService()

    @Provides
    @Singleton
    fun provideRegisterRepository(
        authApi: AuthApi,
    ): RegisterRepository = com.example.grifon.data.auth.RegisterRepositoryImpl(authApi)

    @Provides
    @Singleton
    fun provideWholesaleApplicationRepository(
        authApi: AuthApi,
    ): WholesaleApplicationRepository = WholesaleApplicationRepositoryImpl(authApi)

    @Provides
    fun provideGetActiveShopUseCase(repo: ShopRepository) = GetActiveShopUseCase(repo)

    @Provides
    fun provideSetActiveShopUseCase(repo: ShopRepository) = SetActiveShopUseCase(repo)

    @Provides
    fun provideGetCategoryTreeUseCase(repo: CatalogRepository) = GetCategoryTreeUseCase(repo)

    @Provides
    fun provideGetCategoryFiltersUseCase(repo: CatalogRepository) = GetCategoryFiltersUseCase(repo)

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

    @Provides
    fun provideObserveFavoritesUseCase(repo: FavoriteRepository) = ObserveFavoritesUseCase(repo)

    @Provides
    fun provideObserveFavoriteStatusUseCase(repo: FavoriteRepository) = ObserveFavoriteStatusUseCase(repo)

    @Provides
    fun provideToggleFavoriteUseCase(repo: FavoriteRepository) = ToggleFavoriteUseCase(repo)

    @Provides
    fun provideObserveRecentProductsUseCase(repo: RecentProductRepository) = ObserveRecentProductsUseCase(repo)

    @Provides
    fun provideRecordRecentProductVisitUseCase(repo: RecentProductRepository) =
        RecordRecentProductVisitUseCase(repo)

    @Provides
    fun provideSyncWholesaleCustomersUseCase(repo: WholesaleCustomerRepository) =
        SyncWholesaleCustomersUseCase(repo)

    @Provides
    fun provideSyncCustomersUseCase(repo: CustomerRepository) =
        SyncCustomersUseCase(repo)

    @Provides
    fun provideSyncProductsUseCase(repo: ProductRepository) =
        SyncProductsUseCase(repo)

    @Provides
    fun provideRegisterUseCase(repo: RegisterRepository) =
        RegisterUseCase(repo)

    @Provides
    fun provideSubmitWholesaleApplicationUseCase(repo: WholesaleApplicationRepository) =
        SubmitWholesaleApplicationUseCase(repo)
}
