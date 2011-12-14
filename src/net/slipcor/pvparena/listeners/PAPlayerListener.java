/*
 * player listener class
 * 
 * author: slipcor
 * 
 * version: v0.4.4 - Random spawns per team, not shared
 * 
 * history:
 * 
 *     v0.4.2 - command blacklist
 *     v0.4.1 - command manager, arena information and arena config check
 *     v0.4.0 - mayor rewrite, improved help
 *     v0.3.13 - Telepass via Permission
 *     v0.3.10 - CraftBukkit #1337 config version, rewrite
 *     v0.3.9 - Permissions, rewrite
 *     v0.3.8 - BOSEconomy, rewrite
 *     v0.3.7 - Bugfixes
 *     v0.3.6 - CTF Arena
 *     v0.3.5 - Powerups!!
 *     v0.3.3 - Random spawns possible for every arena
 *     v0.3.1 - New Arena! FreeFight
 *     v0.3.0 - Multiple Arenas
 * 	   v0.2.1 - cleanup, comments
 * 	   v0.1.10 - config: only start with even teams
 * 	   v0.1.9 - configure teleport locations
 * 	   v0.1.2 - class permission requirement
 * 	   v0.1.1 - ready block configurable
 * 	   v0.0.0 - copypaste
 */

package net.slipcor.pvparena.listeners;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arenas.Arena;
import net.slipcor.pvparena.arenas.CTFArena;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.DebugManager;
import net.slipcor.pvparena.powerups.Powerup;
import net.slipcor.pvparena.powerups.PowerupEffect;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class PAPlayerListener extends PlayerListener {
	private DebugManager db = new DebugManager();

	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();

		Arena arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null) {
			return; // no fighting player => OUT
		}
		
		List<String> list = PVPArena.instance.getConfig().getStringList("blacklist");
		db.i("checking command blacklist");
		
		for (String s : list) {
			if (event.getMessage().startsWith("/" + s)) {
				db.i("command blocked: " + s);
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		Arena arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null) {
			return; // no fighting player => OUT
		}
		db.i("onPlayerMove: fighting player!");
		if (arena.pm != null) {
			Powerup p = arena.pm.puActive.get(player);
			if (p != null) {
				if (p.canBeTriggered()) {
					if (p.isEffectActive(PowerupEffect.classes.FREEZE)) {
						db.i("freeze in effect, cancelling!");
						event.setCancelled(true);
					}
					if (p.isEffectActive(PowerupEffect.classes.SPRINT)) {
						db.i("sprint in effect, sprinting!");
						event.getPlayer().setSprinting(true);
					}
					if (p.isEffectActive(PowerupEffect.classes.SLIP)) {
						//
					}
				}
			}
		}
	}

	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		Arena arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null) {
			return; // no fighting player => OUT
		}
		db.i("onPlayerRespawn: fighting player");
		if (arena.playerManager.getRespawn(player) != null) {
			return; // no respawning player => OUT
		}
		db.i("respawning player");
		Location l;

		if (arena.sTPdeath.equals("old")) {
			db.i("=> old location");
			l = arena.getPlayerOldLocation(player);
		} else {
			db.i("=> 'config=>death' location");
			l = arena.getCoords(arena.sTPdeath);
		}
		event.setRespawnLocation(l);

		arena.removePlayer(player, arena.sTPdeath);
		arena.playerManager.setRespawn(player, false);
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Arena arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null)
			return; // no fighting player => OUT
		db.i("onPlayerQuit: fighting player");
		String color = arena.paTeams.get(arena.playerManager.getTeam(player));
		if (!color.equals("")) {
			arena.playerManager.tellEveryoneExcept(
					player,
					PVPArena.lang.parse("playerleave", ChatColor.valueOf(color)
							+ player.getName() + ChatColor.YELLOW));
		} else {
			arena.playerManager.tellEveryoneExcept(
					player,
					PVPArena.lang.parse("playerleave",
							ChatColor.WHITE + player.getName()
									+ ChatColor.YELLOW));
		}
		arena.removePlayer(player, arena.sTPexit);
		arena.checkEndAndCommit();
	}

	@Override
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();

		Arena arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null)
			return; // no fighting player => OUT

		db.i("onPlayerDropItem: fighting player");
		ArenaManager.tellPlayer(player, (PVPArena.lang.parse("dropitem")));
		event.setCancelled(true);
		// cancel the drop event for fighting players, with message
	}

	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();

		Arena arena = ArenaManager.getArenaByPlayer(player);
		if ((arena == null) || (arena.pm == null)
				|| (arena.pm.puTotal.size() < 1))
			return; // no fighting player or no powerups => OUT

		db.i("onPlayerPickupItem: fighting player");
		Iterator<Powerup> pi = arena.pm.puTotal.iterator();
		while (pi.hasNext()) {
			Powerup p = pi.next();
			if (event.getItem().getItemStack().getType().equals(p.item)) {
				Powerup newP = new Powerup(p);
				if (arena.pm.puActive.containsKey(player)) {
					arena.pm.puActive.get(player).disable();
				}
				arena.pm.puActive.put(player, newP);
				arena.playerManager.tellEveryone(PVPArena.lang.parse(
						"playerpowerup", player.getName(), newP.name));
				event.setCancelled(true);
				event.getItem().remove();
				if (newP.canBeTriggered())
					newP.activate(player); // activate for the first time

				return;
			}
		}
	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		Arena arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null)
			return; // no fighting player => OUT

		db.i("onPlayerTeleport: fighting player (uncancel)");
		event.setCancelled(false); // fighting player - first recon NOT to
									// cancel!

		if (arena.playerManager.getTelePass(player)
				|| PVPArena.instance.hasPerms(player, "pvparena.telepass"))
			return; // if allowed => OUT

		db.i("onPlayerTeleport: no tele pass, cancelling!");
		event.setCancelled(true); // cancel and tell
		ArenaManager.tellPlayer(player, PVPArena.lang.parse("usepatoexit"));
	}

	@Override
	public void onPlayerVelocity(PlayerVelocityEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();

		Arena arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null)
			return; // no fighting player or no powerups => OUT

		db.i("inPlayerVelocity: fighting player");
		if (arena.pm != null) {
			Powerup p = arena.pm.puActive.get(player);
			if (p != null) {
				if (p.canBeTriggered()) {
					if (p.isEffectActive(PowerupEffect.classes.JUMP)) {
						p.commit(event);
					}
				}
			}
		}
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		Arena arena = ArenaManager.getArenaByName(Arena.regionmodify);

		if (arena != null && PVPArena.instance.hasAdminPerms(player)
				&& (player.getItemInHand() != null)
				&& (player.getItemInHand().getTypeId() == arena.wand)) {
			// - modify mode is active
			// - player has admin perms
			// - player has wand in hand
			db.i("modify&adminperms&wand");
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				arena.pos1 = event.getClickedBlock().getLocation();
				ArenaManager.tellPlayer(player, PVPArena.lang.parse("pos1"));
				event.setCancelled(true); // no destruction in creative mode :)
				return; // left click => pos1
			}

			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				arena.pos2 = event.getClickedBlock().getLocation();
				ArenaManager.tellPlayer(player, PVPArena.lang.parse("pos2"));
				return; // right click => pos2
			}
		}
		arena = ArenaManager.getArenaByPlayer(player);
		if (arena == null) {
			db.i("onInteract: sign check");
			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				Block block = event.getClickedBlock();
				if (block.getState() instanceof Sign) {
					Sign sign = (Sign) block.getState();
					if (sign.getLine(0).equalsIgnoreCase("[arena]")) {
						String sName = sign.getLine(1);
						String[] newArgs = null;

						Arena a = ArenaManager.getArenaByName(sName);
						if (a == null) {
							ArenaManager.tellPlayer(player, PVPArena.lang
									.parse("arenanotexists", sName));
							return;
						}
						a.parseCommand(player, newArgs);
						return;
					}
				}
			}
		}
		db.i("arena: " + (arena == null ? null : arena.name));
		if (arena != null) {
			db.i("fight: " + arena.fightInProgress);
			db.i("instanceof: " + (arena.getType().equals("ctf")));
		}
		if (arena != null && arena.fightInProgress
				&& (arena.getType().equals("ctf"))) {
			db.i("onInteract: CTF");
			CTFArena ca = (CTFArena) arena;
			ca.checkInteract(player);
			return;
		}

		if (arena == null || arena.fightInProgress)
			return; // not fighting or fight already in progress => OUT

		// fighting player inside the lobby!
		event.setCancelled(true); //

		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			Block block = event.getClickedBlock();
			if (block.getState() instanceof Sign) {
				db.i("sign click!");
				Sign sign = (Sign) block.getState();

				if ((arena.paClassItems.containsKey(sign.getLine(0)) || (sign
						.getLine(0).equalsIgnoreCase("custom")))
						&& (!arena.playerManager.getTeam(player).equals(""))) {

					YamlConfiguration config = new YamlConfiguration();
					try {
						config.load(arena.configFile);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InvalidConfigurationException e) {
						e.printStackTrace();
					}
					boolean classperms = false;
					if (config.get("general.classperms") != null) {
						classperms = config.getBoolean("general.classperms",
								false);
					}

					if (classperms) {
						db.i("checking classperms");
						if (!(PVPArena.instance.hasPerms(player, "fight.group."
								+ sign.getLine(0)) || PVPArena.instance
								.hasPerms(player,
										"pvparena.class." + sign.getLine(0)))) {
							ArenaManager.tellPlayer(player,
									PVPArena.lang.parse("classperms"));
							return; // class permission desired and failed =>
									// announce and OUT
						}
					}

					int i = 0;

					if (!arena.playerManager.getClass(player).equals("")) {
						db.i("removing player from sign");
						// already selected class, remove it!
						Sign sSign = (Sign) arena.playerManager
								.getSignLocation(player).getBlock().getState();

						for (i = 2; i < 4; i++) {
							if (sSign.getLine(i).equalsIgnoreCase(
									player.getName())) {
								sSign.setLine(i, "");
								arena.clearInventory(player);
								sSign.update();
								break; // remove found player, break!
							}
						}
						sSign = arena.getNext(sSign);

						if (sSign != null) {
							for (i = 0; i < 4; i++) {
								if (sSign.getLine(i).equalsIgnoreCase(
										player.getName())) {
									sSign.setLine(i, "");
									arena.clearInventory(player);
									break; // remove found player, break!
								}
							}
							sSign.update();
						}
					}

					db.i("adding player to sign");
					for (i = 2; i < 4; i++) {
						if (sign.getLine(i).equals("")) {
							arena.playerManager.setSignLocation(player, sign
									.getBlock().getLocation());
							arena.playerManager.setClass(player,
									sign.getLine(0));
							sign.setLine(i, player.getName());
							sign.update();
							// select class
							if (sign.getLine(0).equalsIgnoreCase("custom")) {
								// if custom, give stuff back
								arena.loadInventory(player);
							} else {
								arena.givePlayerFightItems(player);
							}
							return;
						}
					}

					Sign nSign = arena.getNext(sign);
					db.i("fetching sign under sign");

					if (nSign != null) {
						db.i("found! trying to add");
						for (i = 0; i < 4; i++) {
							if (nSign.getLine(i).equals("")) {
								arena.playerManager.setSignLocation(player,
										sign.getBlock().getLocation());
								arena.playerManager.setClass(player,
										sign.getLine(0));
								nSign.setLine(i, player.getName());
								nSign.update();
								// select class
								if (sign.getLine(0).equalsIgnoreCase("custom")) {
									// if custom,  give stuff back
									arena.loadInventory(player);
								} else {
									arena.givePlayerFightItems(player);
								}
								return;
							}
						}
					}
					ArenaManager.tellPlayer(player,
							PVPArena.lang.parse("toomanyplayers"));
				}
				return;
			}
		}

		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			Block block = event.getClickedBlock();

			db.i("block click!");

			YamlConfiguration config = new YamlConfiguration();
			try {
				config.load(arena.configFile);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InvalidConfigurationException e1) {
				e1.printStackTrace();
			}
			Material mMat = Material.IRON_BLOCK;
			if (config.get("general.readyblock") != null) {
				db.i("reading ready block");
				try {
					mMat = Material.getMaterial(config
							.getInt("general.readyblock"));
					if (mMat == Material.AIR)
						mMat = Material.getMaterial(config
								.getString("general.readyblock"));
					db.i("mMat now is " + mMat.name());
				} catch (Exception e) {
					db.i("exception reading ready block");
					String sMat = config.getString("general.readyblock");
					try {
						mMat = Material.getMaterial(sMat);
						db.i("mMat now is " + mMat.name());
					} catch (Exception e2) {
						PVPArena.lang.log_warning("matnotfound", sMat);
					}
				}
			}
			db.i("clicked " + block.getType().name() + ", is it " + mMat.name()
					+ "?");
			if (block.getTypeId() == mMat.getId()) {
				db.i("clicked ready block!");
				if (arena.playerManager.getTeam(player).equals(""))
					return; // not a fighting player => OUT
				if (arena.playerManager.getClass(player).equals(""))
					return; // not a fighting player => OUT

				String color = arena.playerManager.getTeam(player);

				if (!arena.playerManager.ready(arena)) {
					ArenaManager.tellPlayer(player,
							PVPArena.lang.parse("notready"));
					return; // team not ready => announce
				}

				if (arena.forceEven) {
					if (arena.playerManager.checkEven()) {
						ArenaManager.tellPlayer(player,
								PVPArena.lang.parse("waitequal"));
						return; // even teams desired, not done => announce
					}
				}

				if (!arena.checkRegions()) {
					ArenaManager.tellPlayer(player,
							PVPArena.lang.parse("checkregionerror"));
					return;
				}

				if (color != "free") {
					String sName = color;
					color = arena.paTeams.get(color);

					arena.playerManager.tellEveryone(PVPArena.lang.parse(
							"ready", ChatColor.valueOf(color) + sName
									+ ChatColor.WHITE));

					arena.teleportAllToSpawn();
					arena.fightInProgress = true;
					arena.playerManager.tellEveryone(PVPArena.lang
							.parse("begin"));
				} else {
					arena.teleportAllToSpawn();
					arena.fightInProgress = true;
					arena.playerManager.tellEveryone(PVPArena.lang
							.parse("begin"));
				}
			}
		}
	}

}