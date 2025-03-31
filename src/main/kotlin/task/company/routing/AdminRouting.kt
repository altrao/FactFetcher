package task.company.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import task.company.utils.getShortenedUrl
import task.company.utils.logger

object AdminRouting : BaseRouting {
    private val logger = logger()

    fun Application.configureAdminRouting() {
        val factsService: task.company.services.FactsService by inject()

        routing {
            route("/admin") {
                get("/statistics") {
                    try {
                        call.respond(factsService.getStatistics().map { it.copy(shortenedUrl = getShortenedUrl(it.shortenedUrl)) })
                    } catch (e: Exception) {
                        logger.error("Error fetching statistics", e)
                        call.respond(HttpStatusCode.InternalServerError, "Failed to fetch statistics")
                    }
                }
            }
        }
    }
}
