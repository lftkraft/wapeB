package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PunishCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public PunishCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (!sender.hasPermission("wapeb.punish.gui")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent("%prefix% <white>Usage: <red>/punish <player>", null));
            return true;
        }

        Player staff = (Player) sender;
        String targetName = args[0];
        
        openPunishGUI(staff, targetName);
        return true;
    }

    private void openPunishGUI(Player staff, String targetName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", targetName);
        
        String title = MessageUtil.replacePlaceholders(configManager.getString("gui.title", "Punish: %player%"), null, placeholders);
        // Note: Inventory title doesn't support MiniMessage gradients natively in older versions, 
        // but we'll use Legacy conversion for the title.
        Inventory gui = Bukkit.createInventory(null, 27, MessageUtil.parse(title).toString()); // Simple title for now

        ConfigurationSection items = configManager.getConfigurationSection("gui.items");
        if (items != null) {
            int slot = 10;
            for (String key : items.getKeys(false)) {
                Material mat = Material.valueOf(items.getString(key + ".material", "BARRIER"));
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    meta.displayName(MessageUtil.parse(items.getString(key + ".name", "")));
                    List<String> loreStrings = items.getStringList(key + ".lore");
                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    for (String l : loreStrings) {
                        lore.add(MessageUtil.parse(l));
                    }
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                
                gui.setItem(slot++, item);
                if (slot == 17) slot = 19; // Skip to next row if needed
            }
        }

        staff.openInventory(gui);
        // We would need an InventoryClickListener to handle the actual punishment
    }
}