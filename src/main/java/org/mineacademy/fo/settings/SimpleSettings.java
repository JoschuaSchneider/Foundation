package org.mineacademy.fo.settings;

import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.update.SpigotUpdater;

/**
 * A simple implementation of a typical main plugin settings
 * where each key can be accessed in a static way from anywhere.
 *
 * Typically we use this class for settings.yml main plugin config.
 */
// Use for settings.yml
@SuppressWarnings("unused")
public abstract class SimpleSettings extends YamlStaticConfig {

	/**
	 * A flag indicating that this class has been loaded
	 *
	 * You can place this class to {@link SimplePlugin#getSettingsClasses()} to make
	 * it load automatically
	 */
	private static boolean settingsClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	@Override
	protected final void load() throws Exception {
		createFileAndLoad(getSettingsFileName());
	}

	/**
	 * Get the file name for these settings, by default settings.yml
	 *
	 * @return
	 */
	protected String getSettingsFileName() {
		return FoConstants.File.SETTINGS;
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * The configuration version number, found in the "Version" key in the file.,
	 */
	protected static Integer VERSION;

	/**
	 * Set and update the config version automatically, however the {@link #VERSION} will
	 * contain the older version used in the file on the disk so you can use
	 * it for comparing in the init() methods
	 *
	 * Please call this as a super method when overloading this!
	 */
	@Override
	protected void preLoad() {
		// Load version first so we can use it later
		pathPrefix(null);

		if ((VERSION = getInteger("Version")) != getConfigVersion())
			set("Version", getConfigVersion());
	}

	/**
	 * Return the very latest config version
	 *
	 * Any changes here must also be made to the "Version" key in your settings file.
	 *
	 * @return
	 */
	protected abstract int getConfigVersion();

	// --------------------------------------------------------------------
	// Settings we offer by default for your main config file
	// Specify those you need to modify
	// --------------------------------------------------------------------

	/**
	 * What debug sections should we enable in {@link Debugger} ? When you call {@link Debugger#debug(String, String...)}
	 * those that are specified in this settings are logged into the console, otherwise no message is shown.
	 *
	 * Typically this is left empty: Debug: []
	 */
	public static StrictList<String> DEBUG_SECTIONS = new StrictList<>();

	/**
	 * The plugin prefix in front of chat/console messages, added automatically unless
	 * disabled in {@link Common#ADD_LOG_PREFIX} and {@link Common#ADD_TELL_PREFIX}.
	 *
	 * Typically for ChatControl:
	 *
	 * Prefix: "&8[&3ChatControl&8]&7 "
	 */
	public static String PLUGIN_PREFIX = "&7" + SimplePlugin.getNamed() + " //";

	/**
	 * The lag threshold used for {@link LagCatcher} in milliseconds. Set to -1 to disable.
	 *
	 * Typically for ChatControl:
	 *
	 * Log_Lag_Over_Milis: 100
	 */
	public static Integer LAG_THRESHOLD_MILLIS = 100;

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 *
	 * Regex_Timeout_Milis: 100
	 */
	public static Integer REGEX_TIMEOUT = 100;

	/**
	 * What commands should trigger the your main plugin command (separated by a comma ,)? See {@link SimplePlugin#getMainCommand()}
	 *
	 * Typical values for ChatControl:
	 *
	 * Command_Aliases: [chatcontrol, chc, cc]
	 *
	 * // ONLY MANDATORY IF YOU OVERRIDE {@link SimplePlugin#getMainCommand()} //
	 */
	public static StrictList<String> MAIN_COMMAND_ALIASES = new StrictList<>();

	/**
	 * The localization prefix, given you are using {@link SimpleLocalization} class to load and manage your
	 * locale file. Typically the file path is: localization/messages_PREFIX.yml with this prefix below.
	 *
	 * Typically: Locale: en
	 *
	 * // ONLY MANDATORY IF YOU USE SIMPLELOCALIZATION //
	 */
	public static String LOCALE_PREFIX = "en";

	/**
	 * The server name used in {server_name} variable or BungeeCord, if your plugin supports either of those.
	 *
	 * Typically for ChatControl:
	 *
	 * Server_Name: "My ChatControl Server"
	 *
	 * // NOT MANDATORY //
	 */
	public static String SERVER_NAME = "Server";

	/**
	 * The server name identifier
	 *
	 * Mandatory if using BungeeCord
	 */
	public static String BUNGEE_SERVER_NAME = "Server";

	/**
	 * Antipiracy stuff for our protected software, leave empty to Serialization: ""
	 *
	 * // NOT MANDATORY //
	 */
	public static String SECRET_KEY = "";

	/**
	 * Should we check for updates from SpigotMC and notify the console and users with permission?
	 *
	 * See {@link SimplePlugin#getUpdateCheck()} that you can make to return {@link SpigotUpdater} with your Spigot plugin ID.
	 *
	 * Typically for ChatControl:
	 *
	 * Notify_Updates: true
	 *
	 * // ONLY MANDATORY IF YOU OVERRIDE {@link SimplePlugin#getUpdateCheck()} //
	 */
	public static Boolean NOTIFY_UPDATES = false;

	/**
	 * Should we enable inbuilt advertisements?
	 * ** We found out that users really hate this feature, you may want not to use this completelly **
	 * ** If you want to broadcast important messages regardless of this feature just implement your **
	 * ** own Runnable that checks for a YAML file on your external server on plugin load. **
	 *
	 * Typically for ChatControl:
	 *
	 * Notify_Promotions: true
	 *
	 * // NOT MANDATORY //
	 */
	public static Boolean NOTIFY_PROMOTIONS = true;

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		Valid.checkBoolean(!settingsClassCalled, "Settings class already loaded!");

		pathPrefix(null);
		upgradeOldSettings();

		if (isSetDefault("Prefix"))
			PLUGIN_PREFIX = getString("Prefix");

		if (isSetDefault("Log_Lag_Over_Milis")) {
			LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
			Valid.checkBoolean(LAG_THRESHOLD_MILLIS == -1 || LAG_THRESHOLD_MILLIS >= 0, "Log_Lag_Over_Milis must be either -1 to disable, 0 to log all or greater!");

			if (LAG_THRESHOLD_MILLIS == 0)
				Common.log("&eLog_Lag_Over_Milis is 0, all performance is logged. Set to -1 to disable.");
		}

		if (isSetDefault("Debug"))
			DEBUG_SECTIONS = new StrictList<>(getStringList("Debug"));

		if (isSetDefault("Regex_Timeout_Milis"))
			REGEX_TIMEOUT = getInteger("Regex_Timeout_Milis");

		if (isSetDefault("Server_Name"))
			SERVER_NAME = Common.colorize(getString("Server_Name"));

		if (isSetDefault("Notify_Promotions"))
			NOTIFY_PROMOTIONS = getBoolean("Notify_Promotions");

		if (isSetDefault("Serialization"))
			SECRET_KEY = getString("Serialization");

		// -------------------------------------------------------------------
		// Load maybe-mandatory values
		// -------------------------------------------------------------------

		{ // Load Bungee server name

			final boolean keySet = isSetDefault("Bungee_Server_Name");

			if (SimplePlugin.getInstance().getBungeeCord() != null && !keySet)
				throw new FoException("Since you override getBungeeCord in your main plugin class you must set the 'Bungee_Server_Name' key in " + getFileName());

			BUNGEE_SERVER_NAME = keySet ? getString("Bungee_Server_Name") : BUNGEE_SERVER_NAME;

			if (SimplePlugin.getInstance().getBungeeCord() != null && BUNGEE_SERVER_NAME.equals("undefined"))
				Common.logFramed(true,
						"Please change your Bungee_Server_Name in",
						"settings.yml to the exact name of this server",
						"as you have in config.yml of your BungeeCord.");
		}

		{ // Load localization
			final boolean hasLocalization = hasLocalization();
			final boolean keySet = isSetDefault("Locale");

			if (hasLocalization && !keySet)
				throw new FoException("Since you have your Localization class you must set the 'Locale' key in " + getFileName());

			LOCALE_PREFIX = keySet ? getString("Locale") : LOCALE_PREFIX;
		}

		{ // Load main command alias

			final boolean keySet = isSetDefault("Command_Aliases");

			if (SimplePlugin.getInstance().getMainCommand() != null && !keySet)
				throw new FoException("Since you override getMainCommand in your main plugin class you must set the 'Command_Aliases' key in " + getFileName());

			MAIN_COMMAND_ALIASES = keySet ? getCommandList("Command_Aliases") : MAIN_COMMAND_ALIASES;
		}

		{ // Load updates notifier

			final boolean keySet = isSetDefault("Notify_Updates");

			if (SimplePlugin.getInstance().getUpdateCheck() != null && !keySet)
				throw new FoException("Since you override getUpdateCheck in your main plugin class you must set the 'Notify_Updates' key in " + getFileName());

			NOTIFY_UPDATES = keySet ? getBoolean("Notify_Updates") : NOTIFY_UPDATES;
		}

		settingsClassCalled = true;
	}

	/**
	 * Inspect if some settings classes extend localization and make sure only one does, if any
	 *
	 * @return
	 */
	private static boolean hasLocalization() {
		final SimplePlugin plugin = SimplePlugin.getInstance();
		int localeClasses = 0;

		if (plugin.getSettings() != null)
			for (final Class<?> clazz : plugin.getSettings())
				if (SimpleLocalization.class.isAssignableFrom(clazz))
					localeClasses++;

		Valid.checkBoolean(localeClasses < 2, "You cannot have more than 1 class extend SimpleLocalization!");
		return localeClasses == 1;
	}

	/**
	 * Upgrade some of the old and ancient settings from our premium plugins.
	 */
	private static void upgradeOldSettings() {
		{ // Debug
			if (isSetAbsolute("Debugger"))
				move("Debugger", "Debug");

			if (isSetAbsolute("Serialization_Number"))
				move("Serialization_Number", "Serialization");

			// ChatControl
			if (isSetAbsolute("Debugger.Keys")) {
				move("Debugger.Keys", "Serialization");
				move("Debugger.Sections", "Debug");
			}

			// Archaic
			if (isSetAbsolute("Debug") && !(getObject("Debug") instanceof List))
				set("Debug", null);
		}

		{ // Prefix
			if (isSetAbsolute("Plugin_Prefix"))
				move("Plugin_Prefix", "Prefix");

			if (isSetAbsolute("Check_Updates"))
				move("Check_Updates", "Notify_Updates");
		}
	}

	/**
	 * Was this class loaded?
	 *
	 * @return
	 */
	public static final Boolean isSettingsCalled() {
		return settingsClassCalled;
	}

	/**
	 * Reset the flag indicating that the class has been loaded,
	 * used in reloading.
	 */
	public static final void resetSettingsCall() {
		settingsClassCalled = false;
	}
}
