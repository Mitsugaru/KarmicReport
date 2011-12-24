package com.mitsugaru.KarmicReport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class PlayerReport {
	//Class variables
	private final KarmicReport kr;
	private final CommandSender sender;
	private final Map<Integer, Report> rep = new HashMap<Integer, Report>();
	private int page;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	private String name;

	public PlayerReport(KarmicReport plugin, CommandSender s, String playername)
	{
		//Instantiate variables
		kr = plugin;
		sender = s;
		name = playername;
		page = 0;
		this.displayReports();
	}

	public void changePage(int adjust)
	{
		//Grab number of pages
		int num = rep.size() / kr.getPluginConfig().limit;
		double rem = (double) rep.size() % (double) kr.getPluginConfig().limit;
		if (rem != 0)
		{
			num++;
		}

		//Bounds check
		page += adjust;
		boolean valid = true;
		{
			if(page < 0)
			{
				//Tried to use previous on page 0
				// reset their current page back to 0
				page = 0;
				valid = false;
			}
			else if((page * kr.getPluginConfig().limit) > rep.size())
			{
				//Tried to go beyond the page bounds
				page = num - 1;
				valid = false;
			}
		}
		if(valid)
		{
			displayReports();
		}
		else
		{
			sender.sendMessage(ChatColor.YELLOW + kr.getPluginPrefix()
						+ " Page does not exist");
		}
	}

	private void displayHeader()
	{
		//Header
		String query = "SELECT * FROM kr_masterlist WHERE playername='"
				+ name + "';";
		ResultSet rs = kr.getLiteDB().select(query);

		// Caluclate amount of pages
		int num = rep.size() / kr.getPluginConfig().limit;
		double rem = (double) rep.size() % (double) kr.getPluginConfig().limit;
		if (rem != 0)
		{
			num++;
		}

		//parse query
		try
		{
			if(rs.next())
			{
				//Initial header with player name and status
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.LIGHT_PURPLE
						+ "===" + ChatColor.WHITE + name + ChatColor.LIGHT_PURPLE + "===");
				String status = rs.getString("status");
				if(status.equals("BANNED"))
				{
					sb.append(ChatColor.RED + status);
				}
				else if(status.equals("KICKED"))
				{
					sb.append(ChatColor.YELLOW + status);
				}
				else if(status.equals("OFFLINE"))
				{
					sb.append(ChatColor.GRAY + status);
				}
				else if(status.equals("ONLINE"))
				{
					sb.append(ChatColor.GREEN + status);
				}
				else
				{
					//Some other custom status is used...
					sb.append(ChatColor.AQUA +  status);
				}
				sb.append(ChatColor.LIGHT_PURPLE + "===");
				sender.sendMessage(sb.toString());
				//Second line with total number of infractions and page number
				sb = new StringBuilder();
				sb.append(ChatColor.GRAY + ""+ rep.size() +" Reports" + ChatColor.LIGHT_PURPLE + "===");
				sb.append(ChatColor.WHITE + "Page " + ChatColor.AQUA+ (page+1) + ChatColor.WHITE +" of " + ChatColor.AQUA + num + ChatColor.LIGHT_PURPLE + "===");
				sender.sendMessage(sb.toString());
			}
			else
			{
				sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
						+ " " + name + "'s record is missing...");
			}
			rs.close();
		}
		catch(SQLException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public void displayReports() {
		//Update reports
		this.updateReports();
		//Header
		this.displayHeader();

		//Generate array
		Report[] array = rep.values().toArray(new Report[0]);
		//Display report summary
		int limit = kr.getPluginConfig().limit;
		for(int i = page * limit; i < ((page * limit) + limit); i++)
		{
			//Don't post if its beyond the length of the array
			if(i < array.length)
			{
				sender.sendMessage(ChatColor.WHITE +""+ (i+1) +". "+array[i].summary());
			}
			else
			{
				break;
			}
		}
	}

	private void updateReports()
	{
		// Create SQL query to see if item is already in
		// database
		String query = "SELECT * FROM kr_reports WHERE playername='"
				+ name + "' ORDER BY id DESC;";
		ResultSet rs = kr.getLiteDB().select(query);

		//Parse query
		try
		{
			if(rs.next())
			{
				do
				{
					final int id = rs.getInt("id");
					Report r;
					final int x = rs.getInt("x");
					//Determine if report has a location or not
					if(rs.wasNull())
					{
						r = new Report(rs.getString("author"), rs.getString("comment"), rs.getString("date"));
					}
					else
					{
						r = new Report(rs.getString("author"), rs.getString("comment"), rs.getString("date"), x, rs.getInt("y"), rs.getInt("z"));
					}
					//Add generated report to list
					rep.put(id, r);
					//Add id to report as well
					r.setID(id);
				}
				while (rs.next());
				rs.close();
			}
		}
		catch(SQLException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public void addReport(Report in)
	{
		//Add to sql
		String query = "INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('"+name+"','" + in.author +"','" +in.date+ "','" + in.comment+"');";
		kr.getLiteDB().standardQuery(query);

		//Query the added report
		query = "SELECT * FROM kr_reports WHERE playername='"
				+ name + "' AND author ='"+in.author + "' AND date='" + in.date + "';";
		ResultSet rs = kr.getLiteDB().select(query);
		try
		{
			if(rs.next())
			{
				int id = rs.getInt("id");
				//Add report to map using the rowid
				rep.put(id, in);
				//Add id to report as well
				in.setID(id);
			}
			else
			{
				sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
						+ " Could not retrieve report...");
			}
			rs.close();
		}
		catch(SQLException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public void amendReport(int id, String comment)
	{
		//Grab specific report
		Report[] array = rep.values().toArray(new Report[0]);
		Report r = array[id-1];
		//Grab its unique id
		int rowid = r.getID();
		//Update report
		if(sender.getName().equals(r.author))
		{
			//Comment being appended by original author
			r.comment += ChatColor.BLUE + " EDIT:" + ChatColor.GRAY + comment;
		}
		else
		{
			//Being appended by different author
			r.comment += ChatColor.BLUE + " EDIT-" + ChatColor.RED + sender.getName() + ChatColor.BLUE + ":" + ChatColor.GRAY + comment;
		}
		//Update database
		String query = "UPDATE kr_reports SET comment='"
				+ r.comment + "' WHERE id='"
				+ rowid + "' AND playername='"
				+ name + "';";
		kr.getLiteDB().standardQuery(query);
	}

	public void removeReport(int id)
	{
		//Grab specific report
		Report[] array = rep.values().toArray(new Report[0]);
		Report r = array[id-1];
		//Grab its unique id
		int rowid = r.getID();
		//Remove from hashmap
		rep.remove(rowid);
		//Remove from database
		String query = "DELETE FROM kr_reports WHERE playername='" + name + "' AND id='" + rowid + "';";
		kr.getLiteDB().standardQuery(query);
	}

	public void addReport(String name, String comment)
	{
		final Report r = new Report(name, comment, dateFormat.format(new Date()).toString());
		this.addReport(r);
	}

	public void getReport(int i)
	{

	}

	public void getEntry(int num) {
		//Grab specific report
		Report[] array = rep.values().toArray(new Report[0]);
		//TODO bounds check
		final Report r = array[num-1];
		StringBuilder sb = new StringBuilder();
		//Initial header
		sb.append(ChatColor.LIGHT_PURPLE
				+ "===" + ChatColor.WHITE + "Report #" + num + ChatColor.LIGHT_PURPLE + "===");
		sb.append(ChatColor.GOLD + r.date + ChatColor.LIGHT_PURPLE + "===");
		sender.sendMessage(sb.toString());
		//Author and optional location if given
		sb = new StringBuilder();
		sb.append(ChatColor.LIGHT_PURPLE + "===" + ChatColor.WHITE + "Author: " + ChatColor.RED + r.author + ChatColor.LIGHT_PURPLE + "===");
		if(r.location)
		{
			sb.append(ChatColor.GOLD + "(" + r.x + "," + r.y + "," + r.z + ")");
			sb.append(ChatColor.LIGHT_PURPLE + "===");
		}
		else
		{
			sb.append(ChatColor.GOLD + "No Location" + ChatColor.LIGHT_PURPLE + "===");
		}
		sender.sendMessage(sb.toString());
		//Split lines if necessary
		if(r.comment.length() > 65)
		{
			String[] lines = r.getComment().split("", 65);
			for(int i = 0; i < lines.length; i++)
			{
				sender.sendMessage(ChatColor.GRAY + lines[i]);
			}
		}
		else
		{
			sender.sendMessage(ChatColor.GRAY + r.getComment());
		}
	}
}
