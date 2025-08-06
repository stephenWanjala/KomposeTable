package com.github.stephenwanjala.komposetable.utils

import kotlinx.serialization.encodeToJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

inline fun <reified T> extractMembers(instance: T): List<Pair<String, String>> {
    val jsonElement = Json.encodeToJsonElement(instance)

    return if (jsonElement is JsonObject) {
        jsonElement.entries.map { (key, value) ->
            key to value.toString().trim('"')
        }
    } else {
        emptyList()
    }
}
