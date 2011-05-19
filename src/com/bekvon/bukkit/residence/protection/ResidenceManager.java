/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bekvon.bukkit.residence.protection;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 *
 * @author Administrator
 */
public class ResidenceManager {
    protected Map<String,ClaimedResidence> residences;

    public ResidenceManager()
    {
        residences = Collections.synchronizedMap(new HashMap<String,ClaimedResidence>());
    }

    public ClaimedResidence getByLoc(Location loc) {
        if(loc==null)
            return null;
        ClaimedResidence res = null;
        boolean found = false;
        Set<Entry<String, ClaimedResidence>> set = residences.entrySet();
        synchronized (residences) {
            for (Entry<String, ClaimedResidence> key : set) {
                res = key.getValue();
                if (res.containsLoc(loc)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            return null;
        }
        ClaimedResidence subres = res.getSubzoneByLoc(loc);
        if (subres == null) {
            return res;
        }
        return subres;
    }

    public ClaimedResidence getByName(String name) {
        if(name==null)
            return null;
        String[] split = name.split("\\.");
        if (split.length == 1) {
            return residences.get(name);
        }
        ClaimedResidence res = residences.get(split[0]);
        for (int i = 1; i < split.length; i++) {
            if (res != null) {
                res = res.getSubzone(split[i]);
            } else {
                return null;
            }
        }
        return res;
    }

    public String getNameByLoc(Location loc) {
        if(loc==null)
            return null;
        Set<Entry<String, ClaimedResidence>> set = residences.entrySet();
        ClaimedResidence res = null;
        String name = null;
        synchronized (residences) {
            for (Entry<String, ClaimedResidence> key : set) {
                res = key.getValue();
                if (res.containsLoc(loc)) {
                    name = key.getKey();
                    break;
                }
            }
        }
        if(name==null)
            return null;
        String szname = res.getSubzoneNameByLoc(loc);
        if (szname != null) {
            return name + "." + szname;
        }
        return name;
    }

    public String getNameByRes(ClaimedResidence res)
    {
        Set<Entry<String, ClaimedResidence>> set = residences.entrySet();
        synchronized(residences)
        {
            for(Entry<String, ClaimedResidence> check : set)
            {
                if(check.getValue()==res)
                    return check.getKey();
                String n = check.getValue().getSubzoneNameByRes(res);
                if(n!=null)
                    return check.getKey() + "." + n;
            }
        }
        return null;
    }

    public void addResidence(Player player, String name, Location loc1, Location loc2)
    {
        name = name.replace(".", "_");
        name = name.replace(":", "_");
        if(player == null)
            return;
        if(loc1==null || loc2==null || !loc1.getWorld().getName().equals(loc2.getWorld().getName()))
        {
            player.sendMessage("§cInvalid selection points.");
            return;
        }
        PermissionGroup group = Residence.getPermissionManager().getGroup(player);
        boolean resadmin = Residence.getPermissionManager().isResidenceAdmin(player);
        boolean createpermission = group.canCreateResidences() || Residence.getPermissionManager().hasAuthority(player, "residence.create", false);
        if (!createpermission && !resadmin) {
            player.sendMessage("§cYou dont have permission to create residences.");
            return;
        }
        if (getOwnedZoneCount(player.getName()) >= group.getMaxZones() && !resadmin)
        {
            player.sendMessage("§cYou reached your max number of residences.");
            return;
        }
        if (residences.containsKey(name)) {
            player.sendMessage("§cA residence by this name already exists.");
            return;
        }
        CuboidArea newArea = new CuboidArea(loc1, loc2);
        Set<Entry<String, ClaimedResidence>> set = residences.entrySet();
        synchronized (set) {
            for (Entry<String, ClaimedResidence> resEntry : set) {
                ClaimedResidence res = resEntry.getValue();
                if (res.checkCollision(newArea)) {
                    player.sendMessage("§cArea collides with residence: §e" + resEntry.getKey());
                    return;
                }
            }
        }
        ClaimedResidence newRes = new ClaimedResidence(player.getName(), loc1.getWorld().getName());
        newRes.getPermissions().applyDefaultFlags();
        newRes.setEnterMessage(group.getDefaultEnterMessage());
        newRes.setLeaveMessage(group.getDefaultLeaveMessage());
        newRes.addArea(player, newArea, "main");
        if(newRes.getAreaCount()!=0)
        {
            residences.put(name, newRes);
            player.sendMessage("§aYou have created residence: §e" + name + "§a!");
            if(Residence.getConfig().useLeases())
                Residence.getLeaseManager().setExpireTime(player, name, group.getLeaseGiveTime());
        }
        else
            player.sendMessage("§cError creating residence...");
    }

    public void listResidences(Player player)
    {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("§eResidences:§3 ");
        Set<Entry<String, ClaimedResidence>> set = residences.entrySet();
        synchronized(residences)
        {
            boolean firstadd = true;
            Iterator<Entry<String, ClaimedResidence>> it = set.iterator();
            while(it.hasNext())
            {
                Entry<String, ClaimedResidence> next = it.next();
                if(next.getValue().getPermissions().getOwner().equalsIgnoreCase(player.getName()))
                {
                    if(!firstadd)
                        sbuilder.append(", ");
                    else
                        firstadd = false;
                    sbuilder.append(next.getKey());
                }
            }
        }
        player.sendMessage(sbuilder.toString());
    }

    public void addPhysicalArea(Player player, String residenceName, String areaID, Location loc1, Location loc2) {
        CuboidArea newarea = new CuboidArea(Residence.getSelectionManager().getPlayerLoc1(player.getName()), Residence.getSelectionManager().getPlayerLoc2(player.getName()));
        ClaimedResidence res = this.getByName(residenceName);
        if (res != null) {
            res.addArea(player, newarea, areaID);
        }
        else
        {
            player.sendMessage("§cInvalid Residence!");
        }
    }

    public String checkAreaCollision(CuboidArea newarea, ClaimedResidence parentResidence) {
        Set<Entry<String, ClaimedResidence>> set = residences.entrySet();
        for (Entry<String, ClaimedResidence> entry : set) {
            ClaimedResidence check = entry.getValue();
            if (check!=parentResidence && check.checkCollision(newarea)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeResidence(Player player, String name) {
        ClaimedResidence res = this.getByName(name);
        if (res != null) {
            if (player!=null && !Residence.getPermissionManager().isResidenceAdmin(player)) {
                if (!res.getPermissions().hasResidencePermission(player, true)) {
                    player.sendMessage("§cYou dont have permission to modify this residence.");
                    return;
                }
            }
            ClaimedResidence parent = res.getParent();
            if (parent != null) {
                String[] split = name.split("\\.");
                parent.removeSubzone(split[split.length - 1]);
            } else {
                residences.remove(name);
            }
            if(player!=null)
                player.sendMessage("§aResidence§e " + name + " §aremoved...");
        } else {
            if(player!=null)
                player.sendMessage("§cInvalid Residence.");
        }
    }

    public int getOwnedZoneCount(String player)
    {
        Collection<ClaimedResidence> set = residences.values();
        int count=0;
        synchronized(residences)
        {
            for(ClaimedResidence res : set)
            {
                if(res.getPermissions().getOwner().equalsIgnoreCase(player))
                {
                    count++;
                }
            }
        }
        return count;
    }

    public String[] getResidenceList()
    {
        return (String[]) residences.keySet().toArray();
    }

    public void listAllResidences(Player player)
    {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("§a");
        Set<String> set = residences.keySet();
        synchronized(residences)
        {
            Iterator<String> it = set.iterator();
            while(it.hasNext())
            {
                String next = it.next();
                sbuilder.append(next);
                if(it.hasNext())
                    sbuilder.append(", ");
            }
        }
        player.sendMessage("§eResidences (Count="+residences.size()+"): " + sbuilder.toString());

    }

    public void printAreaInfo(String areaname, Player player) {
        ClaimedResidence res = this.getByName(areaname);
        if(res==null)
        {
            player.sendMessage("§cInvalid Residence.");
            return;
        }
        ResidencePermissions perms = res.getPermissions();
        if(Residence.getConfig().enableEconomy())
            player.sendMessage("§eResidence:§2 " + areaname + " §eBank: §6" + res.getBank().getStoredMoney());
        else
            player.sendMessage("§eResidence:§2 " + areaname);
        if(Residence.getConfig().enabledRentSystem() && Residence.getRentManager().isRented(areaname))
            player.sendMessage("§eOwner:§c " + perms.getOwner() + "§e Rented by: §c" + Residence.getRentManager().getRentingPlayer(areaname));
        else
            player.sendMessage("§eOwner:§c " + perms.getOwner());
        player.sendMessage("§eFlags:§9 " + perms.listFlags());
        player.sendMessage("§eYour Flags: §a" + perms.listPlayerFlags(player.getName()));
        player.sendMessage("§eGroup Flags:§c " + perms.listGroupFlags());
        player.sendMessage("§eOthers Flags:§c " + perms.listOtherPlayersFlags(player.getName()));
        player.sendMessage("§ePhysical Areas: " + res.getFormattedAreaList());
        String aid = res.getAreaIDbyLoc(player.getLocation());
        if(aid !=null)
            player.sendMessage("§eCurrent Area ID: §6" + aid);
        player.sendMessage("§eTotal Size:§d " + res.getTotalSize());
        player.sendMessage("§eSubZones:§6 " + res.CSVSubzoneList());
        if (Residence.getConfig().useLeases() && Residence.getLeaseManager().leaseExpires(areaname)) {
            player.sendMessage("§eLeaseExpiration:§a " + Residence.getLeaseManager().getExpireTime(areaname));
        }
    }

    public void mirrorPerms(Player reqPlayer, String targetArea, String sourceArea) {
        ClaimedResidence reciever = this.getByName(targetArea);
        ClaimedResidence source = this.getByName(sourceArea);
        if (source == null || reciever == null) {
            reqPlayer.sendMessage("§cEither the target or source area was invalid.");
            return;
        }
        if (!Residence.getPermissionManager().isResidenceAdmin(reqPlayer)) {

            if (!reciever.getPermissions().hasResidencePermission(reqPlayer, true) || !source.getPermissions().hasResidencePermission(reqPlayer, true)) {
                reqPlayer.sendMessage("§cYou must be the owner of both residences to mirror permissions.");
                return;
            }
        }
        reciever.getPermissions().applyTemplate(reqPlayer, source.getPermissions());
    }

    public Map<String,Object> save()
    {
        Map<String,Object> resmap = new LinkedHashMap<String,Object>();
        for(Entry<String, ClaimedResidence> res : residences.entrySet())
        {
            try
            {
                resmap.put(res.getKey(), res.getValue().save());
            }
            catch (Exception ex)
            {
                System.out.println("[Residence] Failed to save residence (" + res.getKey() + ")!");
                Logger.getLogger(ResidenceManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return resmap;
    }

    public static ResidenceManager load(Map<String,Object> root)
    {
        ResidenceManager resm = new ResidenceManager();
        if(root != null)
        {
            for(Entry<String, Object> res : root.entrySet())
            {
                try
                {
                    resm.residences.put(res.getKey(), ClaimedResidence.load((Map<String, Object>) res.getValue(), null));
                }
                catch (Exception ex)
                {
                    System.out.print("[Residence] Failed to load residence (" + res.getKey() + ")! Reason:" + ex.getMessage() + " Error Log:");
                    Logger.getLogger(ResidenceManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return resm;
    }

    public void renameResidence(Player player, String oldName, String newName)
    {
        newName = newName.replace(".", "_");
        newName = newName.replace(":", "_");
        ClaimedResidence res = this.getByName(oldName);
        if(res==null)
        {
            player.sendMessage("§cInvalid Residence...");
            return;
        }
        if(res.getPermissions().hasResidencePermission(player, true))
        {
            if(res.getParent()==null)
            {
                if(residences.containsKey(newName))
                {
                    player.sendMessage("§cAnother residence already has that name...");
                    return;
                }
                residences.put(newName, res);
                residences.remove(oldName);
                if(Residence.getConfig().useLeases())
                    Residence.getLeaseManager().updateLeaseName(oldName, newName);
                if(Residence.getConfig().enabledRentSystem())
                {
                    Residence.getRentManager().updateRentableName(oldName, newName);
                }
                player.sendMessage("§aRenamed §e" + oldName + "§a to §e" + newName + "§a...");
            }
            else
            {
                String[] oldname = oldName.split("\\.");
                ClaimedResidence parent = res.getParent();
                parent.renameSubzone(player, oldname[oldname.length-1], newName);
            }
        }
        else
        {
            player.sendMessage("§cYou dont have permission...");
        }
    }

    public void giveResidence(Player reqPlayer, String targPlayer, String residence)
    {
        ClaimedResidence res = getByName(residence);
        if(res==null)
        {
            reqPlayer.sendMessage("§cInvalid Residence...");
            return;
        }
        if(!res.getPermissions().hasResidencePermission(reqPlayer, true))
        {
            reqPlayer.sendMessage("§cYou dont have permission to give this residence.");
            return;
        }
        boolean admin = Residence.getPermissionManager().isResidenceAdmin(reqPlayer);
        Player giveplayer = Residence.getServ().getPlayer(targPlayer);
        if (giveplayer == null || !giveplayer.isOnline()) {
            reqPlayer.sendMessage("§cTarget player must be online.");
            return;
        }
        CuboidArea[] areas = res.getAreaArray();
        PermissionGroup g = Residence.getPermissionManager().getGroup(giveplayer);
        if (areas.length > g.getMaxPhysicalPerResidence() && !admin) {
            reqPlayer.sendMessage("§cCannot give residence to target player, because it has more areas then allowed for the target players group.");
            return;
        }
        if (getOwnedZoneCount(giveplayer.getName()) >= g.getMaxZones() && !admin) {
            reqPlayer.sendMessage("§cTarget player already owns the maximum number of residences allowed.");
            return;
        }
        if(!admin)
        {
            for (CuboidArea area : areas) {
                if (!g.inLimits(area)) {
                    reqPlayer.sendMessage("§cCannot give residence to target player, because a area is outside the target players limits.");
                    return;
                }
            }
        }
        res.getPermissions().setOwner(giveplayer.getName(), true);
        reqPlayer.sendMessage("§aYou give residence §e" + residence + "§a to player §e" + giveplayer.getName() + "§a.");
        giveplayer.sendMessage("§a" + reqPlayer.getName() + "§e has given Residence §a" + residence + "§e to you.");
    }

    public int getResidenceCount()
    {
        return residences.size();
    }
}
