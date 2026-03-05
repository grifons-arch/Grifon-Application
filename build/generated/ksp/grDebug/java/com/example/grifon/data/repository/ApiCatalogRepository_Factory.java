package com.example.grifon.data.repository;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.local.UserPreferences;
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
public final class ApiCatalogRepository_Factory implements Factory<ApiCatalogRepository> {
  private final Provider<CatalogApi> catalogApiProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  public ApiCatalogRepository_Factory(Provider<CatalogApi> catalogApiProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    this.catalogApiProvider = catalogApiProvider;
    this.userPreferencesProvider = userPreferencesProvider;
  }

  @Override
  public ApiCatalogRepository get() {
    return newInstance(catalogApiProvider.get(), userPreferencesProvider.get());
  }

  public static ApiCatalogRepository_Factory create(Provider<CatalogApi> catalogApiProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    return new ApiCatalogRepository_Factory(catalogApiProvider, userPreferencesProvider);
  }

  public static ApiCatalogRepository newInstance(CatalogApi catalogApi,
      UserPreferences userPreferences) {
    return new ApiCatalogRepository(catalogApi, userPreferences);
  }
}
