package com.example.grifon.viewmodel;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.catalog.HomeProductsWebService;
import com.example.grifon.domain.usecase.GetActiveShopUseCase;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  private final Provider<HomeProductsWebService> homeProductsWebServiceProvider;

  private final Provider<CatalogApi> catalogApiProvider;

  public HomeViewModel_Factory(Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<HomeProductsWebService> homeProductsWebServiceProvider,
      Provider<CatalogApi> catalogApiProvider) {
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
    this.homeProductsWebServiceProvider = homeProductsWebServiceProvider;
    this.catalogApiProvider = catalogApiProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(getActiveShopUseCaseProvider.get(), homeProductsWebServiceProvider.get(), catalogApiProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<HomeProductsWebService> homeProductsWebServiceProvider,
      Provider<CatalogApi> catalogApiProvider) {
    return new HomeViewModel_Factory(getActiveShopUseCaseProvider, homeProductsWebServiceProvider, catalogApiProvider);
  }

  public static HomeViewModel newInstance(GetActiveShopUseCase getActiveShopUseCase,
      HomeProductsWebService homeProductsWebService, CatalogApi catalogApi) {
    return new HomeViewModel(getActiveShopUseCase, homeProductsWebService, catalogApi);
  }
}
