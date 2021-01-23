package dev.dhdf.polo.sponge;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import dev.dhdf.polo.PoloPlugin;
import dev.dhdf.polo.types.PoloPlayer;
import dev.dhdf.polo.util.Sync;
import dev.dhdf.polo.webclient.WebClient;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;


@Plugin(id = "polo",
        name = "Polo",
        version = "@VERSION@",
        description = "A bridge between Matrix and Minecraft")
public class Main implements PoloPlugin {
    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    @Inject
    private Logger logger;

    private WebClient client;

    @Listener
    public void onServerStart(GameStartedServerEvent evt) {
        Config cfg = loadConfig();

        client = new WebClient(this, cfg.getConfig());

        if (!client.vibeCheck()) {
            logger.error("Couldn't properly connect to Marco, is the address and port set properly?");
            return;
        }

        Task.builder()
                .async()
                .execute(new Sync(client))
                .interval(5, TimeUnit.SECONDS)
                .submit(this);
    }

    @Listener(order = Order.POST)
    public void onChat(MessageChannelEvent.Chat evt, @First Player source) {
        // Sponge's broadcast channel may have changed, but we want to capture general player chat,
        // so use the player's default channel MessageChannel.TO_ALL.
        // On the other hand, chat plugins may have explicitly set the channel to the broadcast channel,
        // so check that one as well.
        if (evt.isMessageCancelled() || !evt.getChannel()
                .filter(channel -> MessageChannel.TO_ALL.equals(channel)
                        || Sponge.getServer().getBroadcastChannel().equals(channel))
                .isPresent()) {
            return;
        }

        PoloPlayer player = new PoloPlayer(source.getName(), source.getUniqueId(), null, null);
        String message = evt.getFormatter().getBody().toText().toPlain();
        this.client.postChat(player, message);
    }

    private Config loadConfig() {
        Config cfg = null;
        try {
            cfg = configManager.load().getValue(TypeToken.of(Config.class));
        } catch (IOException | ObjectMappingException ex) {
            logger.error("Failed to load config, saving defaults", ex);
        }

        if (cfg != null)
            return cfg;

        try {
            logger.warn("Generating default config, please change it!");
            configManager.save(configManager.createEmptyNode().setValue(Config.TYPE_TOKEN, Config.DEFAULT));
        } catch (IOException | ObjectMappingException e) {
            logger.error("Unable to save defaults", e);
        }
        return Config.DEFAULT;
    }

    @Override
    public void broadcastMessage(String message) {
        Sponge.getServer().getBroadcastChannel().send(Text.of(message));
    }

    @Override
    public void kickPlayer(UUID uuid, String reason, String source) {
    }

    @Override
    public void banPlayer(UUID uuid, String reason, String source) {
    }

    @Override
    public void unbanPlayer(UUID uuid, String source) {
    }

    @Override
    public void executeAsync(Runnable task) {
        Task.builder()
                .async()
                .execute(task)
                .submit(this);
    }

    @ConfigSerializable
    public static final class Config {
        public static final TypeToken<Config> TYPE_TOKEN = TypeToken.of(Config.class);
        public static final Config DEFAULT = new Config("localhost", 3051, "");

        @Setting
        private String address;
        @Setting
        private int port;
        @Setting
        private String token;

        // Needed for object mapping
        private Config() {}

        public Config(String address, int port, String token) {
            this.address = address;
            this.port = port;
            this.token = token;
        }

        public dev.dhdf.polo.webclient.Config getConfig() {
            return new dev.dhdf.polo.webclient.Config(address, port, token, false, false, false);
        }
    }
}
