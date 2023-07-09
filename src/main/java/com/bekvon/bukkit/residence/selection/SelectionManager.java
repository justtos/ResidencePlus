package com.bekvon.bukkit.residence.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.containers.SelectionSides;
import com.bekvon.bukkit.residence.containers.Visualizer;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.event.ResidenceSelectionVisualizationEvent;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import com.bekvon.bukkit.residence.permissions.PermissionManager.ResPerm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;

import net.Zrips.CMILib.CMILib;
import net.Zrips.CMILib.ActionBar.CMIActionBar;
import net.Zrips.CMILib.Container.CMIWorld;
import net.Zrips.CMILib.Effects.CMIEffect;
import net.Zrips.CMILib.Effects.CMIEffectManager.CMIParticle;
import net.Zrips.CMILib.Logs.CMIDebug;

public class SelectionManager {
    protected Map<UUID, Selection> selections;
    protected Server server;
    protected Residence plugin;

    private HashMap<UUID, Visualizer> vMap = new HashMap<UUID, Visualizer>();

    Permission ignoreyPermission = new Permission(ResPerm.bypass_ignorey.getPermission(), PermissionDefault.FALSE);
    Permission ignoreyinsubzonePermission = new Permission(ResPerm.bypass_ignoreyinsubzone.getPermission(), PermissionDefault.FALSE);

    public enum selectionType {
	noLimits, ignoreY, residenceBounds;
    }

    public class Selection {
	private Player player;
	private Location loc1;
	private Location loc2;

	public Selection(Player player) {
	    this.player = player;
	}

	public Location getBaseLoc1() {
	    return loc1 == null ? null : loc1.clone();
	}

	public void setBaseLoc1(Location loc1) {
	    this.loc1 = loc1.clone();
	}

	public World getWorld() {
	    if (loc1 != null)
		return loc1.getWorld();
	    if (loc2 != null)
		return loc2.getWorld();
	    return player.getWorld();
	}

	public Location getBaseLoc2() {
	    return loc2 == null ? null : loc2.clone();
	}

	public void setBaseLoc2(Location loc2) {
	    this.loc2 = loc2.clone();
	}

	public selectionType getSelectionRestrictions() {
	    if (inSameResidence()) {
		if (plugin.getConfigManager().isSelectionIgnoreYInSubzone()) {
		    if (hasPlacedBoth() && !player.hasPermission(ignoreyinsubzonePermission)) {
			return selectionType.residenceBounds;
		    }
		}
	    } else {
		if (plugin.getConfigManager().isSelectionIgnoreY()) {
		    if (hasPlacedBoth() && !player.hasPermission(ignoreyPermission)) {
			return selectionType.ignoreY;
		    }
		}
	    }
	    return selectionType.noLimits;
	}

	public int getMaxYAllowed() {
	    switch (getSelectionRestrictions()) {
	    case ignoreY:
	    case noLimits:
	    default:
		return getMaxWorldHeight(getWorld());
	    case residenceBounds:
		ClaimedResidence res1 = plugin.getResidenceManager().getByLoc(this.getBaseLoc2());
		if (res1 != null) {
		    CuboidArea area = res1.getAreaByLoc(this.getBaseLoc2());
		    if (area != null) {
			return area.getHighVector().getBlockY();
		    }
		}
		break;
	    }
	    return getMaxWorldHeight(getWorld());
	}

	public int getMinYAllowed() {
	    switch (getSelectionRestrictions()) {
	    case ignoreY:
	    case noLimits:
	    default:
		try {
		    return CMIWorld.getMinHeight(this.getBaseLoc1().getWorld());
		} catch (Throwable e) {
		    return 0;
		}
	    case residenceBounds:
		ClaimedResidence res1 = plugin.getResidenceManager().getByLoc(this.getBaseLoc1());
		if (res1 != null) {
		    CuboidArea area = res1.getAreaByLoc(getBaseLoc1());
		    if (area != null)
			return area.getLowVector().getBlockY();
		}
		break;
	    }
	    try {
		return CMIWorld.getMinHeight(this.getBaseLoc1().getWorld());
	    } catch (Throwable e) {
		return 0;
	    }
	}

	private boolean inSameResidence() {
	    if (!hasPlacedBoth())
		return false;

	    ClaimedResidence res1 = plugin.getResidenceManager().getByLoc(this.getBaseLoc1());

	    if (res1 == null)
		return false;

	    ClaimedResidence res2 = plugin.getResidenceManager().getByLoc(this.getBaseLoc2());

	    if (res2 == null)
		return false;
	    return res1.getName().equals(res2.getName());
	}

	public void vert(boolean resadmin) {
	    if (hasPlacedBoth()) {
		sky(resadmin);
		bedrock(resadmin);
	    } else {
		plugin.msg(player, lm.Select_Points);
	    }
	}

	private void shadowSky(CuboidArea area) {
	    if (!hasPlacedBoth())
		return;
	    area.setHighLocation(this.getBaseArea().getHighLocation());
	    area.getHighVector().setY(this.getMaxYAllowed());
	}

	private void shadowBedrock(CuboidArea area) {
	    if (!hasPlacedBoth())
		return;
	    area.setLowLocation(this.getBaseArea().getLowLocation());
	    area.getLowVector().setY(this.getMinYAllowed());
	}

	public void sky(boolean resadmin) {
	    if (hasPlacedBoth()) {
		ResidencePlayer rPlayer = plugin.getPlayerManager().getResidencePlayer(this.getPlayer());
		PermissionGroup group = rPlayer.getGroup();

		CuboidArea base = this.getBaseArea();

		int y1 = base.getLowVector().getBlockY();

		int newy = this.getMaxYAllowed();

		if (!resadmin) {
		    if (group.getMaxHeight() < newy)
			newy = group.getMaxHeight();
		    if (newy - y1 > (group.getMaxY() - 1))
			newy = y1 + (group.getMaxY() - 1);
		}

		loc1 = base.getLowLocation();
		loc2 = base.getHighLocation();
		loc2.setY(newy);

		plugin.msg(player, lm.Select_Sky);
	    } else {
		plugin.msg(player, lm.Select_Points);
	    }
	}

	public void bedrock(boolean resadmin) {
	    if (hasPlacedBoth()) {
		ResidencePlayer rPlayer = plugin.getPlayerManager().getResidencePlayer(this.getPlayer());
		PermissionGroup group = rPlayer.getGroup();

		CuboidArea base = this.getBaseArea();

		int y2 = base.getHighVector().getBlockY();

		int newy = this.getMinYAllowed();
		if (!resadmin) {
		    if (newy < group.getMinHeight())
			newy = group.getMinHeight();
		    if (y2 - newy > (group.getMaxY() - 1))
			newy = y2 - (group.getMaxY() - 1);
		}

		loc1 = base.getLowLocation();
		loc2 = base.getHighLocation();
		loc1.setY(newy);

		plugin.msg(player, lm.Select_Bedrock);
	    } else {
		plugin.msg(player, lm.Select_Points);
	    }
	}

	public void selectChunk() {
	    Chunk chunk = player.getWorld().getChunkAt(player.getLocation());
	    int xcoord = chunk.getX() * 16;
	    int zcoord = chunk.getZ() * 16;
	    int xmax = xcoord + 15;
	    int zmax = zcoord + 15;

	    this.setBaseLoc1(new Location(player.getWorld(), xcoord, this.getMinYAllowed(), zcoord));
	    this.setBaseLoc2(new Location(player.getWorld(), xmax, this.getMaxYAllowed(), zmax));
	    plugin.msg(player, lm.Select_Success);
	}

	public boolean hasPlacedBoth() {
	    return this.getBaseLoc1() != null && this.getBaseLoc2() != null;
	}

	public Player getPlayer() {
	    return player;
	}

	public void setPlayer(Player player) {
	    this.player = player;
	}

	public CuboidArea getBaseArea() {
	    if (!this.hasPlacedBoth())
		return null;
	    return new CuboidArea(this.loc1, this.loc2);
	}

	public CuboidArea getResizedArea() {

	    CuboidArea area = this.getBaseArea();
	    switch (getSelectionRestrictions()) {
	    case noLimits:
		break;
	    case residenceBounds:
	    case ignoreY:
		shadowSky(area);
		shadowBedrock(area);
		break;
	    default:
		break;
	    }

	    return area;
	}
    }

    private int getMaxWorldHeight(World world) {
	if (world == null)
	    return 319;
	switch (world.getEnvironment()) {
	case NETHER:
	    return plugin.getConfigManager().getSelectionNetherHeight();
	case NORMAL:
	case THE_END:

	    try {
		return CMIWorld.getMaxHeight(world) - 1;
	    } catch (Throwable e) {
		return 319;
	    }
	default:
	    break;
	}

	try {
	    return CMIWorld.getMaxHeight(world) - 1;
	} catch (Throwable e) {
	    return 319;
	}
    }

    public enum Direction {
	UP, DOWN, PLUSX, PLUSZ, MINUSX, MINUSZ;

	public Direction getOpposite() {
	    switch (this) {
	    case DOWN:
		return UP;
	    case MINUSX:
		return PLUSX;
	    case MINUSZ:
		return PLUSZ;
	    case PLUSX:
		return MINUSX;
	    case PLUSZ:
		return MINUSZ;
	    case UP:
		return DOWN;
	    default:
		break;
	    }
	    return this;
	}
    }

    public SelectionManager(Server server, Residence plugin) {
	this.plugin = plugin;
	this.server = server;
	selections = Collections.synchronizedMap(new HashMap<UUID, Selection>());
    }

    public Selection getSelection(Player player) {
	Selection s = selections.get(player.getUniqueId());
	if (s == null) {
	    s = new Selection(player);
	    selections.put(player.getUniqueId(), s);
	}
	return s;
    }

    public void updateLocations(Player player) {
	Selection s = selections.get(player.getUniqueId());
	if (s != null) {
	    updateLocations(player, this.getSelection(player).getBaseLoc1(), this.getSelection(player).getBaseLoc2(), true);
	}
    }

    public void updateLocations(Player player, Location loc1, Location loc2) {
	updateLocations(player, loc1, loc2, false);
    }

    public void updateLocations(Player player, Location loc1, Location loc2, boolean force) {
	Selection selection = getSelection(player);
	if (loc1 != null)
	    selection.setBaseLoc1(loc1);
	if (loc2 != null)
	    selection.setBaseLoc2(loc2);
	this.afterSelectionUpdate(player, force);
    }

    public void placeLoc1(Player player, Location loc) {
	placeLoc1(player, loc, false);
    }

    public void placeLoc1(Player player, Location loc, boolean show) {
	if (loc != null) {
	    getSelection(player).setBaseLoc1(loc);
	    if (show) {
		this.afterSelectionUpdate(player);
	    }
	}
    }

    public void placeLoc2(Player player, Location loc) {
	placeLoc2(player, loc, false);
    }

    public void placeLoc2(Player player, Location loc, boolean show) {
	if (loc != null) {
	    getSelection(player).setBaseLoc2(loc);
	    if (show) {
		this.afterSelectionUpdate(player);
	    }
	}
    }

    public void afterSelectionUpdate(Player player) {
	afterSelectionUpdate(player, false);
    }

    public void afterSelectionUpdate(Player player, boolean force) {
	if (!this.hasPlacedBoth(player))
	    return;
	Visualizer v = vMap.get(player.getUniqueId());
	if (v == null) {
	    v = new Visualizer(player);
	    vMap.put(player.getUniqueId(), v);
	}
	v.setStart(System.currentTimeMillis());
	v.cancelAll();
	if (force)
	    v.setLoc(null);
	v.setAreas(this.getSelectionCuboid(player));
	v.setOnce(false);

	this.showBounds(player, v);
    }

    public Location getPlayerLoc1(Player player) {
	if (player == null)
	    return null;
	Selection sel = getSelection(player);
	if (sel == null)
	    return null;
	CuboidArea area = sel.getResizedArea();
	if (area == null)
	    return null;
	return area.getLowLocation();
    }

    @Deprecated
    public Location getPlayerLoc1(String player) {
	return getPlayerLoc1(Bukkit.getPlayer(player));
    }

    public Location getPlayerLoc2(Player player) {
	if (player == null)
	    return null;
	Selection sel = getSelection(player);
	if (sel == null)
	    return null;
	CuboidArea area = sel.getResizedArea();
	if (area == null)
	    return null;
	return area.getHighLocation();
    }

    @Deprecated
    public Location getPlayerLoc2(String player) {
	return getPlayerLoc2(Bukkit.getPlayer(player));
    }

    public CuboidArea getSelectionCuboid(Player player) {
	if (player == null)
	    return null;
	return getSelection(player).getResizedArea();
    }

    @Deprecated
    public CuboidArea getSelectionCuboid(String player) {
	if (!hasPlacedBoth(player))
	    return null;
	return getSelectionCuboid(Bukkit.getPlayer(player));
    }

    public boolean hasPlacedBoth(Player player) {
	if (player == null)
	    return false;
	return getSelection(player).hasPlacedBoth();
    }

    @Deprecated
    public boolean hasPlacedBoth(String player) {
	return hasPlacedBoth(Bukkit.getPlayer(player));
    }

    public void showSelectionInfoInActionBar(Player player) {

	if (!plugin.getConfigManager().useActionBarOnSelection())
	    return;

	CuboidArea cuboidArea = this.getSelectionCuboid(player);

	String Message = plugin.msg(lm.Select_TotalSize, cuboidArea.getSize());

	ResidencePlayer rPlayer = plugin.getPlayerManager().getResidencePlayer(player);
	PermissionGroup group = rPlayer.getGroup();
	if (plugin.getConfigManager().enableEconomy())
	    Message += " " + plugin.msg(lm.General_LandCost, cuboidArea.getCost(group));

	CMIActionBar.send(player, Message);

    }

    public void showSelectionInfo(Player player) {
	if (hasPlacedBoth(player)) {
	    plugin.msg(player, lm.General_Separator);
	    CuboidArea cuboidArea = this.getSelectionCuboid(player);
	    plugin.msg(player, lm.Select_TotalSize, cuboidArea.getSize());

	    ResidencePlayer rPlayer = plugin.getPlayerManager().getResidencePlayer(player);
	    PermissionGroup group = rPlayer.getGroup();

	    if (plugin.getConfigManager().enableEconomy())
		plugin.msg(player, lm.General_LandCost, cuboidArea.getCost(group));
	    player.sendMessage(ChatColor.YELLOW + "X" + plugin.msg(lm.General_Size, cuboidArea.getXSize()));
	    player.sendMessage(ChatColor.YELLOW + "Y" + plugin.msg(lm.General_Size, cuboidArea.getYSize()));
	    player.sendMessage(ChatColor.YELLOW + "Z" + plugin.msg(lm.General_Size, cuboidArea.getZSize()));
	    plugin.msg(player, lm.General_Separator);
	    Visualizer v = new Visualizer(player);
	    v.setAreas(this.getSelectionCuboid(player));
	    this.showBounds(player, v);
	} else
	    plugin.msg(player, lm.Select_Points);
    }

    public void showBounds(final Player player, final Visualizer v) {

	if (!plugin.getConfigManager().useVisualizer())
	    return;

	Visualizer tv = vMap.get(player.getUniqueId());
	if (tv != null) {
	    tv.cancelAll();
	}
	Bukkit.getScheduler().runTask(plugin, () -> {

	    ResidenceSelectionVisualizationEvent ev = new ResidenceSelectionVisualizationEvent(player, v.getAreas(), v.getErrorAreas());
	    Bukkit.getPluginManager().callEvent(ev);

	    if (ev.isCancelled())
		return;

	    vMap.put(player.getUniqueId(), v);
	    if (!plugin.isEnabled())
		return;
	    v.setBaseShedId(Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
		@Override
		public void run() {
		    if (!v.getAreas().isEmpty())
			MakeBorders(player, false);
		    if (!v.getErrorAreas().isEmpty())
			MakeBorders(player, true);
		}
	    }).getTaskId());

	});
    }

    public List<Location> getLocations(Location lowLoc, Location loc, Double TX, Double TY, Double TZ, Double Range, boolean StartFromZero) {

	double eachCollumn = plugin.getConfigManager().getVisualizerRowSpacing();
	double eachRow = plugin.getConfigManager().getVisualizerCollumnSpacing();

	if (TX == 0D)
	    TX = eachCollumn + eachCollumn * 0.1;
	if (TY == 0D)
	    TY = eachRow + eachRow * 0.1;
	if (TZ == 0D)
	    TZ = eachCollumn + eachCollumn * 0.1;

	double CollumnStart = eachCollumn;
	double RowStart = eachRow;

	if (StartFromZero) {
	    CollumnStart = 0;
	    RowStart = 0;
	}

	List<Location> locList = new ArrayList<Location>();

	if (lowLoc.getWorld() != loc.getWorld())
	    return locList;

	for (double x = CollumnStart; x < TX; x += eachCollumn) {
	    Location CurrentX = lowLoc.clone();
	    if (TX > eachCollumn + eachCollumn * 0.1)
		CurrentX.add(x, 0, 0);
	    for (double y = RowStart; y < TY; y += eachRow) {
		Location CurrentY = CurrentX.clone();
		if (TY > eachRow + eachRow * 0.1)
		    CurrentY.add(0, y, 0);
		for (double z = CollumnStart; z < TZ; z += eachCollumn) {
		    Location CurrentZ = CurrentY.clone();
		    if (TZ > eachCollumn + eachCollumn * 0.1)
			CurrentZ.add(0, 0, z);
		    double dist = loc.distance(CurrentZ);
		    if (dist < Range)
			locList.add(CurrentZ.clone());
		}
	    }
	}

	return locList;
    }

    public List<Location> GetLocationsWallsByData(Location loc, Double TX, Double TY, Double TZ, Location lowLoc, SelectionSides Sides,
	double Range) {
	List<Location> locList = new ArrayList<Location>();

	// North wall
	if (Sides.ShowNorthSide())
	    locList.addAll(getLocations(lowLoc.clone(), loc.clone(), TX, TY, 0D, Range, false));

	// South wall
	if (Sides.ShowSouthSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, 0, TZ), loc.clone(), TX, TY, 0D, Range, false));

	// West wall
	if (Sides.ShowWestSide())
	    locList.addAll(getLocations(lowLoc.clone(), loc.clone(), 0D, TY, TZ, Range, false));

	// East wall
	if (Sides.ShowEastSide())
	    locList.addAll(getLocations(lowLoc.clone().add(TX, 0, 0), loc.clone(), 0D, TY, TZ, Range, false));

	// Roof wall
	if (Sides.ShowTopSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, TY, 0), loc.clone(), TX, 0D, TZ, Range, false));

	// Ground wall
	if (Sides.ShowBottomSide())
	    locList.addAll(getLocations(lowLoc.clone(), loc.clone(), TX, 0D, TZ, Range, false));

	return locList;
    }

    public List<Location> GetLocationsCornersByData(Location loc, Double TX, Double TY, Double TZ, Location lowLoc, SelectionSides Sides,
	double Range) {
	List<Location> locList = new ArrayList<Location>();

	// North bottom line
	if (Sides.ShowBottomSide() && Sides.ShowNorthSide())
	    locList.addAll(getLocations(lowLoc.clone(), loc.clone(), TX, 0D, 0D, Range, true));

	// North top line
	if (Sides.ShowTopSide() && Sides.ShowNorthSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, TY, 0), loc.clone(), TX, 0D, 0D, Range, true));

	// South bottom line
	if (Sides.ShowBottomSide() && Sides.ShowSouthSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, 0, TZ), loc.clone(), TX, 0D, 0D, Range, true));

	// South top line
	if (Sides.ShowTopSide() && Sides.ShowSouthSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, TY, TZ), loc.clone(), TX, 0D, 0D, Range, true));

	// North - West corner
	if (Sides.ShowWestSide() && Sides.ShowNorthSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, 0, 0), loc.clone(), 0D, TY, 0D, Range, true));

	// North - East corner
	if (Sides.ShowEastSide() && Sides.ShowNorthSide())
	    locList.addAll(getLocations(lowLoc.clone().add(TX, 0, 0), loc.clone(), 0D, TY, 0D, Range, true));

	// South - West corner
	if (Sides.ShowSouthSide() && Sides.ShowWestSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, 0, TZ), loc.clone(), 0D, TY, 0D, Range, true));

	// South - East corner
	if (Sides.ShowSouthSide() && Sides.ShowEastSide())
	    locList.addAll(getLocations(lowLoc.clone().add(TX, 0, TZ), loc.clone(), 0D, TY + 1, 0D, Range, true));

	// West bottom corner
	if (Sides.ShowWestSide() && Sides.ShowBottomSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, 0, 0), loc.clone(), 0D, 0D, TZ, Range, true));

	// East bottom corner
	if (Sides.ShowEastSide() && Sides.ShowBottomSide())
	    locList.addAll(getLocations(lowLoc.clone().add(TX, 0, 0), loc.clone(), 0D, 0D, TZ, Range, true));

	// West top corner
	if (Sides.ShowWestSide() && Sides.ShowTopSide())
	    locList.addAll(getLocations(lowLoc.clone().add(0, TY, 0), loc.clone(), 0D, 0D, TZ, Range, true));

	// East top corner
	if (Sides.ShowEastSide() && Sides.ShowTopSide())
	    locList.addAll(getLocations(lowLoc.clone().add(TX, TY, 0), loc.clone(), 0D, 0D, TZ, Range, true));

	return locList;
    }

    public boolean MakeBorders(final Player player, final boolean error) {

	final Visualizer v = vMap.get(player.getUniqueId());

	if (v == null)
	    return false;

	List<CuboidArea> areas = null;

	if (!error)
	    areas = v.getAreas();
	else
	    areas = v.getErrorAreas();

	Location loc = player.getLocation();
	int Range = plugin.getConfigManager().getVisualizerRange();

	final List<Location> locList = new ArrayList<Location>();
	final List<Location> errorLocList = new ArrayList<Location>();

	final boolean same = v.isSameLoc();
	if (!same) {
	    for (CuboidArea area : areas) {
		if (area == null)
		    continue;
		CuboidArea cuboidArea = new CuboidArea(area.getLowLocation(), area.getHighLocation());
		cuboidArea.getHighVector().add(new Vector(1, 1, 1));

		SelectionSides Sides = new SelectionSides();

		double PLLX = loc.getBlockX() - Range;
		double PLLZ = loc.getBlockZ() - Range;
		double PLLY = loc.getBlockY() - Range;
		double PLHX = loc.getBlockX() + Range;
		double PLHZ = loc.getBlockZ() + Range;
		double PLHY = loc.getBlockY() + Range;

		if (cuboidArea.getLowVector().getBlockX() < PLLX) {
		    cuboidArea.getLowVector().setX(PLLX);
		    Sides.setWestSide(false);
		}

		if (cuboidArea.getHighVector().getBlockX() > PLHX) {
		    cuboidArea.getHighVector().setX(PLHX);
		    Sides.setEastSide(false);
		}

		if (cuboidArea.getLowVector().getBlockZ() < PLLZ) {
		    cuboidArea.getLowVector().setZ(PLLZ);
		    Sides.setNorthSide(false);
		}

		if (cuboidArea.getHighVector().getBlockZ() > PLHZ) {
		    cuboidArea.getHighVector().setZ(PLHZ);
		    Sides.setSouthSide(false);
		}

		if (cuboidArea.getLowVector().getBlockY() < PLLY) {
		    cuboidArea.getLowVector().setY(PLLY);
		    Sides.setBottomSide(false);
		}

		if (cuboidArea.getHighVector().getBlockY() > PLHY) {
		    cuboidArea.getHighVector().setY(PLHY);
		    Sides.setTopSide(false);
		}

		double TX = cuboidArea.getXSize() - 1;
		double TY = cuboidArea.getYSize() - 1;
		double TZ = cuboidArea.getZSize() - 1;

		if (!error && v.getId() != -1) {
		    Bukkit.getScheduler().cancelTask(v.getId());
		} else if (error && v.getErrorId() != -1) {
		    Bukkit.getScheduler().cancelTask(v.getErrorId());
		}

		locList.addAll(GetLocationsWallsByData(loc, TX, TY, TZ, cuboidArea.getLowLocation().clone(), Sides, Range));
		errorLocList.addAll(GetLocationsCornersByData(loc, TX, TY, TZ, cuboidArea.getLowLocation().clone(), Sides, Range));
	    }
	    v.setLoc(player.getLocation());
	} else {
	    if (error) {
		locList.addAll(v.getErrorLocations());
		errorLocList.addAll(v.getErrorLocations2());
	    } else {
		locList.addAll(v.getLocations());
		errorLocList.addAll(v.getLocations2());
	    }
	}

	if (!plugin.isEnabled())
	    return false;
	Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
	    @Override
	    public void run() {

		int size = locList.size();
		int errorSize = errorLocList.size();

		int timesMore = 1;
		int errorTimesMore = 1;

		if (size > plugin.getConfigManager().getVisualizerSidesCap()) {
		    timesMore = size / plugin.getConfigManager().getVisualizerSidesCap() + 1;
		}
		if (errorSize > plugin.getConfigManager().getVisualizerFrameCap()) {
		    errorTimesMore = errorSize / plugin.getConfigManager().getVisualizerFrameCap() + 1;
		}

		v.addCurrentSkip();
		if (v.getCurrentSkip() > plugin.getConfigManager().getVisualizerSkipBy())
		    v.setCurrentSkip(1);

		try {
		    showParticles(locList, player, timesMore, error, true, v.getCurrentSkip());
		    showParticles(errorLocList, player, errorTimesMore, error, false, v.getCurrentSkip());
		} catch (Exception e) {
		    return;
		}

		if (error) {
		    v.setErrorLocations(locList);
		    v.setErrorLocations2(errorLocList);
		} else {
		    v.setLocations(locList);
		    v.setLocations2(errorLocList);
		}

		return;
	    }
	});

	if (v.isOnce())
	    return true;

	if (v.getStart() + plugin.getConfigManager().getVisualizerShowFor() < System.currentTimeMillis())
	    return false;

	int scid = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    @Override
	    public void run() {
		if (player.isOnline()) {
		    MakeBorders(player, error);
		}
		return;
	    }
	}, plugin.getConfigManager().getVisualizerUpdateInterval() * 1L);
	if (!error)
	    v.setId(scid);
	else
	    v.setErrorId(scid);

	return true;

    }

    private void showParticles(List<Location> locList, Player player, int timesMore, boolean error, boolean sides, int currentSkipBy) {
	int s = 0;

	for (int i = 0; i < locList.size(); i += timesMore) {
	    s++;

	    if (s > plugin.getConfigManager().getVisualizerSkipBy())
		s = 1;

	    if (s != currentSkipBy)
		continue;

	    Location l = locList.get(i);

	    CMIParticle effect = null;
	    if (sides) {
		effect = error ? plugin.getConfigManager().getOverlapSides() : plugin.getConfigManager().getSelectedSides();
	    } else {
		effect = error ? plugin.getConfigManager().getOverlapFrame() : plugin.getConfigManager().getSelectedFrame();
	    }

	    CMIEffect ef = new CMIEffect(effect);

	    CMILib.getInstance().getReflectionManager().playEffect(player, l, ef);
	}
    }

    public void vert(Player player, boolean resadmin) {
	if (hasPlacedBoth(player)) {
	    this.sky(player, resadmin);
	    this.bedrock(player, resadmin);
	} else {
	    plugin.msg(player, lm.Select_Points);
	}
    }

    public void sky(Player player, boolean resadmin) {
	Selection selection = this.getSelection(player);
	selection.sky(resadmin);
    }

    public void bedrock(Player player, boolean resadmin) {
	Selection selection = this.getSelection(player);
	selection.bedrock(resadmin);
    }

    public void clearSelection(Player player) {
	selections.remove(player.getUniqueId());
    }

    @Deprecated
    public void selectChunk(Player player) {
	Selection selection = this.getSelection(player);
	selection.selectChunk();
    }

    public boolean worldEdit(Player player) {
	plugin.msg(player, lm.General_WorldEditNotFound);
	return false;
    }

    public boolean worldEditUpdate(Player player) {
	plugin.msg(player, lm.General_WorldEditNotFound);
	return false;
    }

    public void selectBySize(Player player, int xsize, int ysize, int zsize) {
	Location myloc = player.getLocation();
	Location loc1 = new Location(myloc.getWorld(), myloc.getBlockX() + xsize, myloc.getBlockY() + ysize, myloc.getBlockZ() + zsize);
	Location loc2 = new Location(myloc.getWorld(), myloc.getBlockX() - xsize, myloc.getBlockY() - ysize, myloc.getBlockZ() - zsize);

	CuboidArea area = new CuboidArea(loc1, loc2);

	placeLoc1(player, loc1, false);
	placeLoc2(player, loc2, false);

	Selection selection = this.getSelection(player);

	if (selection.getMaxYAllowed() < area.getHighVector().getBlockY())
	    selection.getBaseLoc2().setY(selection.getMaxYAllowed());

	if (selection.getMinYAllowed() > area.getLowVector().getBlockY())
	    selection.getBaseLoc1().setY(selection.getMinYAllowed());

//	selection.updateBaseArea();
//	selection.updateShadowLocations();
//	selection.updateShadowArea();

	this.afterSelectionUpdate(player);
	plugin.msg(player, lm.Select_Success);
	showSelectionInfo(player);
    }

    public void modify(Player player, boolean shift, double amount) {
	if (!hasPlacedBoth(player)) {
	    plugin.msg(player, lm.Select_Points);
	    return;
	}
	Direction d = getDirection(player);
	if (d == null) {
	    plugin.msg(player, lm.Invalid_Direction);
	    return;
	}
	CuboidArea area = this.getSelectionCuboid(player);

	int MIN_HEIGHT = 0;

	try {
	    MIN_HEIGHT = CMIWorld.getMinHeight(area.getWorld());
	} catch (Throwable e) {
	}

	switch (d) {
	case DOWN:
	    double oldy = area.getLowVector().getBlockY();
	    oldy = oldy - amount;
	    if (oldy < MIN_HEIGHT) {
		plugin.msg(player, lm.Select_TooLow);
		oldy = MIN_HEIGHT;
	    }
	    area.getLowVector().setY(oldy);
	    if (shift) {
		double oldy2 = area.getHighVector().getBlockY();
		oldy2 = oldy2 - amount;
		area.getHighVector().setY(oldy2);
		plugin.msg(player, lm.Shifting_Down, amount);
	    } else
		plugin.msg(player, lm.Expanding_Down, amount);
	    break;
	case MINUSX:
	    double oldx = area.getLowVector().getBlockX();
	    oldx = oldx - amount;
	    area.getLowVector().setX(oldx);
	    if (shift) {
		double oldx2 = area.getHighVector().getBlockX();
		oldx2 = oldx2 - amount;
		area.getHighVector().setX(oldx2);
		plugin.msg(player, lm.Shifting_West, amount);
	    } else
		plugin.msg(player, lm.Expanding_West, amount);
	    break;
	case MINUSZ:
	    double oldz = area.getLowVector().getBlockZ();
	    oldz = oldz - amount;
	    area.getLowVector().setZ(oldz);
	    if (shift) {
		double oldz2 = area.getHighVector().getBlockZ();
		oldz2 = oldz2 - amount;
		area.getHighVector().setZ(oldz2);
		plugin.msg(player, lm.Shifting_North, amount);
	    } else
		plugin.msg(player, lm.Expanding_North, amount);
	    break;
	case PLUSX:
	    oldx = area.getHighVector().getBlockX();
	    oldx = oldx + amount;
	    area.getHighVector().setX(oldx);
	    if (shift) {
		double oldx2 = area.getLowVector().getBlockX();
		oldx2 = oldx2 + amount;
		area.getLowVector().setX(oldx2);
		plugin.msg(player, lm.Shifting_East, amount);
	    } else
		plugin.msg(player, lm.Expanding_East, amount);
	    break;
	case PLUSZ:
	    oldz = area.getHighVector().getBlockZ();
	    oldz = oldz + amount;
	    area.getHighVector().setZ(oldz);
	    if (shift) {
		double oldz2 = area.getLowVector().getBlockZ();
		oldz2 = oldz2 + amount;
		area.getLowVector().setZ(oldz2);
		plugin.msg(player, lm.Shifting_South, amount);
	    } else
		plugin.msg(player, lm.Expanding_South, amount);
	    break;
	case UP:
	    oldy = area.getHighVector().getBlockY();
	    oldy = oldy + amount;
	    if (oldy > getMaxWorldHeight(player.getLocation().getWorld()) - 1) {
		plugin.msg(player, lm.Select_TooHigh);
		oldy = getMaxWorldHeight(player.getLocation().getWorld()) - 1;
	    }
	    area.getHighVector().setY(oldy);
	    if (shift) {
		double oldy2 = area.getLowVector().getBlockY();
		oldy2 = oldy2 + amount;
		area.getLowVector().setY(oldy2);
		plugin.msg(player, lm.Shifting_Up, amount);
	    } else
		plugin.msg(player, lm.Expanding_Up, amount);
	    break;
	default:
	    break;
	}
	updateLocations(player, area.getHighLocation(), area.getLowLocation(), true);
    }

    public boolean contract(Player player, double amount) {
	return contract(player, amount, false);
    }

    public boolean contract(Player player, double amount, @SuppressWarnings("unused") boolean resadmin) {
	if (!hasPlacedBoth(player)) {
	    plugin.msg(player, lm.Select_Points);
	    return false;
	}
	Direction d = getDirection(player);
	if (d == null) {
	    plugin.msg(player, lm.Invalid_Direction);
	    return false;
	}
	d = d.getOpposite();

	CuboidArea area = this.getSelectionCuboid(player);
	switch (d) {
	case UP:
	    double oldy = area.getHighVector().getBlockY();
	    oldy = oldy - amount;
	    if (oldy > getMaxWorldHeight(player.getLocation().getWorld()) - 1) {
		plugin.msg(player, lm.Select_TooHigh);
		oldy = getMaxWorldHeight(player.getLocation().getWorld()) - 1;
	    }
	    area.getHighVector().setY(oldy);
	    plugin.msg(player, lm.Contracting_Down, amount);
	    break;
	case PLUSX:
	    double oldx = area.getHighVector().getBlockX();
	    oldx = oldx - amount;
	    area.getHighVector().setX(oldx);
	    plugin.msg(player, lm.Contracting_West, amount);
	    break;
	case PLUSZ:
	    double oldz = area.getHighVector().getBlockZ();
	    oldz = oldz - amount;
	    area.getHighVector().setZ(oldz);
	    plugin.msg(player, lm.Contracting_North, amount);
	    break;
	case MINUSX:
	    oldx = area.getLowVector().getBlockX();
	    oldx = oldx + amount;
	    area.getLowVector().setX(oldx);
	    plugin.msg(player, lm.Contracting_East, amount);
	    break;
	case MINUSZ:
	    oldz = area.getLowVector().getBlockZ();
	    oldz = oldz + amount;
	    area.getLowVector().setZ(oldz);
	    plugin.msg(player, lm.Contracting_South, amount);
	    break;
	case DOWN:

	    int MIN_HEIGHT = 0;
	    try {
		MIN_HEIGHT = CMIWorld.getMinHeight(area.getWorld());
	    } catch (Throwable e) {
	    }
	    oldy = area.getLowVector().getBlockY();
	    oldy = oldy + amount;
	    if (oldy < MIN_HEIGHT) {
		plugin.msg(player, lm.Select_TooLow);
		oldy = MIN_HEIGHT;
	    }
	    area.getLowVector().setY(oldy);
	    plugin.msg(player, lm.Contracting_Up, amount);
	    break;
	default:
	    break;
	}

//	if (!ClaimedResidence.isBiggerThanMinSubzone(player, area, resadmin))
//	    return false;

	updateLocations(player, area.getHighLocation(), area.getLowLocation(), true);
	return true;
    }

    private static Direction getDirection(Player player) {

	int yaw = (int) player.getLocation().getYaw();

	if (yaw < 0)
	    yaw += 360;

	yaw += 45;
	yaw %= 360;

	int facing = yaw / 90;

	float pitch = player.getLocation().getPitch();
	if (pitch < -50)
	    return Direction.UP;
	if (pitch > 50)
	    return Direction.DOWN;
	if (facing == 1) // east
	    return Direction.MINUSX;
	if (facing == 3) // west
	    return Direction.PLUSX;
	if (facing == 2) // north
	    return Direction.MINUSZ;
	if (facing == 0) // south
	    return Direction.PLUSZ;
	return null;
    }

    public void regenerate(CuboidArea area) {
    }

    public void onDisable() {
	for (Entry<UUID, Visualizer> one : vMap.entrySet()) {
	    one.getValue().cancelAll();
	}
	vMap.clear();
    }
}
