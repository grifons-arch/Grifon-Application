package com.example.grifon.viewmodel;

import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import com.example.grifon.domain.usecase.GetCartUseCase;
import com.example.grifon.domain.usecase.RemoveFromCartUseCase;
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
public final class CartViewModel_Factory implements Factory<CartViewModel> {
  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  private final Provider<GetCartUseCase> getCartUseCaseProvider;

  private final Provider<RemoveFromCartUseCase> removeFromCartUseCaseProvider;

  public CartViewModel_Factory(Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCartUseCase> getCartUseCaseProvider,
      Provider<RemoveFromCartUseCase> removeFromCartUseCaseProvider) {
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
    this.getCartUseCaseProvider = getCartUseCaseProvider;
    this.removeFromCartUseCaseProvider = removeFromCartUseCaseProvider;
  }

  @Override
  public CartViewModel get() {
    return newInstance(getActiveShopUseCaseProvider.get(), getCartUseCaseProvider.get(), removeFromCartUseCaseProvider.get());
  }

  public static CartViewModel_Factory create(
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCartUseCase> getCartUseCaseProvider,
      Provider<RemoveFromCartUseCase> removeFromCartUseCaseProvider) {
    return new CartViewModel_Factory(getActiveShopUseCaseProvider, getCartUseCaseProvider, removeFromCartUseCaseProvider);
  }

  public static CartViewModel newInstance(GetActiveShopUseCase getActiveShopUseCase,
      GetCartUseCase getCartUseCase, RemoveFromCartUseCase removeFromCartUseCase) {
    return new CartViewModel(getActiveShopUseCase, getCartUseCase, removeFromCartUseCase);
  }
}
