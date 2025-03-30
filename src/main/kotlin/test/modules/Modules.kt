package test.modules

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.module
import test.configuration.FactsServiceConfiguration
import test.services.FactsService

fun ApplicationConfig.buildConfigurationModule() = module {
    single {
        FactsServiceConfiguration(config("services.facts"))
    }
}

val factsServiceModule = module {
    single { FactsService(get(), get()) }
}
