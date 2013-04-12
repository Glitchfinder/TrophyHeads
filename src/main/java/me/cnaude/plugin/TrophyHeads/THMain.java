/*
 * Copyright (c) 2013 cnaude and Sean Porter <glitchkey@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.cnaude.plugin.TrophyHeads;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

/**
 *
 * @author cnaude
 */
public class THMain extends JavaPlugin{

    private THListener listener = null;
    public static String LOG_HEADER;
    static final Logger log = Logger.getLogger("Minecraft");
    private File pluginFolder;
    private File configFile;
    public static ArrayList<String> deathTypes = new ArrayList<String>();
    public static boolean debugEnabled = false;
    public static boolean renameEnabled = false;
    public static boolean playerSkin = true;
    public static boolean sneakPunchInfo = true;
    public static EnumMap<EntityType, List<String>> itemsRequired = new EnumMap<EntityType,List<String>>(EntityType.class);
    public static EnumMap<EntityType, Integer> dropChances = new EnumMap<EntityType, Integer>(EntityType.class);
    public static EnumMap<EntityType, String> customSkins = new EnumMap<EntityType, String>(EntityType.class);
    public static EnumMap<EntityType, String> skullMessages = new EnumMap<EntityType, String>(EntityType.class);
    public static Material renameItem = Material.PAPER;

    @Override
    public void onLoad() {
        listener = new THListener (this);
    }

    @Override
    public void onEnable() {
        LOG_HEADER = "[" + this.getName() + "]";
        pluginFolder = getDataFolder();
        configFile = new File(pluginFolder, "config.yml");
        createConfig();
        this.getConfig().options().copyDefaults(true);
        saveConfig();
        loadConfig();
        listener.register();
        getCommand("headspawn").setExecutor(this);

        if (renameEnabled) {
            ItemStack resultHead = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(resultHead);
            shapelessRecipe.addIngredient(1, Material.SKULL_ITEM, -1);
            shapelessRecipe.addIngredient(1, renameItem, -1);
            getServer().addRecipe(shapelessRecipe);
        }
    }

    @Override
    public void onDisable() {
        listener.unregister();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("trophyheads.spawn")) {
                String pName = player.getName();
                int count = 1;
                if (args.length >= 1) {
                    pName = args[0];
                    if (args.length == 2) {
                        if (args[1].matches("\\d+")) {
                            count = Integer.parseInt(args[1]);
                        }
                    }
                }
                ItemStack item = new ItemStack(Material.SKULL_ITEM, count, (byte) 3);
                Location loc = player.getLocation().clone();
                World world = loc.getWorld();
                ItemMeta itemMeta = item.getItemMeta();
                ((SkullMeta) itemMeta).setOwner(pName);
                item.setItemMeta(itemMeta);
                world.dropItemNaturally(loc, item);

            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");

            }
        } else {
            sender.sendMessage("Only a player can use this command!");
        }
        return true;
    }

    public EntityType getCustomSkullType(String name) {
        for (EntityType et : customSkins.keySet()) {
            if (customSkins.get(et).equals(name)) {
                return et;
            }
        }
        return EntityType.UNKNOWN;
    }

    public boolean isValidItem(EntityType et, Material mat) {
        if (et == null || mat == null) {
            return false;
        }
        if (itemsRequired.containsKey(et)) {
            if (itemsRequired.get(et).contains("ANY")) {
                return true;
            }
            if (itemsRequired.get(et).contains(String.valueOf(mat.getId()))) {
                return true;
            } else {
                for (String s : itemsRequired.get(et)) {
                    if (s.toUpperCase().equals(mat.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void setSkullName(ItemStack item, String name) {
        ItemMeta itemMeta = item.getItemMeta();
        ((SkullMeta) itemMeta).setOwner(name);
        itemMeta.setDisplayName(name + " Head");
        item.setItemMeta(itemMeta);
    }

    private void createConfig() {
        if (!pluginFolder.exists()) {
            try {
                pluginFolder.mkdir();
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }
    }

    private void loadConfig() {
        debugEnabled = getConfig().getBoolean("debug-enabled");
        logDebug("Debug enabled");

        dropChances.put(EntityType.PLAYER,getConfig().getInt("drop-chance"));
        logDebug("Chance to drop head: " + dropChances.get(EntityType.PLAYER) + "%");

        playerSkin = getConfig().getBoolean("player-skin");
        logDebug("Player skins: " + playerSkin);

        sneakPunchInfo = getConfig().getBoolean("sneak-punch-info");
        logDebug("Sneak punch info: " + sneakPunchInfo);

        dropChances.put(EntityType.ZOMBIE,getConfig().getInt("zombie-heads.drop-chance"));
        logDebug("Zombie chance to drop head: " + dropChances.get(EntityType.ZOMBIE) + "%");

        dropChances.put(EntityType.SKELETON,getConfig().getInt("skeleton-heads.drop-chance"));
        logDebug("Skeleton chance to drop head: " + dropChances.get(EntityType.SKELETON) + "%");

        dropChances.put(EntityType.CREEPER,getConfig().getInt("creeper-heads.drop-chance"));
        logDebug("Creeper chance to drop head: " + dropChances.get(EntityType.CREEPER) + "%");

        dropChances.put(EntityType.SPIDER,getConfig().getInt("spider-heads.drop-chance"));
        logDebug("Creeper chance to drop head: " + dropChances.get(EntityType.CREEPER) + "%");

        dropChances.put(EntityType.ENDERMAN,getConfig().getInt("enderman-heads.drop-chance"));
        logDebug("Creeper chance to drop head: " + dropChances.get(EntityType.CREEPER) + "%");

        dropChances.put(EntityType.BLAZE,getConfig().getInt("blaze-heads.drop-chance"));
        logDebug("Creeper chance to drop head: " + dropChances.get(EntityType.CREEPER) + "%");

        skullMessages.put(EntityType.PLAYER, getConfig().getString("message"));

        renameEnabled = getConfig().getBoolean("rename-enabled");
        if (renameEnabled) {
            try {
                renameItem = Material.getMaterial(getConfig().getInt("rename-item"));
            } catch (Exception e) {
                renameItem = Material.PAPER;
            }
            logDebug("Rename recipe enabled: head + " + renameItem.toString());
        }

        itemsRequired.put(EntityType.PLAYER, getConfig().getStringList("items-required"));
        itemsRequired.put(EntityType.ZOMBIE, getConfig().getStringList("zombie-heads.items-required"));
        itemsRequired.put(EntityType.CREEPER, getConfig().getStringList("creeper-heads.items-required"));
        itemsRequired.put(EntityType.SKELETON, getConfig().getStringList("skeleton-heads.items-required"));

        itemsRequired.put(EntityType.SPIDER, getConfig().getStringList("spider-heads.items-required"));
        itemsRequired.put(EntityType.ENDERMAN, getConfig().getStringList("enderman-heads.items-required"));
        itemsRequired.put(EntityType.BLAZE, getConfig().getStringList("blaze-heads.items-required"));

        customSkins.put(EntityType.SPIDER, getConfig().getString("spider-heads.skin"));
        customSkins.put(EntityType.ENDERMAN, getConfig().getString("enderman-heads.skin"));
        customSkins.put(EntityType.BLAZE, getConfig().getString("blaze-heads.skin"));

        skullMessages.put(EntityType.ZOMBIE, getConfig().getString("zombie-heads.message"));
        skullMessages.put(EntityType.CREEPER, getConfig().getString("creeper-heads.message"));
        skullMessages.put(EntityType.SKELETON, getConfig().getString("skeleton-heads.message"));

        skullMessages.put(EntityType.SPIDER, getConfig().getString("spider-heads.message"));
        skullMessages.put(EntityType.ENDERMAN, getConfig().getString("enderman-heads.message"));
        skullMessages.put(EntityType.BLAZE, getConfig().getString("blaze-heads.message"));

        skullMessages.put(EntityType.WITHER, getConfig().getString("wither-heads.message"));

        deathTypes.addAll(getConfig().getStringList("death-types"));

    }

    public void logInfo(String _message) {
        log.log(Level.INFO, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logError(String _message) {
        log.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logDebug(String _message) {
        if (debugEnabled) {
            log.log(Level.INFO, String.format("%s [DEBUG] %s", LOG_HEADER, _message));
        }
    }
}
