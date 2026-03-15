package com.droidbot.agent.brain;

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
public final class CloudInference_Factory implements Factory<CloudInference> {
  @Override
  public CloudInference get() {
    return newInstance();
  }

  public static CloudInference_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CloudInference newInstance() {
    return new CloudInference();
  }

  private static final class InstanceHolder {
    private static final CloudInference_Factory INSTANCE = new CloudInference_Factory();
  }
}
