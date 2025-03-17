package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HubCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;

    @SuppressWarnings("unchecked")
    public HubCommand(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        final var versionLobbies = VelocityPlugin.INSTANCE.getLobbies();
        final var arguments = invocation.arguments();
        logger.info("HubCommand execution started.");
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players."));
            logger.warn("Non-player command source attempted to use HubCommand.");
            return;
        }
        Player player = (Player) source;
        String targetHub = "main";
        if (arguments.length != 0) {
            targetHub = arguments[0];
        }
        Send2Any.INSTANCE.send2Any(player, targetHub);
    }
}
