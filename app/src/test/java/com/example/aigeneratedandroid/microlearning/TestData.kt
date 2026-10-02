package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.model.Segment
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

object TestData {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** Unit tests run with the module directory as the working directory. */
    val library: List<Segment> by lazy {
        json.decodeFromString(ListSerializer(Segment.serializer()), File("src/main/assets/library.json").readText())
    }

    fun byId(id: String) = library.first { it.id == id }
}
