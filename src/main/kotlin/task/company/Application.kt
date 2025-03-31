package task.company

import io.ktor.client.HttpClient
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import task.company.modules.buildConfigurationModule
import task.company.modules.factsServiceModule
import task.company.modules.httpClientModule
import task.company.routing.AdminRouting.configureAdminRouting
import task.company.routing.FactsRouting.configureFactsRouting

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(Koin) {
        modules(
            httpClientModule,
            factsServiceModule,
            environment.config.buildConfigurationModule()
        )
    }

    install(ContentNegotiation) {
        json()
    }

    configureFactsRouting()
    configureAdminRouting()

    monitor.subscribe(ApplicationStopPreparing) {
        val client = getKoin().get<HttpClient>()

        log.info("Closing HttpClient")
        client.close()
    }
}
