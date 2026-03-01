package com.example.grifon.di;

import com.example.grifon.data.repository.BarcodeScannerService;
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
public final class AppModule_ProvideBarcodeScannerServiceFactory implements Factory<BarcodeScannerService> {
  @Override
  public BarcodeScannerService get() {
    return provideBarcodeScannerService();
  }

  public static AppModule_ProvideBarcodeScannerServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BarcodeScannerService provideBarcodeScannerService() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBarcodeScannerService());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideBarcodeScannerServiceFactory INSTANCE = new AppModule_ProvideBarcodeScannerServiceFactory();
  }
}
