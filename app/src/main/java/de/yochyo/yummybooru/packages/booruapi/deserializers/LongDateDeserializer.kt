package de.yochyo.booruapi.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.util.Date

/**
 * Deserializes a millisecond value * 1000 to date
 */
class LongDateDeserializer : JsonDeserializer<Date>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Date {
        val token = p.currentToken
        if (token == JsonToken.VALUE_NUMBER_INT) {
            val long = p.numberValue.toLong()
            return Date(long * 1000)
        }
        throw Exception("Value ${p.currentName} is not a String")
    }
}