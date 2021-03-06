/**
 * KarmicShare
 * CraftBukkit plugin that allows for players to
 * share items via a community pool. Karma system
 * in place so that players cannot leech from the
 * item pool.
 *
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicReport;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.plugin.java.JavaPlugin;

public class KarmicReport extends JavaPlugin {
	// Class variables
	private SQLite database;
	private final static String prefix = "[KarmicReport]";
	private Commander commander;
	private Config config;
	private PermCheck perm;

	@Override
	public void onDisable() {
		//Save config
		this.saveConfig();
		// Disconnect from sql database? Dunno if necessary
		if (database.checkConnection())
		{
			// Close connection
			database.close();
		}
		getLogger().info(prefix + " Plugin disabled");

	}

	@Override
	public void onLoad()
	{
		// Config
		config = new Config(this);
		// TODO MySQL support
		// Connect to sql database
		database = new SQLite(getLogger(), prefix, "report", this.getDataFolder()
				.getAbsolutePath());
		// Check if master player table exists
		if (!database.checkTable("kr_masterlist"))
		{
			getLogger().info(prefix + " Created master list table");
			//Master table
			database.createTable("CREATE TABLE `kr_masterlist` (`playername` varchar(32) NOT NULL, 'date' TEXT NOT NULL, 'status' TEXT, 'ip' TEXT NOT NULL);");
		}
		// Check if player table exists
		if (!database.checkTable("kr_reports"))
		{
			//Reports table
			getLogger().info(prefix + " Created reports table");
			database.createTable("CREATE TABLE `kr_reports` (`id` INTEGER PRIMARY KEY,`playername` TEXT NOT NULL, `author` TEXT NOT NULL, 'date' TEXT NOT NULL, 'comment' TEXT NOT NULL, 'world' TEXT, 'x' INTEGER, 'y' INTEGER, 'z' INTEGER);");
		}
	}

	@Override
	public void onEnable() {
		//Check if need to update
		config.checkUpdate();

		//Grab permission handler
		perm = new PermCheck(this);

		// Grab Commander to handle commands
		commander = new Commander(this);
		getCommand("report").setExecutor(commander);

		//Register Listener
		KarmicReportListener listener = new KarmicReportListener(this);
		this.getServer().getPluginManager().registerEvents(listener, this);

		//Register KarmicJail listener if plugin exists
		if(this.getServer().getPluginManager().getPlugin("KarmicJail") != null)
		{
			getLogger().info(prefix + " Hooked into KarmicJail");
			KarmicJailListener jailListener = new KarmicJailListener(this);
			this.getServer().getPluginManager().registerEvents(jailListener, this);
		}
		//Notify that its enabled
		getLogger().info(prefix + " KarmicReport v" + this.getDescription().getVersion() + " enabled");
	}

	/**
	 *
	 * @return Plugin's prefix
	 */
	public String getPluginPrefix() {
		return prefix;
	}

	// Returns SQLite database
	public SQLite getLiteDB() {
		return database;
	}

	public Config getPluginConfig() {
		return config;
	}

	public PermCheck getPermissionHandler()
	{
		return perm;
	}
}