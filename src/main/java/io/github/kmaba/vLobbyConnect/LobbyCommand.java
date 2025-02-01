package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

public class LobbyCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;
    private final Map<String, String> lobbies;

    @SuppressWarnings("unchecked")
    public LobbyCommand(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        Map<String, String> loadedLobbies = null;
        try {
            logger.info("Loading lobby configuration for LobbyCommand...");
            Yaml yaml = new Yaml();
            File configFile = new File("plugins/vLobbyConnect/config.yml");
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                Files.copy(getClass().getResourceAsStream("/config.yml"), configFile.toPath());
                logger.info("Config file created from resource.");
            }
            Map<String, Object> config = yaml.load(Files.newInputStream(configFile.toPath()));
            loadedLobbies = (Map<String, String>) config.get("lobbies");
            if (loadedLobbies == null) {
                logger.error("Failed to load valid lobby settings.");
            } else {
                logger.info("Lobby configuration loaded successfully.");
            }
        } catch (IOException e) {
            logger.error("Error loading config.yml", e);
        }
        this.lobbies = loadedLobbies;
    }

    @Override
    public void execute(Invocation invocation) {
        logger.info("LobbyCommand execution started.");
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players."));
            logger.warn("Non-player command source attempted to use LobbyCommand.");
            return;
        }

        Player player = (Player) source;
        // Use player.getProtocolVersion() directly instead of casting to ConnectedPlayer
        int protocol = player.getProtocolVersion().getProtocol();
        String defaultLobbyKey = (protocol <= 47) ? "1.8lobby1" : "1.20lobby1";
        String typedKey = (args.length > 0) ? args[0] : defaultLobbyKey;

        String actualLobbyName = (lobbies != null) ? lobbies.get(typedKey) : null;
        if (actualLobbyName == null) {
            player.sendMessage(Component.text("No valid lobby config found for key: " + typedKey));
            logger.error("No lobby configuration found for key: {}", typedKey);
            return;
        }

        Optional<RegisteredServer> targetServer = server.getServer(actualLobbyName);
        if (!targetServer.isPresent()) {
            player.sendMessage(Component.text("Invalid lobby name: " + typedKey));
            logger.error("Invalid lobby name: {}. Check your velocity.toml configuration.", typedKey);
            return;
        }

        logger.info("Player {} connecting to lobby '{}'", player.getUsername(), typedKey);
        player.createConnectionRequest(targetServer.get()).fireAndForget();
        player.sendMessage(Component.text("Connecting to " + typedKey + "..."));
    }
}