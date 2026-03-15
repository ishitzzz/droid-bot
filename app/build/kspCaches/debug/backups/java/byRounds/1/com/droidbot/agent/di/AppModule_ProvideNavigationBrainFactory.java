package com.droidbot.agent.di;

import com.droidbot.agent.brain.HybridRouter;
import com.droidbot.agent.brain.NavigationBrain;
import com.droidbot.agent.hive.SharedKnowledgeBase;
import com.droidbot.agent.identity.IdentityVault;
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
public final class AppModule_ProvideNavigationBrainFactory implements Factory<NavigationBrain> {
  private final Provider<HybridRouter> hybridRouterProvider;

  private final Provider<SelfHealingEngine> selfHealingEngineProvider;

  private final Provider<SharedKnowledgeBase> knowledgeBaseProvider;

  private final Provider<IdentityVault> identityVaultProvider;

  public AppModule_ProvideNavigationBrainFactory(Provider<HybridRouter> hybridRouterProvider,
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
    return provideNavigationBrain(hybridRouterProvider.get(), selfHealingEngineProvider.get(), knowledgeBaseProvider.get(), identityVaultProvider.get());
  }

  public static AppModule_ProvideNavigationBrainFactory create(
      Provider<HybridRouter> hybridRouterProvider,
      Provider<SelfHealingEngine> selfHealingEngineProvider,
      Provider<SharedKnowledgeBase> knowledgeBaseProvider,
      Provider<IdentityVault> identityVaultProvider) {
    return new AppModule_ProvideNavigationBrainFactory(hybridRouterProvider, selfHealingEngineProvider, knowledgeBaseProvider, identityVaultProvider);
  }

  public static NavigationBrain provideNavigationBrain(HybridRouter hybridRouter,
      SelfHealingEngine selfHealingEngine, SharedKnowledgeBase knowledgeBase,
      IdentityVault identityVault) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNavigationBrain(hybridRouter, selfHealingEngine, knowledgeBase, identityVault));
  }
}
