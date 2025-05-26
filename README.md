# Multiplayer Game Server

A Java-based multiplayer game server that handles real-time player connections and game state synchronization.

## Overview

This server implementation manages multiple client connections for a multiplayer game, handling player positions, block
rendering, and state broadcasting to all connected clients.

## Components

### Server (Server.java)

- Main server class that accepts client connections
- Uses port 12345 for communication
- Maintains a thread pool for handling multiple clients
- Automatically detects and displays local IPv4 addresses

### ClientHandler (ClientHandler.java)

- Handles individual client connections
- Manages player registration and spawning
- Processes position updates
- Broadcasts game state to all connected clients
- Uses JSON for data serialization

### GameState (GameState.java)

- Maintains the current game state
- Manages player collection using ConcurrentHashMap
- Handles blocks in the game world
- Provides methods for player management (add/remove/update)

### Block (Block.java)

- Represents obstacles or elements in the game
- Stores position, dimensions, and color information

## Technical Details

### Dependencies

- Java Socket API for network communication
- Google Gson for JSON serialization
- Java Concurrent utilities for thread safety

### Network Protocol

1. Initial Connection:
    - Server sends "ENTER_REGISTER"
    - Client responds with "REGISTER:{UUID}:{username}"
    - Server assigns random spawn coordinates

2. Game Updates:
    - Position updates sent as JSON objects
    - Game state broadcast includes all player positions
    - Block information sent on initial connection

### Data Structures

- Players: ConcurrentHashMap<UUID, Player>
- Blocks: ArrayList<Block>
- Client Handlers: Synchronized ArrayList

## Setup and Usage

1. Start the server application
2. Note the displayed IP addresses and port (12345)
3. Connect clients using the server's IP address
4. Players will automatically spawn at random positions

## Security and Synchronization

- Thread-safe collections for concurrent access
- Synchronized broadcasting to prevent conflicts
- Connection monitoring and cleanup
