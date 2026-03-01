package com.example.grifon.di;

import com.example.grifon.data.repository.ShopRepository;
import com.example.grifon.domain.usecase.SetActiveShopUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideSetActiveShopUseCaseFactory implements Factory<SetActiveShopUseCase> {
  private final Provider<ShopRepository> repoProvider;

  public AppModule_ProvideSetActiveShopUseCaseFactory(Provider<ShopRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public SetActiveShopUseCase get() {
    return provideSetActiveShopUseCase(repoProvider.get());
  }

  public static AppModule_ProvideSetActiveShopUseCaseFactory create(
      Provider<ShopRepository> repoProvider) {
    return new AppModule_ProvideSetActiveShopUseCaseFactory(repoProvider);
  }

  public static SetActiveShopUseCase provideSetActiveShopUseCase(ShopRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSetActiveShopUseCase(repo));
  }
}
