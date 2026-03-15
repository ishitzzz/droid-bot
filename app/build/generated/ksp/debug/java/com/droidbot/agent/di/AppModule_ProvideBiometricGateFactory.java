package com.droidbot.agent.di;

import com.droidbot.agent.identity.BiometricGate;
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
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppModule_ProvideBiometricGateFactory implements Factory<BiometricGate> {
  @Override
  public BiometricGate get() {
    return provideBiometricGate();
  }

  public static AppModule_ProvideBiometricGateFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BiometricGate provideBiometricGate() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBiometricGate());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideBiometricGateFactory INSTANCE = new AppModule_ProvideBiometricGateFactory();
  }
}
