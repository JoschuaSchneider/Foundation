package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class SimpleScoreboard {

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The name of our objective
	 */
	private final static String OBJECTIVE_NAME = "FoundationBoard";

	/**
	 * List of all active scoreboard (added upon creating a new instance)
	 */
	@Getter
	private final static List<SimpleScoreboard> registeredBoards = new ArrayList<>();

	/**
	 * Clears registered boards, usually called on reload
	 */
	public static final void clearBoards() {
		registeredBoards.clear();
	}

	/**
	 * Removes all scoreboard for a player
	 *
	 * @param player
	 */
	public static final void clearBoardsFor(Player player) {
		for (final SimpleScoreboard scoreboard : registeredBoards)
			if (scoreboard.isViewing(player))
				scoreboard.hide(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Public entries
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Stored scoreboard lines
	 */
	@Getter
	private final List<String> rows = new ArrayList<>();

	/**
	 * Players viewing the scoreboard
	 */
	@Getter
	private final StrictList<String> viewingPlayers = new StrictList<>();

	/**
	 * The color theme for key: value pairs such as
	 *
	 * Players: 12
	 * Mode: playing
	 */
	private final String[] theme = new String[2];

	/**
	 * The title of this scoreboard
	 */
	@Getter
	@Setter
	private String title;

	/**
	 * The update tick delay
	 */
	@Getter
	@Setter
	private int updateDelayTicks;

	// ------------------------------------------------------------------------------------------------------------
	// Private entries
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The bukkit scoreboard
	 */
	private final Scoreboard scoreboard;

	/**
	 * The bukkit objective
	 */
	private Objective objective;

	/**
	 * The running update task
	 */
	private BukkitTask updateTask;

	/**
	 * Create a new scoreboard
	 */
	public SimpleScoreboard() {
		this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

		registeredBoards.add(this);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Add new rows
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(String... entries) {
		addRows(Arrays.asList(entries));
	}

	/**
	 * Add rows onto the scoreboard
	 *
	 * @param entries
	 */
	public final void addRows(List<String> entries) {
		rows.addAll(entries);
	}

	/**
	 * Remove all rows
	 */
	public final void clearRows() {
		rows.clear();
	}

	/**
	 * Remove row at the given index
	 *
	 * @param index
	 */
	public final void removeRow(int index) {
		rows.remove(index);
	}

	/**
	 * Remove row that contains the given text
	 *
	 * @param thatContains
	 */
	public final void removeRow(String thatContains) {
		for (final Iterator<String> it = rows.iterator(); it.hasNext();) {
			final String row = it.next();

			if (row.contains(thatContains))
				it.remove();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Start / stop
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Starts visualizing this scoreboard
	 */
	private final void start() {
		Valid.checkBoolean(updateTask == null, "Scoreboard " + this + " already running");

		updateTask = new BukkitRunnable() {
			@Override
			public void run() {

				try {
					update();

				} catch (final Throwable t) {
					final String lines = String.join(" ", rows);

					Common.error(t,
							"Error displaying " + SimpleScoreboard.this,
							"Entries: " + lines,
							"%error",
							"Stopping rendering for safety.");

					stop();
				}
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, updateDelayTicks);
	}

	/**
	 * Updates this scoreboard
	 */
	private final void update() {
		onUpdate();
		resetObjective();
		reloadEntries();
	}

	/**
	 * Resets the objective
	 */
	private final void resetObjective() {
		if (objective != null)
			objective.unregister();

		objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy");

		objective.setDisplayName(Common.colorize(title));
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	/**
	 * Reload all entries
	 */
	private final void reloadEntries() {
		final StrictList<String> duplicates = new StrictList<>();

		for (int i = rows.size(); i > 0; i--) {
			final String sidebarEntry = rows.get(rows.size() - i);
			final String entry = replaceVariables(replaceTheme(sidebarEntry));

			String line = fixDuplicates(duplicates, entry);

			if (line.length() > 40)
				line = line.substring(0, 40);

			Remain.getScore(objective, line).setScore(i);
		}
	}

	/**
	 * Adds theme colors to the row if applicable
	 *
	 * @param row
	 * @return
	 */
	private final String replaceTheme(String row) {
		if (theme != null && row.contains(":")) {
			if (theme.length == 1)
				return theme[0] + row;

			else {
				final String[] split = row.split("\\:");

				return theme[0] + split[0] + ":" + theme[1] + split[1];
			}
		}

		return row;
	}

	/**
	 * Set the coloring theme for rows having : such as
	 *
	 * Players: 12
	 * Mode: playing
	 *
	 * To use simply put color codes
	 *
	 * @param theme the theme to set
	 */
	public final void setTheme(@NonNull ChatColor primary, @Nullable ChatColor secondary) {
		if (secondary != null) {
			this.theme[0] = "&" + primary.getChar();
			this.theme[1] = "&" + secondary.getChar();
		} else
			this.theme[0] = "&" + primary.getChar();
	}

	/**
	 * Fixes color and empty lines colliding into one by adding random colors
	 *
	 * @param duplicates
	 * @param message
	 * @return
	 */
	private final String fixDuplicates(StrictList<String> duplicates, String message) {
		message = StringUtils.substring(message, 0, 40);

		final boolean cut = MinecraftVersion.olderThan(V.v1_8);

		if (cut && message.length() > 16)
			message = message.substring(0, 16);

		if (duplicates.contains(message))
			for (int i = 0; i < duplicates.size() && message.length() < 40; i++)
				message += RandomUtil.nextChatColor();

		if (cut && message.length() > 16)
			message = message.substring(0, 16);

		duplicates.add(message);
		return message;
	}

	/**
	 * Replaces variables in the message
	 *
	 * @param message
	 * @return
	 */
	protected String replaceVariables(String message) {
		return message;
	}

	/**
	 * Called when this scoreboard is ticked
	 */
	protected void onUpdate() {
	}

	/**
	 * Stops this scoreboard and removes it from all viewers
	 */
	public final void stop() {
		for (final Iterator<String> iterator = viewingPlayers.iterator(); iterator.hasNext();) {
			final String name = iterator.next();
			final Player player = Bukkit.getPlayer(name);

			player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
			iterator.remove();
		}

		if (updateTask != null)
			cancelUpdateTask();
	}

	/**
	 * Cancels the update task
	 */
	private final void cancelUpdateTask() {
		Valid.checkNotNull(updateTask, "Scoreboard " + this + " not running");

		updateTask.cancel();
		updateTask = null;
	}

	/**
	 * Return true if the scoreboard is running and rendering?
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return updateTask != null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Show this scoreboard to the player
	 *
	 * @param player
	 */
	public final void show(Player player) {
		Valid.checkBoolean(!isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + getTitle());

		if (updateTask == null)
			start();

		player.setScoreboard(scoreboard);
		viewingPlayers.add(player.getName());
	}

	/**
	 * Hide this scoreboard from the player
	 *
	 * @param player
	 */
	public final void hide(Player player) {
		Valid.checkBoolean(isViewing(player), "Player " + player.getName() + " is not viewing scoreboard: " + getTitle());

		player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		viewingPlayers.remove(player.getName());

		if (viewingPlayers.isEmpty())
			cancelUpdateTask();
	}

	/**
	 * Returns true if the given player is viewing the scoreboard
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(Player player) {
		return viewingPlayers.contains(player.getName());
	}

	@Override
	public final String toString() {
		return "Scoreboard{title=" + getTitle() + "}";
	}
}