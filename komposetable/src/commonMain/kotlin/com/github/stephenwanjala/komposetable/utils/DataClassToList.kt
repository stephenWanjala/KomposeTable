package com.github.stephenwanjala.komposetable.utils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

inline fun <reified T : Any> extractMembers(instance: T): List<Pair<String, String>> {
    // Get the serializer for the type T
    val serializer = serializer<T>()
    val descriptor = serializer.descriptor

    val json = Json

    val encodedValues = mutableListOf<Pair<String, String>>()

    // Encode the object to JSON first to access the actual values
    val jsonElement = json.encodeToJsonElement(serializer, instance)

    if (jsonElement is JsonObject) {
        for (i in 0 until descriptor.elementsCount) {
            val name = descriptor.getElementName(i)
            val value = jsonElement[name]?.toString() ?: "---"
            encodedValues.add(name to value)
        }
    }

    return encodedValues
}
