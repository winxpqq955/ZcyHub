# vLobbyConnect – The Ultimate Lobby Manager for Velocity Proxy  

vLobbyConnect is a **powerful and lightweight Velocity plugin** designed to seamlessly manage lobby connections for **players using different Minecraft protocol versions**. Whether your server supports multiple Minecraft versions or needs efficient load balancing, vLobbyConnect ensures players are sent to the **correct lobby** every time.  

## 🚀 Why Use vLobbyConnect?  
- **Version-Specific Lobby Assignment** – Automatically sends players to the appropriate lobby based on their Minecraft version.  
- **Seamless Load Balancing** – Distributes players evenly across multiple lobbies, preventing overcrowding and lag.  
- **Failsafe Mechanisms** – If a lobby is misconfigured or full, players are redirected to an available fallback lobby.  
- **Easy Setup & Configuration** – Just drop the plugin into Velocity, configure the lobbies, and you're good to go!  

## Setup

1. Place the plugin jar in your Velocity plugins folder.
2. Configure your lobbies in two places:

### Plugin Config (config.yml)
This file is located in `src/main/resources/config.yml` (it will be copied to `plugins/vLobbyConnect/config.yml` on first run):

```yaml
lobbies:
  1.20lobby1: "name1"
  1.20lobby2: "name2"
  1.8lobby1: "name3"
  1.8lobby2: "name4"

# To add more lobbies, follow the pattern "VERSIONlobbyX"
# Example:
# 1.13lobby8: "name5"
```

### Velocity Server Configuration (velocity.toml)
In your `velocity.toml`, configure the servers with the required modifications. For example:

```toml
[servers]
name1 = "ip"
name2 = "ip"
name3 = "ip"
name4 = "ip"
try = []             # keep fallback empty
```

## ⚡ Commands  
- **/lobby**  
- **/hub**  

These 2 Instantly teleports the player to the appropriate lobby.

## 🛡️ Future Enhancements (Planned Features)  
- **Customizable Messages** – Modify join/fallback messages in `config.yml`.  
- **Priority Lobbies** – Assign preferred lobbies based on player rank or permissions.  

## 🎮 Conclusion  
vLobbyConnect is the **ultimate lobby management solution** for Velocity servers, ensuring a smooth, version-compatible experience for all players. Download it today and **enhance your network’s performance and player experience!** 🚀
