package com.example.grifon.viewmodel;

import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase;
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase;
import com.example.grifon.domain.usecase.SearchProductsUseCase;
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
public final class PlpViewModel_Factory implements Factory<PlpViewModel> {
  private final Provider<GetProductsByCategoryUseCase> getProductsByCategoryUseCaseProvider;

  private final Provider<SearchProductsUseCase> searchProductsUseCaseProvider;

  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  private final Provider<GetCategoryTreeUseCase> getCategoryTreeUseCaseProvider;

  public PlpViewModel_Factory(
      Provider<GetProductsByCategoryUseCase> getProductsByCategoryUseCaseProvider,
      Provider<SearchProductsUseCase> searchProductsUseCaseProvider,
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCategoryTreeUseCase> getCategoryTreeUseCaseProvider) {
    this.getProductsByCategoryUseCaseProvider = getProductsByCategoryUseCaseProvider;
    this.searchProductsUseCaseProvider = searchProductsUseCaseProvider;
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
    this.getCategoryTreeUseCaseProvider = getCategoryTreeUseCaseProvider;
  }

  @Override
  public PlpViewModel get() {
    return newInstance(getProductsByCategoryUseCaseProvider.get(), searchProductsUseCaseProvider.get(), getActiveShopUseCaseProvider.get(), getCategoryTreeUseCaseProvider.get());
  }

  public static PlpViewModel_Factory create(
      Provider<GetProductsByCategoryUseCase> getProductsByCategoryUseCaseProvider,
      Provider<SearchProductsUseCase> searchProductsUseCaseProvider,
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCategoryTreeUseCase> getCategoryTreeUseCaseProvider) {
    return new PlpViewModel_Factory(getProductsByCategoryUseCaseProvider, searchProductsUseCaseProvider, getActiveShopUseCaseProvider, getCategoryTreeUseCaseProvider);
  }

  public static PlpViewModel newInstance(GetProductsByCategoryUseCase getProductsByCategoryUseCase,
      SearchProductsUseCase searchProductsUseCase, GetActiveShopUseCase getActiveShopUseCase,
      GetCategoryTreeUseCase getCategoryTreeUseCase) {
    return new PlpViewModel(getProductsByCategoryUseCase, searchProductsUseCase, getActiveShopUseCase, getCategoryTreeUseCase);
  }
}
