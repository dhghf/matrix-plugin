package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.types.PoloPlayer;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


/**
 * This is a base class for listeners of events happening on Minecraft.
 */
public class PoloListener {
    protected final Plugin plugin;
    protected final WebClient client;
    private final BukkitHelper helper;

    public PoloListener(Plugin plugin, WebClient client) {
        this.plugin = plugin;
        this.client = client;
        this.helper = new BukkitHelper();
    }

    protected PoloPlayer newPoloPlayer(Player player) {
        // Strip out Minecraft formatting codes from the display name
        String displayName = player.getDisplayName().replaceAll("\u00a7.", "");
        return new PoloPlayer(player.getName(), player.getUniqueId(),
                              displayName, helper.getTexture(player));
    }
}
