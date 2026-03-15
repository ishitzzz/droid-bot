package com.droidbot.agent.di;

import android.content.Context;
import com.droidbot.agent.brain.CloudInference;
import com.droidbot.agent.brain.EdgeInference;
import com.droidbot.agent.brain.HybridRouter;
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
public final class AppModule_ProvideHybridRouterFactory implements Factory<HybridRouter> {
  private final Provider<Context> contextProvider;

  private final Provider<EdgeInference> edgeInferenceProvider;

  private final Provider<CloudInference> cloudInferenceProvider;

  public AppModule_ProvideHybridRouterFactory(Provider<Context> contextProvider,
      Provider<EdgeInference> edgeInferenceProvider,
      Provider<CloudInference> cloudInferenceProvider) {
    this.contextProvider = contextProvider;
    this.edgeInferenceProvider = edgeInferenceProvider;
    this.cloudInferenceProvider = cloudInferenceProvider;
  }

  @Override
  public HybridRouter get() {
    return provideHybridRouter(contextProvider.get(), edgeInferenceProvider.get(), cloudInferenceProvider.get());
  }

  public static AppModule_ProvideHybridRouterFactory create(Provider<Context> contextProvider,
      Provider<EdgeInference> edgeInferenceProvider,
      Provider<CloudInference> cloudInferenceProvider) {
    return new AppModule_ProvideHybridRouterFactory(contextProvider, edgeInferenceProvider, cloudInferenceProvider);
  }

  public static HybridRouter provideHybridRouter(Context context, EdgeInference edgeInference,
      CloudInference cloudInference) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHybridRouter(context, edgeInference, cloudInference));
  }
}
