package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.PoloPlugin;
import dev.dhdf.polo.util.Sync;
import dev.dhdf.polo.webclient.Config;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.BanList;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.UUID;


/**
 * This starts the plugin
 */
public class Main extends JavaPlugin implements PoloPlugin {
    @Override
    public void onEnable() {
        // Save the default config if it isn't already there
        this.saveDefaultConfig();

        // First read from the config
        FileConfiguration pluginConfig = this.getConfig();
        Logger logger = getLogger();
        boolean isValid = checkIntegrity(pluginConfig);

        logger.info("Started Polo plugin");

        if (!isValid)
            logger.severe("Incomplete config.yml, please modify");

        Config config = new Config(
                pluginConfig.getString("address"),
                pluginConfig.getInt("port"),
                pluginConfig.getString("token"),
                pluginConfig.getBoolean("relay-minecraft-membership"),
                pluginConfig.getBoolean("relay-matrix-kicks"),
                pluginConfig.getBoolean("relay-matrix-bans")
        );

        // Start up the web client
        WebClient webClient = new WebClient(
                this,
                config
        );

        // Make the WebClient log through our PluginLogger
        Logger.getLogger(WebClient.class.getName()).setParent(getLogger());

        // Start up the Minecraft event listener
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new MCListener(this, webClient), this);

        logger.info("Started webclient and chat listeners");

        // Start up the Essentials event listener if the plugin is enabled
        if (manager.isPluginEnabled("Essentials")) {
            manager.registerEvents(new Ess3Listener(this, webClient), this);
            logger.info("Started Essentials listener");
        }

        // See if the address and port are pointing to Marco
        boolean vibeCheck = webClient.vibeCheck();

        if (vibeCheck) {
            logger.finer("Started bridge");
            Sync sync = new Sync(webClient);
            getServer().getScheduler().runTaskTimerAsynchronously(this, sync, 0, 5*20);
        } else {
            logger.severe("Couldn't properly connect to marco is the address and port set properly?");
        }
    }

    @Override
    public void onDisable() {
    }


    /**
     * This checks the integrity of the config.yaml
     *
     * @param config The config to review
     * @return {boolean}
     */
    private boolean checkIntegrity(FileConfiguration config) {
        String address = config.getString("address");
        String token = config.getString("token");
        int port = config.getInt("port");

        if (address == null || address.isEmpty())
            return false;
        else if (token == null || token.isEmpty())
            return false;
        else
            return (port != 0);
    }

    @Override
    public void broadcastMessage(String message) {
        this.getServer().broadcastMessage(message);
    }

    @Override
    public void kickPlayer(UUID uuid, String reason, String source) {
        Player player = this.getServer().getPlayer(uuid);
        if (player == null) {
            Logger logger = getLogger();
            logger.info("No player to kick with UUID " + uuid.toString());
            return;
        }
        getServer().getScheduler().runTask(this, new Runnable() {
            public void run() {
                player.kickPlayer(reason);
                getServer().broadcast(source + " kicked " + player.getName() + " for " + reason, "matrix.kick.notify");
            }
        });
    }

    @Override
    public void banPlayer(UUID uuid, String reason, String source) {
        // Ban the player using UUID
        String uuidString = uuid.toString();
        if (getServer().getBanList(BanList.Type.NAME).addBan(uuidString, reason, null, source) == null) {
            Logger logger = getLogger();
            logger.warning("No player to ban with UUID " + uuidString);
            return;
        }

        // Notify others of the ban
        OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uuid);
        getServer().broadcast(source + " banned " + offlinePlayer.getName() + " for " + reason, "matrix.ban.notify");

        // If online, kick now
        Player player = offlinePlayer.getPlayer();
        if (player != null) {
            getServer().getScheduler().runTask(this, new Runnable() {
                public void run() {
                    player.kickPlayer("You have been banned: " + reason);
                }
            });
        }
    }

    @Override
    public void unbanPlayer(UUID uuid, String source) {
        String uuidString = uuid.toString();

        OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        if (name == null)
            name = uuidString;

        // Skip unban if there is no ban already present (which would imply we have a name)
        if (!offlinePlayer.isBanned()) {
            Logger logger = getLogger();
            logger.info("Player '" + name + "' isn't banned, so can't be unbanned");
            return;
        }

        // Unban the player using UUID
        getServer().getBanList(BanList.Type.NAME).pardon(uuidString);

        // Notify others of the unban
        getServer().broadcast(source + " unbanned " + name, "matrix.unban.notify");
    }

    @Override
    public void executeAsync(Runnable task) {
        this.getServer().getScheduler().runTaskAsynchronously(this, task);
    }
}
