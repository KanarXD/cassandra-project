package hector.backend

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class Config(
    @SerialName("contact_point") val contactPoint: String,
    val keyspace: String,
    val replication: Replication,
) {
    companion object {
        @JvmStatic
        @OptIn(ExperimentalSerializationApi::class)
        fun from(source: String): Config? {
            val serialized = Config::class.java.classLoader.getResourceAsStream(source)

            return serialized?.let { stream ->
                try {
                    Json.decodeFromStream<Config>(stream)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}


@Serializable
class Replication(val strategy: String, val factor: Int)
