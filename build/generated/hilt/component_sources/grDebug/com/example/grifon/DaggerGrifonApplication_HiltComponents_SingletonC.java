package com.example.grifon;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.grifon.data.catalog.CatalogApi;
import com.example.grifon.data.catalog.HomeProductsWebService;
import com.example.grifon.data.local.ShopPreferences;
import com.example.grifon.data.local.UserPreferences;
import com.example.grifon.data.repository.BarcodeScannerService;
import com.example.grifon.data.repository.CartRepository;
import com.example.grifon.data.repository.CatalogRepository;
import com.example.grifon.data.repository.ShopRepository;
import com.example.grifon.data.repository.UserRepository;
import com.example.grifon.di.AppModule_ProvideAddToCartUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideBarcodeScannerServiceFactory;
import com.example.grifon.di.AppModule_ProvideCartRepositoryFactory;
import com.example.grifon.di.AppModule_ProvideCatalogApiFactory;
import com.example.grifon.di.AppModule_ProvideCatalogRepositoryFactory;
import com.example.grifon.di.AppModule_ProvideDataStoreFactory;
import com.example.grifon.di.AppModule_ProvideGetActiveShopUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideGetCartUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideGetCategoryTreeUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideGetProductByIdUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideGetProductsByCategoryUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideMoshiFactory;
import com.example.grifon.di.AppModule_ProvideOkHttpClientFactory;
import com.example.grifon.di.AppModule_ProvideRemoveFromCartUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideRetrofitFactory;
import com.example.grifon.di.AppModule_ProvideSearchProductsUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideSetActiveShopUseCaseFactory;
import com.example.grifon.di.AppModule_ProvideShopPreferencesFactory;
import com.example.grifon.di.AppModule_ProvideShopRepositoryFactory;
import com.example.grifon.di.AppModule_ProvideUserPreferencesFactory;
import com.example.grifon.di.AppModule_ProvideUserRepositoryFactory;
import com.example.grifon.domain.usecase.AddToCartUseCase;
import com.example.grifon.domain.usecase.GetActiveShopUseCase;
import com.example.grifon.domain.usecase.GetCartUseCase;
import com.example.grifon.domain.usecase.GetCategoryTreeUseCase;
import com.example.grifon.domain.usecase.GetProductByIdUseCase;
import com.example.grifon.domain.usecase.GetProductsByCategoryUseCase;
import com.example.grifon.domain.usecase.RemoveFromCartUseCase;
import com.example.grifon.domain.usecase.SearchProductsUseCase;
import com.example.grifon.domain.usecase.SetActiveShopUseCase;
import com.example.grifon.viewmodel.AccountViewModel;
import com.example.grifon.viewmodel.AccountViewModel_HiltModules;
import com.example.grifon.viewmodel.AppViewModel;
import com.example.grifon.viewmodel.AppViewModel_HiltModules;
import com.example.grifon.viewmodel.CartViewModel;
import com.example.grifon.viewmodel.CartViewModel_HiltModules;
import com.example.grifon.viewmodel.CategoriesViewModel;
import com.example.grifon.viewmodel.CategoriesViewModel_HiltModules;
import com.example.grifon.viewmodel.HomeViewModel;
import com.example.grifon.viewmodel.HomeViewModel_HiltModules;
import com.example.grifon.viewmodel.PdpViewModel;
import com.example.grifon.viewmodel.PdpViewModel_HiltModules;
import com.example.grifon.viewmodel.PlpViewModel;
import com.example.grifon.viewmodel.PlpViewModel_HiltModules;
import com.example.grifon.viewmodel.ScanViewModel;
import com.example.grifon.viewmodel.ScanViewModel_HiltModules;
import com.example.grifon.viewmodel.SettingsViewModel;
import com.example.grifon.viewmodel.SettingsViewModel_HiltModules;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.squareup.moshi.Moshi;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerGrifonApplication_HiltComponents_SingletonC {
  private DaggerGrifonApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public GrifonApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements GrifonApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public GrifonApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements GrifonApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public GrifonApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements GrifonApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public GrifonApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements GrifonApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public GrifonApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements GrifonApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public GrifonApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements GrifonApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public GrifonApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements GrifonApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public GrifonApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends GrifonApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends GrifonApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends GrifonApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends GrifonApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public void injectSplashActivity(SplashActivity splashActivity) {
      injectSplashActivity2(splashActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(9).put(LazyClassKeyProvider.com_example_grifon_viewmodel_AccountViewModel, AccountViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_AppViewModel, AppViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_CartViewModel, CartViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_CategoriesViewModel, CategoriesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_PdpViewModel, PdpViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_PlpViewModel, PlpViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_ScanViewModel, ScanViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_grifon_viewmodel_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private SplashActivity injectSplashActivity2(SplashActivity instance) {
      SplashActivity_MembersInjector.injectCatalogApi(instance, singletonCImpl.provideCatalogApiProvider.get());
      SplashActivity_MembersInjector.injectGetActiveShopUseCase(instance, singletonCImpl.getActiveShopUseCase());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_example_grifon_viewmodel_AccountViewModel = "com.example.grifon.viewmodel.AccountViewModel";

      static String com_example_grifon_viewmodel_CategoriesViewModel = "com.example.grifon.viewmodel.CategoriesViewModel";

      static String com_example_grifon_viewmodel_PlpViewModel = "com.example.grifon.viewmodel.PlpViewModel";

      static String com_example_grifon_viewmodel_HomeViewModel = "com.example.grifon.viewmodel.HomeViewModel";

      static String com_example_grifon_viewmodel_CartViewModel = "com.example.grifon.viewmodel.CartViewModel";

      static String com_example_grifon_viewmodel_AppViewModel = "com.example.grifon.viewmodel.AppViewModel";

      static String com_example_grifon_viewmodel_SettingsViewModel = "com.example.grifon.viewmodel.SettingsViewModel";

      static String com_example_grifon_viewmodel_ScanViewModel = "com.example.grifon.viewmodel.ScanViewModel";

      static String com_example_grifon_viewmodel_PdpViewModel = "com.example.grifon.viewmodel.PdpViewModel";

      @KeepFieldType
      AccountViewModel com_example_grifon_viewmodel_AccountViewModel2;

      @KeepFieldType
      CategoriesViewModel com_example_grifon_viewmodel_CategoriesViewModel2;

      @KeepFieldType
      PlpViewModel com_example_grifon_viewmodel_PlpViewModel2;

      @KeepFieldType
      HomeViewModel com_example_grifon_viewmodel_HomeViewModel2;

      @KeepFieldType
      CartViewModel com_example_grifon_viewmodel_CartViewModel2;

      @KeepFieldType
      AppViewModel com_example_grifon_viewmodel_AppViewModel2;

      @KeepFieldType
      SettingsViewModel com_example_grifon_viewmodel_SettingsViewModel2;

      @KeepFieldType
      ScanViewModel com_example_grifon_viewmodel_ScanViewModel2;

      @KeepFieldType
      PdpViewModel com_example_grifon_viewmodel_PdpViewModel2;
    }
  }

  private static final class ViewModelCImpl extends GrifonApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AccountViewModel> accountViewModelProvider;

    private Provider<AppViewModel> appViewModelProvider;

    private Provider<CartViewModel> cartViewModelProvider;

    private Provider<CategoriesViewModel> categoriesViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<PdpViewModel> pdpViewModelProvider;

    private Provider<PlpViewModel> plpViewModelProvider;

    private Provider<ScanViewModel> scanViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private HomeProductsWebService homeProductsWebService() {
      return new HomeProductsWebService(singletonCImpl.provideCatalogApiProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.accountViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.appViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.cartViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.categoriesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.pdpViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.plpViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.scanViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(9).put(LazyClassKeyProvider.com_example_grifon_viewmodel_AccountViewModel, ((Provider) accountViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_AppViewModel, ((Provider) appViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_CartViewModel, ((Provider) cartViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_CategoriesViewModel, ((Provider) categoriesViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_PdpViewModel, ((Provider) pdpViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_PlpViewModel, ((Provider) plpViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_ScanViewModel, ((Provider) scanViewModelProvider)).put(LazyClassKeyProvider.com_example_grifon_viewmodel_SettingsViewModel, ((Provider) settingsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_example_grifon_viewmodel_CartViewModel = "com.example.grifon.viewmodel.CartViewModel";

      static String com_example_grifon_viewmodel_HomeViewModel = "com.example.grifon.viewmodel.HomeViewModel";

      static String com_example_grifon_viewmodel_PlpViewModel = "com.example.grifon.viewmodel.PlpViewModel";

      static String com_example_grifon_viewmodel_ScanViewModel = "com.example.grifon.viewmodel.ScanViewModel";

      static String com_example_grifon_viewmodel_CategoriesViewModel = "com.example.grifon.viewmodel.CategoriesViewModel";

      static String com_example_grifon_viewmodel_PdpViewModel = "com.example.grifon.viewmodel.PdpViewModel";

      static String com_example_grifon_viewmodel_AccountViewModel = "com.example.grifon.viewmodel.AccountViewModel";

      static String com_example_grifon_viewmodel_AppViewModel = "com.example.grifon.viewmodel.AppViewModel";

      static String com_example_grifon_viewmodel_SettingsViewModel = "com.example.grifon.viewmodel.SettingsViewModel";

      @KeepFieldType
      CartViewModel com_example_grifon_viewmodel_CartViewModel2;

      @KeepFieldType
      HomeViewModel com_example_grifon_viewmodel_HomeViewModel2;

      @KeepFieldType
      PlpViewModel com_example_grifon_viewmodel_PlpViewModel2;

      @KeepFieldType
      ScanViewModel com_example_grifon_viewmodel_ScanViewModel2;

      @KeepFieldType
      CategoriesViewModel com_example_grifon_viewmodel_CategoriesViewModel2;

      @KeepFieldType
      PdpViewModel com_example_grifon_viewmodel_PdpViewModel2;

      @KeepFieldType
      AccountViewModel com_example_grifon_viewmodel_AccountViewModel2;

      @KeepFieldType
      AppViewModel com_example_grifon_viewmodel_AppViewModel2;

      @KeepFieldType
      SettingsViewModel com_example_grifon_viewmodel_SettingsViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.example.grifon.viewmodel.AccountViewModel 
          return (T) new AccountViewModel(singletonCImpl.provideUserRepositoryProvider.get());

          case 1: // com.example.grifon.viewmodel.AppViewModel 
          return (T) new AppViewModel(singletonCImpl.getActiveShopUseCase(), singletonCImpl.getCartUseCase());

          case 2: // com.example.grifon.viewmodel.CartViewModel 
          return (T) new CartViewModel(singletonCImpl.getActiveShopUseCase(), singletonCImpl.getCartUseCase(), singletonCImpl.removeFromCartUseCase());

          case 3: // com.example.grifon.viewmodel.CategoriesViewModel 
          return (T) new CategoriesViewModel(singletonCImpl.getActiveShopUseCase(), singletonCImpl.getCategoryTreeUseCase());

          case 4: // com.example.grifon.viewmodel.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.getActiveShopUseCase(), viewModelCImpl.homeProductsWebService(), singletonCImpl.provideCatalogApiProvider.get());

          case 5: // com.example.grifon.viewmodel.PdpViewModel 
          return (T) new PdpViewModel(singletonCImpl.getActiveShopUseCase(), singletonCImpl.getProductByIdUseCase(), singletonCImpl.addToCartUseCase());

          case 6: // com.example.grifon.viewmodel.PlpViewModel 
          return (T) new PlpViewModel(singletonCImpl.getProductsByCategoryUseCase(), singletonCImpl.searchProductsUseCase(), singletonCImpl.getActiveShopUseCase(), singletonCImpl.getCategoryTreeUseCase());

          case 7: // com.example.grifon.viewmodel.ScanViewModel 
          return (T) new ScanViewModel(singletonCImpl.provideBarcodeScannerServiceProvider.get());

          case 8: // com.example.grifon.viewmodel.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideShopRepositoryProvider.get(), singletonCImpl.getActiveShopUseCase(), singletonCImpl.setActiveShopUseCase());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends GrifonApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends GrifonApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends GrifonApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Moshi> provideMoshiProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<CatalogApi> provideCatalogApiProvider;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<ShopPreferences> provideShopPreferencesProvider;

    private Provider<ShopRepository> provideShopRepositoryProvider;

    private Provider<UserPreferences> provideUserPreferencesProvider;

    private Provider<UserRepository> provideUserRepositoryProvider;

    private Provider<CartRepository> provideCartRepositoryProvider;

    private Provider<CatalogRepository> provideCatalogRepositoryProvider;

    private Provider<BarcodeScannerService> provideBarcodeScannerServiceProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private GetActiveShopUseCase getActiveShopUseCase() {
      return AppModule_ProvideGetActiveShopUseCaseFactory.provideGetActiveShopUseCase(provideShopRepositoryProvider.get());
    }

    private GetCartUseCase getCartUseCase() {
      return AppModule_ProvideGetCartUseCaseFactory.provideGetCartUseCase(provideCartRepositoryProvider.get());
    }

    private RemoveFromCartUseCase removeFromCartUseCase() {
      return AppModule_ProvideRemoveFromCartUseCaseFactory.provideRemoveFromCartUseCase(provideCartRepositoryProvider.get());
    }

    private GetCategoryTreeUseCase getCategoryTreeUseCase() {
      return AppModule_ProvideGetCategoryTreeUseCaseFactory.provideGetCategoryTreeUseCase(provideCatalogRepositoryProvider.get());
    }

    private GetProductByIdUseCase getProductByIdUseCase() {
      return AppModule_ProvideGetProductByIdUseCaseFactory.provideGetProductByIdUseCase(provideCatalogRepositoryProvider.get());
    }

    private AddToCartUseCase addToCartUseCase() {
      return AppModule_ProvideAddToCartUseCaseFactory.provideAddToCartUseCase(provideCartRepositoryProvider.get());
    }

    private GetProductsByCategoryUseCase getProductsByCategoryUseCase() {
      return AppModule_ProvideGetProductsByCategoryUseCaseFactory.provideGetProductsByCategoryUseCase(provideCatalogRepositoryProvider.get());
    }

    private SearchProductsUseCase searchProductsUseCase() {
      return AppModule_ProvideSearchProductsUseCaseFactory.provideSearchProductsUseCase(provideCatalogRepositoryProvider.get());
    }

    private SetActiveShopUseCase setActiveShopUseCase() {
      return AppModule_ProvideSetActiveShopUseCaseFactory.provideSetActiveShopUseCase(provideShopRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 2));
      this.provideMoshiProvider = DoubleCheck.provider(new SwitchingProvider<Moshi>(singletonCImpl, 3));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 1));
      this.provideCatalogApiProvider = DoubleCheck.provider(new SwitchingProvider<CatalogApi>(singletonCImpl, 0));
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 6));
      this.provideShopPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<ShopPreferences>(singletonCImpl, 5));
      this.provideShopRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ShopRepository>(singletonCImpl, 4));
      this.provideUserPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<UserPreferences>(singletonCImpl, 8));
      this.provideUserRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserRepository>(singletonCImpl, 7));
      this.provideCartRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CartRepository>(singletonCImpl, 9));
      this.provideCatalogRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CatalogRepository>(singletonCImpl, 10));
      this.provideBarcodeScannerServiceProvider = DoubleCheck.provider(new SwitchingProvider<BarcodeScannerService>(singletonCImpl, 11));
    }

    @Override
    public void injectGrifonApplication(GrifonApplication grifonApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.example.grifon.data.catalog.CatalogApi 
          return (T) AppModule_ProvideCatalogApiFactory.provideCatalogApi(singletonCImpl.provideRetrofitProvider.get());

          case 1: // retrofit2.Retrofit 
          return (T) AppModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideMoshiProvider.get());

          case 2: // okhttp3.OkHttpClient 
          return (T) AppModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 3: // com.squareup.moshi.Moshi 
          return (T) AppModule_ProvideMoshiFactory.provideMoshi();

          case 4: // com.example.grifon.data.repository.ShopRepository 
          return (T) AppModule_ProvideShopRepositoryFactory.provideShopRepository(singletonCImpl.provideShopPreferencesProvider.get(), singletonCImpl.provideCatalogApiProvider.get());

          case 5: // com.example.grifon.data.local.ShopPreferences 
          return (T) AppModule_ProvideShopPreferencesFactory.provideShopPreferences(singletonCImpl.provideDataStoreProvider.get());

          case 6: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // com.example.grifon.data.repository.UserRepository 
          return (T) AppModule_ProvideUserRepositoryFactory.provideUserRepository(singletonCImpl.provideCatalogApiProvider.get(), singletonCImpl.provideUserPreferencesProvider.get(), singletonCImpl.provideMoshiProvider.get());

          case 8: // com.example.grifon.data.local.UserPreferences 
          return (T) AppModule_ProvideUserPreferencesFactory.provideUserPreferences(singletonCImpl.provideDataStoreProvider.get());

          case 9: // com.example.grifon.data.repository.CartRepository 
          return (T) AppModule_ProvideCartRepositoryFactory.provideCartRepository();

          case 10: // com.example.grifon.data.repository.CatalogRepository 
          return (T) AppModule_ProvideCatalogRepositoryFactory.provideCatalogRepository(singletonCImpl.provideCatalogApiProvider.get());

          case 11: // com.example.grifon.data.repository.BarcodeScannerService 
          return (T) AppModule_ProvideBarcodeScannerServiceFactory.provideBarcodeScannerService();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
