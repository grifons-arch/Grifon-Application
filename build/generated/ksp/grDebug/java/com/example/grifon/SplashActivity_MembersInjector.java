package com.example.grifon;

import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SplashActivity_MembersInjector implements MembersInjector<SplashActivity> {
  private final Provider<CatalogApi> catalogApiProvider;

  private final Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider;

  public SplashActivity_MembersInjector(Provider<CatalogApi> catalogApiProvider,
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider) {
    this.catalogApiProvider = catalogApiProvider;
    this.getActiveShopUseCaseProvider = getActiveShopUseCaseProvider;
  }

  public static MembersInjector<SplashActivity> create(Provider<CatalogApi> catalogApiProvider,
      Provider<GetActiveShopUseCase> getActiveShopUseCaseProvider) {
    return new SplashActivity_MembersInjector(catalogApiProvider, getActiveShopUseCaseProvider);
  }

  @Override
  public void injectMembers(SplashActivity instance) {
    injectCatalogApi(instance, catalogApiProvider.get());
    injectGetActiveShopUseCase(instance, getActiveShopUseCaseProvider.get());
  }

  @InjectedFieldSignature("com.example.grifon.SplashActivity.catalogApi")
  public static void injectCatalogApi(SplashActivity instance, CatalogApi catalogApi) {
    instance.catalogApi = catalogApi;
  }

  @InjectedFieldSignature("com.example.grifon.SplashActivity.getActiveShopUseCase")
  public static void injectGetActiveShopUseCase(SplashActivity instance,
      GetActiveShopUseCase getActiveShopUseCase) {
    instance.getActiveShopUseCase = getActiveShopUseCase;
  }
}
