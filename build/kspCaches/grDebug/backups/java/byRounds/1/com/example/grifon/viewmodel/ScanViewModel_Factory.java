package com.example.grifon.viewmodel;

import com.example.grifon.data.repository.BarcodeScannerService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ScanViewModel_Factory implements Factory<ScanViewModel> {
  private final Provider<BarcodeScannerService> scannerServiceProvider;

  public ScanViewModel_Factory(Provider<BarcodeScannerService> scannerServiceProvider) {
    this.scannerServiceProvider = scannerServiceProvider;
  }

  @Override
  public ScanViewModel get() {
    return newInstance(scannerServiceProvider.get());
  }

  public static ScanViewModel_Factory create(
      Provider<BarcodeScannerService> scannerServiceProvider) {
    return new ScanViewModel_Factory(scannerServiceProvider);
  }

  public static ScanViewModel newInstance(BarcodeScannerService scannerService) {
    return new ScanViewModel(scannerService);
  }
}
