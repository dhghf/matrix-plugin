package dev.dhdf.polo.bukkit;

import dev.dhdf.polo.types.PoloPlayer;
import dev.dhdf.polo.webclient.WebClient;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import net.ess3.api.events.NickChangeEvent;


/**
 * This class listens to all Essentials specific events happening on Minecraft.
 */
public class Ess3Listener extends PoloListener implements Listener {
    public Ess3Listener(Plugin plugin, WebClient client) {
        super(plugin, client);
    }

    // EssentialsX events
    @EventHandler(ignoreCancelled = true)
    public void onNickChange(NickChangeEvent ev) {
        // To avoid complexities with interpreting the new nickname before it
        // has been applied, just wait a tick before posting the update.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = ev.getController().getBase();
            // Avoid race with disconnect
            if (!player.isOnline())
                return;

            PoloPlayer poloPlayer = newPoloPlayer(player);
            this.client.postJoin(poloPlayer);
        });
    }
}
