package io.github.kmaba.vLobbyConnect;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.nio.charset.StandardCharsets;

public enum BackendServer {
    INSTANCE;
    private static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("zcyhub:v1");

    public void init() {
        VelocityPlugin.INSTANCE.getServer().getChannelRegistrar().register(IDENTIFIER);
    }

    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        if (!IDENTIFIER.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        final var p = new String(event.getData(), StandardCharsets.UTF_8).split(",", 2);
        final var maybePlayer = VelocityPlugin.INSTANCE.getServer().getPlayer(p[0]);
        if (maybePlayer.isPresent()) {
            final var player = maybePlayer.get();
            Send2Any.INSTANCE.send2Any(player, p[1]);
        }
    }



}
