package task.company

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import task.company.configuration.FactsServiceConfiguration
import task.company.model.Fact

class FactsServiceTest {
    private val uselessFactsConfig = FactsServiceConfiguration(100, "https://example.com")

    @Test
    fun `should return a fact on useless facts api successful response`() = runBlocking {
        val client = buildClient {
            respondJson(Fact("This is a test fact", "https://example.com", "test-fact"))
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)
        val fact = factsService.fetchRandomFact()

        checkNotNull(fact)
        assertEquals("This is a test fact", fact.text)
        assertEquals("test-fact", fact.shortened)
    }

    @Test
    fun `should return null on exception thrown`() = runBlocking {
        val client = buildClient {
            throw Exception("Error")
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)
        val fact = factsService.fetchRandomFact()

        assertNull(fact)
    }

    @Test
    fun `should return null on error response`() = runBlocking {
        val client = buildClient {
            respondError(HttpStatusCode.InternalServerError, "Error")
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)
        val fact = factsService.fetchRandomFact()

        assertNull(fact)
    }

    @Test
    fun `should return fact if present in cache`() = runBlocking {
        val client = buildClient {
            respondJson(Fact("Another test fact", "https://example.com", "another-fact"))
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)

        factsService.fetchRandomFact()

        val fact = factsService.getFactByShortenedUrl("another-fact")

        checkNotNull(fact)
        assertEquals("Another test fact", fact.text)
    }

    @Test
    fun `should return null if not present in cache`() {
        val mockHttpClient = HttpClient()
        val factsService = task.company.services.FactsService(mockHttpClient, uselessFactsConfig)
        val fact = factsService.getFactByShortenedUrl("non-existent-fact")

        assertNull(fact)
    }

    @Test
    fun `should return all statistics in cache`() = runBlocking {
        var count = 0

        val client = buildClient {
            respondJson(Fact("Statistic test fact ${++count}", "https://example.com", "statistic-fact-$count"))
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)
        factsService.fetchRandomFact()
        factsService.fetchRandomFact()

        val allStatistics = factsService.getStatistics()

        assertEquals(2, allStatistics.size)
        assertTrue(allStatistics.any { it.shortenedUrl == "statistic-fact-1" && it.accessCount.get() == 0 })
        assertTrue(allStatistics.any { it.shortenedUrl == "statistic-fact-2" && it.accessCount.get() == 0 })
    }

    @Test
    fun `should not reset fact statistics if already present`() = runBlocking {
        val client = buildClient {
            respondJson(Fact("Test fact", "https://example.com", "test-fact"))
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)
        val fact1 = factsService.fetchRandomFact()

        checkNotNull(fact1)
        factsService.incrementAccessCount(fact1.shortened)

        assertEquals(1, factsService.getStatistics().single().accessCount.get())

        repeat(5)  {
            factsService.fetchRandomFact()
        }

        assertEquals(1, factsService.getFacts(0, 0).size)
        assertEquals(1, factsService.getStatistics().single().accessCount.get())
    }

    @Test
    fun `should remove older facts from cache when it's full but preserve statistics`() = runBlocking {
        var count = 0

        val client = buildClient {
            respondJson(
                Fact("Test fact", "https://example.com", "test-fact-${count++}")
            )
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)

        repeat(150) { factsService.fetchRandomFact() }

        assertEquals(100, factsService.getFacts(200, 0).size)
        assertEquals(150, factsService.getStatistics().size)
    }

    @Test
    fun `should paginate facts`() = runBlocking {
        var count = 0

        val client = buildClient {
            respondJson(Fact("Fact ${++count}", "https://example.com", "test-fact-$count"))
        }

        val factsService = task.company.services.FactsService(client, uselessFactsConfig)
        repeat(3) { factsService.fetchRandomFact() }

        // Test first page
        var facts = factsService.getFacts(limit = 2, offset = 0)
        assertEquals(2, facts.size)
        assertEquals("Fact 1", facts[0].text)
        assertEquals("Fact 2", facts[1].text)

        // Test second page
        facts = factsService.getFacts(limit = 2, offset = 2)
        assertEquals(1, facts.size)
        assertEquals("Fact 3", facts[0].text)

        // Test out of bounds
        facts = factsService.getFacts(limit = 2, offset = 4)
        assertEquals(0, facts.size)
    }

    private fun buildClient(block: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient {
        return HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }

            engine {
                addHandler(block)
            }
        }
    }

    private fun MockRequestHandleScope.respondJson(fact: Fact): HttpResponseData {
        return respond(
            content = Json.encodeToString(Fact.serializer(), fact),
            status = HttpStatusCode.OK,
            headers = Headers.build {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        )
    }
}