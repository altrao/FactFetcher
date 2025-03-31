package task.company.model

import kotlinx.serialization.Serializable
import task.company.utils.getAdler32Checksum

@Serializable
data class Fact(
    val text: String,
    val permalink: String,
    val shortened: String = getAdler32Checksum(text)
)