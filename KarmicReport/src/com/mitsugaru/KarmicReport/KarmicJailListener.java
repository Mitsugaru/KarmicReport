package com.mitsugaru.KarmicReport;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import com.mitsugaru.karmicjail.JailEvent;

public class KarmicJailListener extends CustomEventListener implements Listener {

	private KarmicReport plugin;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public KarmicJailListener(KarmicReport kr)
	{
		plugin = kr;
	}

	public void onKarmicJailPlayerJailed(Event event)
	{
		//Make sure its a JailEvent
		if(event instanceof JailEvent)
		{
			//Cast
			JailEvent je = (JailEvent) event;
			//Grab dates
			final String date = dateFormat.format(new Date()).toString();
			//Create query
			StringBuilder sb = new StringBuilder();
			sb.append("INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('");
			sb.append(je.name+"','" + je.jailer +"','" +date + "'");
			if(je.reason.equals(""))
			{
				//Default reason
				sb.append(",'Jailed'");
			}
			else
			{
				//Add reasons
				sb.append(",'" + je.reason + "');");
			}
			//Send query
			plugin.getLiteDB().standardQuery(sb.toString());
		}
	}
}
