package com.droidbot.agent.payments;

import com.droidbot.agent.identity.BiometricGate;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PravaPaymentsBridge_Factory implements Factory<PravaPaymentsBridge> {
  private final Provider<BiometricGate> biometricGateProvider;

  public PravaPaymentsBridge_Factory(Provider<BiometricGate> biometricGateProvider) {
    this.biometricGateProvider = biometricGateProvider;
  }

  @Override
  public PravaPaymentsBridge get() {
    return newInstance(biometricGateProvider.get());
  }

  public static PravaPaymentsBridge_Factory create(Provider<BiometricGate> biometricGateProvider) {
    return new PravaPaymentsBridge_Factory(biometricGateProvider);
  }

  public static PravaPaymentsBridge newInstance(BiometricGate biometricGate) {
    return new PravaPaymentsBridge(biometricGate);
  }
}
