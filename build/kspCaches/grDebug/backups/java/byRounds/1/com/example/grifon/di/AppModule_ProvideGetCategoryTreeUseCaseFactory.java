package com.example.grifon.di;

import com.example.grifon.data.repository.CatalogRepository;
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase;
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
public final class AppModule_ProvideGetCategoryTreeUseCaseFactory implements Factory<GetCategoryTreeUseCase> {
  private final Provider<CatalogRepository> repoProvider;

  public AppModule_ProvideGetCategoryTreeUseCaseFactory(Provider<CatalogRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetCategoryTreeUseCase get() {
    return provideGetCategoryTreeUseCase(repoProvider.get());
  }

  public static AppModule_ProvideGetCategoryTreeUseCaseFactory create(
      Provider<CatalogRepository> repoProvider) {
    return new AppModule_ProvideGetCategoryTreeUseCaseFactory(repoProvider);
  }

  public static GetCategoryTreeUseCase provideGetCategoryTreeUseCase(CatalogRepository repo) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGetCategoryTreeUseCase(repo));
  }
}
