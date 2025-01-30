package io.github.kmaba.vLobbyConnect;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.util.Optional;

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

	private RegisteredServer lobby1;
	private RegisteredServer lobby2;
	private RegisteredServer lobby3;
	private RegisteredServer lobby4;

	@Subscribe
	void onProxyInitialization(final ProxyInitializeEvent event) {
		// Initialize lobbies
		Optional<RegisteredServer> lobby1Opt = event.getServer().getServer("lobby1");
		Optional<RegisteredServer> lobby2Opt = event.getServer().getServer("lobby2");
		Optional<RegisteredServer> lobby3Opt = event.getServer().getServer("lobby3");
		Optional<RegisteredServer> lobby4Opt = event.getServer().getServer("lobby4");

		if (lobby1Opt.isPresent() && lobby2Opt.isPresent() && lobby3Opt.isPresent() && lobby4Opt.isPresent()) {
			lobby1 = lobby1Opt.get();
			lobby2 = lobby2Opt.get();
			lobby3 = lobby3Opt.get();
			lobby4 = lobby4Opt.get();
			logger.info("vLobbyConnect initialized successfully.");
		} else {
			logger.error("Failed to initialize lobbies. Make sure all lobbies are registered.");
		}
	}

	@Subscribe
	void onPlayerJoin(final PlayerChooseInitialServerEvent event) {
		Player player = event.getPlayer();
		int protocolVersion = player.getProtocolVersion().getProtocol();

		logger.info("Player {} joined with protocol version {}", player.getUsername(), protocolVersion);

		if (protocolVersion >= 760) { // 1.20+
			if (lobby1.getPlayersConnected().size() < lobby1.getServerInfo().getMaxPlayers()) {
				event.setInitialServer(lobby1);
				logger.info("Redirecting player {} to lobby1", player.getUsername());
			} else if (lobby2.getPlayersConnected().size() < lobby2.getServerInfo().getMaxPlayers()) {
				event.setInitialServer(lobby2);
				logger.info("Redirecting player {} to lobby2", player.getUsername());
			} else {
				logger.warn("All 1.20+ lobbies are full. Player {} could not be redirected.", player.getUsername());
			}
		} else if (protocolVersion == 47) { // 1.8
			if (lobby3.getPlayersConnected().size() < lobby3.getServerInfo().getMaxPlayers()) {
				event.setInitialServer(lobby3);
				logger.info("Redirecting player {} to lobby3", player.getUsername());
			} else if (lobby4.getPlayersConnected().size() < lobby4.getServerInfo().getMaxPlayers()) {
				event.setInitialServer(lobby4);
				logger.info("Redirecting player {} to lobby4", player.getUsername());
			} else {
				logger.warn("All 1.8 lobbies are full. Player {} could not be redirected.", player.getUsername());
			}
		} else {
			logger.warn("Unsupported protocol version {} for player {}", protocolVersion, player.getUsername());
		}
	}
}
