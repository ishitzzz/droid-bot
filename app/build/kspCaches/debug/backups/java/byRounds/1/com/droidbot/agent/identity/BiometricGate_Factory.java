package com.droidbot.agent.identity;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class BiometricGate_Factory implements Factory<BiometricGate> {
  @Override
  public BiometricGate get() {
    return newInstance();
  }

  public static BiometricGate_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BiometricGate newInstance() {
    return new BiometricGate();
  }

  private static final class InstanceHolder {
    private static final BiometricGate_Factory INSTANCE = new BiometricGate_Factory();
  }
}
