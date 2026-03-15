package com.droidbot.agent.di;

import android.content.Context;
import com.droidbot.agent.brain.EdgeInference;
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
public final class AppModule_ProvideEdgeInferenceFactory implements Factory<EdgeInference> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideEdgeInferenceFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public EdgeInference get() {
    return provideEdgeInference(contextProvider.get());
  }

  public static AppModule_ProvideEdgeInferenceFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideEdgeInferenceFactory(contextProvider);
  }

  public static EdgeInference provideEdgeInference(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideEdgeInference(context));
  }
}
