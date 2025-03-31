package task.company.modules

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val httpClientModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 5)
                exponentialDelay()

                retryOnExceptionIf { _, cause ->
                    when (cause) {
                        is ResponseException -> cause.response.status.value !in 200..299
                        else -> false
                    }
                }
            }

            engine {
                endpoint {
                    connectTimeout = 10_000
                    requestTimeout = 20_000
                }
            }
        }
    }
}
