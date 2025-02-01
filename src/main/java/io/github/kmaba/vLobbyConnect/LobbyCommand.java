package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Optional;

public class LobbyCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;

    public LobbyCommand(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players."));
            return;
        }

        Player player = (Player) source;
        String targetLobby = args.length > 0 ? args[0] : "lobby1"; // Default to lobby1 if no argument is provided

        Optional<RegisteredServer> targetServer = server.getServer(targetLobby);
        if (!targetServer.isPresent()) {
            player.sendMessage(Component.text("Invalid lobby name: " + targetLobby));
            logger.error("Invalid lobby name: " + targetLobby);
            return;
        }

        player.createConnectionRequest(targetServer.get()).fireAndForget();
        player.sendMessage(Component.text("Connecting to " + targetLobby + "..."));
    }
}
