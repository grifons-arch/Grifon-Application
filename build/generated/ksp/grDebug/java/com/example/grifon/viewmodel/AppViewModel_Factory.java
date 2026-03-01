package com.example.grifon.viewmodel;

import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import com.example.grifon.domain.usecase.GetCartUseCase;
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
public final class AppViewModel_Factory implements Factory<AppViewModel> {
  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  private final Provider<GetCartUseCase> getCartUseCaseProvider;

  public AppViewModel_Factory(Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCartUseCase> getCartUseCaseProvider) {
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
    this.getCartUseCaseProvider = getCartUseCaseProvider;
  }

  @Override
  public AppViewModel get() {
    return newInstance(getActiveShopUseCaseProvider.get(), getCartUseCaseProvider.get());
  }

  public static AppViewModel_Factory create(
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<GetCartUseCase> getCartUseCaseProvider) {
    return new AppViewModel_Factory(getActiveShopUseCaseProvider, getCartUseCaseProvider);
  }

  public static AppViewModel newInstance(GetActiveShopUseCase getActiveShopUseCase,
      GetCartUseCase getCartUseCase) {
    return new AppViewModel(getActiveShopUseCase, getCartUseCase);
  }
}
