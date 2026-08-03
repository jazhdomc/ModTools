package mc.jazhdo;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.TextComponent;

public class ModTools extends JavaPlugin {
    private FileConfiguration config;

    private class Commands implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            // Make sure a sender exists
            if (sender == null) return false;

            // Make sure a player sent the command
            if (sender instanceof Player player) {
                // Perform different functions depending on command
                switch (command.getName()) {
                    case "spectate" -> {
                        // Whether to teleport them to the spectating location or to take them back
                        String base = "spectate-origins." + player.getName().toLowerCase() + ".";
                        if (config.getBoolean(base + "out") == false) {
                            // Make sure player argument was given
                            if (args.length < 1) {
                                sendMessage(player, ChatColor.RED + "<player> argument required. (Command usage: /spectate <player>)");
                                return true;
                            }

                            // Make sure player to teleport to exists
                            Player to = Bukkit.getPlayer(args[0]);
                            if (to == null) {
                                sendMessage(player, ChatColor.RED + "Player " + args[0] + " not found. Make sure you got the spelling right and they are online.");
                                return true;
                            }

                            // Save location
                            Location playerLoc = player.getLocation();
                            config.set(base + "out", true);
                            config.set(base + "world", playerLoc.getWorld().getName());
                            config.set(base + "loc", List.of(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), playerLoc.getYaw(), playerLoc.getPitch()));
                            saveConfig();

                            // Teleport player
                            player.setGameMode(GameMode.SPECTATOR);
                            sendMessage(player, ChatColor.GOLD + "Teleporting you to player " + args[0] + "...\nUse the command \"/spectate\" to exit spectating mode and go back.");
                            player.teleport(to);
                        } else {
                            // Teleport player
                            sendMessage(player, ChatColor.GOLD + "Teleporting you back...");
                            List<Double> coords = config.getDoubleList(base + "loc");
                            player.teleport(new Location(Bukkit.getWorld(config.getString(base + "world")), coords.get(0), coords.get(1), coords.get(2), coords.get(3).floatValue(), coords.get(4).floatValue()));
                            player.setGameMode(GameMode.SURVIVAL);

                            // Change status to normal mode
                            config.set(base + "out", false);
                            config.set(base + "loc", null);
                            config.set(base + "world", null);
                            saveConfig();
                        }
                    }
                    case "inventory" -> {
                        // Make sure there is a target for whose inventory to check
                        if (args.length < 1) {
                            sendMessage(player, ChatColor.RED + "<player> argument required. (Command usage: /inventory <player>)");
                            return true;
                        }

                        // Verify target player exists
                        Player target = Bukkit.getPlayer(args[0]);
                        if (target == null) {
                            sendMessage(player, ChatColor.RED + "Player " + args[0] + " not found. Make sure you got the spelling right and they are online.");
                            return true;
                        }

                        // Create inventory and get target player's inventory
                        Inventory view = Bukkit.createInventory(null, 54, "Player " + args[0] + "'s inventory");
                        PlayerInventory targetInv = target.getInventory();

                        // Set the first two rows for the items not seen in the 4-row inventory
                        ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                        view.setItem(0, border);
                        view.setItem(1, getOrAir(targetInv.getItemInOffHand()));
                        view.setItem(2, border);
                        view.setItem(3, border);
                        view.setItem(4, getOrAir(targetInv.getHelmet()));
                        view.setItem(5, getOrAir(targetInv.getChestplate()));
                        view.setItem(6, getOrAir(targetInv.getLeggings()));
                        view.setItem(7, getOrAir(targetInv.getBoots()));
                        view.setItem(8, getOrAir(border));

                        // Fill the rest in with the target player's inventory
                        ItemStack[] items = targetInv.getContents();
                        for (int i = 0; i < items.length; i++) view.setItem(9 + i, getOrAir(items[i]));
                        
                        // Show player the created inventory
                        player.openInventory(view);
                    }
                }
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        private void sendMessage(Player player, String msg) {
            player.spigot().sendMessage(TextComponent.fromLegacyText(msg));
        }

        private ItemStack getOrAir(ItemStack input) {
            return (input == null) ? new ItemStack(Material.AIR) : input;
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("Starting...");
        saveDefaultConfig();

        config = getConfig();
        
        Commands commands = new Commands();
        Server server = getServer();
        server.getPluginCommand("spectate").setExecutor(commands);
        server.getPluginCommand("inventory").setExecutor(commands);
    }

    @Override
    public void onDisable() {
        getLogger().info("Shuting down...");
        saveConfig();
    }
}