package com.example.grifon.di;

import com.example.grifon.data.repository.CatalogRepository;
import com.example.grifon.domain.usecase.SearchProductsUseCase;
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
public final class AppModule_ProvideSearchProductsUseCaseFactory implements Factory<SearchProductsUseCase> {
  private final Provider<CatalogRepository> repoProvider;

  public AppModule_ProvideSearchProductsUseCaseFactory(Provider<CatalogRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public SearchProductsUseCase get() {
    return provideSearchProductsUseCase(repoProvider.get());
  }

  public static AppModule_ProvideSearchProductsUseCaseFactory create(
      Provider<CatalogRepository> repoProvider) {
    return new AppModule_ProvideSearchProductsUseCaseFactory(repoProvider);
  }

  public static SearchProductsUseCase provideSearchProductsUseCase(CatalogRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSearchProductsUseCase(repo));
  }
}
