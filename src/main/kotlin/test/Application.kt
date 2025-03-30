package test

import io.ktor.client.HttpClient
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import test.modules.buildConfigurationModule
import test.modules.factsServiceModule
import test.modules.httpClientModule
import test.routing.AdminRouting.configureAdminRouting
import test.routing.FactsRouting.configureFactsRouting

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
