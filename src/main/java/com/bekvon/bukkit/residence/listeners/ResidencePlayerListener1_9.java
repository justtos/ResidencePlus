package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.event.ResidenceChangedEvent;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

public class ResidencePlayerListener1_9 implements Listener {

    private Residence plugin;

    public ResidencePlayerListener1_9(Residence plugin) {
	this.plugin = plugin;
    }

    @EventHandler
    public void EntityToggleGlideEvent(EntityToggleGlideEvent event) {

	// disabling event on world
	if (plugin.isDisabledWorldListener(event.getEntity().getWorld()))
	    return;

	if (!(event.getEntity() instanceof Player))
	    return;

	if (plugin.isResAdminOn(event.getEntity())) {
	    return;
	}

	Player player = (Player) event.getEntity();

	FlagPermissions perms = plugin.getPermsByLocForPlayer(player.getLocation(), player);

	if (perms.playerHas(player, Flags.elytra, FlagCombo.TrueOrNone))
	    return;

	plugin.msg(player, lm.Flag_Deny, Flags.elytra);

	event.setCancelled(true);
	Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
	    player.setGliding(false);
	}, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceChange(ResidenceChangedEvent event) {

	ClaimedResidence newRes = event.getTo();

	Player player = event.getPlayer();
	if (player == null)
	    return;

	if (!player.isGliding())
	    return;

	if (newRes == null)
	return;
	
	if (newRes.getPermissions().playerHas(player, Flags.elytra, FlagCombo.TrueOrNone))
	    return;

	player.setGliding(false);

	Location loc = ResidencePlayerListener.getSafeLocation(player.getLocation());
	if (loc == null) {
	    // get defined land location in case no safe landing spot are found
	    loc = plugin.getConfigManager().getFlyLandLocation();
	    if (loc == null) {
		// get main world spawn location in case valid location is not found
		loc = Bukkit.getWorlds().get(0).getSpawnLocation();
	    }
	}
	if (loc != null) {
	    plugin.msg(player, lm.Flag_Deny, Flags.elytra);
	    player.closeInventory();
	    player.teleport(loc);
	}
    }
}
