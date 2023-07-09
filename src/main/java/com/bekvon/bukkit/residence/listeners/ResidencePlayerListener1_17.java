package com.bekvon.bukkit.residence.listeners;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

import net.Zrips.CMILib.CMILib;
import net.Zrips.CMILib.Items.CMIItemStack;
import net.Zrips.CMILib.Items.CMIMaterial;

public class ResidencePlayerListener1_17 implements Listener {

    private Residence plugin;

    public ResidencePlayerListener1_17(Residence plugin) {
	this.plugin = plugin;
    }

    private static int MAX_ENTRIES = 50;
    public static LinkedHashMap<String, BlockData> powder_snow = new LinkedHashMap<String, BlockData>(MAX_ENTRIES + 1, .75F, false) {
	@Override
	protected boolean removeEldestEntry(Map.Entry<String, BlockData> eldest) {
	    return size() > MAX_ENTRIES;
	}
    };

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerBucketEntityEvent(PlayerBucketEntityEvent event) {

	Player player = event.getPlayer();
	if (Residence.getInstance().isResAdminOn(player))
	    return;

	Entity ent = event.getEntity();

	ItemStack iih = CMIItemStack.getItemInMainHand(player);
	if (iih == null)
	    return;

	if (!CMIMaterial.get(iih).equals(CMIMaterial.WATER_BUCKET))
	    return;

	FlagPermissions perms = Residence.getInstance().getPermsByLocForPlayer(ent.getLocation(), player);

	if (!perms.playerHas(player, Flags.animalkilling, FlagCombo.TrueOrNone)) {
	    event.setCancelled(true);
	    Residence.getInstance().msg(player, lm.Flag_Deny, Flags.animalkilling);
	}
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractRespawn(PlayerInteractEvent event) {

	if (event.getPlayer() == null)
	    return;
	// disabling event on world
	if (plugin.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;

	if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
	    return;
	try {
	    if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND)
		return;
	} catch (Exception e) {
	}
	Player player = event.getPlayer();

	Block block = event.getClickedBlock();
	if (block == null)
	    return;

	Material mat = block.getType();

	if (!CMIMaterial.isCopperBlock(mat))
	    return;

	ClaimedResidence res = plugin.getResidenceManager().getByLoc(block.getLocation());
	if (res == null)
	    return;

	ItemStack item = null;
	if (event.getHand() == EquipmentSlot.OFF_HAND) {
	    item = CMILib.getInstance().getReflectionManager().getItemInOffHand(player);
	} else {
	    item = CMILib.getInstance().getReflectionManager().getItemInMainHand(player);
	}

	if (item == null || item.getType().equals(Material.AIR))
	    return;
	boolean waxed = CMIMaterial.isWaxedCopper(mat);

	if ((CMIMaterial.get(item).equals(CMIMaterial.HONEYCOMB) && !waxed || item.getType().toString().contains("_AXE") && CMIMaterial.getCopperStage(mat) > 1) &&
	    !res.isOwner(player) && !res.getPermissions().playerHas(player, Flags.copper, FlagCombo.TrueOrNone) && !plugin.isResAdminOn(player)) {

	    plugin.msg(player, lm.Residence_FlagDeny, Flags.copper, res.getName());
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLandDryPhysics(BlockPhysicsEvent event) {

	// Disabling listener if flag disabled globally
	if (!Flags.place.isGlobalyEnabled())
	    return;
	// disabling event on world
	if (plugin.isDisabledWorldListener(event.getBlock().getWorld()))
	    return;

	if (!event.getSourceBlock().getType().equals(Material.POWDER_SNOW) || event.getBlock().getType().equals(Material.AIR) || event.getBlock().getType().equals(Material.POWDER_SNOW))
	    return;

	Block block = event.getBlock();
	if (block == null)
	    return;

	if (block.getLocation().getY() == event.getSourceBlock().getLocation().getY())
	    return;

	ClaimedResidence res = plugin.getResidenceManager().getByLoc(block.getLocation());
	if (res == null)
	    return;

	powder_snow.put(event.getSourceBlock().getLocation().toString(), block.getBlockData().clone());

    }
}
