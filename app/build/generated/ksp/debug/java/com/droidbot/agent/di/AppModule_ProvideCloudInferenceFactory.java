package com.droidbot.agent.di;

import com.droidbot.agent.brain.CloudInference;
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
public final class AppModule_ProvideCloudInferenceFactory implements Factory<CloudInference> {
  @Override
  public CloudInference get() {
    return provideCloudInference();
  }

  public static AppModule_ProvideCloudInferenceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CloudInference provideCloudInference() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCloudInference());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideCloudInferenceFactory INSTANCE = new AppModule_ProvideCloudInferenceFactory();
  }
}
