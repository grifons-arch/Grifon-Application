package com.example.grifon.di;

import com.example.grifon.domain.usecase.ApplyFiltersUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideApplyFiltersUseCaseFactory implements Factory<ApplyFiltersUseCase> {
  @Override
  public ApplyFiltersUseCase get() {
    return provideApplyFiltersUseCase();
  }

  public static AppModule_ProvideApplyFiltersUseCaseFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ApplyFiltersUseCase provideApplyFiltersUseCase() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideApplyFiltersUseCase());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideApplyFiltersUseCaseFactory INSTANCE = new AppModule_ProvideApplyFiltersUseCaseFactory();
  }
}
