package com.example.grifon.di;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.repository.CatalogRepository;
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
public final class AppModule_ProvideCatalogRepositoryFactory implements Factory<CatalogRepository> {
  private final Provider<CatalogApi> catalogApiProvider;

  public AppModule_ProvideCatalogRepositoryFactory(Provider<CatalogApi> catalogApiProvider) {
    this.catalogApiProvider = catalogApiProvider;
  }

  @Override
  public CatalogRepository get() {
    return provideCatalogRepository(catalogApiProvider.get());
  }

  public static AppModule_ProvideCatalogRepositoryFactory create(
      Provider<CatalogApi> catalogApiProvider) {
    return new AppModule_ProvideCatalogRepositoryFactory(catalogApiProvider);
  }

  public static CatalogRepository provideCatalogRepository(CatalogApi catalogApi) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCatalogRepository(catalogApi));
  }
}
