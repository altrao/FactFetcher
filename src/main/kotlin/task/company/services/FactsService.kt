package task.company.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import task.company.configuration.FactsServiceConfiguration
import task.company.model.Fact
import task.company.model.Statistics
import task.company.utils.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.set
import kotlin.concurrent.withLock

/**
 * The FactsService is responsible for interacting with an external API to fetch and manage random facts.
 * It also maintains a cache of facts and tracks access statistics for each fact.
 *
 * @property client The HTTP client used to send requests to the external API.
 * @property uselessFactsConfig Configuration that contains the API URL for fetching facts.
 */
class FactsService(private val client: HttpClient, private val uselessFactsConfig: FactsServiceConfiguration) {
    private val logger = logger()
    private val lock = ReentrantLock()

    /**
     * Thread safe LRU cache.
     *
     * Caches previously required facts from the API, limited to [FactsServiceConfiguration::cacheSize].
     */
    private val factsCache = object : LinkedHashMap<String, Fact>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Fact>?): Boolean {
            return size > uselessFactsConfig.cacheSize
        }

        override fun put(key: String, value: Fact): Fact? {
            lock.withLock {
                return super.put(key, value)
            }
        }
    }

    private val statisticsCache = ConcurrentHashMap<String, Statistics>()

    /**
     * Fetches a random fact from the configured URL.
     *
     * @return [Fact] or null if an error happens.
     */
    suspend fun fetchRandomFact(): Fact? {
        try {
            val response = client.get(uselessFactsConfig.remoteUrl)

            if (response.status.value >= 300) {
                logger.warn("Error fetching fact from API. ${response.status}")
                return null
            }

            return response.body<Fact>().also {
                    if (!factsCache.containsKey(it.shortened)) {
                        factsCache[it.shortened] = it
                        statisticsCache[it.shortened] = Statistics(it.shortened)
                    }
            }
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Timeout fetching fact from API.")
            return null
        } catch (e: Exception) {
            logger.error("Failed to fetch fact.", e)
            return null
        }
    }

    fun getFactByShortenedUrl(shortenedUrl: String): Fact? {
        return factsCache[shortenedUrl]
    }

    fun getFacts(limit: Int, offset: Int): List<Fact> {
        return getOffset(factsCache.values.toList(), limit, offset)
    }

    fun getStatistics() = statisticsCache.values.toList()

    /**
     * Returns a sublist of the given list based on the specified limit and offset.
     *
     * @param values The original list from which a sublist will be extracted.
     * @param limit The maximum number of elements to include in the resulting sublist.
     * @param offset The starting position in the original list from which to begin the sublist.
     * @return A list containing a sublist of the original list, or an empty list if the offset is out of bounds or the range is invalid.
     */
    private fun <T> getOffset(values: List<T>, limit: Int, offset: Int): List<T> {
        val startIndex = offset.coerceAtMost(values.size)
        val newLimit = if (limit > 0) limit else 50
        val endIndex = minOf(startIndex + newLimit, values.size)

        if (startIndex == values.size || startIndex == endIndex || startIndex < 0) {
            return emptyList()
        }

        return values.subList(startIndex, endIndex)
    }

    fun incrementAccessCount(shortenedUrl: String) {
        statisticsCache[shortenedUrl]?.accessCount?.incrementAndGet()
    }
}
