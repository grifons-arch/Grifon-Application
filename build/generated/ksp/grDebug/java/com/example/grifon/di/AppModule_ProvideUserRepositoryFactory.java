package com.example.grifon.di;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.local.UserPreferences;
import com.example.grifon.data.repository.UserRepository;
import com.squareup.moshi.Moshi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideUserRepositoryFactory implements Factory<UserRepository> {
  private final Provider<CatalogApi> catalogApiProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  private final Provider<Moshi> moshiProvider;

  public AppModule_ProvideUserRepositoryFactory(Provider<CatalogApi> catalogApiProvider,
      Provider<UserPreferences> userPreferencesProvider, Provider<Moshi> moshiProvider) {
    this.catalogApiProvider = catalogApiProvider;
    this.userPreferencesProvider = userPreferencesProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public UserRepository get() {
    return provideUserRepository(catalogApiProvider.get(), userPreferencesProvider.get(), moshiProvider.get());
  }

  public static AppModule_ProvideUserRepositoryFactory create(
      Provider<CatalogApi> catalogApiProvider, Provider<UserPreferences> userPreferencesProvider,
      Provider<Moshi> moshiProvider) {
    return new AppModule_ProvideUserRepositoryFactory(catalogApiProvider, userPreferencesProvider, moshiProvider);
  }

  public static UserRepository provideUserRepository(CatalogApi catalogApi,
      UserPreferences userPreferences, Moshi moshi) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUserRepository(catalogApi, userPreferences, moshi));
  }
}
