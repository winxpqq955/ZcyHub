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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Plugin(
	id = "vlobbyconnect",
	name = "vLobbyConnect",
	url = "https://kmaba.link/",
	description = "A Velocity Plugin for Lobby Connection",
	version = Constants.VERSION,
	authors = { "kmaba" }
)
public final class VelocityPlugin {
	@Inject
	private Logger logger;

	@Inject
	private com.velocitypowered.api.proxy.ProxyServer server;

	@Inject
	private Metrics.Factory metricsFactory;

	private final Map<String, List<RegisteredServer>> versionLobbies = new HashMap<>();
	private final Map<UUID, Integer> connectionAttempts = new ConcurrentHashMap<>();

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		int pluginId = 24615;
		Metrics metrics = metricsFactory.make(this, pluginId);

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

			// Validate and log the configuration
			Pattern pattern = Pattern.compile("^(\\d+\\.\\d+)lobby(\\d+)$");
			for (Map.Entry<String, String> entry : lobbies.entrySet()) {
				Matcher matcher = pattern.matcher(entry.getKey());
				if (matcher.matches()) {
					String version = matcher.group(1);
					String lobbyName = entry.getValue();
					Optional<RegisteredServer> serverOpt = server.getServer(lobbyName);
					if (serverOpt.isPresent()) {
						versionLobbies.computeIfAbsent(version, k -> new ArrayList<>()).add(serverOpt.get());
						// Updated logging: Removed protocol version from the log
						logger.info("Config Lobbies, [VERSION] {} Lobby number: {} IP: {}", version, matcher.group(2), serverOpt.get().getServerInfo().getAddress());
					} else {
						logger.warn("Lobby server '{}' not found in Velocity configuration.", lobbyName);
					}
				} else {
					logger.warn("Invalid lobby configuration key: {}", entry.getKey());
				}
			}

			// Check if all lobbies were retrieved successfully
			if (versionLobbies.isEmpty()) {
				logger.error("No valid lobbies were found. Ensure they are defined in velocity.toml.");
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

		String version = player.getProtocolVersion().getName();
		List<RegisteredServer> lobbies = versionLobbies.get(version);
		// Fallback if no exact match exists:
		if (lobbies == null || lobbies.isEmpty()) {
			lobbies = getFallbackLobbies(version);
		}
		
		if (lobbies == null || lobbies.isEmpty()) {
			player.sendMessage(Component.text("No lobbies available for your Minecraft version."));
			logger.warn("No lobbies available for version {}", version);
			return;
		}

		RegisteredServer targetServer = getLeastLoadedLobby(lobbies);

		if (targetServer == null) {
			// Try fallback lobbies if all version-specific lobbies are offline
			List<RegisteredServer> fallbackLobbies = getFallbackLobbies(version);
			if (fallbackLobbies != null && !fallbackLobbies.isEmpty()) {
				targetServer = getLeastLoadedLobby(fallbackLobbies);
			}
		}

		if (targetServer == null) {
			player.sendMessage(Component.text("All lobbies are currently unavailable, please try again later."));
			logger.warn("All lobbies are offline for version {}", version);
			return;
		}

		if (player.getCurrentServer().isPresent() &&
			player.getCurrentServer().get().getServerInfo().getName().equals(targetServer.getServerInfo().getName())) {
			player.sendMessage(Component.text("You are already in a lobby."));
			return;
		}

		logger.info("Player {} connecting to lobby '{}'", player.getUsername(), targetServer.getServerInfo().getName());
		// Instead of a connection request, set the initial server directly:
		event.setInitialServer(targetServer);
	}

	private RegisteredServer getLeastLoadedLobby(List<RegisteredServer> lobbies) {
		// Filter only online lobbies
		List<RegisteredServer> onlineLobbies = lobbies.stream()
			.filter(this::isServerOnline)
			.collect(Collectors.toList());
		
		if (onlineLobbies.isEmpty()) {
			return null;
		}

		return onlineLobbies.stream()
			.min(Comparator.comparingInt(server -> server.getPlayersConnected().size()))
			.orElse(null);
	}

	// Helper: Fallback to the highest available lobby version when an exact match is missing.
	private List<RegisteredServer> getFallbackLobbies(String playerVersion) {
		return versionLobbies.entrySet().stream()
				.filter(entry -> compareVersions(entry.getKey(), playerVersion) <= 0)
				.max((a, b) -> compareVersions(a.getKey(), b.getKey()))
				.map(Map.Entry::getValue)
				.orElse(null);
	}

	// Helper: Compare version strings (e.g. "1.8" vs "1.21.1")
	private int compareVersions(String v1, String v2) {
		String[] parts1 = v1.split("\\.");
		String[] parts2 = v2.split("\\.");
		int len = Math.max(parts1.length, parts2.length);
		for (int i = 0; i < len; i++) {
			int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
			int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
			if (num1 != num2) {
				return num1 - num2;
			}
		}
		return 0;
	}

	// Add this new helper method
	private boolean isServerOnline(RegisteredServer server) {
		try {
			// Try to ping the server with a short timeout
			server.ping().get(2, TimeUnit.SECONDS);
			return true;
		} catch (Exception e) {
			logger.warn("Lobby '{}' appears to be offline", server.getServerInfo().getName());
			return false;
		}
	}

	@Subscribe
	public void onServerKick(com.velocitypowered.api.event.player.KickedFromServerEvent event) {
		Player player = event.getPlayer();
		RegisteredServer kickedServer = event.getServer();
		String serverName = kickedServer.getServerInfo().getName();

		// If the kicked server is already a lobby, do nothing.
		if (versionLobbies.values().stream().flatMap(List::stream).anyMatch(server -> server.getServerInfo().getName().equals(serverName))) {
			return;
		}

		RegisteredServer fallback = null;
		String version = player.getProtocolVersion().getName();
		List<RegisteredServer> lobbies = versionLobbies.get(version);

		if (lobbies != null && !lobbies.isEmpty()) {
			fallback = getLeastLoadedLobby(lobbies);
		}

		if (fallback != null) {
			event.setResult(com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer.create(fallback));
		}
	}

	@Subscribe
	public void onPlayerDisconnect(Player player) {
		UUID uuid = player.getUniqueId();
		connectionAttempts.remove(uuid);
		logger.info("Player {} disconnected.", player.getUsername());
	}
}
