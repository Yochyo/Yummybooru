package de.yochyo.booruapi.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * Deserializes "0" or "1" to a Boolean
 */
class NumericBooleanDeserializer : JsonDeserializer<Boolean>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Boolean {
        val token = p.currentToken
        if (token == JsonToken.VALUE_STRING) {
            val text = p.text.trim()
            return text == "1"
        }
        throw Exception("Value ${p.currentName} is not a String")
    }
}