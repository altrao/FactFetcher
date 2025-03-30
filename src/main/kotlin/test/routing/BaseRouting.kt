package test.routing

import io.ktor.server.routing.RoutingContext

/**
 * Base interface for defining common functionality and properties used across routing classes.
 * Provides utility functions for extracting query parameters and managing pagination.
 */
interface BaseRouting {
    val pageLimit: Int get() = 50

    fun RoutingContext.getOffsetPagingParameters(): Pair<Int, Int> {
        val offset = getQueryParameter("offset")?.toInt() ?: 0
        val limit = getQueryParameter("limit")?.toInt()?.takeIf { it > 0 } ?: pageLimit

        return Pair(offset, limit)
    }

    fun RoutingContext.getQueryParameter(parameter: String) = call.queryParameters[parameter]?.takeIf { it.isNotBlank() }
}
