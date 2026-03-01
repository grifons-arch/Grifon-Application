package com.example.grifon.data.catalog;

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
public final class HomeProductsWebService_Factory implements Factory<HomeProductsWebService> {
  private final Provider<CatalogApi> catalogApiProvider;

  public HomeProductsWebService_Factory(Provider<CatalogApi> catalogApiProvider) {
    this.catalogApiProvider = catalogApiProvider;
  }

  @Override
  public HomeProductsWebService get() {
    return newInstance(catalogApiProvider.get());
  }

  public static HomeProductsWebService_Factory create(Provider<CatalogApi> catalogApiProvider) {
    return new HomeProductsWebService_Factory(catalogApiProvider);
  }

  public static HomeProductsWebService newInstance(CatalogApi catalogApi) {
    return new HomeProductsWebService(catalogApi);
  }
}
