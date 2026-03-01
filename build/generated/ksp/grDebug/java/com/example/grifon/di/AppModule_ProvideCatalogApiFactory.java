package com.example.grifon.di;

import com.example.grifon.data.catalog.CatalogApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class AppModule_ProvideCatalogApiFactory implements Factory<CatalogApi> {
  private final Provider<Retrofit> retrofitProvider;

  public AppModule_ProvideCatalogApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public CatalogApi get() {
    return provideCatalogApi(retrofitProvider.get());
  }

  public static AppModule_ProvideCatalogApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new AppModule_ProvideCatalogApiFactory(retrofitProvider);
  }

  public static CatalogApi provideCatalogApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCatalogApi(retrofit));
  }
}
