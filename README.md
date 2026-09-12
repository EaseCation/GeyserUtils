# GeyserUtils
- camera shake
- NPC form
- animation
- camera features(require modify geyser's startgamepacket by yourself XP)
- (and thank daoge cmd so much ❤️ XD)

_Install_
- GeyserUtils-Spigot -> your spigot server plugins folder
- GeyserUtils-Velocity -> your velocity proxy (if exists)
- GeyserUtils-Geyser -> your geyser extensions folder

## Observer entity IDs

Paper callers can use `EntityIdQueries.resolve(viewer, javaIds)` to asynchronously obtain the actual
Bedrock IDs tracked by that Geyser connection. The future returns a map excluding missing entities;
signed IDs, including negative local-player IDs, are valid. Do not block the server thread on the future.

Queries use `geyserutils:main` and require the proxy's `geyserutils:entity_ids_v1` registration marker.
They time out after two seconds and are cancelled on disconnect, world change or plugin shutdown.
Both the Spigot plugin and Geyser extension must be updated together. These results resolve identity
only; they are not acknowledgments that a UI or animation has played.
