**Description:**  
vLobbyConnect is a Velocity plugin that automatically directs players to the correct lobby based on their Minecraft version upon joining the server. This ensures seamless matchmaking and prevents version incompatibility issues.  

### **Features:**  
✅ **Automatic Version Detection** – Identifies the player’s Minecraft version on join.  
✅ **Smart Lobby Assignment** – Sends players to an available lobby that matches their version.  
✅ **Multiple Lobby Support** – Supports both 1.20+ and 1.8 versions with predefined lobbies.  
✅ **Fallback Handling** – If a lobby is full, the player is sent to another available option.  
✅ **Efficient & Lightweight** – Optimized for performance with minimal impact on server resources.  
✅ **Logging & Debugging** – Provides logs for join events and redirections.  

### **Lobby Mapping:**  
- **1.20+ Players →** `lobby1` or `lobby2`  
- **1.8 Players →** `lobby3` or `lobby4`  

### **Configuration:**
To configure the lobby names, edit the `config.yml` file:

```yaml
lobbies:
  1.20lobby1: "lobby1"
  1.20lobby2: "lobby2"
  1.8lobby1: "lobby3"
  1.8lobby2: "lobby4"
```

These names must match the server entries defined in your velocity.toml.

### **Commands:**
The plugin provides the following commands for players to switch between lobbies:

- `/hub [lobbyName]`: Switch to the specified lobby or default to `lobby1` if no lobby name is provided.
- `/lobby [lobbyName]`: Switch to the specified lobby or default to `lobby1` if no lobby name is provided.

### **Usage:**
To use the commands, simply type them in the chat while connected to the server. For example:

- `/hub`: Connects you to the default lobby (`lobby1`).
- `/hub lobby2`: Connects you to `lobby2`.
- `/lobby`: Connects you to the default lobby (`lobby1`).
- `/lobby lobby3`: Connects you to `lobby3`.

### **Error Handling:**
The plugin handles invalid lobby names and lobby switching errors gracefully by:

- Logging an error message indicating the issue.
- Notifying the player about the error.
- Providing a fallback mechanism to redirect the player to a default lobby or disconnect them gracefully.

### **Player Disconnections:**
The plugin handles player disconnections by:

- Logging an informational message indicating the player disconnection.
- Notifying the player that they have been disconnected.
- Ensuring that any resources or data associated with the player are cleaned up properly to avoid memory leaks or other issues.
