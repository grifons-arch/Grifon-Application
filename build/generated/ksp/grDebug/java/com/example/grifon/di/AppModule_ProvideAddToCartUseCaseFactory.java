package com.example.grifon.di;

import com.example.grifon.data.repository.CartRepository;
import com.example.grifon.domain.usecase.AddToCartUseCase;
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
public final class AppModule_ProvideAddToCartUseCaseFactory implements Factory<AddToCartUseCase> {
  private final Provider<CartRepository> repoProvider;

  public AppModule_ProvideAddToCartUseCaseFactory(Provider<CartRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public AddToCartUseCase get() {
    return provideAddToCartUseCase(repoProvider.get());
  }

  public static AppModule_ProvideAddToCartUseCaseFactory create(
      Provider<CartRepository> repoProvider) {
    return new AppModule_ProvideAddToCartUseCaseFactory(repoProvider);
  }

  public static AddToCartUseCase provideAddToCartUseCase(CartRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAddToCartUseCase(repo));
  }
}
