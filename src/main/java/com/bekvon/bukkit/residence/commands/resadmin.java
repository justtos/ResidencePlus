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

public class resadmin implements cmd {

    @Override
    @CommandAnnotation(simple = false, priority = 5300)
    public Boolean perform(Residence plugin, CommandSender sender, String[] args, boolean resadmin) {
	if (!(sender instanceof Player))
	    return false;
	if (args.length != 1)
	    return true;

	Player player = (Player) sender;
	if (args[0].equals("on")) {
	    plugin.resadminToggle.add(player.getName());
	    plugin.msg(player, lm.General_AdminToggleTurnOn);
	} else if (args[0].equals("off")) {
	    plugin.resadminToggle.remove(player.getName());
	    plugin.msg(player, lm.General_AdminToggleTurnOff);
	}
	return true;
    }

    @Override
    public void getLocale() {
	ConfigReader c = Residence.getInstance().getLocaleManager().getLocaleConfig();
	c.get("Description", "Enabled or disable residence admin");
	c.get("Info", Arrays.asList("&eUsage: &6/res resadmin [on/off]"));
	LocaleManager.addTabCompleteMain(this, "on%%off");
    }
}
