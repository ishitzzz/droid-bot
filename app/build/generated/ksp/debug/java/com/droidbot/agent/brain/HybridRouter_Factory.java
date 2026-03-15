package com.droidbot.agent.brain;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class HybridRouter_Factory implements Factory<HybridRouter> {
  private final Provider<Context> contextProvider;

  private final Provider<EdgeInference> edgeInferenceProvider;

  private final Provider<CloudInference> cloudInferenceProvider;

  public HybridRouter_Factory(Provider<Context> contextProvider,
      Provider<EdgeInference> edgeInferenceProvider,
      Provider<CloudInference> cloudInferenceProvider) {
    this.contextProvider = contextProvider;
    this.edgeInferenceProvider = edgeInferenceProvider;
    this.cloudInferenceProvider = cloudInferenceProvider;
  }

  @Override
  public HybridRouter get() {
    return newInstance(contextProvider.get(), edgeInferenceProvider.get(), cloudInferenceProvider.get());
  }

  public static HybridRouter_Factory create(Provider<Context> contextProvider,
      Provider<EdgeInference> edgeInferenceProvider,
      Provider<CloudInference> cloudInferenceProvider) {
    return new HybridRouter_Factory(contextProvider, edgeInferenceProvider, cloudInferenceProvider);
  }

  public static HybridRouter newInstance(Context context, EdgeInference edgeInference,
      CloudInference cloudInference) {
    return new HybridRouter(context, edgeInference, cloudInference);
  }
}
