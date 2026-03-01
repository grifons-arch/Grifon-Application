package com.example.grifon.viewmodel;

import com.example.grifon.data.repository.ShopRepository;
import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import com.example.grifon.domain.usecase.SetActiveShopUseCase;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<ShopRepository> shopRepositoryProvider;

  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  private final Provider<SetActiveShopUseCase> setActiveShopUseCaseProvider;

  public SettingsViewModel_Factory(Provider<ShopRepository> shopRepositoryProvider,
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<SetActiveShopUseCase> setActiveShopUseCaseProvider) {
    this.shopRepositoryProvider = shopRepositoryProvider;
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
    this.setActiveShopUseCaseProvider = setActiveShopUseCaseProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(shopRepositoryProvider.get(), getActiveShopUseCaseProvider.get(), setActiveShopUseCaseProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<ShopRepository> shopRepositoryProvider,
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider,
      Provider<SetActiveShopUseCase> setActiveShopUseCaseProvider) {
    return new SettingsViewModel_Factory(shopRepositoryProvider, getActiveShopUseCaseProvider, setActiveShopUseCaseProvider);
  }

  public static SettingsViewModel newInstance(ShopRepository shopRepository,
      GetActiveShopUseCase getActiveShopUseCase, SetActiveShopUseCase setActiveShopUseCase) {
    return new SettingsViewModel(shopRepository, getActiveShopUseCase, setActiveShopUseCase);
  }
}
