package test.model

import kotlinx.serialization.Serializable
import test.utils.getAdler32Checksum

@Serializable
data class Fact(
    val text: String,
    val permalink: String,
    val shortened: String = getAdler32Checksum(text)
)