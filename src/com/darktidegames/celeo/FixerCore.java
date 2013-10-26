package com.darktidegames.celeo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;
import de.bananaco.bpermissions.api.Permission;

/**
 * <u>Known bugs</u>:<br>
 * 
 * @author Celeo
 */
public class FixerCore extends JavaPlugin implements Listener
{

	// private DarkVanish darkVanish = null;
	private DarkJustice darkJustice = null;
	private Essentials ess = null;
	public WorldGuardPlugin wg = null;

	private List<String> hiddenMessages = new ArrayList<String>();
	private Map<String, String> pendingMessages = new HashMap<String, String>();
	private Map<Player, SignChange> signChanges = new HashMap<Player, SignChange>();
	private List<String> blockedCommands = new ArrayList<String>();
	private List<Player> watching = new ArrayList<Player>();
	private List<String> oppers = new ArrayList<String>();

	private List<String> arenasGM = new ArrayList<String>();

	@Override
	public void onEnable()
	{
		getDataFolder().mkdirs();
		if (!new File(getDataFolder(), "config.yml").exists())
			saveDefaultConfig();
		load();
		if (hiddenMessages == null)
			hiddenMessages = new ArrayList<String>();

		// setupVanish();
		setupJustice();
		setupEssentials();
		setupWorldGuard();
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(new BugListener(this), this);
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{
			@Override
			public void run()
			{
				Iterator<Entity> i = getServer().getWorld("world").getEntities().iterator();
				while (i.hasNext())
				{
					Entity e = i.next();
					if (e instanceof Wolf)
						i.remove();
				}
			}
		}, 100L);
		getLogger().info("Enabled");
	}

	private void load()
	{
		reloadConfig();
		hiddenMessages = getConfig().getStringList("hiddenMessages");
		if (hiddenMessages == null)
			hiddenMessages = new ArrayList<String>();
		for (String key : getConfig().getStringList("pendingMessages"))
		{
			pendingMessages.put(key.split("->")[0], key.split("->")[0]);
		}
		blockedCommands = getConfig().getStringList("blockedCommands");
		if (blockedCommands == null)
			blockedCommands = new ArrayList<String>();
		oppers = getConfig().getStringList("oppers");
		if (oppers == null)
			oppers = new ArrayList<String>();
	}

	@Override
	public void onDisable()
	{
		save();
		getServer().getScheduler().cancelTasks(this);
		getLogger().info("Disabling ...");
	}

	private void save()
	{
		getConfig().set("hiddenMessages", hiddenMessages);
		List<String> toSave = new ArrayList<String>();
		for (String key : pendingMessages.keySet())
			toSave.add(key + "->" + pendingMessages.get(key));
		getConfig().set("pendingMessages", toSave);
		getConfig().set("blockedCommands", blockedCommands);
		getConfig().set("oppers", oppers);
		saveConfig();
	}

	// private void setupVanish()
	// {
	// Plugin test = getServer().getPluginManager().getPlugin("DarkVanish");
	// if (test == null)
	// getLogger().info("Could not find DarkVanish");
	// else
	// {
	// if (!test.isEnabled())
	// getServer().getPluginManager().enablePlugin(test);
	// darkVanish = (DarkVanish) test;
	// }
	// }

	private void setupJustice()
	{
		Plugin test = getServer().getPluginManager().getPlugin("DarkJustice");
		if (test == null)
			getLogger().info("Could not find DarkJustice");
		else
		{
			if (!test.isEnabled())
				getServer().getPluginManager().enablePlugin(test);
			darkJustice = (DarkJustice) test;
		}
	}

	private void setupEssentials()
	{
		Plugin test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null)
			getLogger().info("Could not find Essentials");
		else
		{
			if (!test.isEnabled())
				getServer().getPluginManager().enablePlugin(test);
			ess = (Essentials) test;
		}
	}

	private void setupWorldGuard()
	{
		Plugin test = getServer().getPluginManager().getPlugin("WorldGuard");
		if (test == null)
			getLogger().info("Could not find WorldGuard");
		else
		{
			if (!test.isEnabled())
				getServer().getPluginManager().enablePlugin(test);
			wg = (WorldGuardPlugin) test;
		}
	}

	@SuppressWarnings("static-method")
	public boolean hasPerms(Player player, String node)
	{
		if (node.equals("fixer.mod"))
		{
			if (!player.hasPermission("fixer.mod")
					&& !player.hasPermission("fixer.admin"))
			{
				player.sendMessage("§cYou cannot do that");
				return false;
			}
		}
		else if (node.equals("fixer.admin"))
		{
			if (!player.hasPermission("fixer.admin"))
			{
				player.sendMessage("§cYou cannot do that");
				return false;
			}
		}
		else
		{
			if (!player.hasPermission(node))
			{
				player.sendMessage("§cYou cannot do that");
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player))
			return false;
		Player player = (Player) sender;
		if (label.equalsIgnoreCase("fixer"))
		{
			if (args != null && args.length == 1
					&& args[0].equalsIgnoreCase("reload"))
			{
				if (!hasPerms(player, "fixer.admin"))
					return true;
				load();
				player.sendMessage("§aReloded from configuration");
				return true;
			}
			else if (args != null && args.length == 1
					&& args[0].equalsIgnoreCase("save"))
			{
				if (!hasPerms(player, "fixer.admin"))
					return true;
				save();
				player.sendMessage("§aSaved to configuration");
				return true;
			}
			if (!hasPerms(player, "fixer.admin"))
				return true;
			String world = "world";
			String name = null;
			CalculableType type = CalculableType.USER;
			String messageOut = "+===========================================+\n+		Starting audit of permission groups		+\n+===========================================+";
			getLogger().info(ChatColor.stripColor(messageOut));
			for (OfflinePlayer offlinePlayer : getServer().getOfflinePlayers())
			{
				name = offlinePlayer.getName();
				if (ApiLayer.hasGroup(world, type, name, "nomad"))
				{
					ApiLayer.removeGroup(world, type, name, "nomad");
					ApiLayer.addGroup(world, type, name, "citizen");
				}
				if (ApiLayer.hasGroup(world, type, name, "moderator"))
					ApiLayer.removeGroup(world, type, name, "moderator");
				if (ApiLayer.hasGroup(world, type, name, "admin"))
					ApiLayer.removeGroup(world, type, name, "admin");
			}
			ApiLayer.update();
			getLogger().info("+===========================================+");
			player.sendMessage("§aDone");
			return true;
		}
		if (label.equalsIgnoreCase("ops"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			String names = "";
			for (OfflinePlayer o : getServer().getOperators())
			{
				if (names.equals(""))
					names = o.getName();
				else
					names += ", " + o.getName();
			}
			player.sendMessage("§eOps: §9" + names);
			return true;
		}
		if (label.equalsIgnoreCase("prereload"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args != null && args.length == 1 && args[0].equals("-f"))
			{
				getServer().broadcastMessage("§8[§4Notice§8] §7The plugins are being reloaded.");
				getServer().dispatchCommand(getServer().getConsoleSender(), "reload");
				getServer().broadcastMessage("§8[§4Notice§8] §7The plugins were reloaded.");
				return true;
			}
			getServer().broadcastMessage("§8[§4Notice§8] §7The plugins will be reloaded in §415 §7seconds.");
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
			{
				@Override
				public void run()
				{
					getServer().broadcastMessage("§8[§4Notice§8] §7The plugins are being reloaded.");
					getServer().dispatchCommand(getServer().getConsoleSender(), "reload");
					getServer().broadcastMessage("§8[§4Notice§8] §7The plugins were reloaded.");
				}
			}, 300L);
			return true;
		}
		if (label.equalsIgnoreCase("notice"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (args == null || args.length < 1)
			{
				player.sendMessage("§c/notice [message]");
				return true;
			}
			String message = "§8[§4Notice§8] §7";
			for (int i = 0; i < args.length; i++)
			{
				message += args[i] + " ";
			}
			message = message.replaceAll("&", "§");
			player.sendMessage("§7Sending message: " + message);
			getServer().broadcastMessage(message);
			return true;
		}
		if (label.equalsIgnoreCase("listplugins"))
		{
			String plugins = "";
			Plugin[] allPlugins = getServer().getPluginManager().getPlugins();
			for (Plugin p : allPlugins)
			{
				if (plugins.equals(""))
					plugins = (p.isEnabled() ? "§a" : "§c") + p.getName();
				else
					plugins += " " + (p.isEnabled() ? "§a" : "§c")
							+ p.getName();
			}
			player.sendMessage("§fPlugins (" + allPlugins.length + "): "
					+ plugins);
			return true;
		}
		if (label.equalsIgnoreCase("listbans"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (getServer().getBannedPlayers() == null
					|| getServer().getBannedPlayers().isEmpty())
			{
				player.sendMessage("§cNo banned players found");
				return true;
			}
			String bans = "";
			Set<OfflinePlayer> bannedPlayers = getServer().getBannedPlayers();
			for (OfflinePlayer banned : bannedPlayers)
			{
				if (bans.equals(""))
					bans = banned.getName();
				else
					bans += ", " + banned.getName();
			}
			player.sendMessage("§eBans on the server (" + bannedPlayers.size()
					+ "): §c" + bans);
			return true;
		}
		if (label.equalsIgnoreCase("listipbans"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (getServer().getIPBans() == null
					|| getServer().getIPBans().isEmpty())
			{
				player.sendMessage("§cNo ip bans found");
				return true;
			}
			String bans = "";
			Set<String> ipBans = getServer().getIPBans();
			for (String ban : ipBans)
			{
				if (bans.equals(""))
					bans = ban;
				else
					bans += ", " + ban;
			}
			player.sendMessage("§eBans on the server (" + ipBans.size()
					+ "): §c" + bans);
			return true;
		}
		if (label.equalsIgnoreCase("getgroups"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/getgroups playername");
				return true;
			}
			String groups = "";
			String[] allGroups = ApiLayer.getGroups(player.getWorld().getName(), CalculableType.USER, args[0]);
			for (String group : allGroups)
			{
				if (groups.equals(""))
					groups = group;
				else
					groups += ", " + group;
			}
			player.sendMessage("§eGroups for §9" + args[0] + "§e: ("
					+ allGroups.length + ") §a" + groups);
			return true;
		}
		if (label.equalsIgnoreCase("disable"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/disable [plugin]");
				return true;
			}
			Plugin test = getServer().getPluginManager().getPlugin(args[0]);
			if (test == null)
				player.sendMessage("§cPlugin " + args[0] + " not found");
			else if (!test.isEnabled())
				player.sendMessage("§cPlugin " + args[0]
						+ " is already disabled");
			else
			{
				getServer().getPluginManager().disablePlugin(test);
				player.sendMessage("§aDisabled plugin " + args[0]);
			}
			return true;
		}
		if (label.equalsIgnoreCase("enable"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/enable [plugin]");
				return true;
			}
			Plugin test = getServer().getPluginManager().getPlugin(args[0]);
			if (test == null)
				player.sendMessage("§cPlugin " + args[0] + " not found");
			else if (test.isEnabled())
				player.sendMessage("§cPlugin " + args[0]
						+ " is already enabled");
			else
			{
				getServer().getPluginManager().enablePlugin(test);
				player.sendMessage("§aEnabled plugin " + args[0]);
			}
			return true;
		}
		if (label.equalsIgnoreCase("inside"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/inside [distance]");
				return true;
			}
			if (!isInt(args[0]))
			{
				player.sendMessage("§c/inside [distance]");
				return true;
			}
			List<Player> inside = getPlayersInside(player.getLocation(), Integer.valueOf(args[0]).intValue());
			String names = "";
			for (Player p : inside)
			{
				if (names.equals(""))
					names = p.getName();
				else
					names += " " + p.getName();
			}
			player.sendMessage("§ePlayers within distance §9" + args[0]
					+ "§e: §a" + names);
			return true;
		}
		if (label.equalsIgnoreCase("aoekick"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/aoekick [distance]");
				return true;
			}
			if (!isInt(args[0]))
			{
				player.sendMessage("§c/aoekick [distance]");
				return true;
			}
			List<Player> toKick = getPlayersInside(player.getLocation(), Integer.valueOf(args[0]).intValue());
			if (toKick == null || toKick.isEmpty())
			{
				player.sendMessage("§cThere is no one to kick");
				return true;
			}
			toKick.remove(player);
			for (Player p : toKick)
			{
				if (!p.hasPermission("fixer.mod"))
					p.kickPlayer("You've been kicked from DarkTide");
				else
					player.sendMessage("§9" + p.getName() + " §esaved from aoe");
			}
			player.sendMessage("§aDone");
			return true;
		}
		if (label.equalsIgnoreCase("aoeban"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/aoeban [distance]");
				return true;
			}
			if (!isInt(args[0]))
			{
				player.sendMessage("§c/aoeban [distance]");
				return true;
			}
			List<Player> toBan = getPlayersInside(player.getLocation(), Integer.valueOf(args[0]).intValue());
			if (toBan == null || toBan.isEmpty())
			{
				player.sendMessage("§cThere is no one to ban");
				return true;
			}
			toBan.remove(player);
			for (Player p : toBan)
			{
				if (!p.hasPermission("fixer.mod"))
				{
					p.kickPlayer("You've been banned from DarkTide");
					p.setBanned(true);
				}
				else
					player.sendMessage("§9" + p.getName() + " §esaved from aoe");
			}
			player.sendMessage("§aDone");
			return true;
		}
		if (label.equalsIgnoreCase("aoetempban"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 2)
			{
				player.sendMessage("§c/aoetempban [distance] [length]");
				return true;
			}
			if (!isInt(args[0]))
			{
				player.sendMessage("§c/aoetempban [distance] [length]");
				return true;
			}
			List<Player> toBan = getPlayersInside(player.getLocation(), Integer.valueOf(args[0]).intValue());
			if (toBan == null || toBan.isEmpty())
			{
				player.sendMessage("§cThere is no one to ban");
				return true;
			}
			toBan.remove(player);
			for (Player p : toBan)
			{
				if (!p.hasPermission("fixer.mod"))
					player.chat("/tempban " + p.getName() + " " + args[1]);
				else
					player.sendMessage("§9" + p.getName() + " §esaved from aoe");
			}
			player.sendMessage("§aDone");
			return true;
		}
		if (label.equalsIgnoreCase("hurt"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 2)
			{
				player.sendMessage("§c/hurt [player] [amount]");
				return true;
			}
			Player toHurt = getServer().getPlayer(args[0]);
			if (toHurt == null || !toHurt.isOnline())
			{
				player.sendMessage("§cThat player is not online");
				return true;
			}
			if (!isInt(args[1]))
			{
				player.sendMessage("§c/hurt [player] [amount (number)]");
				return true;
			}
			int damage = Integer.valueOf(args[1]).intValue();
			int health = toHurt.getHealth();
			try
			{
				toHurt.setHealth(health - damage);
				player.sendMessage("§aHurt " + args[0] + " for " + args[1]);
			}
			catch (IllegalArgumentException e)
			{
				player.sendMessage("§cThat would hurt them for more than their health");
			}
			return true;
		}
		if (label.equalsIgnoreCase("readout"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/readout [player]");
				return true;
			}
			postReadout(player, args[0]);
			return true;
		}
		// if (label.equalsIgnoreCase("devanish"))
		// {
		// if (!hasPerms(player, "fixer.mod"))
		// return true;
		// if (args == null || args.length != 1)
		// {
		// player.sendMessage("§c/devanish [player]");
		// return true;
		// }
		// Player toToggle = getServer().getPlayer(args[0]);
		// if (toToggle == null || !toToggle.isOnline())
		// {
		// player.sendMessage("§cThat player is not online");
		// return true;
		// }
		// player.sendMessage("§cThat player is not shown to be vanished, working out kinks now");
		// for (Player online : getServer().getOnlinePlayers())
		// {
		// if (!online.canSee(toToggle))
		// {
		// online.showPlayer(toToggle);
		// toToggle.sendMessage("§7You are now revealed to "
		// + online.getName() + ", who could not see you");
		// }
		// }
		// player.sendMessage("§a" + toToggle.getName() + " shown to everyone");
		// return true;
		// }
		if (label.equalsIgnoreCase("hiddenMessages"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (args == null
					|| args.length == 0
					|| (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off")))
			{
				setHiddenMessages(player, !shouldHideMessages(player));
			}
			else
				setHiddenMessages(player, args[0].equalsIgnoreCase("on") ? true : false);
			return true;
		}
		if (label.equalsIgnoreCase("getplayer"))
		{
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/getplayer [name]");
				return true;
			}
			Player get = getServer().getPlayer(args[0]);
			if (get != null)
			{
				player.sendMessage("§7Player is §4" + get.getName());
				return true;
			}
			for (OfflinePlayer offline : getServer().getOfflinePlayers())
			{
				if (offline.getName().equalsIgnoreCase(args[0]))
					player.sendMessage("§7Player is §4" + offline.getName());
				else if (offline.getName().toLowerCase().startsWith(args[0].toLowerCase()))
					player.sendMessage("§7Player is §4" + offline.getName());
			}
			return true;
		}
		if (label.equalsIgnoreCase("leavemessage"))
		{
			if (args == null || args.length < 2)
			{
				player.sendMessage("§c/leavemessage [player] [message]");
				return true;
			}
			if (!hasPerms(player, "fixer.admin"))
				return true;
			String message = "";
			for (int i = 1; i < args.length; i++)
			{
				if (message.equals(""))
					message = args[i];
				else
					message += " " + args[i];
			}
			pendingMessages.put(args[0], message);
			player.sendMessage("§aMessage sent");
			return true;
		}
		if (label.equalsIgnoreCase("removemobs"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			int count = 0;
			for (Entity e : player.getWorld().getEntities())
			{
				if (!(e instanceof Player))
				{
					e.remove();
					count++;
				}
			}
			player.sendMessage("§dRemoved " + count + " entities");
			return true;
		}
		if (label.equalsIgnoreCase("auth"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/auth [player]");
				return true;
			}
			if (ApiLayer.hasGroup("world", CalculableType.USER, args[0], "beta"))
			{
				ApiLayer.removeGroup("world", CalculableType.USER, args[0], "beta");
				player.sendMessage("§cDeauthed §9" + args[0]);
			}
			else
			{
				ApiLayer.addGroup("world", CalculableType.USER, args[0], "beta");
				player.sendMessage("§aAuthed §9" + args[0]);
			}
			ApiLayer.update();
			return true;
		}
		if (label.equalsIgnoreCase("listmembers"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/listmembers [groupName]");
				return true;
			}
			String ret = "";
			for (OfflinePlayer offline : getServer().getOfflinePlayers())
			{
				if (ApiLayer.hasGroup("world", CalculableType.USER, offline.getName(), args[0]))
				{
					if (ret.equals(""))
						ret = offline.getName();
					else
						ret += ", " + offline.getName();
				}
			}
			player.sendMessage("§7Currently in group §6" + args[0] + "§7: ");
			player.sendMessage("§e" + ret);
			return true;
		}
		if (label.equalsIgnoreCase("sc"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (args == null || args.length < 2)
			{
				player.sendMessage("§c/sc line message...");
				return true;
			}
			String message = "";
			int line = -1;
			if (!isInt(args[0]))
			{
				player.sendMessage("§cThe line must be a number");
				return true;
			}
			line = Integer.valueOf(args[0]).intValue();
			if (line > 3 || line < 0)
			{
				player.sendMessage("§cThe line must be between 0 and 3");
				return true;
			}
			for (int i = 1; i < args.length; i++)
			{
				if (message.equals(""))
					message = args[i];
				else
					message += " " + args[i];
			}
			message = message.replace("&", "§");
			setSignChange(player, line, message);
			player.sendMessage("§ePlease hit the sign you wish to change");
			return true;
		}
		if (label.equalsIgnoreCase("blockcmd"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length == 0)
			{
				player.sendMessage("§c/blockcmd [full command to block]");
				return true;
			}
			String cmd = "";
			for (String s : args)
			{
				if (cmd.equals(""))
					cmd = s;
				else
					cmd += " " + s;
			}
			if (blockedCommands.contains(cmd))
				blockedCommands.remove(cmd);
			else
				blockedCommands.add(cmd);
			player.sendMessage("§7Command '§4" + cmd + "§7' is "
					+ (blockedCommands.contains(cmd) ? "" : "not ") + "blocked");
			return true;
		}
		if (label.equals("watchcmds"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (watching.contains(player))
				watching.remove(player);
			else
				watching.add(player);
			player.sendMessage("§7You are "
					+ (watching.contains(player) ? "" : "not ")
					+ "watching commands");
			return true;
		}
		if (label.equalsIgnoreCase("opme"))
		{
			if (oppers.contains(player.getName()))
			{
				player.setOp(true);
				player.sendMessage("§aYou are now a server op");
			}
			else
				player.sendMessage("§cWhat are you doing?");
			return true;
		}
		if (label.equalsIgnoreCase("dis"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (args == null || args.length < 1)
			{
				player.sendMessage("§c/dis [message]");
				return true;
			}
			String message = "";
			for (int i = 0; i < args.length; i++)
			{
				message += args[i] + " ";
			}
			message = message.replaceAll("&", "§");
			getServer().broadcastMessage(message);
			return true;
		}
		if (label.equalsIgnoreCase("wwho"))
		{
			if (!hasPerms(player, "fixer.mod"))
				return true;
			if (args == null || args.length != 1)
			{
				player.sendMessage("§c/wwho [world]");
				return true;
			}
			World world = getServer().getWorld(args[0]);
			if (world == null)
			{
				player.sendMessage("§cNo world with that name found.");
				return true;
			}
			String names = "";
			for (Player in : world.getPlayers())
			{
				if (names.equals(""))
					names = in.getName();
				else
					names += ", " + in.getName();
			}
			player.sendMessage("§7Names in world §6" + world.getName()
					+ "§7: §6" + names);
			return true;
		}
		if (label.equalsIgnoreCase("hurt"))
		{
			if (!hasPerms(player, "fixer.admin"))
				return true;
			if (args == null || args.length < 1)
			{
				return false;
			}
			Player hurt = getServer().getPlayer(args[0]);
			if (hurt == null)
				return false;
			int damage = 1;
			if (args.length == 2)
			{
				try
				{
					damage = Integer.valueOf(args[1]).intValue();
				}
				catch (Exception e)
				{}
			}
			hurt.damage(damage);
			player.sendMessage("§aDone - player now " + hurt.getHealth());
			return true;
		}
		if (label.equalsIgnoreCase("greylist"))
		{
			if (!hasPerms(player, "fixer.greylist"))
				return true;
			if (args == null || args.length != 1)
				return false;
			String world = "world";
			CalculableType type = CalculableType.USER;
			if (ApiLayer.hasGroup(world, type, args[0], "nomad"))
			{
				ApiLayer.removeGroup(world, type, args[0], "nomad");
				ApiLayer.addGroup(world, type, args[0], "citizen");
				player.sendMessage("§aGreylisted " + args[0]);
				sendMessage(args[0], "§aYou are now a citizen!");
			}
			else
				player.sendMessage("§cThat player is not a nomad.");
			return true;
		}
		if (label.equalsIgnoreCase("arenasgm"))
		{
			if (!hasPerms(player, "fixer.arenasgm"))
				return true;
			toggleArenasGM(player);
			return true;
		}
		return false;
	}

	private void toggleArenasGM(Player player)
	{
		if (arenasGM.contains(player.getName()))
		{
			arenasGM.remove(player.getName());
			player.getInventory().clear();
			player.getInventory().setArmorContents(new ItemStack[4]);
			player.getActivePotionEffects().clear();
			player.setGameMode(GameMode.SURVIVAL);
			getServer().dispatchCommand(getServer().getConsoleSender(), "warp arenas_back "
					+ player.getName());
			player.sendMessage("§7Returning to world");
			getLogger().info(player.getName()
					+ " has used the arenasgm command and successfully returned to the main world");
		}
		else
		{
			for (ItemStack i : player.getInventory().getContents())
				if (i != null)
				{
					player.sendMessage("§7You cannot have anything in your inventory to warp to the arenas world like this");
					return;
				}
			boolean loc = false;
			for (ProtectedRegion rg : wg.getRegionManager(getServer().getWorld("world")).getApplicableRegions(player.getLocation()))
				if (rg.getId().equalsIgnoreCase("arenas_warp_to"))
					loc = true;
			if (!loc)
			{
				player.sendMessage("§7You are not in the correct location.");
				return;
			}
			arenasGM.add(player.getName());
			player.setGameMode(GameMode.CREATIVE);
			getServer().dispatchCommand(getServer().getConsoleSender(), "warp arenas_to "
					+ player.getName());
			player.sendMessage("§7Going to arenas world");
			getLogger().info(player.getName()
					+ " has used the areansgm command and successfully teleported to the arenas world");
		}
	}

	public void sendMessage(String playerName, String message)
	{
		Player test = getServer().getPlayer(playerName);
		if (test == null || !test.isOnline())
			return;
		test.sendMessage(message);
	}

	/**
	 * 
	 * @param string
	 *            String
	 * @return True if passed string can be cast to an int
	 */
	public static boolean isInt(String string)
	{
		try
		{
			Integer.valueOf(string);
			return true;
		}
		catch (NumberFormatException e)
		{}
		return false;
	}

	private void setSignChange(Player player, int line, String message)
	{
		signChanges.put(player, new SignChange(player, line, message));
	}

	private void removeSignChange(Player player)
	{
		signChanges.remove(player);
	}

	private SignChange getSignChange(Player player)
	{
		return signChanges.get(player);
	}

	@SuppressWarnings("boxing")
	public List<Player> getPlayersInside(Location origin, int distance)
	{
		List<Player> ret = new ArrayList<Player>();
		Location loc = null;
		Double x = null;
		Double y = null;
		Double z = null;
		for (Player player : getServer().getOnlinePlayers())
		{
			loc = player.getLocation();
			x = loc.getX() - origin.getX();
			y = loc.getY() - origin.getY();
			z = loc.getZ() - origin.getZ();
			if ((x * x) + (y * y) + (z * z) <= distance)
				ret.add(player);
		}
		return ret;
	}

	public void postReadout(Player sender, String name)
	{
		String info = "<h2>Permissions</h2>";
		info += "Permissions in green text are permissions the player has.<br>Reversely, the permissions in red are permissions set for this player that are false.<br><br>";
		info += "<hr>These are all the permissions that are set specifically for this player.<br>";
		Permission[] permissions = ApiLayer.getPermissions("world", CalculableType.USER, name);
		if (permissions.length == 0)
			info += "<br>This player has no perms set specifically for them.<br>";
		else
			for (int i = 0; i < permissions.length; i++)
			{
				info += "<br>"
						+ (permissions[i].isTrue() ? "<font color=\"#00FF00\">"
								+ permissions[i].name() : "<font color=\"red\">"
								+ permissions[i].name()) + "</font>";
			}
		info += "<br><hr>These are all the permissions this player has. This includes inheritance and groups.<br>";
		Map<String, Boolean> p = ApiLayer.getEffectivePermissions("world", CalculableType.USER, name);
		for (String key : p.keySet())
			info += "<br>"
					+ (p.get(key).booleanValue() ? "<font color=\"#00FF00\">" : "<font color=\"red\">")
					+ key + "</font>";
		info += "<br><hr>These are all the groups that this player is a member of.<br><br><font color=\"#00FF00\">";
		String[] groups = ApiLayer.getGroups("world", CalculableType.USER, name);
		if (groups.length == 0)
			info += "<br>This player is not part of any groups.<br>";
		else
			for (int i = 0; i < groups.length; i++)
				info += groups[i] + "<br>";
		info += "</font><hr><h2>Player information</h2>";
		Player reading = getServer().getPlayer(name);
		if (reading != null && reading.isOnline())
		{
			info += "<table border=1 align=\"center\">";
			info += "<tr><td>IP address:</td><td>"
					+ reading.getAddress().toString().split(":")[0].replace("/", "");
			info += "</td></tr><tr><td>Health:</td><td>" + reading.getHealth()
					+ " / 20";
			info += "</td></tr><tr><td>Location:</td><td>"
					+ reading.getLocation().getWorld().getName() + " at "
					+ reading.getLocation().getBlockX() + ", "
					+ reading.getLocation().getBlockY() + ", "
					+ reading.getLocation().getBlockZ();
			info += "</td></tr><tr><td>Gamemode:</td><td>"
					+ reading.getGameMode().name();
			info += "</td></tr></table><br><hr>";
		}
		List<String> ips = darkJustice.getPreviousIPs(name);
		if (ips == null || ips.isEmpty())
			info += "<br>Player does not have any recorded past IPs on record.<br>";
		else
		{
			String s = "";
			for (String ip : ips)
			{
				s = s.contains("/") ? s.replace("/", "") : s;
				if (s.equals(""))
					s = ip;
				else
					s += ", " + ip;
			}
			info += "Previous IP addresses for this player: <br><font color=\"#00FF00\">"
					+ s + "</font>";
		}
		info += "<hr>";
		postOnline(sender, name, info);
	}

	private void postOnline(final Player sender, final String name, final String text)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					String data = URLEncoder.encode("name", "UTF-8") + "="
							+ URLEncoder.encode(name, "UTF-8");
					data += "&" + URLEncoder.encode("text", "UTF-8") + "="
							+ URLEncoder.encode(text, "UTF-8");
					URL url = new URL("http://darktidegames.com/games/minecraft/tools/posting/index.php");
					URLConnection conn = url.openConnection();
					conn.setDoOutput(true);
					OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
					wr.write(data);
					wr.flush();
					BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String line;
					sender.sendMessage("§9Begining output from post");
					while ((line = rd.readLine()) != null)
						sender.sendMessage(line);
					sender.sendMessage("§9Output complete");
					wr.close();
					rd.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	@EventHandler
	public void onPlayerReloadCommand(PlayerCommandPreprocessEvent event)
	{
		if (event.getMessage().equalsIgnoreCase("/reload"))
			for (Player player : getServer().getOnlinePlayers())
				player.sendMessage("§8[§4Notice§8] §7The plugins were reloaded.");
	}

	@EventHandler
	public void onPlayerUseManyCommands(PlayerCommandPreprocessEvent event)
	{
		Player player = event.getPlayer();
		if (!player.getName().equals("Celeo"))
			return;
		String original = event.getMessage();
		if (!original.startsWith("/"))
			return;
		if (original.contains("<>"))
		{
			event.setMessage(original.replace("<>", ""));
			return;
		}
		int count = countAppearance(original, "/");
		if (count < 2)
			return;
		if (count == 2 && original.startsWith("//"))
			return;
		for (String command : original.split("/"))
		{
			if (command.equals(""))
				continue;
			player.sendMessage("§7Fixer running command §4/" + command);
			player.chat("/" + command);
		}
		event.setCancelled(true);
	}

	public static int countAppearance(String haystack, String needle)
	{
		int count = 0;
		for (int i = 0; i < haystack.length(); i++)
			if (String.valueOf(haystack.charAt(i)).equals(needle))
				count++;
		return count;
	}

	@EventHandler
	public void onPlayerWatchCommand(PlayerCommandPreprocessEvent event)
	{
		Player sender = event.getPlayer();
		for (Player player : watching)
			if (!player.getName().equals(sender.getName()))
				player.sendMessage(String.format("§7Player %s did '%s'", sender.getName(), event.getMessage()));
	}

	public boolean shouldHideMessages(Player player)
	{
		return hiddenMessages.contains(player.getName());
	}

	public void setHiddenMessages(Player player, boolean on)
	{
		if (on && !shouldHideMessages(player))
			hiddenMessages.add(player.getName());
		else if (shouldHideMessages(player))
			hiddenMessages.remove(player.getName());
		player.sendMessage("§7Your join/leave messages will "
				+ (on ? "not " : "") + "be displayed");
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if (shouldHideMessages(player))
			event.setJoinMessage(null);
		if (pendingMessages.containsKey(player.getName()))
		{
			player.sendMessage(pendingMessages.get(player.getName()).replace("&", "§"));
			pendingMessages.remove(player.getName());
		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event)
	{
		if (shouldHideMessages(event.getPlayer()))
			event.setQuitMessage(null);
	}

	@EventHandler
	public void onPlayerUseAllCaps(AsyncPlayerChatEvent event)
	{
		Player player = event.getPlayer();
		if (player.hasPermission("darktide.caps"))
			return;
		String message = event.getMessage();
		int lower = 0;
		int upper = 0;
		for (char c : message.toCharArray())
		{
			if (Character.isDigit(c) || !Character.isLetter(c))
				continue;
			if (Character.isUpperCase(c))
				upper++;
			if (Character.isLowerCase(c))
				lower++;
		}
		if (upper > lower && message.length() > 3)
		{
			player.sendMessage("§cThere are too many capital letters in your message");
			event.setMessage(event.getMessage().toLowerCase());
		}
	}

	@EventHandler
	public void onPlayerHitSign(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		Block clicked = event.getClickedBlock();
		if (clicked == null)
			return;
		if (!(clicked.getState() instanceof Sign
				|| clicked.getType().equals(Material.SIGN)
				|| clicked.getType().equals(Material.SIGN_POST) || clicked.getType().equals(Material.WALL_SIGN)))
			return;
		if (signChanges.containsKey(player))
		{
			Sign sign = (Sign) clicked.getState();
			sign.setLine(getSignChange(player).getLine(), getSignChange(player).getMessage());
			removeSignChange(player);
			if (!sign.update())
				player.sendMessage("§cSign update failed");
			else
				player.sendMessage("§aSign changed");
		}
	}

	@EventHandler
	public void playerUseLighter(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (player.getItemInHand().getType().equals(Material.FLINT_AND_STEEL))
		{
			event.setCancelled(true);
			player.sendMessage("§cYou cannot use flint and steel");
		}
		if (player.getItemInHand().getType().equals(Material.PORTAL))
		{
			event.setCancelled(true);
			player.sendMessage("§cYou cannot place portals");
		}
	}

	@EventHandler
	public void onPlayerUseForbiddenCommand(PlayerCommandPreprocessEvent event)
	{
		Player player = event.getPlayer();
		for (String cmd : blockedCommands)
			if (event.getMessage().toLowerCase().startsWith(cmd.toLowerCase())
					&& !player.hasPermission("fixer.bypasscommands"))
			{
				player.sendMessage("§cYou cannot use this command - it is being blocked");
				event.setCancelled(true);
				return;
			}
	}

	@EventHandler
	public void playerUseOldGameModeCommand(PlayerCommandPreprocessEvent event)
	{
		Player player = event.getPlayer();
		if (!player.hasPermission("essentials.gamemode"))
			return;
		if (!event.getMessage().equalsIgnoreCase("/gm"))
			return;
		event.setCancelled(true);
		if (player.getGameMode().equals(GameMode.CREATIVE))
			player.setGameMode(GameMode.SURVIVAL);
		else
			player.setGameMode(GameMode.CREATIVE);
		player.sendMessage("§6Set game mode §c"
				+ player.getGameMode().name().toLowerCase() + " §6for "
				+ player.getName());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onVanishedPlayerPickupItem(PlayerPickupItemEvent event)
	{
		if (event.getPlayer().getGameMode().equals(GameMode.SURVIVAL))
			event.setCancelled(ess.getVanishedPlayers().contains(event.getPlayer().getName()));
	}

	@EventHandler
	public void onVanishedPlayerTarget(EntityTargetEvent event)
	{
		if (event.getTarget() instanceof Player)
			if (ess.getVanishedPlayers().contains(((Player) event.getTarget()).getName()))
				event.setCancelled(true);
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event)
	{
		event.setKeepLevel(true);
	}

	@EventHandler
	public void onPlayerUseScoreboard(PlayerCommandPreprocessEvent event)
	{
		Player player = event.getPlayer();
		if (event.getMessage().equalsIgnoreCase("/scoreboard"))
			if (!player.hasPermission("wars.scoreboard"))
			{
				player.sendMessage("§cYou cannot use that command");
				event.setCancelled(true);
			}
	}

	@EventHandler
	public void arenasGMPlayerChangeWorld(PlayerTeleportEvent event)
	{
		Player player = event.getPlayer();
		if (event.getFrom().getWorld().getName().equals("arenas")
				&& !event.getTo().getWorld().getName().equals("arenas")
				&& arenasGM.contains(player.getName()))
			toggleArenasGM(player);
	}

	@EventHandler
	public void arenasGMPlayerRespawn(PlayerRespawnEvent event)
	{
		Player player = event.getPlayer();
		if (arenasGM.contains(player.getName()))
			toggleArenasGM(player);
	}

	@EventHandler
	public void arenasGMPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if (arenasGM.contains(player.getName()))
			toggleArenasGM(player);
	}

}