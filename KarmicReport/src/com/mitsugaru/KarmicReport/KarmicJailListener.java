package com.mitsugaru.KarmicReport;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.event.CustomEventListener;

import com.mitsugaru.karmicjail.JailEvent;

public class KarmicJailListener extends CustomEventListener {

	private KarmicReport plugin;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public KarmicJailListener(KarmicReport kr)
	{
		plugin = kr;
	}

	public void onJailEvent(JailEvent event)
	{
			//Grab dates
			final String date = dateFormat.format(new Date()).toString();
			//Create query
			StringBuilder sb = new StringBuilder();
			sb.append("INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('");
			sb.append(event.name+"','" + event.jailer +"','" +date + "'");
			if(event.reason.equals(""))
			{
				//Default reason
				sb.append(",'Jailed'");
			}
			else
			{
				//Add reasons
				sb.append(",'" + event.reason + "');");
			}
			//Send query
			plugin.getLiteDB().standardQuery(sb.toString());
		}
}
