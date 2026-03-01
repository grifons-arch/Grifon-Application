package com.example.grifon.di;

import com.example.grifon.data.repository.CartRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideCartRepositoryFactory implements Factory<CartRepository> {
  @Override
  public CartRepository get() {
    return provideCartRepository();
  }

  public static AppModule_ProvideCartRepositoryFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CartRepository provideCartRepository() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCartRepository());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideCartRepositoryFactory INSTANCE = new AppModule_ProvideCartRepositoryFactory();
  }
}
