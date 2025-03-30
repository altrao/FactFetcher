package test

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import test.model.Statistics
import test.routing.AdminRouting.configureAdminRouting
import test.services.FactsService
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminRoutingTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `should return statistics`() = testApplication {
        val statistics = Statistics("1bc2v3", AtomicInteger(5))

        val client = buildCommon {
            every { getStatistics() } returns listOf(statistics)
        }

        val response = client.get("/admin/statistics")
        val body = response.body<List<Statistics>>()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, body.size)
        assertTrue { body.single().shortenedUrl.contains(statistics.shortenedUrl) }
        assertEquals(statistics.accessCount.get(), body.single().accessCount.get())
    }

    @Test
    fun `should return 500 when an exception is thrown`() = testApplication {
        val client = buildCommon {
            every { getStatistics() } throws Exception("Simulated error")
        }

        val response = client.get("/admin/statistics")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    private fun ApplicationTestBuilder.buildCommon(block: FactsService.() -> Unit): HttpClient {
        install(Koin) {
            modules(module {
                single {
                    mockk<FactsService>(relaxed = true, block = block)
                }
            })
        }

        install(ContentNegotiation) {
            json()
        }

        application {
            configureAdminRouting()
        }

        return createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }
}
