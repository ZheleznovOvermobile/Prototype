package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlin.random.Random

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(ContentNegotiation) {
        json()
    }

    val players = ConcurrentHashMap<String, Player>()

    routing {
        webSocket("/play") {
            val id = UUID.randomUUID().toString()
            val player = Player(id, "player-$id", 0.0, 0.0)
            player.session = this
            players[id] = player

            sendSerialized(ServerMessage(players.values.map { it.toState() }))

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val msg = Json.decodeFromString(ClientMessage.serializer(), frame.readText())
                    msg.nick?.let { player.nickname = it }
                    player.update(msg)
                }
            }

            player.session = null
            players.remove(id)
        }
    }

    // broadcast game state
    environment.monitor.subscribe(ApplicationStopped) {
        players.clear()
    }
    launchBroadcast(players)
}

private fun Application.launchBroadcast(players: ConcurrentHashMap<String, Player>) {
    environment.monitor.subscribe(ApplicationStarted) {
        val scope = this
        scope.launch {
            while (true) {
                val snapshot = ServerMessage(players.values.map { it.toState() })
                players.values.forEach { player ->
                    player.session?.sendSerialized(snapshot)
                }
                delay(50)
            }
        }
    }
}

private data class Player(
    val id: String,
    var nickname: String,
    var x: Double,
    var y: Double,
    var mass: Double = 10.0,
    val color: Int = Random.nextInt(0xFFFFFF),
    var hasCore: Boolean = true,
    var session: DefaultWebSocketServerSession? = null
) {
    fun toState() = PlayerState(id, nickname, x, y, mass, color, hasCore)

    fun update(msg: ClientMessage) {
        val speed = 1.0
        if (msg.up) y -= speed
        if (msg.down) y += speed
        if (msg.left) x -= speed
        if (msg.right) x += speed
        if (msg.split && mass > 5) {
            mass /= 2
        }
    }
}
