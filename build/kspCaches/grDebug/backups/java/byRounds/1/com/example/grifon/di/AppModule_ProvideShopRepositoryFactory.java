package com.example.grifon.di;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.local.ShopPreferences;
import com.example.grifon.data.repository.ShopRepository;
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
public final class AppModule_ProvideShopRepositoryFactory implements Factory<ShopRepository> {
  private final Provider<ShopPreferences> preferencesProvider;

  private final Provider<CatalogApi> catalogApiProvider;

  public AppModule_ProvideShopRepositoryFactory(Provider<ShopPreferences> preferencesProvider,
      Provider<CatalogApi> catalogApiProvider) {
    this.preferencesProvider = preferencesProvider;
    this.catalogApiProvider = catalogApiProvider;
  }

  @Override
  public ShopRepository get() {
    return provideShopRepository(preferencesProvider.get(), catalogApiProvider.get());
  }

  public static AppModule_ProvideShopRepositoryFactory create(
      Provider<ShopPreferences> preferencesProvider, Provider<CatalogApi> catalogApiProvider) {
    return new AppModule_ProvideShopRepositoryFactory(preferencesProvider, catalogApiProvider);
  }

  public static ShopRepository provideShopRepository(ShopPreferences preferences,
      CatalogApi catalogApi) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideShopRepository(preferences, catalogApi));
  }
}
