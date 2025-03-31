package task.company

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import task.company.model.Fact
import task.company.routing.FactsRouting.configureFactsRouting
import kotlin.test.*

class FactsRoutingTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `should return a new fact`() = testApplication {
        val text = "Dragons and Wyverns are completely different creatures. Or not."
        val permalink = "https://example.com/fact/test_id"
        val mockFact = Fact(text, permalink)

        val client = buildCommon {
            coEvery { fetchRandomFact() } returns mockFact
        }

        val response = client.post("/facts")
        assertEquals(HttpStatusCode.OK, response.status)

        val fact = response.body<Map<String, String>>()
        assertEquals(text, fact["original_fact"])
        assertTrue { fact["shortened_url"]!!.contains(mockFact.shortened) }
    }

    @Test
    fun `should return server error when fact is null`() = testApplication {
        val client = buildCommon {
            coEvery { fetchRandomFact() } returns null
        }

        val response = client.post("/facts")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Failed to fetch fact", response.body<String>())
    }

    @Test
    fun `should return server error on fetching a random fact when an exception is thrown`() = testApplication {
        val client = buildCommon {
            coEvery { fetchRandomFact() } throws Exception("Test exception")
        }

        val response = client.post("/facts")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Failed to fetch fact", response.body<String>())
    }

    @Test
    fun `should return facts`() = testApplication {
        val text = "Dragons and Wyverns are completely different creatures. Or not."
        val permalink = "https://example.com/fact/test_id"
        val fact = Fact(text, permalink)

        val client = buildCommon {
            coEvery { getFacts(any(), any()) } returns listOf(fact)
        }

        val response = client.get("/facts")
        assertEquals(HttpStatusCode.OK, response.status)

        val facts = response.body<List<Map<String, String>>>()
        assertEquals(1, facts.size)
        assertEquals(text, facts.single()["fact"])
        assertEquals(permalink, facts.single()["original_permalink"])
    }

    @Test
    fun `should return OK, empty body when there are no facts`() = testApplication {
        val client = buildCommon {
            coEvery { getFacts(any(), any()) } returns emptyList()
        }

        val response = client.get("/facts")
        assertEquals(HttpStatusCode.OK, response.status)

        val facts = response.body<List<Map<String, String>>>()
        assertEquals(0, facts.size)
    }

    @Test
    fun `should return server error if an exception is thrown when fetching facts`() = testApplication {
        val client = buildCommon {
            coEvery { getFacts(any(), any()) } throws Exception("Test exception")
        }

        val response = client.get("/facts")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Failed to fetch facts", response.body<String>())
    }

    @Test
    fun `should return cached fact when requested with shortened url`() = testApplication {
        val shortenedUrl = "validShortenedUrl"
        val fact = Fact("fact text", "https://example.com/facts/1", shortenedUrl)

        val client = buildCommon {
            coEvery { getFactByShortenedUrl(shortenedUrl) } returns fact
        }

        val response = client.get("/facts/${fact.shortened}")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody: Map<String, String> = response.body()
        assertEquals(fact.text, responseBody["fact"])
        assertEquals(fact.permalink, responseBody["original_permalink"])
    }

    @Test
    fun `should return not found when requested with shortened url but fact is not cached`() = testApplication {
        val shortenedUrl = "invalidShortenedUrl"

        val client = buildCommon {
            coEvery { getFactByShortenedUrl(shortenedUrl) } returns null
        }

        val response = client.get("/facts/$shortenedUrl")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Fact not found", response.body<String>())
    }

    @Test
    fun `should return not found if no shortened id is provided`() = testApplication {
        val client = buildCommon {
            coEvery { getFactByShortenedUrl(any()) } returns null
        }

        val response = client.get("/facts/")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `should return server error if an exception is thrown when requesting a cached fact`() = testApplication {
        val shortenedUrl = "shortenedUrl"

        val client = buildCommon {
            coEvery { getFactByShortenedUrl(shortenedUrl) } throws RuntimeException("Simulated error")
        }

        val response = client.get("/facts/$shortenedUrl")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Failed to fetch fact", response.body<String>())
    }

    @Test
    fun `should redirect request to fact permalink`() = testApplication {
        val permalink = "test/redirect/fact"
        val shortenedUrl = "validShortenedUrl"
        val fact = Fact("a random fact", permalink, shortenedUrl)

        routing {
            get("/facts/$shortenedUrl/$permalink") {
                call.respondText("A random fact from redirect")
            }
        }

        val client = buildCommon {
            coEvery { getFactByShortenedUrl(shortenedUrl) } returns fact
        }

        val response = client.get("/facts/$shortenedUrl/redirect")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("A random fact from redirect", response.bodyAsText())
    }

    @Test
    fun `should return not found on redirect endpoint if fact is not cached`() = testApplication {
        val shortenedUrl = "invalidShortenedUrl"

        val client = buildCommon {
            coEvery { getFactByShortenedUrl(shortenedUrl) } returns null
        }

        val response = client.get("/facts/$shortenedUrl/redirect")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertNull(response.headers["Location"])
        assertEquals("Fact not found", response.bodyAsText())
    }

    @Test
    fun `should return server error if an exception is thrown on requesting redirect`() = testApplication {
        val shortenedUrl = "shortenedUrl"
        val exception = RuntimeException("Simulated error")

        val client = buildCommon {
            coEvery { getFactByShortenedUrl(shortenedUrl) } throws exception
        }

        val response = client.get("/facts/$shortenedUrl/redirect")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertNull(response.headers["Location"])
    }

    private fun ApplicationTestBuilder.buildCommon(mockkFactsService: task.company.services.FactsService.() -> Unit): HttpClient {
        install(Koin) {
            modules(
                module {
                    single {
                        mockk<task.company.services.FactsService>(relaxed = true, block = mockkFactsService)
                    }
                }
            )
        }

        install(ContentNegotiation) {
            json()
        }

        application {
            configureFactsRouting()
        }

        return createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }

}
