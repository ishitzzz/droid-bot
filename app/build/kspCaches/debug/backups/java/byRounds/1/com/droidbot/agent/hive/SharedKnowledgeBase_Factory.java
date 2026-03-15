package com.droidbot.agent.hive;

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
public final class SharedKnowledgeBase_Factory implements Factory<SharedKnowledgeBase> {
  @Override
  public SharedKnowledgeBase get() {
    return newInstance();
  }

  public static SharedKnowledgeBase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SharedKnowledgeBase newInstance() {
    return new SharedKnowledgeBase();
  }

  private static final class InstanceHolder {
    private static final SharedKnowledgeBase_Factory INSTANCE = new SharedKnowledgeBase_Factory();
  }
}
