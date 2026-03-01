package com.example.grifon.viewmodel;

import com.example.grifon.domain.usecase.AddToCartUseCase;
import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import com.example.grifon.domain.usecase.GetProductByIdUseCase;
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
public final class PdpViewModel_Factory implements Factory<PdpViewModel> {
  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  private final Provider<GetProductByIdUseCase> getProductByIdUseCaseProvider;

  private final Provider<AddToCartUseCase> addToCartUseCaseProvider;

  public PdpViewModel_Factory(Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetProductByIdUseCase> getProductByIdUseCaseProvider,
      Provider<AddToCartUseCase> addToCartUseCaseProvider) {
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
    this.getProductByIdUseCaseProvider = getProductByIdUseCaseProvider;
    this.addToCartUseCaseProvider = addToCartUseCaseProvider;
  }

  @Override
  public PdpViewModel get() {
    return newInstance(getActiveShopUseCaseProvider.get(), getProductByIdUseCaseProvider.get(), addToCartUseCaseProvider.get());
  }

  public static PdpViewModel_Factory create(
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetProductByIdUseCase> getProductByIdUseCaseProvider,
      Provider<AddToCartUseCase> addToCartUseCaseProvider) {
    return new PdpViewModel_Factory(getActiveShopUseCaseProvider, getProductByIdUseCaseProvider, addToCartUseCaseProvider);
  }

  public static PdpViewModel newInstance(GetActiveShopUseCase getActiveShopUseCase,
      GetProductByIdUseCase getProductByIdUseCase, AddToCartUseCase addToCartUseCase) {
    return new PdpViewModel(getActiveShopUseCase, getProductByIdUseCase, addToCartUseCase);
  }
}
