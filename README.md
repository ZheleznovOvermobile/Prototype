# Prototype Game Template

This project contains a minimal example of a multiplayer soft-body game.
The server is written with [Ktor](https://ktor.io) and communicates over WebSocket.
The client uses [Pixi.js](https://pixijs.com) to render players.

## Structure

```
server/  - Ktor server implementation (Kotlin)
client/  - Web client using Pixi.js
```

### Running the Server

```
cd server
./gradlew run
```

### Running the Client

Serve the `client` folder with any static HTTP server (e.g. `python3 -m http.server`).
Open `http://localhost:8000/index.html` in a browser.

## Gameplay

* Connects to the server via WebSocket.
* Move with **WASD** keys, split with **Space**.
* Each player is rendered as a colored body with a visible core.
* Basic mass affects body size.
* Simple UI shows nickname, ping and mass.
