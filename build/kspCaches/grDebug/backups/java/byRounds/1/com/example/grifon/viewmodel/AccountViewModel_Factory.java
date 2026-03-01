package com.example.grifon.viewmodel;

import com.example.grifon.data.repository.UserRepository;
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
public final class AccountViewModel_Factory implements Factory<AccountViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  public AccountViewModel_Factory(Provider<UserRepository> userRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public AccountViewModel get() {
    return newInstance(userRepositoryProvider.get());
  }

  public static AccountViewModel_Factory create(Provider<UserRepository> userRepositoryProvider) {
    return new AccountViewModel_Factory(userRepositoryProvider);
  }

  public static AccountViewModel newInstance(UserRepository userRepository) {
    return new AccountViewModel(userRepository);
  }
}
