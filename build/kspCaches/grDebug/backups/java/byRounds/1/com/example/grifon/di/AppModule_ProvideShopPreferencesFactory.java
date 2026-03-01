package com.example.grifon.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.example.grifon.data.local.ShopPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AppModule_ProvideShopPreferencesFactory implements Factory<ShopPreferences> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public AppModule_ProvideShopPreferencesFactory(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ShopPreferences get() {
    return provideShopPreferences(dataStoreProvider.get());
  }

  public static AppModule_ProvideShopPreferencesFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AppModule_ProvideShopPreferencesFactory(dataStoreProvider);
  }

  public static ShopPreferences provideShopPreferences(DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideShopPreferences(dataStore));
  }
}
