package com.droidbot.agent.di

import android.content.Context
import com.droidbot.agent.brain.CloudInference
import com.droidbot.agent.brain.EdgeInference
import com.droidbot.agent.brain.HybridRouter
import com.droidbot.agent.brain.NavigationBrain
import com.droidbot.agent.hive.SharedKnowledgeBase
import com.droidbot.agent.identity.BiometricGate
import com.droidbot.agent.identity.IdentityVault
import com.droidbot.agent.navigation.SelfHealingEngine
import com.droidbot.agent.payments.PravaPaymentsBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module — wires the DroidBot dependency graph.
 *
 * All @Singleton scoped to ensure single instances across the app.
 * The graph flows:
 *   EdgeInference + CloudInference → HybridRouter
 *   HybridRouter + SelfHealingEngine + SharedKnowledgeBase + IdentityVault → NavigationBrain
 *   BiometricGate → PravaPaymentsBridge
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEdgeInference(@ApplicationContext context: Context): EdgeInference {
        return EdgeInference(context)
    }

    @Provides
    @Singleton
    fun provideCloudInference(): CloudInference {
        return CloudInference()
    }

    @Provides
    @Singleton
    fun provideHybridRouter(
        @ApplicationContext context: Context,
        edgeInference: EdgeInference,
        cloudInference: CloudInference
    ): HybridRouter {
        return HybridRouter(context, edgeInference, cloudInference)
    }

    @Provides
    @Singleton
    fun provideSelfHealingEngine(cloudInference: CloudInference): SelfHealingEngine {
        return SelfHealingEngine(cloudInference)
    }

    @Provides
    @Singleton
    fun provideSharedKnowledgeBase(): SharedKnowledgeBase {
        return SharedKnowledgeBase()
    }

    @Provides
    @Singleton
    fun provideIdentityVault(@ApplicationContext context: Context): IdentityVault {
        return IdentityVault(context)
    }

    @Provides
    @Singleton
    fun provideNavigationBrain(
        hybridRouter: HybridRouter,
        selfHealingEngine: SelfHealingEngine,
        knowledgeBase: SharedKnowledgeBase,
        identityVault: IdentityVault
    ): NavigationBrain {
        return NavigationBrain(hybridRouter, selfHealingEngine, knowledgeBase, identityVault)
    }

    @Provides
    @Singleton
    fun provideBiometricGate(): BiometricGate {
        return BiometricGate()
    }

    @Provides
    @Singleton
    fun providePravaPaymentsBridge(biometricGate: BiometricGate): PravaPaymentsBridge {
        return PravaPaymentsBridge(biometricGate)
    }
}
