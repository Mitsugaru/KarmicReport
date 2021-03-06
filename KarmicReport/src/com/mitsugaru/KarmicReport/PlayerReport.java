package com.mitsugaru.KarmicReport;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import lib.PatPeter.SQLibrary.Database.Query;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerReport {
	// Class variables
	private final KarmicReport kr;
	private final CommandSender sender;
	private final Map<Integer, Report> rep = new LinkedHashMap<Integer, Report>();
	private int page;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	private String name;

	public PlayerReport(KarmicReport plugin, CommandSender s, String playername) {
		// Instantiate variables
		kr = plugin;
		sender = s;
		name = playername;
		page = 0;
		this.displayReports();
	}

	//TODO page method
	public void changePage(int adjust) {
		// Grab number of pages
		int num = rep.size() / kr.getPluginConfig().limit;
		double rem = (double) rep.size() % (double) kr.getPluginConfig().limit;
		if (rem != 0)
		{
			num++;
		}

		// Bounds check
		page += adjust;
		boolean valid = true;
		{
			if (page < 0)
			{
				// Tried to use previous on page 0
				// reset their current page back to 0
				page = 0;
				valid = false;
			}
			else if ((page * kr.getPluginConfig().limit) > rep.size())
			{
				// Tried to go beyond the page bounds
				page = num - 1;
				valid = false;
			}
		}
		if (valid)
		{
			displayReports();
		}
		else
		{
			sender.sendMessage(ChatColor.YELLOW + kr.getPluginPrefix()
					+ " Page does not exist");
		}
	}

	private void displayHeader() {
		// Header
		String query = "SELECT * FROM kr_masterlist WHERE playername='" + name
				+ "';";
		Query rs = kr.getLiteDB().select(query);

		// Caluclate amount of pages
		int num = rep.size() / kr.getPluginConfig().limit;
		double rem = (double) rep.size() % (double) kr.getPluginConfig().limit;
		if (rem != 0)
		{
			num++;
		}

		// parse query
		try
		{
			if (rs.getResult().next())
			{
				// Initial header with player name and status
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.LIGHT_PURPLE + "===" + ChatColor.WHITE
						+ name + ChatColor.LIGHT_PURPLE + "===");
				String status = rs.getResult().getString("status");
				if (status.equals("BANNED"))
				{
					sb.append(ChatColor.RED + status);
				}
				else if (status.equals("KICKED"))
				{
					sb.append(ChatColor.YELLOW + status);
				}
				else if (status.equals("OFFLINE"))
				{
					sb.append(ChatColor.GRAY + status);
				}
				else if (status.equals("ONLINE"))
				{
					sb.append(ChatColor.GREEN + status);
				}
				else
				{
					// Some other custom status is used...
					sb.append(ChatColor.AQUA + status);
				}
				sb.append(ChatColor.BLUE + ":" + ChatColor.RED + rs.getResult().getString("ip"));
				sb.append(ChatColor.LIGHT_PURPLE + "===");
				sender.sendMessage(sb.toString());
				// Second line with total number of infractions and page number
				sb = new StringBuilder();
				sb.append(ChatColor.GRAY + "" + rep.size() + " Reports"
						+ ChatColor.LIGHT_PURPLE + "===");
				sb.append(ChatColor.WHITE + "Page " + ChatColor.AQUA
						+ (page + 1) + ChatColor.WHITE + " of "
						+ ChatColor.AQUA + num + ChatColor.LIGHT_PURPLE + "===");
				sender.sendMessage(sb.toString());
			}
			else
			{
				sender.sendMessage(ChatColor.RED + kr.getPluginPrefix() + " "
						+ name + "'s record is missing...");
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public void displayReports() {
		// Update reports
		this.updateReports();
		// Header
		this.displayHeader();

		// Generate array
		Report[] array = rep.values().toArray(new Report[0]);
		// Display report summary
		int limit = kr.getPluginConfig().limit;
		for (int i = page * limit; i < ((page * limit) + limit); i++)
		{
			// Don't post if its beyond the length of the array
			if (i < array.length)
			{
				sender.sendMessage(ChatColor.WHITE + "" + (i + 1) + ". "
						+ array[i].summary());
			}
			else
			{
				break;
			}
		}
	}

	private void updateReports() {
		// Create SQL query to see if item is already in
		// database
		String query = "SELECT * FROM kr_reports WHERE playername='" + name
				+ "' ORDER BY id DESC;";
		Query rs = kr.getLiteDB().select(query);

		// Parse query
		try
		{
			if (rs.getResult().next())
			{
				do
				{
					final int id = rs.getResult().getInt("id");
					Report r;
					final int x = rs.getResult().getInt("x");
					// Determine if report has a location or not
					if (rs.getResult().wasNull())
					{
						r = new Report(rs.getResult().getString("author"),
								rs.getResult().getString("comment"), rs.getResult().getString("date"));
					}
					else
					{
						World w = kr.getServer()
								.getWorld(rs.getResult().getString("world"));
						// Check if world exists
						if (w != null)
						{
							r = new Report(rs.getResult().getString("author"),
									rs.getResult().getString("comment"),
									rs.getResult().getString("date"), w, x, rs.getResult().getInt("y"),
									rs.getResult().getInt("z"));
						}
						else
						{
							// world no longer available, so add as regular
							r = new Report(rs.getResult().getString("author"),
									rs.getResult().getString("comment"),
									rs.getResult().getString("date"));
						}
					}
					// Add generated report to list
					rep.put(id, r);
					// Add id to report as well
					r.setID(id);
				}
				while (rs.getResult().next());
				rs.closeQuery();
			}
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public void addReport(Report in) {
		// Add to sql
		String query = "INSERT INTO 'kr_reports' (playername,author,date,comment) VALUES('"
				+ name
				+ "','"
				+ in.author
				+ "','"
				+ in.date
				+ "','"
				+ in.comment + "');";
		kr.getLiteDB().standardQuery(query);

		// Query the added report
		query = "SELECT * FROM kr_reports WHERE playername='" + name
				+ "' AND author ='" + in.author + "' AND date='" + in.date
				+ "';";
		Query rs = kr.getLiteDB().select(query);
		try
		{
			if (rs.getResult().next())
			{
				int id = rs.getResult().getInt("id");
				// Add report to map using the rowid
				rep.put(id, in);
				// Add id to report as well
				in.setID(id);
			}
			else
			{
				sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
						+ " Could not retrieve report...");
			}
			rs.closeQuery();
			sender.sendMessage(ChatColor.GREEN + kr.getPluginPrefix()
					+ " Added comment to report.");
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public void amendReport(int id, String comment) {
		try
		{
			// Grab specific report
			Report[] array = rep.values().toArray(new Report[0]);
			Report r = array[id - 1];
			// Grab its unique id
			int rowid = r.getID();
			// Update report
			if (sender.getName().equals(r.author))
			{
				// Comment being appended by original author
				r.comment += ChatColor.BLUE + " EDIT:" + ChatColor.GRAY
						+ comment;
			}
			else
			{
				// Being appended by different author
				r.comment += ChatColor.BLUE + " EDIT-" + ChatColor.RED
						+ sender.getName() + ChatColor.BLUE + ":"
						+ ChatColor.GRAY + comment;
			}
			// Update database
			String query = "UPDATE kr_reports SET comment='" + r.comment
					+ "' WHERE id='" + rowid + "' AND playername='" + name
					+ "';";
			kr.getLiteDB().standardQuery(query);
			sender.sendMessage(ChatColor.GREEN + kr.getPluginPrefix()
					+ " Added to comment in report.");
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " Report " + id + " does not exist");
		}
	}

	public void removeReport(int id) {
		try
		{
			// Grab specific report
			Report[] array = rep.values().toArray(new Report[0]);
			Report r = array[id - 1];
			// Grab its unique id
			int rowid = r.getID();
			// Remove from hashmap
			rep.remove(rowid);
			// Remove from database
			String query = "DELETE FROM kr_reports WHERE playername='" + name
					+ "' AND id='" + rowid + "';";
			kr.getLiteDB().standardQuery(query);
			sender.sendMessage(ChatColor.GREEN + kr.getPluginPrefix()
					+ " Removed comment from report.");
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " Report " + id + " does not exist");
		}
	}

	public void addLocation(int id) {
		if (sender instanceof Player)
		{
			Player p = (Player) sender;
			try
			{
				// Grab specific report
				Report[] array = rep.values().toArray(new Report[0]);
				Report r = array[id - 1];
				// Grab its unique id
				int rowid = r.getID();
				// Update report
				r.setLocation(p.getLocation());
				// Update database
				String query = "UPDATE kr_reports SET world='"
						+ p.getLocation().getWorld().getName().toString()
						+ "', x='" + p.getLocation().getX() + "', y='"
						+ p.getLocation().getY() + "', z='"
						+ p.getLocation().getZ() + "' WHERE id='" + rowid
						+ "' AND playername='" + name + "';";
				kr.getLiteDB().standardQuery(query);
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
						+ " Report " + id + " does not exist");
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " Cannot add location as console");
		}
	}

	public void addReport(String name, String comment) {
		final Report r = new Report(name, comment, dateFormat
				.format(new Date()).toString());
		this.addReport(r);
	}

	public Report getReport(int id) {
		Report r = null;
		try
		{
			// Grab specific report
			Report[] array = rep.values().toArray(new Report[0]);
			r = array[id - 1];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			sender.sendMessage(ChatColor.RED + kr.getPluginPrefix()
					+ " Report " + id + " does not exist");
		}
		return r;
	}

	public void getEntry(int num) {
		// Grab specific report
		Report[] array = rep.values().toArray(new Report[0]);
		// TODO bounds check
		final Report r = array[num - 1];
		StringBuilder sb = new StringBuilder();
		// Initial header
		sb.append(ChatColor.LIGHT_PURPLE + "===" + ChatColor.WHITE + "Report #"
				+ num + ChatColor.LIGHT_PURPLE + "===");
		sb.append(ChatColor.GOLD + r.date + ChatColor.LIGHT_PURPLE + "===");
		sender.sendMessage(sb.toString());
		// Author and optional location if given
		sb = new StringBuilder();
		sb.append(ChatColor.LIGHT_PURPLE + "===" + ChatColor.WHITE + "Author: "
				+ ChatColor.RED + r.author + ChatColor.LIGHT_PURPLE + "===");
		if (r.hasLocation)
		{
			DecimalFormat twoDForm = new DecimalFormat("#.##");
			sb.append(ChatColor.GREEN + ""
					+ r.location.getWorld().getName().toString());
			sb.append(ChatColor.LIGHT_PURPLE + "@");
			sb.append(ChatColor.BLUE + "(" + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(r.location.getX()))
					+ ChatColor.BLUE + "," + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(r.location.getY()))
					+ ChatColor.BLUE + "," + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(r.location.getZ()))
					+ ChatColor.BLUE + ")");
			sb.append(ChatColor.LIGHT_PURPLE + "===");
		}
		else
		{
			sb.append(ChatColor.GOLD + "No Location" + ChatColor.LIGHT_PURPLE
					+ "===");
		}
		sender.sendMessage(sb.toString());
		// Split lines if necessary
		if (r.comment.length() > 65)
		{
			String[] lines = this.splitByLength(r.comment, 65);
			for (int i = 0; i < lines.length; i++)
			{
				sender.sendMessage(ChatColor.GRAY + colorizeText(lines[i]));
			}
		}
		else
		{
			sender.sendMessage(ChatColor.GRAY + r.getComment());
		}
	}

	private String[] splitByLength(String s, int chunk) {
		int arraySize = (int) Math.ceil((double) s.length() / chunk);
		String[] array = new String[arraySize];
		int index = 0;
		for(int i = 0; i < s.length(); i = i+ chunk)
		{
			if(s.length() - i < chunk)
			{
				array[index++] = s.substring(i);
			}
			else
			{
				array[index++] = s.substring(i, i+chunk);
			}
		}
		return array;
	}

	/**
     * Colorizes a given string to Bukkit standards
     * @param string
     * @return String with appropriate Bukkit ChatColor in them
     * @author Coryf88
     */
    public String colorizeText(String string) {
        for (ChatColor color : ChatColor.values()) {
            string = string.replace(String.format("&%x", color.getChar()), color.toString());
        }
        return string;
    }
}
