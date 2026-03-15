package com.droidbot.agent.di;

import com.droidbot.agent.hive.SharedKnowledgeBase;
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
public final class AppModule_ProvideSharedKnowledgeBaseFactory implements Factory<SharedKnowledgeBase> {
  @Override
  public SharedKnowledgeBase get() {
    return provideSharedKnowledgeBase();
  }

  public static AppModule_ProvideSharedKnowledgeBaseFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SharedKnowledgeBase provideSharedKnowledgeBase() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSharedKnowledgeBase());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideSharedKnowledgeBaseFactory INSTANCE = new AppModule_ProvideSharedKnowledgeBaseFactory();
  }
}
