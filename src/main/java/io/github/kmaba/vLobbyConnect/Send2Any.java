package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public enum Send2Any {
    INSTANCE;
    private final org.slf4j.Logger logger = LoggerFactory.getLogger("VLobbyConnect-Send2Any");

    public void send2Any(Player player, String targetHub) {
        List<RegisteredServer> lobbies = VelocityPlugin.INSTANCE.getLobbies().get(targetHub);

        if (lobbies == null || lobbies.isEmpty()) {
            player.sendMessage(Component.text("没有大厅 [SendAny-1]"));
            logger.warn("No lobbies available");
            return;
        }

        RegisteredServer targetServer = getLeastLoadedLobby(lobbies);

        if (targetServer == null) {
            player.sendMessage(Component.text("大厅已满 [SendAny-2]"));
            logger.warn("All lobbies are full");
            return;
        }

        // Instead of checking if current server equals target only, check if player's current server is any hub.
        if (player.getCurrentServer().isPresent() &&
                lobbies.stream().anyMatch(s -> s.getServerInfo().getName().equals(
                        player.getCurrentServer().get().getServerInfo().getName()
                ))) {
            //让后端把这个蠢货送回出生点
            player.getCurrentServer().ifPresent(serverConnection ->
                    serverConnection.sendPluginMessage(
                            () -> "hub2spawn:v1", player.getUsername().getBytes(StandardCharsets.UTF_8)
                    )
            );
            //player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cYou are already in a lobby."));
            return;
        }

        logger.info("Player {} connecting to lobby '{}'", player.getUsername(), targetServer.getServerInfo().getName());
        player.createConnectionRequest(targetServer).fireAndForget();
    }

    private RegisteredServer getLeastLoadedLobby(List<RegisteredServer> lobbies) {
        return lobbies.stream()
                .filter(this::isServerOnline)
                .max(Comparator.comparingInt(server -> server.getPlayersConnected().size()))
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
}
