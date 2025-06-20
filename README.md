# Prototype Game Server

This repository contains a very small prototype of an Agar.io style server built with **Kotlin** and **Ktor** using WebSockets.

The key points illustrated in `Server.kt`:

- Data classes for players, zones, and game state serialized with `kotlinx.serialization`.
- WebSocket endpoint `/play` accepting client input and sending snapshots.
- Simple update loop running 20 times per second in `GameServer`.

The code is not production ready but demonstrates how a multiplayer arena server could be structured.
