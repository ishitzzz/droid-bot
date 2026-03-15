package com.droidbot.agent.di;

import com.droidbot.agent.identity.BiometricGate;
import com.droidbot.agent.payments.PravaPaymentsBridge;
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
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppModule_ProvidePravaPaymentsBridgeFactory implements Factory<PravaPaymentsBridge> {
  private final Provider<BiometricGate> biometricGateProvider;

  public AppModule_ProvidePravaPaymentsBridgeFactory(
      Provider<BiometricGate> biometricGateProvider) {
    this.biometricGateProvider = biometricGateProvider;
  }

  @Override
  public PravaPaymentsBridge get() {
    return providePravaPaymentsBridge(biometricGateProvider.get());
  }

  public static AppModule_ProvidePravaPaymentsBridgeFactory create(
      Provider<BiometricGate> biometricGateProvider) {
    return new AppModule_ProvidePravaPaymentsBridgeFactory(biometricGateProvider);
  }

  public static PravaPaymentsBridge providePravaPaymentsBridge(BiometricGate biometricGate) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePravaPaymentsBridge(biometricGate));
  }
}
