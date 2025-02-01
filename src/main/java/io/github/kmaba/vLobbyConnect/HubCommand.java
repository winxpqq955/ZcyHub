package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

public class HubCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;
    private final Map<String, String> lobbies;

    @SuppressWarnings("unchecked")
    public HubCommand(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        Map<String, String> loadedLobbies = null;
        try {
            logger.info("Loading lobby configuration for HubCommand...");
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
                logger.error("Failed to load valid lobby settings from config file.");
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
        logger.info("HubCommand execution started.");
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players."));
            logger.warn("Non-player command source attempted to use HubCommand.");
            return;
        }

        Player player = (Player) source;
        int protocol = player.getProtocolVersion().getProtocol();
        RegisteredServer targetServer = null;

        if (protocol <= 47) {
            String lobby1Name = lobbies.get("1.8lobby1");
            String lobby2Name = lobbies.get("1.8lobby2");
            Optional<RegisteredServer> lobby1Opt = server.getServer(lobby1Name);
            Optional<RegisteredServer> lobby2Opt = server.getServer(lobby2Name);
            if (lobby1Opt.isPresent() && lobby1Opt.get().getPlayersConnected().size() < 500) {
                targetServer = lobby1Opt.get();
            } else if (lobby2Opt.isPresent() && lobby2Opt.get().getPlayersConnected().size() < 500) {
                targetServer = lobby2Opt.get();
            } else {
                player.sendMessage(Component.text("All 1.8 lobbies are full, please try again later."));
                return;
            }
        } else {
            String lobby1Name = lobbies.get("1.20lobby1");
            String lobby2Name = lobbies.get("1.20lobby2");
            Optional<RegisteredServer> lobby1Opt = server.getServer(lobby1Name);
            Optional<RegisteredServer> lobby2Opt = server.getServer(lobby2Name);
            if (lobby1Opt.isPresent() && lobby1Opt.get().getPlayersConnected().size() < 500) {
                targetServer = lobby1Opt.get();
            } else if (lobby2Opt.isPresent() && lobby2Opt.get().getPlayersConnected().size() < 500) {
                targetServer = lobby2Opt.get();
            } else {
                player.sendMessage(Component.text("All 1.20+ lobbies are full, please try again later."));
                return;
            }
        }

        if (player.getCurrentServer().isPresent() &&
            player.getCurrentServer().get().getServerInfo().getName().equals(targetServer.getServerInfo().getName())) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cYou are already in a lobby."));
            return;
        }

        logger.info("Player {} connecting to lobby '{}'", player.getUsername(), targetServer.getServerInfo().getName());
        player.createConnectionRequest(targetServer).fireAndForget();
    }
}
