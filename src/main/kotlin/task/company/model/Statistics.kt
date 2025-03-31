package task.company.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import task.company.utils.AtomicIntegerSerializer
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class Statistics(
    @SerialName("shortened_url")
    val shortenedUrl: String,
    @SerialName("access_count")
    @Serializable(AtomicIntegerSerializer::class)
    var accessCount: AtomicInteger = AtomicInteger(0)
)
