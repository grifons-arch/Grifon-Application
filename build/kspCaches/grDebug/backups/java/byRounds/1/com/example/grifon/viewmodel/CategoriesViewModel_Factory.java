package com.example.grifon.viewmodel;

import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase;
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
public final class CategoriesViewModel_Factory implements Factory<CategoriesViewModel> {
  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  private final Provider<GetCategoryTreeUseCase> getCategoryTreeUseCaseProvider;

  public CategoriesViewModel_Factory(Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCategoryTreeUseCase> getCategoryTreeUseCaseProvider) {
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
    this.getCategoryTreeUseCaseProvider = getCategoryTreeUseCaseProvider;
  }

  @Override
  public CategoriesViewModel get() {
    return newInstance(getActiveShopUseCaseProvider.get(), getCategoryTreeUseCaseProvider.get());
  }

  public static CategoriesViewModel_Factory create(
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCategoryTreeUseCase> getCategoryTreeUseCaseProvider) {
    return new CategoriesViewModel_Factory(getActiveShopUseCaseProvider, getCategoryTreeUseCaseProvider);
  }

  public static CategoriesViewModel newInstance(GetActiveShopUseCase getActiveShopUseCase,
      GetCategoryTreeUseCase getCategoryTreeUseCase) {
    return new CategoriesViewModel(getActiveShopUseCase, getCategoryTreeUseCase);
  }
}
