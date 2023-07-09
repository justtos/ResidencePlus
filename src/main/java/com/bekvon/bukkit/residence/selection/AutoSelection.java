package com.bekvon.bukkit.residence.selection;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.AutoSelector;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import com.bekvon.bukkit.residence.protection.CuboidArea;

import net.Zrips.CMILib.Logs.CMIDebug;

public class AutoSelection {

    private HashMap<UUID, AutoSelector> list = new HashMap<UUID, AutoSelector>();
    private Residence plugin;

    public AutoSelection(Residence residence) {
	this.plugin = residence;
    }

    public void switchAutoSelection(Player player) {
	if (!list.containsKey(player.getUniqueId())) {
	    ResidencePlayer rPlayer = plugin.getPlayerManager().getResidencePlayer(player);
	    PermissionGroup group = rPlayer.getGroup(player.getWorld().getName());
	    list.put(player.getUniqueId(), new AutoSelector(group, System.currentTimeMillis()));
	    plugin.msg(player, lm.Select_AutoEnabled);
	} else {
	    list.remove(player.getUniqueId());
	    plugin.msg(player, lm.Select_AutoDisabled);
	}
    }

    public void UpdateSelection(Player player) {
	UpdateSelection(player, player.getLocation());
    }

    public void UpdateSelection(Player player, Location targetLoc) {
	if (!list.containsKey(player.getUniqueId()))
	    return;

	AutoSelector AutoSelector = list.get(player.getUniqueId());
	
	ResidencePlayer rplayer = ResidencePlayer.get(player);

	int Curenttime = (int) (System.currentTimeMillis() - AutoSelector.getTime()) / 1000;

	if (Curenttime > 270) {
	    list.remove(player.getUniqueId());
	    plugin.msg(player, lm.Select_AutoDisabled);
	    return;
	}

	Location cloc = targetLoc.clone();

	Location loc1 = plugin.getSelectionManager().getPlayerLoc1(player);
	Location loc2 = plugin.getSelectionManager().getPlayerLoc2(player);

	if (loc1 == null) {
	    plugin.getSelectionManager().placeLoc1(player, cloc, false);
	    loc1 = player.getLocation();
	}

	if (loc2 == null) {
	    plugin.getSelectionManager().placeLoc2(player, cloc, true);
	    return;
	}

	boolean changed = false;

	CuboidArea area = new CuboidArea(loc1, loc2);
	Location hloc = area.getHighLocation();
	Location lloc = area.getLowLocation();

	if (cloc.getBlockX() < lloc.getBlockX()) {
	    lloc.setX(cloc.getBlockX());
	    changed = true;
	}

	if (cloc.getBlockY() <= lloc.getBlockY()) {
	    lloc.setY(cloc.getBlockY() - 1);
	    changed = true;
	}

	if (cloc.getBlockZ() < lloc.getBlockZ()) {
	    lloc.setZ(cloc.getBlockZ());
	    changed = true;
	}

	if (cloc.getBlockX() > hloc.getBlockX()) {
	    hloc.setX(cloc.getBlockX());
	    changed = true;
	}

	if (cloc.getBlockY() >= hloc.getBlockY()) {
	    hloc.setY(cloc.getBlockY() + 1);
	    changed = true;
	}

	if (cloc.getBlockZ() > hloc.getBlockZ()) {
	    hloc.setZ(cloc.getBlockZ());
	    changed = true;
	}

	PermissionGroup group = AutoSelector.getGroup();

	if (area.getXSize() > rplayer.getMaxX()) {
	    return;
	}

	if (area.getYSize() > group.getMaxY() && !plugin.getConfigManager().isSelectionIgnoreY()) {
	    return;
	}

	if (area.getZSize() > rplayer.getMaxZ()) {
	    return;
	}

	if (changed) {
	    plugin.getSelectionManager().placeLoc1(player, hloc, false);
	    plugin.getSelectionManager().placeLoc2(player, lloc, true);
	    plugin.getSelectionManager().showSelectionInfoInActionBar(player);
	}
    }

    public HashMap<UUID, AutoSelector> getList() {
	return list;
    }
}
