package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ConfigLoader {
    INSTANCE;
    private final Logger logger = LoggerFactory.getLogger("ZcyHub-Config");
    private String fallbackGroup;

    public void load(Path directory) {
        try {
            // Load the config.yml file
            Yaml yaml = new Yaml();
            File configFile = new File(directory.toString() + "/config.yml");
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")), configFile.toPath());
            }

            // Parse the config.yml file
            Map<String, Object> config = yaml.load(Files.newInputStream(configFile.toPath()));
            this.fallbackGroup = (String) config.get("fallbackGroup");
            Map<String, Object> lobbies = (Map<String, Object>) config.get("lobbies");
            if (lobbies == null) {
                logger.error("Failed to load lobby settings.");
                return;
            }

            // 清除旧的大厅配置
            VelocityPlugin.INSTANCE.getLobbies().clear();

            // 处理每个大厅组
            for (Map.Entry<String, Object> groupEntry : lobbies.entrySet()) {
                String groupName = groupEntry.getKey();
                Map<String, Object> groupConfig = (Map<String, Object>) groupEntry.getValue();

                // 处理main部分（必须存在）
                if (groupConfig.containsKey("main")) {
                    Object mainConfig = groupConfig.get("main");
                    if (mainConfig instanceof ArrayList) {
                        List<RegisteredServer> mainServers = registerLobbyServer(groupName, (ArrayList<?>) mainConfig);
                        VelocityPlugin.INSTANCE.getLobbies().put(groupName, mainServers);
                    } else {
                        logger.warn("组 '{}' 的main配置格式不正确", groupName);
                    }
                } else {
                    logger.warn("组 '{}' 缺少main配置", groupName);
                }

                // 处理sub部分（可选）
                if (groupConfig.containsKey("sub")) {
                    Map<String, Object> subConfig = (Map<String, Object>) groupConfig.get("sub");
                    for (Map.Entry<String, Object> subEntry : subConfig.entrySet()) {
                        String subGroupName = subEntry.getKey();
                        String fullGroupName = groupName + "." + subGroupName;

                        if (subEntry.getValue() instanceof ArrayList) {
                            List<RegisteredServer> subServers = registerLobbyServer(fullGroupName, (ArrayList<?>) subEntry.getValue());
                            VelocityPlugin.INSTANCE.getLobbies().put(fullGroupName, subServers);
                        } else {
                            logger.warn("子组 '{}' 的配置格式不正确", fullGroupName);
                        }
                    }
                }
            }
            // Check if all lobbies were retrieved successfully
            if (VelocityPlugin.INSTANCE.getLobbies().isEmpty()) {
                logger.error("No valid lobbies were found. Ensure they are defined in velocity.toml.");
            } else {
                logger.info("initialized successfully.");
            }
        } catch (IOException e) {
            logger.error("Failed to load config.yml", e);
        }
    }

    private List<RegisteredServer> registerLobbyServer(final String groupName, final ArrayList<?> targetServerList) {
        final List<RegisteredServer> servers = new ArrayList<>();
        for (Object item : targetServerList) {
            if (item instanceof String serverName) {
                // 处理直接的服务器名称
                Optional<RegisteredServer> serverOpt = VelocityPlugin.INSTANCE.getServer().getServer(serverName);
                if (serverOpt.isPresent()) {
                    RegisteredServer server = serverOpt.get();
                    servers.add(server);
                    logger.info("注册大厅: 组 '{}', 名称 '{}', 地址 '{}'",
                            groupName, server.getServerInfo().getName(), server.getServerInfo().getAddress());
                } else {
                    logger.warn("组 '{}' 中的服务器 '{}' 未在Velocity配置中找到", groupName, serverName);
                }
            } else if (item instanceof LinkedHashMap<?,?>) {
                // 处理名称:地址 格式
                @SuppressWarnings("unchecked")
                Map<String, String> serverMap = (Map<String, String>) item;
                for (Map.Entry<String, String> server : serverMap.entrySet()) {
                    String serverName = server.getKey();
                    String addressStr = server.getValue();

                    // 如果地址为空，则跳过
                    if (addressStr == null || addressStr.isEmpty()) {
                        logger.warn("组 '{}' 中的服务器 '{}' 缺少有效地址，已跳过", groupName, serverName);
                        continue;
                    }

                    try {
                        String[] addressParts = addressStr.split(":");
                        String host = addressParts[0];
                        int port = addressParts.length > 1 ? Integer.parseInt(addressParts[1]) : 25565;
                        InetSocketAddress address = new InetSocketAddress(host, port);

                        RegisteredServer registeredServer = VelocityPlugin.INSTANCE.getServer().registerServer(
                                new ServerInfo(serverName, address)
                        );
                        servers.add(registeredServer);
                        logger.info("动态注册大厅: 组 '{}', 名称 '{}', 地址 '{}'",
                                groupName, serverName, address);
                    } catch (Exception e) {
                        logger.error("注册服务器 '{}' 时出错: {}", serverName, e.getMessage());
                    }
                }
            }
        }
        return servers;
    }

    public String getFallbackGroup() {
        return fallbackGroup;
    }
}
