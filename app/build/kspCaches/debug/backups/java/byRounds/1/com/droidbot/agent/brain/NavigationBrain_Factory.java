package com.droidbot.agent.brain;

import com.droidbot.agent.hive.SharedKnowledgeBase;
import com.droidbot.agent.identity.IdentityVault;
import com.droidbot.agent.navigation.SelfHealingEngine;
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
public final class NavigationBrain_Factory implements Factory<NavigationBrain> {
  private final Provider<HybridRouter> hybridRouterProvider;

  private final Provider<SelfHealingEngine> selfHealingEngineProvider;

  private final Provider<SharedKnowledgeBase> knowledgeBaseProvider;

  private final Provider<IdentityVault> identityVaultProvider;

  public NavigationBrain_Factory(Provider<HybridRouter> hybridRouterProvider,
      Provider<SelfHealingEngine> selfHealingEngineProvider,
      Provider<SharedKnowledgeBase> knowledgeBaseProvider,
      Provider<IdentityVault> identityVaultProvider) {
    this.hybridRouterProvider = hybridRouterProvider;
    this.selfHealingEngineProvider = selfHealingEngineProvider;
    this.knowledgeBaseProvider = knowledgeBaseProvider;
    this.identityVaultProvider = identityVaultProvider;
  }

  @Override
  public NavigationBrain get() {
    return newInstance(hybridRouterProvider.get(), selfHealingEngineProvider.get(), knowledgeBaseProvider.get(), identityVaultProvider.get());
  }

  public static NavigationBrain_Factory create(Provider<HybridRouter> hybridRouterProvider,
      Provider<SelfHealingEngine> selfHealingEngineProvider,
      Provider<SharedKnowledgeBase> knowledgeBaseProvider,
      Provider<IdentityVault> identityVaultProvider) {
    return new NavigationBrain_Factory(hybridRouterProvider, selfHealingEngineProvider, knowledgeBaseProvider, identityVaultProvider);
  }

  public static NavigationBrain newInstance(HybridRouter hybridRouter,
      SelfHealingEngine selfHealingEngine, SharedKnowledgeBase knowledgeBase,
      IdentityVault identityVault) {
    return new NavigationBrain(hybridRouter, selfHealingEngine, knowledgeBase, identityVault);
  }
}
