package task.company.utils

import ch.qos.logback.classic.Logger
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.routing.RoutingContext
import org.slf4j.LoggerFactory
import java.util.zip.Adler32

/**
 * Generates an Adler-32 checksum for the given text.
 *
 * @param text The input string for which the checksum will be calculated.
 * @return The Adler-32 checksum of the input as a hexadecimal string.
 */
fun getAdler32Checksum(text: String): String {
    return Adler32().apply {
        update(text.toByteArray())
    }.value.toString(16)
}

inline fun <reified T> T.logger(): Logger {
    return LoggerFactory.getLogger(T::class.java) as Logger
}

fun RoutingContext.getShortenedUrl(shortened: String) = "${call.request.origin.scheme}://${call.request.host()}:${call.request.port()}/facts/$shortened"
