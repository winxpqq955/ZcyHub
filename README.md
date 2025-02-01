# vLobbyConnect

vLobbyConnect is a Velocity plugin that manages lobby connections for different Minecraft protocol versions.  
**Note:** Both Velocity and the backend servers must have `online-mode=false`.

## Setup

1. Place the plugin jar in your Velocity plugins folder.
2. Ensure your backend servers have `online-mode=false`.
3. Configure your lobbies in two places:

### Plugin Config (config.yml)
This file is located in `src/main/resources/config.yml` (it will be copied to `plugins/vLobbyConnect/config.yml` on first run):

```yaml
lobbies:
  1.20lobby1: "name1"
  1.20lobby2: "name2"
  1.8lobby1: "name3"
  1.8lobby2: "name4"
```

### Velocity Server Configuration (velocity.toml)
In your `velocity.toml`, configure the servers with the required modifications. For example:

```toml
[servers]
name1 = "ip"
name2 = "ip"
name3 = "ip"
name4 = "ip"
try = []             # Fallback is empty
```

## Commands

- **/lobby**: Connects the player to the correct lobby based on their protocol version.
- **/hub**: Transfers the player back to the designated hub/lobby.

Logs will provide further details if lobbies are full or misconfigured.

Enjoy using vLobbyConnect!