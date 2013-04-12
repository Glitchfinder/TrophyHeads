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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.Recipe;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.SkullType.*;
import org.bukkit.World;

/**
 *
 * @author cnaude
 */
public class THListener implements Listener {

    private THMain plugin;
    private Random random;

    public THListener(THMain plugin) {
        this.plugin = plugin;
        this.random = new Random();
        register();
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!plugin.renameEnabled)
            return;
        else if (event == null)
            return;
        else if (!(event.getRecipe() instanceof Recipe))
            return;
        else if (event.getInventory() == null)
            return;

        CraftingInventory ci = event.getInventory();
        ItemStack result = ci.getResult();

        if (result == null) {
            return;
        }

        if (!result.getType().equals(Material.SKULL_ITEM))
            return;

        for (ItemStack i : ci.getContents()) {
            if (!i.getType().equals(Material.SKULL_ITEM))
                continue;

            if (i.getData().getData() != (byte) 3) {
                ci.setResult(new ItemStack(0));
                return;
            }
        }

        for (ItemStack i : ci.getContents()) {
            if (!i.hasItemMeta() || !i.getType().equals(plugin.renameItem))
                continue;

            ItemMeta im = i.getItemMeta();

            if (!im.hasDisplayName())
                continue;

            ItemStack res = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
            ItemMeta itemMeta = res.getItemMeta();
            ((SkullMeta) itemMeta).setOwner(im.getDisplayName());
            res.setItemMeta(itemMeta);
            ci.setResult(res);
            break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.sneakPunchInfo)
            return;
        else if (event == null)
            return;

        Player player = event.getPlayer();

        if (player == null)
            return;
        else if (!player.isSneaking())
            return;
        else if (!player.hasPermission("trophyheads.info"))
            return;

        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            return;

        Block block = event.getClickedBlock();

        if (block.getType() != Material.SKULL)
            return;

        BlockState bs = block.getState();
        Skull skull = (Skull) bs;
        String pName = "Unknown";
        String message;

        switch (skull.getSkullType()) {
            case PLAYER:
                if (skull.hasOwner()) {
                    pName = skull.getOwner();
                    if (plugin.customSkins.containsValue(pName)) {
                        EntityType type = plugin.getCustomSkullType(pName);
                        message = plugin.skullMessages.get(type);
                    } else {
                        message = plugin.skullMessages.get(EntityType.PLAYER);
                    }
                } else {
                    message = plugin.skullMessages.get(EntityType.PLAYER);
                }
                break;
            case CREEPER:
                message = plugin.skullMessages.get(EntityType.CREEPER);
                break;
            case SKELETON:
                message = plugin.skullMessages.get(EntityType.SKELETON);
                break;
            case WITHER:
                message = plugin.skullMessages.get(EntityType.WITHER);
                break;
            case ZOMBIE:
                message = plugin.skullMessages.get(EntityType.ZOMBIE);
                break;
            default:
                message = plugin.skullMessages.get(EntityType.PLAYER);
                break;
        }

        message = message.replaceAll("%%NAME%%", pName);
        message = ChatColor.translateAlternateColorCodes('&', message);
        player.sendMessage(message);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event == null)
            return;

        Player player = (Player) event.getEntity();

        if (player == null)
            return;
        else if (!player.hasPermission("trophyheads.drop"))
            return;

        int chance = plugin.dropChances.get(EntityType.PLAYER);

        if (random.nextInt(100) >= chance)
            return;

        boolean dropOkay = false;
        DamageCause dc = player.getLastDamageCause().getCause();
        plugin.logDebug("DamageCause: " + dc.toString());

        if (plugin.deathTypes.contains(dc.toString()))
            dropOkay = true;
        else if (plugin.deathTypes.contains("ALL"))
            dropOkay = true;

        if (player.getKiller() instanceof Player) {
            if (plugin.deathTypes.contains("PVP")) {
                Material type = player.getKiller().getItemInHand().getType();
                dropOkay = plugin.isValidItem(EntityType.PLAYER, type);
            }
        }

        if (!dropOkay) {
            plugin.logDebug("Match: false");
            return;
        }

        plugin.logDebug("Match: true");
        Location loc = player.getLocation().clone();
        World world = loc.getWorld();
        String pName = player.getName();

        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
        ItemMeta itemMeta = item.getItemMeta();
        ArrayList<String> itemDesc = new ArrayList<String>();
        itemMeta.setDisplayName("Head of " + pName);
        itemDesc.add(event.getDeathMessage());
        itemMeta.setLore(itemDesc);

        if (plugin.playerSkin)
            ((SkullMeta) itemMeta).setOwner(pName);

        item.setItemMeta(itemMeta);
        world.dropItemNaturally(loc, item);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event == null)
            return;

        Block block = event.getBlock();

        if (event.getPlayer() instanceof Player) {
            if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                return;
            }
        }
        else if (block == null)
            return;

        if (block.getType() != Material.SKULL)
            return;

        Skull skull = (Skull) block.getState();

        if (!skull.getSkullType().equals(SkullType.PLAYER))
            return;

        if (!skull.hasOwner())
            return;

        String pName = skull.getOwner();

        if (!plugin.customSkins.containsValue(pName))
            return;

        Location loc = block.getLocation().clone();
        event.setCancelled(true);
        block.setType(Material.AIR);
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
        ItemMeta itemMeta = item.getItemMeta();
        ((SkullMeta) itemMeta).setOwner(pName);
        String name = plugin.getCustomSkullType(pName).getName();
        itemMeta.setDisplayName(ChatColor.GREEN + name + " Head");
        item.setItemMeta(itemMeta);

        World world = loc.getWorld();
        world.dropItemNaturally(loc, item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event == null)
            return;

        EntityType type = event.getEntityType();
        Entity entity = event.getEntity();
        int data;
        boolean dropOkay;

        Player player;
        Material mat = Material.AIR;

        if (((LivingEntity)entity).getKiller() instanceof Player) {
            player = (Player)((LivingEntity)entity).getKiller();
            mat = player.getItemInHand().getType();
        }

        dropOkay = plugin.isValidItem(type, mat);

        switch (type) {
            case SKELETON:
                Skeleton skeleton = (Skeleton) entity;

                if (skeleton.getSkeletonType().equals(SkeletonType.NORMAL)) {
                    if (random.nextInt(100) >= plugin.dropChances.get(type))
                        return;

                    data = 0;
                } else
                    return;

                break;
            case ZOMBIE:
                if (random.nextInt(100) >= plugin.dropChances.get(type))
                    return;

                data = 2;
                break;
            case CREEPER:
                if (random.nextInt(100) >= plugin.dropChances.get(type))
                    return;

                data = 4;
                break;
            case SPIDER:
                if (random.nextInt(100) >= plugin.dropChances.get(type))
                    return;

                data = 3;
                break;
            case ENDERMAN:
                if (random.nextInt(100) >= plugin.dropChances.get(type))
                    return;

                data = 3;
                break;
            case BLAZE:
                if (random.nextInt(100) >= plugin.dropChances.get(type))
                    return;

                data = 3;
                break;
            default:
                return;
        }

        if (!dropOkay)
            return;

        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) data);

        if (data == 3 && plugin.customSkins.containsKey(type)) {
            ItemMeta itemMeta = item.getItemMeta();
            ((SkullMeta) itemMeta).setOwner(plugin.customSkins.get(type));
            itemMeta.setDisplayName(type.getName() + " Head");
            item.setItemMeta(itemMeta);
        }

        Location loc = entity.getLocation().clone();
        World world = loc.getWorld();
        world.dropItemNaturally(loc, item);
    }
}
