package io.github.kmaba.vLobbyConnect;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Plugin(
	id = "zcyhub",
	name = "ZcyHub",
	description = "A Velocity Plugin for Lobby Connection",
	version = "1.0",
	authors = { "ZhangChengyu" }
)
public final class VelocityPlugin {
	public static VelocityPlugin INSTANCE;
	@Inject
	private Logger logger;
	@Inject
	private ProxyServer server;
	private final Map<String, List<RegisteredServer>> lobbies = new HashMap<>();
	private final Map<UUID, Integer> connectionAttempts = new ConcurrentHashMap<>();
	private final Path directory;
	@Inject
	public VelocityPlugin(@DataDirectory Path directory) {
		INSTANCE = this;
		this.directory = directory;
	}

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		ConfigLoader.INSTANCE.load(directory);
		final var commandManager = server.getCommandManager();
		final var meta = commandManager.metaBuilder("hub")
				.aliases("lobby")
				.build();
		commandManager.register(meta, new HubCommand(server, logger));
		server.getEventManager().register(this, BackendServer.INSTANCE);
		BackendServer.INSTANCE.init();
	}

	@Subscribe(order = PostOrder.FIRST)
	void onPlayerJoin(final PlayerChooseInitialServerEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		int attempts = connectionAttempts.getOrDefault(uuid, 0) + 1;
		connectionAttempts.put(uuid, attempts);

		String version = player.getProtocolVersion().getName();
		List<RegisteredServer> lobbies = this.lobbies.get(version);
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
			.toList();
		
		if (onlineLobbies.isEmpty()) {
			return null;
		}

		return onlineLobbies.stream()
			.min(Comparator.comparingInt(server -> server.getPlayersConnected().size()))
			.orElse(null);
	}

	// Helper: Fallback to the highest available lobby version when an exact match is missing.
	private List<RegisteredServer> getFallbackLobbies(String playerVersion) {
		return lobbies.values().stream()
			.flatMap(List::stream)
			.filter(this::isServerOnline)
			.collect(Collectors.toList());
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
	public void onServerKick(KickedFromServerEvent event) {
		Player player = event.getPlayer();
		RegisteredServer kickedServer = event.getServer();
		String serverName = kickedServer.getServerInfo().getName();

		// If the kicked server is already a lobby, do nothing.
		if (lobbies.values().stream().flatMap(List::stream).anyMatch(server -> server.getServerInfo().getName().equals(serverName))) {
			return;
		}

		RegisteredServer fallback = null;
		String version = player.getProtocolVersion().getName();
		List<RegisteredServer> lobbies = this.lobbies.get(version);

		if (lobbies != null && !lobbies.isEmpty()) {
			fallback = getLeastLoadedLobby(lobbies);
		}

		if (fallback != null) {
			event.setResult(KickedFromServerEvent.RedirectPlayer.create(fallback));
		}
	}

	@Subscribe
	public void onPlayerDisconnect(Player player) {
		UUID uuid = player.getUniqueId();
		connectionAttempts.remove(uuid);
		logger.info("Player {} disconnected.", player.getUsername());
	}

	public ProxyServer getServer() {
		return server;
	}

	public Map<String, List<RegisteredServer>> getLobbies() {
		return lobbies;
	}

	public Map<UUID, Integer> getConnectionAttempts() {
		return connectionAttempts;
	}

	public Path getDirectory() {
		return directory;
	}
}
