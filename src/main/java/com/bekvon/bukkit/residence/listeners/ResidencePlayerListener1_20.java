package com.bekvon.bukkit.residence.listeners;

import com.sun.source.tree.TypeCastTree;
import net.Zrips.CMILib.Colors.CMIChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagCombo;

import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;

import net.Zrips.CMILib.Items.CMIMaterial;

import java.util.regex.Pattern;

public class ResidencePlayerListener1_20 implements Listener {

    private Residence plugin;

    public static final Pattern[][] CHESTSHOP_SIGN_PATTERN = {
            { Pattern.compile("^[1-9][0-9]{0,5}$"), Pattern.compile("^Q [1-9][0-9]{0,4} : C [0-9]{1,5}$") },
            {
                    Pattern.compile("(?i)^((\\d{1,}([.e]\\d+)?)|free)$"),
                    Pattern.compile("(?i)^([BS] *((\\d*([.e]\\d+)?)|free))( *: *([BS] *((\\d*([.e]\\d+)?)|free)))?$"),
                    Pattern.compile("(?i)^(((\\d*([.e]\\d+)?)|free) *[BS])( *: *([BS] *((\\d*([.e]\\d+)?)|free)))?$"),
                    Pattern.compile("(?i)^(((\\d*([.e]\\d+)?)|free) *[BS]) *: *(((\\d*([.e]\\d+)?)|free) *[BS])$"),
                    Pattern.compile("(?i)^([BS] *((\\d*([.e]\\d+)?)|free)) *: *(((\\d*([.e]\\d+)?)|free) *[BS])$"),
            },
            { Pattern.compile("^[\\p{L}\\d_? #:\\-]+$") }
    };

    public ResidencePlayerListener1_20(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getPlayer() == null)
            return;
        // disabling event on world
        if (plugin.isDisabledWorldListener(event.getPlayer().getWorld()))
            return;

        Block block = event.getClickedBlock();

        if (block == null || !CMIMaterial.isSign(block.getType()))
            return;

        Player player = event.getPlayer();
        if (player.hasMetadata("NPC"))
            return;

        // Check if a sign matches the ChestShop format
        String[] lines = ((Sign) block.getState()).getSide(Side.FRONT).getLines();
        boolean isChestShopSign = false;
        for (int i = 0; i < 3; i++) {
            boolean matches = false;
            for (Pattern pattern : CHESTSHOP_SIGN_PATTERN[i]) {
                if (pattern.matcher( CMIChatColor.stripColor(lines[i+1])).matches() ) {
                    matches = true;
                    break;
                }
            }

            if(matches)
                isChestShopSign = true;
            else {
                isChestShopSign = false;
                break;
            }
        }
        FlagPermissions perms = plugin.getPermsByLocForPlayer(block.getLocation(), player);

        boolean hasuse = perms.playerHas(player, Flags.use, FlagCombo.TrueOrNone);
        if (hasuse || isChestShopSign || plugin.isResAdminOn(player)) // if isChestShopSign ChestShop handles the protection
            return; // Allow

        event.setCancelled(true);
        plugin.msg(player, lm.Flag_Deny, Flags.use);

    }
}
