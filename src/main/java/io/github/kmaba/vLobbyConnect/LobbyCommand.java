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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LobbyCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;
    private final Map<String, List<RegisteredServer>> versionLobbies = new HashMap<>();

    @SuppressWarnings("unchecked")
    public LobbyCommand(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        try {
            Yaml yaml = new Yaml();
            File configFile = new File("plugins/vLobbyConnect/config.yml");
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                Files.copy(getClass().getResourceAsStream("/config.yml"), configFile.toPath());
                // Removed config logging
            }
            Map<String, Object> config = yaml.load(Files.newInputStream(configFile.toPath()));
            Map<String, String> lobbies = (Map<String, String>) config.get("lobbies");
            if (lobbies == null) {
                logger.error("Failed to load valid lobby settings from config file.");
            } else {
                Pattern pattern = Pattern.compile("^(\\d+\\.\\d+)lobby(\\d+)$");
                for (Map.Entry<String, String> entry : lobbies.entrySet()) {
                    Matcher matcher = pattern.matcher(entry.getKey());
                    if (matcher.matches()) {
                        String version = matcher.group(1);
                        String lobbyName = entry.getValue();
                        Optional<RegisteredServer> serverOpt = server.getServer(lobbyName);
                        if (serverOpt.isPresent()) {
                            versionLobbies.computeIfAbsent(version, k -> new ArrayList<>()).add(serverOpt.get());
                            // Removed config logging
                        } else {
                            // Removed config logging for missing lobby server
                        }
                    } else {
                        logger.warn("Invalid lobby configuration key: {}", entry.getKey());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error loading config.yml", e);
        }
    }

    @Override
    public void execute(Invocation invocation) {
        logger.info("LobbyCommand execution started.");
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players."));
            logger.warn("Non-player command source attempted to use LobbyCommand.");
            return;
        }

        Player player = (Player) source;
        String version = player.getProtocolVersion().getName();
        List<RegisteredServer> lobbies = versionLobbies.get(version);
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
            player.sendMessage(Component.text("All lobbies are full, please try again later."));
            logger.warn("All lobbies are full for version {}", version);
            return;
        }

        // Updated "already in a lobby" check:
        if (player.getCurrentServer().isPresent() &&
            lobbies.stream().anyMatch(s -> s.getServerInfo().getName()
                         .equals(player.getCurrentServer().get().getServerInfo().getName()))) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cYou are already in a lobby."));
            return;
        }

        logger.info("Player {} connecting to lobby '{}'", player.getUsername(), targetServer.getServerInfo().getName());
        player.createConnectionRequest(targetServer).fireAndForget();
    }

    private RegisteredServer getLeastLoadedLobby(List<RegisteredServer> lobbies) {
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

    private boolean isServerOnline(RegisteredServer server) {
        try {
            server.ping().get(2, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.warn("Lobby '{}' appears to be offline", server.getServerInfo().getName());
            return false;
        }
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
}
