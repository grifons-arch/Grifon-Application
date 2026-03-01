package com.example.grifon.data.repository;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.local.UserPreferences;
import com.squareup.moshi.Moshi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ApiUserRepository_Factory implements Factory<ApiUserRepository> {
  private final Provider<CatalogApi> catalogApiProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  private final Provider<Moshi> moshiProvider;

  public ApiUserRepository_Factory(Provider<CatalogApi> catalogApiProvider,
      Provider<UserPreferences> userPreferencesProvider, Provider<Moshi> moshiProvider) {
    this.catalogApiProvider = catalogApiProvider;
    this.userPreferencesProvider = userPreferencesProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public ApiUserRepository get() {
    return newInstance(catalogApiProvider.get(), userPreferencesProvider.get(), moshiProvider.get());
  }

  public static ApiUserRepository_Factory create(Provider<CatalogApi> catalogApiProvider,
      Provider<UserPreferences> userPreferencesProvider, Provider<Moshi> moshiProvider) {
    return new ApiUserRepository_Factory(catalogApiProvider, userPreferencesProvider, moshiProvider);
  }

  public static ApiUserRepository newInstance(CatalogApi catalogApi,
      UserPreferences userPreferences, Moshi moshi) {
    return new ApiUserRepository(catalogApi, userPreferences, moshi);
  }
}
