package com.mitsugaru.KarmicReport;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mitsugaru.karmicjail.JailEvent;

public class KarmicJailListener implements Listener {

	private KarmicReport plugin;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public KarmicJailListener(KarmicReport kr) {
		plugin = kr;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJailEvent(final JailEvent event)
	{
		// Grab dates
		final String date = dateFormat.format(new Date()).toString();
		// Create query
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('");
		sb.append(event.name + "','" + event.jailer + "','" + date + "'");

		if (event.reason.equals(""))
		{
			// Default reason
			sb.append(",'&4Jailed');");
		}
		else
		{
			// Add reason
			sb.append(",'&4Jailed &7for: " + event.reason + "');");
		}
		// Send query
		plugin.getLiteDB().standardQuery(sb.toString());
	}
}
