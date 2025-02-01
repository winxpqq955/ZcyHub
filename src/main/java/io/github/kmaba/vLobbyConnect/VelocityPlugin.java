package io.github.kmaba.vLobbyConnect;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.yaml.snakeyaml.Yaml;

import net.kyori.adventure.text.Component;

import java.util.UUID;

@Plugin(
	id = "vlobbyconnect",
	name = "vLobbyConnect",
	description = "A Velocity Plugin for Lobby Connection",
	version = Constants.VERSION,
	authors = { "kmaba" }
)
public final class VelocityPlugin {
	@Inject
	private Logger logger;

	@Inject
	private com.velocitypowered.api.proxy.ProxyServer server;

	private RegisteredServer lobby1;
	private RegisteredServer lobby2;
	private RegisteredServer lobby3;
	private RegisteredServer lobby4;

	private final Map<UUID, Integer> connectionAttempts = new ConcurrentHashMap<>();

	@Subscribe
public void onProxyInitialize(ProxyInitializeEvent event) {
    try {
        // Load the config.yml file
        Yaml yaml = new Yaml();
        File configFile = new File("plugins/vLobbyConnect/config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            Files.copy(getClass().getResourceAsStream("/config.yml"), configFile.toPath());
        }

        // Parse the config.yml file
        Map<String, Object> config = yaml.load(Files.newInputStream(configFile.toPath()));
        Map<String, String> lobbies = (Map<String, String>) config.get("lobbies");
        if (lobbies == null) {
            logger.error("Failed to load lobby settings.");
            return;
        }

        // Retrieve the registered servers
        String lobby1Name = lobbies.get("1.20lobby1");
        String lobby2Name = lobbies.get("1.20lobby2");
        String lobby3Name = lobbies.get("1.8lobby1");
        String lobby4Name = lobbies.get("1.8lobby2");

        lobby1 = server.getServer(lobby1Name).orElse(null);
        lobby2 = server.getServer(lobby2Name).orElse(null);
        lobby3 = server.getServer(lobby3Name).orElse(null);
        lobby4 = server.getServer(lobby4Name).orElse(null);

        // Check if all lobbies were retrieved successfully
        if (lobby1 == null || lobby2 == null || lobby3 == null || lobby4 == null) {
            logger.error("One or more lobbies were not found. Ensure they are defined in velocity.toml.");
        } else {
            logger.info("vLobbyConnect initialized successfully.");
        }
    } catch (IOException e) {
        logger.error("Failed to load config.yml", e);
    }

    // Register commands
    server.getCommandManager().register("hub", new HubCommand(server, logger));
    server.getCommandManager().register("lobby", new LobbyCommand(server, logger));
}

	@Subscribe(order = PostOrder.FIRST)
    void onPlayerJoin(final PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        int attempts = connectionAttempts.getOrDefault(uuid, 0) + 1;
        connectionAttempts.put(uuid, attempts);

        int protocolVersion = player.getProtocolVersion().getProtocol();
        if (protocolVersion <= 47) {
            // 1.8 users → lobby3 or lobby4
            if (lobby3 != null && lobby3.getPlayersConnected().size() < 500) {
                event.setInitialServer(lobby3);
            } else if (lobby4 != null && lobby4.getPlayersConnected().size() < 500) {
                event.setInitialServer(lobby4);
            } else {
                logger.warn("All 1.8 lobbies are full for player {}", player.getUsername());
                player.sendMessage(Component.text("All 1.8 lobbies are full, please try again later."));
                player.disconnect(Component.text("All lobbies are full."));
            }
        } else {
            // 1.20+ users → lobby1 or lobby2
            if (lobby1 != null && lobby1.getPlayersConnected().size() < 500) {
                event.setInitialServer(lobby1);
            } else if (lobby2 != null && lobby2.getPlayersConnected().size() < 500) {
                event.setInitialServer(lobby2);
            } else {
                logger.warn("All 1.20+ lobbies are full for player {}", player.getUsername());
                player.sendMessage(Component.text("All 1.20+ lobbies are full, please try again later."));
                player.disconnect(Component.text("All lobbies are full."));
            }
        }
    }

    @Subscribe
    public void onPlayerDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        connectionAttempts.remove(uuid);
        logger.info("Player {} disconnected.", player.getUsername());
    }
}
