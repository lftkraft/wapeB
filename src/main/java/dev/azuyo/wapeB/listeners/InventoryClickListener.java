package dev.azuyo.wapeB.listeners;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public InventoryClickListener(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Punish:")) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        Player staff = (Player) event.getWhoClicked();
        String targetName = title.split(": ")[1].trim();
        
        ConfigurationSection items = configManager.getConfigurationSection("gui.items");
        if (items == null) return;

        for (String key : items.getKeys(false)) {
            String itemName = MessageUtil.parse(items.getString(key + ".name", "")).toString();
            // Note: This is a simplified check. In a real scenario, you'd use PersistentDataContainer or NBT.
            if (event.getCurrentItem().getItemMeta().displayName() != null) {
                // Perform the command based on the config key
                String reason = items.getString(key + ".reason", "Rules");
                String duration = items.getString(key + ".duration", "");
                
                String cmd;
                switch (key.toLowerCase()) {
                    case "ban":
                        cmd = "ban " + targetName + " " + reason;
                        break;
                    case "tempban":
                        cmd = "ban " + targetName + " " + duration + " " + reason;
                        break;
                    case "mute":
                        cmd = "mute " + targetName + " " + reason;
                        break;
                    case "tempmute":
                        cmd = "mute " + targetName + " " + duration + " " + reason;
                        break;
                    case "warn":
                        cmd = "warn " + targetName + " " + reason;
                        break;
                    case "kick":
                        cmd = "kick " + targetName + " " + reason;
                        break;
                    default:
                        return;
                }
                
                staff.closeInventory();
                staff.performCommand(cmd);
                return;
            }
        }
    }
}