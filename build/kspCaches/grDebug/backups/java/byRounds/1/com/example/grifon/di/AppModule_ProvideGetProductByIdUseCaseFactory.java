package com.example.grifon.di;

import com.example.grifon.data.repository.CatalogRepository;
import com.example.grifon.domain.usecase.GetProductByIdUseCase;
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
public final class AppModule_ProvideGetProductByIdUseCaseFactory implements Factory<GetProductByIdUseCase> {
  private final Provider<CatalogRepository> repoProvider;

  public AppModule_ProvideGetProductByIdUseCaseFactory(Provider<CatalogRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetProductByIdUseCase get() {
    return provideGetProductByIdUseCase(repoProvider.get());
  }

  public static AppModule_ProvideGetProductByIdUseCaseFactory create(
      Provider<CatalogRepository> repoProvider) {
    return new AppModule_ProvideGetProductByIdUseCaseFactory(repoProvider);
  }

  public static GetProductByIdUseCase provideGetProductByIdUseCase(CatalogRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGetProductByIdUseCase(repo));
  }
}
