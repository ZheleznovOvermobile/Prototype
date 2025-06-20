package io.game

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

/**
 * Represents a mutation ability for a creature.
 */
@Serializable
enum class Mutation {
    SPIKES,
    DASH,
    SPLIT,
    AUTO_ABSORB
}

/**
 * Simplified 2D vector.
 */
@Serializable
data class Vector2(val x: Float = 0f, val y: Float = 0f)

/**
 * State of a player creature in the world.
 */
@Serializable
data class Player(
    val id: String,
    var position: Vector2 = Vector2(),
    var mass: Float = 1f,
    var velocity: Vector2 = Vector2(),
    var isBoosting: Boolean = false,
    val mutations: MutableList<Mutation> = mutableListOf(),
    var coreExposed: Boolean = false
)

/**
 * Zone that affects gameplay (e.g., compression, starvation).
 */
@Serializable
data class Zone(
    val id: String,
    val center: Vector2,
    val radius: Float
)

/**
 * Complete world snapshot sent to clients.
 */
@Serializable
data class GameState(
    val players: List<Player>,
    val zones: List<Zone>,
    val tick: Long
)

/**
 * Client input message.
 */
@Serializable
data class InputMessage(
    val dx: Float,
    val dy: Float,
    val action: String? = null
)

/**
 * Wrapper around a websocket session with per-player data.
 */
class Client(val id: String, val session: DefaultWebSocketServerSession) {
    var lastInput: InputMessage = InputMessage(0f, 0f)
}

fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
        }
        routing {
            webSocket("/play") {
                val client = Client(id = hashCode().toString(), session = this)
                GameServer.register(client)
                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val msg = Json.decodeFromString<InputMessage>(frame.readText())
                            client.lastInput = msg
                        }
                    }
                } finally {
                    GameServer.unregister(client)
                }
            }
        }
    }
    server.start(wait = true)
}

/**
 * Singleton game server handling state and updates.
 */
object GameServer {
    private val clients = mutableSetOf<Client>()
    private val players = mutableMapOf<String, Player>()
    private var tick: Long = 0

    init {
        startGameLoop()
    }

    fun register(client: Client) {
        clients += client
        players[client.id] = Player(id = client.id)
    }

    fun unregister(client: Client) {
        clients -= client
        players.remove(client.id)
    }

    private fun startGameLoop() {
        GlobalScope.launch {
            while (true) {
                update(50)
                broadcastSnapshot()
                delay(50L) // 20 ticks per second
            }
        }
    }

    private fun update(deltaMillis: Long) {
        tick++
        for (client in clients) {
            val player = players[client.id] ?: continue
            val input = client.lastInput
            player.velocity = Vector2(input.dx, input.dy)
            player.isBoosting = input.action == "boost"
            player.position = Vector2(
                player.position.x + player.velocity.x * deltaMillis / 1000f,
                player.position.y + player.velocity.y * deltaMillis / 1000f
            )
        }
    }

    private suspend fun broadcastSnapshot() {
        val snapshot = GameState(players.values.toList(), emptyList(), tick)
        val json = Json.encodeToString(snapshot)
        clients.forEach { client ->
            client.session.send(Frame.Text(json))
        }
    }
}

