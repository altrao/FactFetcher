package task.company.modules

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.module
import task.company.configuration.FactsServiceConfiguration

fun ApplicationConfig.buildConfigurationModule() = module {
    single {
        FactsServiceConfiguration(config("services.facts"))
    }
}

val factsServiceModule = module {
    single { task.company.services.FactsService(get(), get()) }
}
