package com.example.grifon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.grifon.data.fake.FakeBarcodeScannerService
import com.example.grifon.data.fake.FakeCartRepository
import com.example.grifon.data.fake.FakeCatalogRepository
import com.example.grifon.data.fake.FakeShopRepository
import com.example.grifon.data.fake.FakeUserRepository
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.data.repository.BarcodeScannerService
import com.example.grifon.data.repository.CatalogRepository
import com.example.grifon.data.repository.CartRepository
import com.example.grifon.data.repository.ShopRepository
import com.example.grifon.data.repository.UserRepository
import com.example.grifon.domain.usecase.AddToCartUseCase
import com.example.grifon.domain.usecase.ApplyFiltersUseCase
import com.example.grifon.domain.usecase.GetActiveShopUseCase
import com.example.grifon.domain.usecase.GetCartUseCase
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase
import com.example.grifon.domain.usecase.GetProductByIdUseCase
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase
import com.example.grifon.domain.usecase.RemoveFromCartUseCase
import com.example.grifon.domain.usecase.SearchProductsUseCase
import com.example.grifon.domain.usecase.SetActiveShopUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
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
    fun provideShopRepository(preferences: ShopPreferences): ShopRepository =
        FakeShopRepository(preferences)

    @Provides
    @Singleton
    fun provideCatalogRepository(): CatalogRepository = FakeCatalogRepository()

    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository = FakeCartRepository()

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = FakeUserRepository()

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
