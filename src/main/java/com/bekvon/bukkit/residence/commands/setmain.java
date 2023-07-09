package com.bekvon.bukkit.residence.commands;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.Zrips.CMILib.FileHandler.ConfigReader;
import com.bekvon.bukkit.residence.LocaleManager;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.CommandAnnotation;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.containers.cmd;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

public class setmain implements cmd {

    @Override
    @CommandAnnotation(simple = true, priority = 2900)
    public Boolean perform(Residence plugin, CommandSender sender, String[] args, boolean resadmin) {

	if (!(sender instanceof Player))
	    return false;

	Player player = (Player) sender;
	if (args.length != 0 && args.length != 1) {
	    return false;
	}

	ClaimedResidence res = null;

	if (args.length == 0)
	    res = plugin.getResidenceManager().getByLoc(player.getLocation());
	else
	    res = plugin.getResidenceManager().getByName(args[0]);

	if (res == null) {
	    plugin.msg(sender, lm.Invalid_Residence);
	    return false;
	}

	if (res.isOwner(player)) {
	    res.setMainResidence(res.isMainResidence() ? false : true);
	} else if (plugin.getRentManager().isRented(res.getName()) && !plugin.getRentManager().getRentingPlayer(res.getName()).equalsIgnoreCase(player.getName())) {
	    plugin.msg(sender, lm.Invalid_Residence);
	    return false;
	}

	plugin.msg(player, lm.Residence_ChangedMain, res.getTopParentName());

	ResidencePlayer rplayer = plugin.getPlayerManager().getResidencePlayer(player);
	if (rplayer != null)
	    rplayer.setMainResidence(res);

	return true;
    }

    @Override
    public void getLocale() {
	ConfigReader c = Residence.getInstance().getLocaleManager().getLocaleConfig();
	c.get("Description", "Sets defined residence as main to show up in chat as prefix");
	c.get("Info", Arrays.asList("&eUsage: &6/res setmain (residence)", "Set defined residence as main."));
	LocaleManager.addTabCompleteMain(this, "[residence]");
    }
}
