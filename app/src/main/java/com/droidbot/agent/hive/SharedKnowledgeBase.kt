package com.droidbot.agent.hive

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedKnowledgeBase — The Hive.
 *
 * Interface for the shared Vector DB where all DroidBot instances
 * contribute their learned navigation paths. When one bot learns
 * how to book a flight on a new app version, all bots instantly
 * know that UI layout.
 *
 * ┌─────────────────────────────────────────────┐
 * │              Vector DB (Cloud)               │
 * │                                              │
 * │  ┌──────────────┐  ┌──────────────────────┐  │
 * │  │  UI Maps     │  │  Semantic Search     │  │
 * │  │  (paths)     │  │  (find similar tasks)│  │
 * │  └──────────────┘  └──────────────────────┘  │
 * │                                              │
 * │  Upload: bot records successful path → push  │
 * │  Query:  new task → find closest known path  │
 * │  Stale:  app updated → mark old paths stale  │
 * └─────────────────────────────────────────────┘
 *
 * Backend options: Pinecone, Weaviate, Qdrant, or custom.
 */
@Singleton
class SharedKnowledgeBase @Inject constructor() {

    companion object {
        private const val TAG = "SharedKnowledgeBase"
    }

    // In-memory cache of recently fetched/uploaded UI maps
    private val localCache = mutableMapOf<String, UIMap>()

    /**
     * Upload a successful navigation path to the Hive.
     *
     * @param uiMap The recorded navigation path
     */
    suspend fun uploadUIMap(uiMap: UIMap) {
        Log.i(TAG, "🐝 Uploading UI Map: ${uiMap.appPackage} — \"${uiMap.taskDescription}\" (${uiMap.steps.size} steps)")

        // Cache locally
        localCache[uiMap.id] = uiMap

        // TODO: Upload to remote Vector DB
        // val embedding = generateEmbedding(uiMap.taskDescription)
        // vectorDBClient.upsert(
        //     id = uiMap.id,
        //     vector = embedding,
        //     metadata = mapOf(
        //         "appPackage" to uiMap.appPackage,
        //         "appVersion" to uiMap.appVersion,
        //         "taskDescription" to uiMap.taskDescription,
        //         "steps" to serializeSteps(uiMap.steps),
        //         "successRate" to uiMap.successRate,
        //         "timestamp" to uiMap.timestamp
        //     )
        // )

        Log.i(TAG, "✅ UI Map uploaded (cached locally, remote pending)")
    }

    /**
     * Query the Hive for a known navigation path matching the task.
     *
     * @param taskDescription What the agent needs to do
     * @param appPackage Optional — filter by specific app
     * @return The best matching UIMap, or null if none found
     */
    suspend fun queryRelevantPaths(
        taskDescription: String,
        appPackage: String = ""
    ): UIMap? {
        Log.d(TAG, "🔍 Querying Hive: \"$taskDescription\" (app: ${appPackage.ifBlank { "any" }})")

        // Check local cache first
        val cachedMatch = localCache.values
            .filter { it.isReliable }
            .filter { appPackage.isBlank() || it.appPackage == appPackage }
            .maxByOrNull { calculateSimilarity(taskDescription, it.taskDescription) }

        if (cachedMatch != null) {
            val similarity = calculateSimilarity(taskDescription, cachedMatch.taskDescription)
            if (similarity > 0.7f) {
                Log.i(TAG, "✅ Found cached match: \"${cachedMatch.taskDescription}\" (similarity: $similarity)")
                return cachedMatch
            }
        }

        // TODO: Query remote Vector DB
        // val embedding = generateEmbedding(taskDescription)
        // val results = vectorDBClient.query(
        //     vector = embedding,
        //     topK = 5,
        //     filter = if (appPackage.isNotBlank()) mapOf("appPackage" to appPackage) else null
        // )
        // return results.firstOrNull()?.let { deserializeUIMap(it) }

        Log.d(TAG, "No matching path found in Hive")
        return null
    }

    /**
     * Mark a navigation path as stale (e.g., app was updated).
     */
    suspend fun markPathStale(mapId: String) {
        Log.i(TAG, "🔄 Marking path as stale: $mapId")
        localCache.remove(mapId)

        // TODO: Update remote Vector DB
        // vectorDBClient.update(id = mapId, metadata = mapOf("stale" to true))
    }

    /**
     * Clear all locally cached paths.
     */
    fun clearCache() {
        localCache.clear()
        Log.i(TAG, "Local cache cleared")
    }

    // ═══════════════════════════════════════════════════
    // Similarity (simple heuristic — replaced by embeddings in production)
    // ═══════════════════════════════════════════════════

    private fun calculateSimilarity(query: String, candidate: String): Float {
        val queryWords = query.lowercase().split(" ", ",", ".", "!").filter { it.isNotBlank() }.toSet()
        val candidateWords = candidate.lowercase().split(" ", ",", ".", "!").filter { it.isNotBlank() }.toSet()

        if (queryWords.isEmpty() || candidateWords.isEmpty()) return 0f

        val intersection = queryWords.intersect(candidateWords).size.toFloat()
        val union = queryWords.union(candidateWords).size.toFloat()

        return intersection / union // Jaccard similarity
    }
}
