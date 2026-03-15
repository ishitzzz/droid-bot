package com.droidbot.agent.navigation;

import com.droidbot.agent.brain.CloudInference;
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
public final class SelfHealingEngine_Factory implements Factory<SelfHealingEngine> {
  private final Provider<CloudInference> cloudInferenceProvider;

  public SelfHealingEngine_Factory(Provider<CloudInference> cloudInferenceProvider) {
    this.cloudInferenceProvider = cloudInferenceProvider;
  }

  @Override
  public SelfHealingEngine get() {
    return newInstance(cloudInferenceProvider.get());
  }

  public static SelfHealingEngine_Factory create(Provider<CloudInference> cloudInferenceProvider) {
    return new SelfHealingEngine_Factory(cloudInferenceProvider);
  }

  public static SelfHealingEngine newInstance(CloudInference cloudInference) {
    return new SelfHealingEngine(cloudInference);
  }
}
