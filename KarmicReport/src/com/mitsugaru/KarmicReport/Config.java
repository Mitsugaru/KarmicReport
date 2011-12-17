package com.mitsugaru.KarmicReport;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;

public class Config {
	//Class variables
	private KarmicReport kr;
	public boolean ipchange, kick, ban, debugTime;
	public int limit;

	public Config(KarmicReport plugin)
	{
		kr = plugin;
		//Grab config
		ConfigurationSection config = kr.getConfig();
		//Hashmap of defaults
		final Map<String, Object> defaults = new HashMap<String, Object>();
		defaults.put("version", kr.getDescription().getVersion());
		defaults.put("report.IPchange", true);
		defaults.put("report.kick", true);
		defaults.put("report.ban", true);
		defaults.put("limit", 10);
		//TODO defaults
		//Insert defaults into config file if they're not present
		for(final Entry<String, Object> e : defaults.entrySet())
		{
			if(!config.contains(e.getKey()))
			{
				config.set(e.getKey(), e.getValue());
			}
		}
		//Save config
		kr.saveConfig();
		//Load variables from config
		debugTime = config.getBoolean("debugTime", false);
		ipchange = config.getBoolean("report.IPchange", true);
		kick = config.getBoolean("report.kick", true);
		ban = config.getBoolean("report.ban", true);
		limit = config.getInt("limit", 10);
		if(limit < 1)
		{
			//Gave a negative or zero limit
			limit = 10;
			kr.getLogger().warning(kr.getPluginPrefix() + " Limit is beyond bounds. Using default.");
		}
	}

	/**
	 * This method is called to make the appropriate changes,
	 * most likely only necessary for database schema modification,
	 * for a proper update.
	 */
	public void update()
	{
		//Update version number in config.yml
		kr.getConfig().set("version", kr.getDescription().getVersion());
		kr.saveConfig();
	}

	public void reload()
	{
		//Save config
		kr.saveConfig();
		//Grab config
		ConfigurationSection config = kr.getConfig();
		//Load variables from config
		debugTime = config.getBoolean("debugTime", false);
		ipchange = config.getBoolean("report.IPchange", true);
		kick = config.getBoolean("report.kick", true);
		ban = config.getBoolean("report.ban", true);
		limit = config.getInt("limit", 10);
		if(limit < 1)
		{
			//Gave a negative or zero limit
			limit = 10;
			kr.getLogger().warning(kr.getPluginPrefix() + " Limit is beyond bounds. Using default.");
		}
	}
}
