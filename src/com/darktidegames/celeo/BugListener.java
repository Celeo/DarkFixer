package com.darktidegames.celeo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.managers.RegionManager;

/**
 * Listeners and code for fixing Minecraft and Bukkit bugs, errors, and glitches
 * that we don't want to persist on our server.
 * 
 * @author Celeo
 */
public class BugListener implements Listener
{

	private final FixerCore plugin;

	/**
	 * Constructor
	 * 
	 * @param plugin
	 *            FixerCore
	 */
	public BugListener(FixerCore plugin)
	{
		this.plugin = plugin;
	}

	/**
	 * Extra barrier against villagers spawning
	 * 
	 * @param event
	 *            CreatureSpawnEvent
	 * @author Celeo (DarkTape)
	 */
	@EventHandler
	public static void onVillagerSpawn(CreatureSpawnEvent event)
	{
		if (event.isCancelled())
			return;
		Entity entity = event.getEntity();
		if (entity.getType().equals(EntityType.VILLAGER)
				|| entity instanceof Villager)
			event.setCancelled(true);
	}

	/**
	 * Disable wolf breeding and spawning
	 * 
	 * @param event
	 *            CreatureSpawnEvent
	 * @author Celeo (DarkTape)
	 */
	@EventHandler
	public void onWolfBreed(CreatureSpawnEvent event)
	{
		if (event.isCancelled())
			return;
		Entity entity = event.getEntity();
		if ((entity.getType().equals(EntityType.WOLF) || entity instanceof Wolf))
			// && event.getSpawnReason().equals(SpawnReason.BREEDING))
			event.setCancelled(true);
	}

	/**
	 * Warps players back to DTC if they got past the correct border<br>
	 * <br>
	 * Displacement check of 2010 to let BorderGuard do it's thing
	 * 
	 * @param event
	 *            PlayerMoveEvent event
	 * @author Celeo (DarkTape)
	 */
	@EventHandler
	public static void onPlayerMove(PlayerMoveEvent event)
	{
		if (event.isCancelled())
			return;
		Player player = event.getPlayer();
		Location location = player.getLocation();
		double x = location.getX();
		double z = location.getZ();
		if (x > 2010 || x < -2010 || z > 2010 || z < -2010)
		{
			player.sendMessage("§cYou're not supposed to be there.");
			player.teleport(new Location(player.getWorld(), -7, 70, -2));
		}
	}

	/**
	 * If a player is joining the server that is not a mod, for whatever reason,
	 * put them back into survival mode, just in case.
	 * 
	 * @param event
	 *            PlayerJoinEvent
	 * @author Celeo (DarkTape)
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if (player.getGameMode().equals(GameMode.CREATIVE))
			if (!player.hasPermission("sudo.mod"))
				player.setGameMode(GameMode.SURVIVAL);
	}

	public FixerCore getPlugin()
	{
		return plugin;
	}

	/**
	 * Extra block for WorldGuard - if a player is trying to put out a fire
	 * where they cannot build, and if they are not a mod, stop them
	 * 
	 * @param event
	 *            PlayerInteractEvent
	 * @author Celeo (DarkTape)
	 */
	// @EventHandler
	// public void onPlayerTryToExtinguishFire(PlayerInteractEvent event)
	// {
	// Player player = event.getPlayer();
	// if (event.getBlockFace().equals(BlockFace.UP))
	// if
	// (event.getClickedBlock().getRelative(BlockFace.UP).getType().equals(Material.FIRE))
	// if (!plugin.canBuildFactions(player,
	// event.getClickedBlock().getLocation()))
	// event.setCancelled(!player.hasPermission("sudo.mod"));
	// }

	/**
	 * Do not let players set their bed where they cannot build
	 * 
	 * @param event
	 *            PlayerInteractEvent
	 * @author Celeo (DarkTape)
	 */
	// @EventHandler
	// public void playerTryingToSetBedWhereTheyCannotBuild(PlayerInteractEvent
	// event)
	// {
	// Player player = event.getPlayer();
	// if (!plugin.canBuildFactions(player,
	// event.getClickedBlock().getLocation()))
	// {
	// if (event.getClickedBlock().getType().equals(Material.BED))
	// {
	// player.sendMessage("§cYou cannot set your bed there as you cannot build at that location");
	// event.setCancelled(true);
	// }
	// }
	// }

	@EventHandler
	public void onBoatPlace(PlayerInteractEvent event)
	{
		if (event.isCancelled())
			return;
		Player player = event.getPlayer();
		if (player == null)
			return;
		if (event.isCancelled())
			return;
		if (player.getItemInHand().getType().equals(Material.BOAT))
		{
			if (event.getAction().equals(Action.RIGHT_CLICK_AIR)
					|| event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			{
				List<Block> inSight = new ArrayList<Block>(player.getLineOfSight(null, 4));
				boolean inWater = false;
				for (Block b : inSight)
				{
					if (b.getTypeId() == 9 || b.getTypeId() == 10)
					{
						inWater = true;
						break;
					}
				}
				if (!inWater)
				{
					event.setCancelled(true);
					player.sendMessage("§cCan only place boats in water.");
				}
			}
		}
	}

	@EventHandler
	public void onGolemBreed(CreatureSpawnEvent event)
	{
		if (event.isCancelled())
			return;
		Entity e = event.getEntity();
		if (e.getType().equals(EntityType.IRON_GOLEM) || e instanceof Golem
				&& event.getSpawnReason().equals(SpawnReason.BREEDING))
			event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerUseBucket(PlayerInteractEvent event)
	{
		if (event.isCancelled())
			return;
		Player player = event.getPlayer();
		if (player.getItemInHand().getType().equals(Material.WATER_BUCKET)
				|| player.getItemInHand().getType().equals(Material.LAVA_BUCKET))
		{
			if (event.getAction().equals(Action.LEFT_CLICK_AIR)
					|| event.getAction().equals(Action.LEFT_CLICK_BLOCK))
				return;
			if (event.getClickedBlock() == null)
			{
				event.setCancelled(true);
				return;
			}
			RegionManager rm = plugin.wg.getRegionManager(player.getWorld());
			List<String> regions = rm.getApplicableRegionsIDs(BukkitUtil.toVector(event.getClickedBlock()));
			if (regions == null)
			{
				event.setCancelled(true);
				return;
			}
			for (Block block : player.getLineOfSight(null, 5))
				if (block.getTypeId() == 10 || block.getTypeId() == 11)
					block.setTypeId(0);
			if (regions.isEmpty())
				event.setCancelled(true);
			for (String key : regions)
				if (rm.getRegion(key).isMember(player.getName()))
					return;
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents water from flowing into a location with different regions
	 * 
	 * @param event
	 *            BlockFromToEvent
	 */
	@EventHandler
	public void waterFlowingIntoDifferentRegions(BlockFromToEvent event)
	{
		if (event.isCancelled())
			return;
		Block to = event.getToBlock();
		Block from = event.getBlock();
		Set<String> toRegions = new HashSet<String>(plugin.wg.getRegionManager(to.getWorld()).getApplicableRegionsIDs(BukkitUtil.toVector(to)));
		Set<String> fromRegions = new HashSet<String>(plugin.wg.getRegionManager(to.getWorld()).getApplicableRegionsIDs(BukkitUtil.toVector(from)));
		event.setCancelled(!toRegions.equals(fromRegions));
	}

	@EventHandler
	public void onSandGravelPlace(BlockPlaceEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (event.isCancelled())
			return;
		if (block.getTypeId() != 12 && block.getTypeId() != 13)
			return;
		if (!canBuildBelow(player, block))
		{
			Location finLoc = getFinalResting(block);
			finLoc.getBlock().getWorld().dropItemNaturally(finLoc, new ItemStack(block.getType()));
			event.setCancelled(true);
			player.getInventory().removeItem(new ItemStack(block.getTypeId(), 1));
		}
	}

	public Location getFinalResting(Block block)
	{
		int t = block.getTypeId();
		if (t == 8 || t == 9 || t == 10 || t == 11)
			return null;
		if (t != 12 && t != 13)
			return block.getLocation();
		World world = block.getWorld();
		int x = (int) block.getLocation().getX();
		int z = (int) block.getLocation().getZ();
		Location testLoc = null;
		for (int i = (int) block.getLocation().getY() - 1; i > -240; i--)
		{
			testLoc = new Location(world, x, i, z);
			if (testLoc.getBlock().getRelative(BlockFace.DOWN).getTypeId() != 0)
				return testLoc;
		}
		return null;
	}

	/**
	 * 
	 * @param player
	 *            Player
	 * @param block
	 *            Block
	 * @return True if the player can build in any regions below the block
	 */
	public boolean canBuildBelow(Player player, Block block)
	{
		Location start = block.getLocation();
		World world = block.getWorld();
		int x = (int) start.getX();
		int z = (int) start.getZ();
		Block testBlock = null;
		Location testLoc = null;
		for (int i = (int) start.getY() - 1; i > -240; i--)
		{
			testLoc = new Location(world, x, i, z);
			testBlock = testLoc.getBlock();
			if (testBlock.getTypeId() != 0)
				return true;
			if (!plugin.wg.canBuild(player, testLoc))
				return false;
		}
		return true;
	}

	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event)
	{
		if (event.isCancelled())
			return;
		Set<String> movingRegions, pistonRegions;
		pistonRegions = getRegionSet(event.getBlock());
		for (int i = 0; i <= event.getLength(); i++)
		{
			Block block = event.getBlock().getRelative(event.getDirection(), i + 1);
			movingRegions = getRegionSet(block);
			if (!movingRegions.isEmpty()
					&& !pistonRegions.equals(movingRegions))
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent event)
	{
		if (event.isCancelled())
			return;
		Set<String> movingRegions, pistonRegions;
		pistonRegions = getRegionSet(event.getBlock());
		for (int i = 0; i <= 1; i++)
		{
			Block block = event.getBlock().getRelative(event.getDirection(), i + 1);
			movingRegions = getRegionSet(block);
			if (!movingRegions.isEmpty()
					&& !pistonRegions.equals(movingRegions))
				event.setCancelled(true);
		}
	}

	private Set<String> getRegionSet(Block block)
	{
		RegionManager regionManager = plugin.wg.getGlobalRegionManager().get(block.getWorld());
		return new HashSet<String>(regionManager.getApplicableRegionsIDs(BukkitUtil.toVector(block)));
	}

	@EventHandler
	public void onTntcartPlace(BlockPlaceEvent event)
	{
		if (event.getPlayer().getItemInHand().getType().equals(Material.EXPLOSIVE_MINECART))
		{
			event.setCancelled(true);
			event.getPlayer().sendMessage("§cBLOCK_PLACE: Explosive minecarts are temporarily disabled");
		}
	}

	@EventHandler
	public void onTntcartInteract(PlayerInteractEvent event)
	{
		if (event.getPlayer().getItemInHand().getType().equals(Material.EXPLOSIVE_MINECART))
		{
			event.setCancelled(true);
			event.getPlayer().sendMessage("§cINTERACT: Explosive minecarts are temporarily disabled");
		}
	}

	@EventHandler
	public void onTntcartMake(CraftItemEvent event)
	{
		if (event.getCurrentItem().getType().equals(Material.EXPLOSIVE_MINECART))
		{
			event.setCancelled(true);
			if (event.getWhoClicked() instanceof Player)
				((Player) event.getWhoClicked()).sendMessage("§cMAKE: Explosive minecarts are temporarily disabled");
		}
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getBlock();
		String mech = event.getLine(1);
		if (!mech.startsWith("["))
			return;
		if (mech.equalsIgnoreCase("[door]") || mech.equalsIgnoreCase("[gate]")
				|| mech.equalsIgnoreCase("[bridge]"))
		{
			if (getRegionSet(block).isEmpty())
			{
				if (!player.hasPermission("fixer.mech.anywhere"))
				{
					event.setCancelled(true);
					player.sendMessage("§cYou cannot place Craftbook mechanics outside of regions");
				}
			}
		}
	}

	@EventHandler
	public void onSpleefSnowBreak(ItemSpawnEvent event)
	{
		ItemStack i = event.getEntity().getItemStack();
		if (i.getType().equals(Material.SNOW_BALL))
			for (String region : getRegionSet(event.getEntity().getLocation().getBlock()))
				if (region.startsWith("spleef"))
				{
					event.setCancelled(true);
					return;
				}
	}

	@EventHandler
	public void onPlayerClickInventory(InventoryClickEvent event)
	{
		Player player = (Player) event.getWhoClicked();
		if (plugin.wg.canBuild(player, player.getLocation().getBlock()))
			return;
		String view = event.getView().getTitle();
		if (view.equals("container.dispenser")
				|| view.equals("container.dropper"))
		{
			boolean deny = false;
			if (event.getCursor() != null)
				if (event.getCursor().getTypeId() == 326
						|| event.getCursor().getTypeId() == 327)
					deny = true;
			if (event.getCurrentItem() != null)
				if (event.getCurrentItem().getTypeId() == 326
						|| event.getCurrentItem().getTypeId() == 327)
					deny = true;
			if (deny)
			{
				player.sendMessage("§cYou cannot place water buckets nor lava buckets in a dispenser/dropper where you cannot build");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onCreatureSpawnFromSpawner(CreatureSpawnEvent event)
	{
		if (event.getSpawnReason().equals(SpawnReason.SPAWNER))
			event.setCancelled(true);
	}

}