package com.example

import kotlinx.serialization.Serializable

@Serializable
data class ClientMessage(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val split: Boolean = false,
    val nick: String? = null
)

@Serializable
data class PlayerState(
    val id: String,
    val nickname: String,
    val x: Double,
    val y: Double,
    val mass: Double,
    val color: Int,
    val hasCore: Boolean
)

@Serializable
data class ServerMessage(
    val players: List<PlayerState>,
    val time: Long = System.currentTimeMillis()
)
