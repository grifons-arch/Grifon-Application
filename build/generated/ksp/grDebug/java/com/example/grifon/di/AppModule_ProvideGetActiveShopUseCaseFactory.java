package com.example.grifon.di;

import com.example.grifon.data.repository.ShopRepository;
import com.example.grifon.domain.usecase.GetActiveShopUseCase;
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
public final class AppModule_ProvideGetActiveShopUseCaseFactory implements Factory<GetActiveShopUseCase> {
  private final Provider<ShopRepository> repoProvider;

  public AppModule_ProvideGetActiveShopUseCaseFactory(Provider<ShopRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetActiveShopUseCase get() {
    return provideGetActiveShopUseCase(repoProvider.get());
  }

  public static AppModule_ProvideGetActiveShopUseCaseFactory create(
      Provider<ShopRepository> repoProvider) {
    return new AppModule_ProvideGetActiveShopUseCaseFactory(repoProvider);
  }

  public static GetActiveShopUseCase provideGetActiveShopUseCase(ShopRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGetActiveShopUseCase(repo));
  }
}
