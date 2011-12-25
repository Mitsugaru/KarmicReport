package com.mitsugaru.KarmicReport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Listener extends PlayerListener {
	// Class variables
	private final KarmicReport kr;
	private final Config config;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public Listener(KarmicReport plugin) {
		// Instantiate variables
		kr = plugin;
		config = kr.getPluginConfig();
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		if(config.debug)
		{
			kr.getLogger().warning(kr.getPluginPrefix() + " PlayerQuit event");
		}
		final String name = event.getPlayer().getName();
		String query = "SELECT * FROM 'kr_masterlist' WHERE playername='"
				+ name + "';";
		ResultSet rs = kr.getLiteDB().select(query);
		try
		{
			boolean has = false;
			String status = "";
			if (rs.next())
			{
				// They're already in the database
				has = true;
				status = rs.getString("status");
			}
			rs.close();
			if(has)
			{
				if(config.debug)
				{
					kr.getLogger().warning(kr.getPluginPrefix() + " PlayerQuit - update status");
				}
				//if they were kicked/banned and ignore update
				if(!status.equals("BANNED") || !status.equals("KICKED"))
				{
					query = "UPDATE 'kr_masterlist' SET status='OFFLINE' WHERE playername='"
							+ name + "';";
					kr.getLiteDB().standardQuery(query);
				}
			}
		}
		catch (SQLException e)
		{
			kr.getLogger().warning(kr.getPluginPrefix() + " SQL Exception");
			e.printStackTrace();
		}
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		if(config.debug)
		{
			kr.getLogger().warning(kr.getPluginPrefix() + " PlayerJoin event");
		}
		String newip = event.getPlayer().getAddress().toString()
				.substring(1).split(":")[0];
		final String date = dateFormat.format(new Date()).toString();
		String query = "SELECT COUNT(*) FROM 'kr_masterlist' WHERE playername='"
				+ event.getPlayer().getName() + "';";
		ResultSet rs = kr.getLiteDB().select(query);
		try
		{
			boolean has = false;
			if (rs.next())
			{
				if (rs.getInt(1) >= 1)
				{
					// They're already in the database
					has = true;
				}
			}
			rs.close();
			if (!has)
			{
				if(config.debug)
				{
					kr.getLogger().warning(kr.getPluginPrefix() + " PlayerJoin - add new player");
				}
				// Add to master list
				//Thanks to @Baummann for the concise command for getting player ip
				query = "INSERT INTO 'kr_masterlist' VALUES('"
						+ event.getPlayer().getName()
						+ "','"
						+ date
						+ "','ONLINE','"
						+ newip + "');";
				kr.getLiteDB().standardQuery(query);
			}
			else
			{
				if(config.debug)
				{
					kr.getLogger().warning(kr.getPluginPrefix() + " PlayerJoin - update date");
				}
				// Update date
				query = "UPDATE 'kr_masterlist' SET date='"
						+ dateFormat.format(new Date()).toString()
						+ "' WHERE playername='" + event.getPlayer().getName()
						+ "';";
				kr.getLiteDB().standardQuery(query);
			}
			//Update status to online
			if(config.debug)
			{
				kr.getLogger().warning(kr.getPluginPrefix() + " PlayerJoin - update state");
			}
			query = "UPDATE 'kr_masterlist' SET status='ONLINE' WHERE playername='"
					+ event.getPlayer().getName() + "';";
			kr.getLiteDB().standardQuery(query);
			//Grab last ip
			if(config.ipchange)
			{
				if(config.debug)
				{
					kr.getLogger().warning(kr.getPluginPrefix() + " PlayerJoin - check ip");
				}
				query = "SELECT * FROM 'kr_masterlist' WHERE playername='"
					+ event.getPlayer().getName() + "';";
				rs = kr.getLiteDB().select(query);
				String ip = "";
				boolean ipChanged = false;
				if(rs.next())
				{
					ip = rs.getString("ip");
					if(!ip.equals(newip))
					{
						//Their current ip is different from the master list
						ipChanged= true;
					}
				}
				rs.close();
				if(ipChanged)
				{
					if(config.debug)
					{
						kr.getLogger().warning(kr.getPluginPrefix() + " PlayerJoin - new ip, update");
					}
					// Autolog kick reason to player's record
					query = "INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('"+event.getPlayer().getName()+"','IPCHANGE','" +date+ "','OLD: " + ip + " NEW: "+ newip+"');";
					kr.getLiteDB().standardQuery(query);
					//Update ip in masterlist
					query = "UPDATE 'kr_masterlist' SET ip='"
							+ newip + "' WHERE playername='" + event.getPlayer().getName() + "';";
					kr.getLiteDB().standardQuery(query);
				}
			}

		}
		catch (SQLException e)
		{
			kr.getLogger().warning(kr.getPluginPrefix() + " SQL Exception");
			e.printStackTrace();
		}
	}

	@Override
	public void onPlayerLogin(PlayerLoginEvent event) {
		if(config.debug)
		{
			kr.getLogger().warning(kr.getPluginPrefix() + " PlayerLogin event");
		}
		try
		{
			String query = "SELECT COUNT(*) FROM 'kr_masterlist' WHERE playername='"
				+ event.getPlayer().getName() + "';";
			ResultSet rs = kr.getLiteDB().select(query);
			boolean has = false;
			if (rs.next())
			{
				if (rs.getInt(1) != 0)
				{
					// They're already in the database
					has = true;
				}
			}
			rs.close();
			if (has && event.getResult() == PlayerLoginEvent.Result.KICK_BANNED)
			{
				if(config.debug)
				{
					kr.getLogger().warning(kr.getPluginPrefix() + " PlayerLogin - update state");
				}
				// Update their status to banned
				query = "UPDATE 'kr_masterlist' SET status='BANNED' WHERE playername='"
						+ event.getPlayer().getName() + "';";
				kr.getLiteDB().standardQuery(query);
			}
		}
		catch (SQLException e)
		{
			kr.getLogger().warning(kr.getPluginPrefix() + " SQL Exception");
			e.printStackTrace();
		}
	}

	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		if(config.debug)
		{
			kr.getLogger().warning(kr.getPluginPrefix() + " PlayerKick event");
		}
		final String date = dateFormat.format(new Date()).toString();
		if (event.getPlayer().isBanned() && config.ban)
		{
			if(config.debug)
			{
				kr.getLogger().warning(kr.getPluginPrefix() + " PlayerKick - set banned");
			}
			String query = "UPDATE 'kr_masterlist' SET status='BANNED' WHERE playername='"
					+ event.getPlayer().getName() + "';";
			kr.getLiteDB().standardQuery(query);
			// Autolog ban reason to player's record
			query = "INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('"+event.getPlayer().getName()+"','BAN','" +date+ "','" + event.getReason()+"');";
			kr.getLiteDB().standardQuery(query);
		}
		else if(config.kick)
		{
			if(config.debug)
			{
				kr.getLogger().warning(kr.getPluginPrefix() + " PlayerKick - set kicked");
			}
			String query = "UPDATE 'kr_masterlist' SET status='KICKED' WHERE playername='"
					+ event.getPlayer().getName() + "';";
			kr.getLiteDB().standardQuery(query);
			// Autolog kick reason to player's record
			query = "INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('"+event.getPlayer().getName()+"','KICK','" +date+ "','" + event.getReason()+"');";
			kr.getLiteDB().standardQuery(query);
		}
	}
}
