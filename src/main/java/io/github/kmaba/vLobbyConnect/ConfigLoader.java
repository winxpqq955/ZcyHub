package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ConfigLoader {
    INSTANCE;
    private final Logger logger = LoggerFactory.getLogger("ZcyHub-Config");

    public void load(Path directory) {
        try {
            // Load the config.yml file
            Yaml yaml = new Yaml();
            final var base = directory.toString();
            File configFile = new File(base + "/config.yml");
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                Files.copy(getClass().getResourceAsStream("/config.yml"), configFile.toPath());
            }

            // Parse the config.yml file
            Map<String, Object> config = yaml.load(Files.newInputStream(configFile.toPath()));
            Map<String, Object> lobbies = (Map<String, Object>) config.get("lobbies");
            if (lobbies == null) {
                logger.error("Failed to load lobby settings.");
                return;
            }
            for (Map.Entry<String, Object> sub_lobbies : lobbies.entrySet()) {
                final var target_lobby_name = sub_lobbies.getKey();
                final var target_sub_server_list = (List<String>) sub_lobbies.getValue();
                final var temp = new ArrayList<RegisteredServer>();
                for (String s : target_sub_server_list) {
                    Optional<RegisteredServer> serverOpt = VelocityPlugin.INSTANCE.getServer().getServer(s);
                    if (serverOpt.isPresent()) {
                        final var registeredServer = serverOpt.get();
                        temp.add(registeredServer);
                        logger.info("Added Lobby Server, Group '{}' Name '{}' IP: '{}'", target_lobby_name, registeredServer.getServerInfo().getName(), registeredServer.getServerInfo().getAddress());
                    } else {
                        logger.warn("Group '{}' Lobby server '{}' not found in Velocity configuration.", target_lobby_name , s);
                    }
                }
                VelocityPlugin.INSTANCE.getLobbies().put(target_lobby_name, temp);
            }
            // Check if all lobbies were retrieved successfully
            if (VelocityPlugin.INSTANCE.getLobbies().isEmpty()) {
                logger.error("No valid lobbies were found. Ensure they are defined in velocity.toml.");
            } else {
                logger.info("vLobbyConnect initialized successfully.");
            }
        } catch (IOException e) {
            logger.error("Failed to load config.yml", e);
        }
    }
}
