package com.example.grifon.di;

import com.example.grifon.data.repository.CartRepository;
import com.example.grifon.domain.usecase.RemoveFromCartUseCase;
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
public final class AppModule_ProvideRemoveFromCartUseCaseFactory implements Factory<RemoveFromCartUseCase> {
  private final Provider<CartRepository> repoProvider;

  public AppModule_ProvideRemoveFromCartUseCaseFactory(Provider<CartRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public RemoveFromCartUseCase get() {
    return provideRemoveFromCartUseCase(repoProvider.get());
  }

  public static AppModule_ProvideRemoveFromCartUseCaseFactory create(
      Provider<CartRepository> repoProvider) {
    return new AppModule_ProvideRemoveFromCartUseCaseFactory(repoProvider);
  }

  public static RemoveFromCartUseCase provideRemoveFromCartUseCase(CartRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRemoveFromCartUseCase(repo));
  }
}
