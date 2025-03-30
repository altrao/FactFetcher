package test.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import test.services.FactsService
import test.utils.getShortenedUrl
import test.utils.logger


object FactsRouting : BaseRouting {
    private val logger = logger()

    fun Application.configureFactsRouting() {
        val factsService: FactsService by inject()

        routing {
            route("/facts") {
                post {
                    try {
                        val fact = factsService.fetchRandomFact()

                        if (fact == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to fetch fact")
                            return@post
                        }

                        call.respond(
                            HttpStatusCode.OK, mapOf(
                                "original_fact" to fact.text,
                                "shortened_url" to getShortenedUrl(fact.shortened)
                            )
                        )
                    } catch (e: Exception) {
                        logger.error("Error generating a new fact from FactsService", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Failed to fetch fact"
                        )
                    }
                }

                get {
                    val result = runCatching {
                        val (offset, limit) = getOffsetPagingParameters()

                        factsService.getFacts(limit, offset).map {
                            mapOf(
                                "fact" to it.text,
                                "original_permalink" to it.permalink
                            )
                        }
                    }.onFailure {
                        logger.error("Error getting all facts", it)
                    }

                    if (result.isSuccess) {
                        call.respond(HttpStatusCode.OK, result.getOrNull() ?: emptyList())
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to fetch facts")
                    }
                }

                route("/{shortenedUrl}") {
                    get {
                        try {
                            val shortenedUrl = call.parameters["shortenedUrl"] ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Missing shortened URL"
                            )

                            val fact = factsService.getFactByShortenedUrl(shortenedUrl)

                            if (fact != null) {
                                factsService.incrementAccessCount(shortenedUrl)
                                call.respond(
                                    mapOf(
                                        "fact" to fact.text,
                                        "original_permalink" to fact.permalink
                                    )
                                )
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Fact not found")
                            }
                        } catch (e: Exception) {
                            logger.error("Error getting a fact by shortened URL", e)
                            call.respond(HttpStatusCode.InternalServerError, "Failed to fetch fact")
                        }
                    }

                    get("/redirect") {
                        try {
                            val shortenedUrl = call.parameters["shortenedUrl"] ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Missing shortened URL"
                            )

                            val fact = factsService.getFactByShortenedUrl(shortenedUrl)

                            if (fact != null) {
                                factsService.incrementAccessCount(shortenedUrl)
                                call.respondRedirect(fact.permalink, permanent = false)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Fact not found")
                            }
                        } catch (e: Exception) {
                            logger.error("Error redirecting to original fact", e)
                            call.respond(HttpStatusCode.InternalServerError, "Failed to fetch fact")
                        }
                    }
                }
            }
        }
    }
}