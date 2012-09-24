package net.slipcor.pvparena.arena;

import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.StringParser;

/**
 * <pre>Arena Team class</pre>
 * 
 * contains Arena Team methods and variables for quicker access
 * 
 * @author slipcor
 * 
 * @version v0.9.0
 */

public class ArenaTeam {
	
	private static Debug db = new Debug(6);

	private final HashSet<ArenaPlayer> players;
	private final ChatColor color;
	private final String name;

	/**
	 * create an arena team instance
	 * 
	 * @param name
	 *            the arena team name
	 * @param color
	 *            the arena team color string
	 */
	public ArenaTeam(String name, String color) {
		this.players = new HashSet<ArenaPlayer>();
		this.color = StringParser.getChatColorFromWoolEnum(color);
		this.name = name;
	}

	/**
	 * add an arena player to the arena team
	 * 
	 * @param player
	 *            the player to add
	 */
	public void add(ArenaPlayer player) {
		this.players.add(player);
		db.i("Added player " + player.getName() + " to team " + name);
		player.getArena().increasePlayerCount();
	}

	/**
	 * colorize a player name
	 * 
	 * @param player
	 *            the player to colorize
	 * @return the colorized player name
	 */
	public String colorizePlayer(Player player) {
		return color + player.getName();
	}

	/**
	 * return the team color
	 * 
	 * @return the team color
	 */
	public ChatColor getColor() {
		return color;
	}

	/**
	 * colorize the team name
	 * 
	 * @return the colorized team name
	 */
	public String getColoredName() {
		return color + name;
	}

	/**
	 * return the team color code
	 * 
	 * @return the team color code
	 */
	public String getColorCodeString() {
		return "&" + Integer.toHexString(color.ordinal());
	}

	/**
	 * return the team name
	 * 
	 * @return the team name
	 */
	public String getName() {
		return name;
	}

	/**
	 * return the team members
	 * 
	 * @return a HashSet of all arena players
	 */
	public HashSet<ArenaPlayer> getTeamMembers() {
		return players;
	}

	public boolean hasPlayer(Player p) {
		return players.contains(ArenaPlayer.parsePlayer(p.getName()));
	}

	/**
	 * remove a player from the team
	 * 
	 * @param player
	 *            the player to remove
	 */
	public void remove(ArenaPlayer player) {
		this.players.remove(player);
	}
}