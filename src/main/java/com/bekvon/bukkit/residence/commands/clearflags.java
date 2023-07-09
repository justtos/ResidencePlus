package com.bekvon.bukkit.residence.commands;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.Zrips.CMILib.FileHandler.ConfigReader;
import com.bekvon.bukkit.residence.LocaleManager;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.CommandAnnotation;
import com.bekvon.bukkit.residence.containers.cmd;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

public class clearflags implements cmd {

    @Override
    @CommandAnnotation(simple = false, priority = 3600, regVar = { 2, 3 }, consoleVar = { 666 })
    public Boolean perform(Residence plugin, CommandSender sender, String[] args, boolean resadmin) {
	Player player = (Player) sender;

	if (!resadmin) {
	    plugin.msg(player, lm.General_NoPermission);
	    return null;
	}

	ClaimedResidence area = plugin.getResidenceManager().getByName(args[0]);
	if (area == null) {
	    plugin.msg(player, lm.Invalid_Residence);
	    return null;
	}
	
	if (area.getRaid().isRaidInitialized()) {
	    plugin.msg(sender, lm.Raid_cantDo);
	    return null;
	}
	area.getPermissions().clearFlags();
	plugin.msg(player, lm.Flag_Cleared);

	return true;
    }

    @Override
    public void getLocale() {
	ConfigReader c = Residence.getInstance().getLocaleManager().getLocaleConfig();
	c.get("Description", "Remove all flags from residence");
	c.get("Info", Arrays.asList("&eUsage: &6/res clearflags <residence>"));
	LocaleManager.addTabCompleteMain(this, "[residence]");
    }
}
