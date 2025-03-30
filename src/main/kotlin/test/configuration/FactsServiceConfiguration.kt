package test.configuration

import io.ktor.server.config.ApplicationConfig
import kotlinx.serialization.Serializable

@Serializable
class FactsServiceConfiguration(val cacheSize: Int, val remoteUrl: String) {
    constructor(configValue: ApplicationConfig) : this(
        configValue.property("cacheSize").getString().toInt(),
        configValue.property("remote.uselessFacts.url").getString()
    )
}
