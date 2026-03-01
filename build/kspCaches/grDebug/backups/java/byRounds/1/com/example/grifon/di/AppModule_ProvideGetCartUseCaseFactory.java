package com.example.grifon.di;

import com.example.grifon.data.repository.CartRepository;
import com.example.grifon.domain.usecase.GetCartUseCase;
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
public final class AppModule_ProvideGetCartUseCaseFactory implements Factory<GetCartUseCase> {
  private final Provider<CartRepository> repoProvider;

  public AppModule_ProvideGetCartUseCaseFactory(Provider<CartRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetCartUseCase get() {
    return provideGetCartUseCase(repoProvider.get());
  }

  public static AppModule_ProvideGetCartUseCaseFactory create(
      Provider<CartRepository> repoProvider) {
    return new AppModule_ProvideGetCartUseCaseFactory(repoProvider);
  }

  public static GetCartUseCase provideGetCartUseCase(CartRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGetCartUseCase(repo));
  }
}
