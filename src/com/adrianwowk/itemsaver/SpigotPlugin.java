package com.adrianwowk.itemsaver;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SpigotPlugin extends JavaPlugin {
    private static File file;
    private static FileConfiguration itemsFile;
    Server server;
    ConsoleCommandSender console;

    public SpigotPlugin() {
        this.server = Bukkit.getServer();
        this.console = this.server.getConsoleSender();
        file = new File(getDataFolder(), "items.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        itemsFile = YamlConfiguration.loadConfiguration(file);

        if (getConfig().getBoolean("example")) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Example");
            meta.setLore(new ArrayList<>(Arrays.asList(ChatColor.AQUA + "Lore Line 1", ChatColor.RED + "Lore Line 2")));
            meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
            item.setItemMeta(meta);
            serializeItem(item, "example");
        }

    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("messages.prefix"));
    }

    public void onEnable() {

        this.saveDefaultConfig();

        Metrics metrics = new Metrics(this, 10032   );

        // Server Console Message
        console.sendMessage(getPrefix() + "Successfully enabled :)");

    }

    public void onDisable() {
        this.getLogger().info(getPrefix() + ChatColor.YELLOW + "Plugin Successfully Disabled");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        save();
        reload();
        List<String> list = new ArrayList<>();

        if (label.equalsIgnoreCase("give-item")){
            if (args.length == 1){
                for (String key : itemsFile.getKeys(false)){
                    list.add(key);
                }
            }
        } else if (label.equalsIgnoreCase("itemsaver")){
            if (args.length == 1){
                list.add("reload");
            } else if (args.length == 2){
                list.add("items");
                list.add("config");
            }
        }
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("itemsaver.use")) {
            sender.sendMessage(translate("messages.no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(translate("messages.console"));
            return true;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("save-item")) {

            if (args.length == 0) {
                player.sendMessage(translate("messages.save-item.invalid.no-args"));
            } else if (args.length == 1) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (serializeItem(item, args[0])){
                    player.sendMessage(translate("messages.save-item.overwriting").replace("%PATH%", args[0]));
                }

                player.sendMessage(translate("messages.save-item.added-to-file").replace("%PATH%", args[0]));
            } else {
                player.sendMessage(translate("messages.save-item.invalid.to-many-args"));
            }
        } else if (label.equalsIgnoreCase("give-item")) {

            if (args.length == 0)
                player.sendMessage(translate("messages.give-item.invalid.no-args"));
            else if (args.length == 1) {
                ItemStack item = deserializeItem(args[0], -1);

                if (item == null) {
                    // user messed up config or messed up the command
                    player.sendMessage(translate("messages.give-item.deserial-error").replace("%PATH%", args[0]));
                    return true;
                }

                player.getInventory().addItem(item);
                player.sendMessage(translate("messages.give-item.add-to-inv").replace("%PATH%", args[0]));
            } else if (args.length == 2){
                Integer amount;

                try {
                   amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e){
                    player.sendMessage(translate("messages.give-item.number-format-exception"));
                    return true;
                }

                ItemStack item = deserializeItem(args[0], amount);

                if (item == null) {
                    // user messed up config or messed up the command
                    player.sendMessage(translate("messages.give-item.deserial-error").replace("%PATH%", args[0]));
                    return true;
                }

                player.getInventory().addItem(item);
                player.sendMessage(translate("messages.give-item.add-to-inv").replace("%PATH%", args[0]));

            } else {
                player.sendMessage(translate("messages.give-item.invalid.to-many-args"));
            }
        } else if (label.equalsIgnoreCase("itemsaver")){
            // Usage /itemsaver reload <config|items>
            if (args.length == 0){
                // not enough args
                player.sendMessage(translate("messages.reload.no-args"));
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")){
                    // more args needed
                    player.sendMessage(translate("messages.reload.no-args"));
                } else {
                    //unknown arg
                    player.sendMessage(translate("messages.reload.unknown-arg").replace("%ARG%", args[0]));
                }
            } else if (args.length == 2){
                if (args[0].equalsIgnoreCase("reload")){
                    if (args[1].equalsIgnoreCase("config")){
                        // reload config
                        player.sendMessage(translate("messages.reload.config"));
                        reloadConfig();
                    } else if (args[1].equalsIgnoreCase("items")){
                        // reload items
                        player.sendMessage(translate("messages.reload.items"));
                        reload();
                    } else {
                        // unknown arg
                        player.sendMessage(translate("messages.reload.unknown-arg").replace("%ARG%", args[1]));
                    }
                } else {
                    //unknown arg
                    player.sendMessage(translate("messages.reload.unknown-arg").replace("%ARG%", args[0]));
                }
            } else {
                // to many args
                player.sendMessage(translate("messages.reload.to-many-args"));
            }
        }

        return false;
    }

    public String translate(String path) {
        return getPrefix() + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString(path));
    }

    private boolean serializeItem(ItemStack item, String path){
        boolean overWritten = false;
        ItemMeta meta = item.getItemMeta();

        if (itemsFile.getConfigurationSection(path) != null)
            overWritten = true;

        ConfigurationSection section = itemsFile.createSection(path);

        section.set("material", item.getType().toString());
        section.set("amount", item.getAmount());
        if (meta.hasDisplayName())
            section.set("displayname", unTranslateAlternateColorCodes(meta.getDisplayName()));

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            List<String> newLore = new ArrayList<>();
            for (String line : lore) {
                newLore.add(unTranslateAlternateColorCodes(line));
            }
            section.set("lore", newLore);
        }
        if (meta.hasEnchants()) {

            ConfigurationSection enchantSection = section.createSection("enchants");

            Map<Enchantment, Integer> enchants = meta.getEnchants();
            for (Enchantment ench : enchants.keySet())
                enchantSection.set(ench.getKey().getKey(), enchants.get(ench));
        }

        save();
        reload();
        return overWritten;
    }

    private ItemStack deserializeItem(String path, int am) {
        ConfigurationSection cs = itemsFile.getConfigurationSection(path);
        if (cs == null)
            return null;

        Material mat = Material.getMaterial(cs.getString("material"));
        Integer amount;
        if (am == -1) {
            amount = cs.getInt("amount");
        } else
            amount = am;

        String dn = cs.getString("displayname");
        List<String> lore = cs.getStringList("lore");
        ConfigurationSection es = cs.getConfigurationSection("enchants");

        ItemStack item;
        if (mat == null)
            return null;
        item = new ItemStack(mat, amount);

        ItemMeta meta = item.getItemMeta();

        if (dn != null)
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', dn));
        List<String> newLore = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                newLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(newLore);
        }

        if (es != null)
            for (String key : es.getKeys(false))
                meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(key)), es.getInt(key), true);

        item.setItemMeta(meta);
        return item;
    }

    public static void save() {
        try {
            itemsFile.save(file);
        } catch (IOException e) {
            System.out.println("Error Saving Items File");
        }
    }

    public static void reload() {
        itemsFile = YamlConfiguration.loadConfiguration(file);
    }

    public static String unTranslateAlternateColorCodes(String text) {
        char[] array = text.toCharArray();
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] == ChatColor.COLOR_CHAR && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(array[i + 1]) != -1) {
                array[i] = '&';
                array[i + 1] = Character.toLowerCase(array[i + 1]);
            }
        }
        return new String(array);
    }
}