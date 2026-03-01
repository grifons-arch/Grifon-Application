package com.example.grifon.di;

import com.example.grifon.data.repository.CatalogRepository;
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase;
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
public final class AppModule_ProvideGetProductsByCategoryUseCaseFactory implements Factory<GetProductsByCategoryUseCase> {
  private final Provider<CatalogRepository> repoProvider;

  public AppModule_ProvideGetProductsByCategoryUseCaseFactory(
      Provider<CatalogRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetProductsByCategoryUseCase get() {
    return provideGetProductsByCategoryUseCase(repoProvider.get());
  }

  public static AppModule_ProvideGetProductsByCategoryUseCaseFactory create(
      Provider<CatalogRepository> repoProvider) {
    return new AppModule_ProvideGetProductsByCategoryUseCaseFactory(repoProvider);
  }

  public static GetProductsByCategoryUseCase provideGetProductsByCategoryUseCase(
      CatalogRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGetProductsByCategoryUseCase(repo));
  }
}
