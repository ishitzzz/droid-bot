package com.droidbot.agent.di;

import android.content.Context;
import com.droidbot.agent.identity.IdentityVault;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppModule_ProvideIdentityVaultFactory implements Factory<IdentityVault> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideIdentityVaultFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public IdentityVault get() {
    return provideIdentityVault(contextProvider.get());
  }

  public static AppModule_ProvideIdentityVaultFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideIdentityVaultFactory(contextProvider);
  }

  public static IdentityVault provideIdentityVault(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideIdentityVault(context));
  }
}
