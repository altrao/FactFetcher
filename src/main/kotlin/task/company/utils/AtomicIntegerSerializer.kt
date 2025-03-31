package task.company.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.concurrent.atomic.AtomicInteger

object AtomicIntegerSerializer : KSerializer<AtomicInteger> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AtomicInteger", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: AtomicInteger) {
        encoder.encodeInt(value.get())
    }

    override fun deserialize(decoder: Decoder): AtomicInteger {
        return AtomicInteger(decoder.decodeInt())
    }
}
