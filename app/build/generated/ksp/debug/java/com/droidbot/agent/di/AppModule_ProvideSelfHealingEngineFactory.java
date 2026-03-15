package com.droidbot.agent.di;

import com.droidbot.agent.brain.CloudInference;
import com.droidbot.agent.navigation.SelfHealingEngine;
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
public final class AppModule_ProvideSelfHealingEngineFactory implements Factory<SelfHealingEngine> {
  private final Provider<CloudInference> cloudInferenceProvider;

  public AppModule_ProvideSelfHealingEngineFactory(
      Provider<CloudInference> cloudInferenceProvider) {
    this.cloudInferenceProvider = cloudInferenceProvider;
  }

  @Override
  public SelfHealingEngine get() {
    return provideSelfHealingEngine(cloudInferenceProvider.get());
  }

  public static AppModule_ProvideSelfHealingEngineFactory create(
      Provider<CloudInference> cloudInferenceProvider) {
    return new AppModule_ProvideSelfHealingEngineFactory(cloudInferenceProvider);
  }

  public static SelfHealingEngine provideSelfHealingEngine(CloudInference cloudInference) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSelfHealingEngine(cloudInference));
  }
}
