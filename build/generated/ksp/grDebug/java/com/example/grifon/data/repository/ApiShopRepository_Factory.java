package com.example.grifon.data.repository;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.local.ShopPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class ApiShopRepository_Factory implements Factory<ApiShopRepository> {
  private final Provider<ShopPreferences> preferencesProvider;

  private final Provider<CatalogApi> catalogApiProvider;

  public ApiShopRepository_Factory(Provider<ShopPreferences> preferencesProvider,
      Provider<CatalogApi> catalogApiProvider) {
    this.preferencesProvider = preferencesProvider;
    this.catalogApiProvider = catalogApiProvider;
  }

  @Override
  public ApiShopRepository get() {
    return newInstance(preferencesProvider.get(), catalogApiProvider.get());
  }

  public static ApiShopRepository_Factory create(Provider<ShopPreferences> preferencesProvider,
      Provider<CatalogApi> catalogApiProvider) {
    return new ApiShopRepository_Factory(preferencesProvider, catalogApiProvider);
  }

  public static ApiShopRepository newInstance(ShopPreferences preferences, CatalogApi catalogApi) {
    return new ApiShopRepository(preferences, catalogApi);
  }
}
