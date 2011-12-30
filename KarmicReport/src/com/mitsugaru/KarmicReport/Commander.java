package com.mitsugaru.KarmicReport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commander implements CommandExecutor {
	// Class variables
	private final KarmicReport kr;
	private final String prefix;
	private final Config config;
	private final PermCheck perm;
	private Map<String, PlayerReport> lookup = new HashMap<String, PlayerReport>();
	private final static String BAR = "======================";

	public Commander(KarmicReport karmicReport) {
		kr = karmicReport;
		prefix = kr.getPluginPrefix();
		config = kr.getPluginConfig();
		perm = kr.getPermissionHandler();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLebel, String[] args) {
		// Process time for debugging
		long time = 0;
		if (config.debugTime)
		{
			time = System.nanoTime();
		}
		// See if arguments were given
		if (args.length == 0)
		{
			// No arguments -> Open up last report
			// Permission check
			if (perm.checkPermission(sender, "KarmicReport.view"))
			{
				// Check if a report is already open
				if (lookup.containsKey(sender.getName()))
				{
					lookup.get(sender.getName()).displayReports();
				}
				else
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " No report open.");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + prefix
						+ " Lack permission: KarmicReport.view");
			}
		}
		else
		{
			// There were arguments given, attempt to parse
			final String com = args[0].toLowerCase();

			// Check if version command
			if (com.equals("version") || com.equals("ver"))
			{
				// Version and author
				sender.sendMessage(ChatColor.BLUE + BAR + "=====");
				sender.sendMessage(ChatColor.GREEN + "KarmicReport v"
						+ kr.getDescription().getVersion());
				sender.sendMessage(ChatColor.GREEN + "Coded by Mitsugaru");
				sender.sendMessage(ChatColor.BLUE + BAR + "=====");
			}
			else if (com.equals("help") || com.equals("?"))
			{
				this.showHelp(sender);
			}
			// Check if its admin command
			else if (com.equals("admin"))
			{
				// Check permissions
				if (perm.checkPermission(sender, "KarmicReport.admin"))
				{
					if (args.length > 1)
					{
						// They have a parameter, thus
						// parse in adminCommand method
						if (this.adminCommand(sender, args))
						{
							if (config.debugTime)
							{
								debugTime(sender, time);
							}
							return true;
						}
					}
					else
					{
						//Show admin help
						// Show admin commands help menu
						sender.sendMessage(ChatColor.BLUE + "==="
								+ ChatColor.RED + "KarmicReport Admin"
								+ ChatColor.BLUE + "===");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED
							+ " Lack permission: KarmicReport.admin");
				}
			}
			// Check if its reload comand
			else if (com.equals("reload"))
			{
				// Check permissions
				if (perm.checkPermission(sender, "KarmicReport.admin"))
				{
					config.reload();
				}
				else
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " Lack permission: KarmicReport.admin");
				}
			}
			//Previous page command
			else if (com.equals("prev"))
			{
				if (lookup.containsKey(sender.getName()))
				{
					lookup.get(sender.getName()).changePage(-1);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " No report to page through.");
				}
				if (config.debugTime)
				{
					debugTime(sender, time);
				}
				return true;
			}
			//Next page command
			else if (com.equals("next"))
			{
				if (lookup.containsKey(sender.getName()))
				{
					lookup.get(sender.getName()).changePage(1);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " No report to page through.");
				}
			}
			//Page command
			else if (com.equals("page"))
			{
				if (lookup.containsKey(sender.getName()))
				{
					// Check if they gave a page number
					if (args.length > 1)
					{
						int num = 0;
						try
						{
							// Parse entry number
							num = Integer.parseInt(args[1]);
							lookup.get(sender.getName()).changePage(num);
						}
						catch (NumberFormatException e)
						{
							sender.sendMessage(ChatColor.RED + prefix
									+ " Not a number.");
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED + prefix
								+ " Missing page number.");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " No report to page through.");
				}
			}
			//Add command
			else if (com.equals("new") || com.equals("add"))
			{
				this.addComment(sender, args);
			}
			//Remove command
			else if (com.equals("remove") || com.equals("delete"))
			{
				this.removeComment(sender, args);
			}
			//Edit command
			else if(com.equals("append") || com.equals("amend") || com.equals("edit"))
			{
				this.amendComment(sender, args);
			}
			//Warp command
			else if(com.equals("tp") || com.equals("warp"))
			{
				this.warpToCommentLocation(sender, args);
			}
			else if(com.equals("gps") || com.equals("pos"))
			{
				this.addWarp(sender, args);
			}
			//View command
			else if (com.equals("view") || com.equals("check"))
			{
				this.viewComment(sender, args);
			}
			else
			{
				this.viewReport(sender, args);
			}
		}

		if (config.debugTime)
		{
			debugTime(sender, time);
		}
		return true;
	}

	private void addWarp(CommandSender sender, String[] args) {
		if(perm.checkPermission(sender, "KarmicReport.edit"))
		{
			//Check if comment id is given
			if(args.length > 1)
			{
				try
				{
					int id = Integer.parseInt(args[1]);
					final String name = sender.getName();
					if (lookup.containsKey(name))
					{

						lookup.get(name).addLocation(id);
						sender.sendMessage(ChatColor.GREEN + prefix
								+ " Added current location to comment " + id +".");
					}
					else
					{
						sender.sendMessage(ChatColor.RED
								+ prefix
								+ " No report to attach to. Look up a player's report first.");
					}
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " Given comment id is not an integer");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + prefix
						+ " No comment number given");
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + prefix
					+ " Lack permission: KarmicReport.edit");
		}
	}

	private void warpToCommentLocation(CommandSender sender, String[] args) {
		if(perm.checkPermission(sender, "KarmicReport.warp"))
		{
			//Check if player
			if(sender instanceof Player)
			{
				Player p = (Player) sender;
				//Check if comment id is given
				if(args.length > 1)
				{
					try
					{
						int id = Integer.parseInt(args[1]);
						final String name = sender.getName();
						if (lookup.containsKey(name))
						{
							//Check if report exists
							final Report r = lookup.get(name).getReport(id);
							if(r != null)
							{
								//Check if report has a location
								if(r.hasLocation)
									{
									//Check if world exists
									World w = kr.getServer().getWorld(r.location.getWorld().getName().toString());
									if(w != null)
									{
										p.teleport(r.location);
										sender.sendMessage(ChatColor.GREEN +  prefix
												+ " Warped to location");
									}
									else
									{
										sender.sendMessage(ChatColor.RED + prefix
												+ " World '" + r.location.getWorld().toString() + "' is not available");
									}
								}
								else
								{
									sender.sendMessage(ChatColor.RED + prefix
											+ " Report does not have a location attached");
								}
							}
						}
						else
						{
							sender.sendMessage(ChatColor.RED
									+ prefix
									+ " No report to attach to. Look up a player's report first.");
						}
					}
					catch(NumberFormatException e)
					{
						sender.sendMessage(ChatColor.RED +  prefix
								+ " Given comment id is not an integer");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " No comment number given");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + prefix
						+ " Cannot warp as console");
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + prefix
					+ " Lack permission: KarmicReport.warp");
		}
	}

	private void amendComment(CommandSender sender, String[] args)
	{
		if (perm.checkPermission(sender, "KarmicReport.edit"))
		{
			//Check if comment id is given
			if(args.length > 1)
			{
				try
				{
					int id = Integer.parseInt(args[1]);
					if(args.length > 2)
					{
						//Grab comment to add
						StringBuffer sb = new StringBuffer();
						for(int i = 2; i < args.length; i++)
						{
							sb.append(args[i] + " ");
						}
						String comment = sb.toString();
						final String name = sender.getName();
						if (lookup.containsKey(name))
						{
							if(comment.contains("\"") || comment.contains("'"))
							{
								sender.sendMessage(ChatColor.RED
										+ prefix
										+ " Illegal characters: \" or '");
							}
							else
							{
								lookup.get(name).amendReport(id, comment);
							}
						}
						else
						{
							sender.sendMessage(ChatColor.RED
									+ prefix
									+ " No report to attach to. Look up a player's report first.");
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED + prefix
								+ " Nothing to append");
					}

				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " Given comment id is not an integer");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + prefix
						+ " No comment number given");
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + prefix
					+ " Lack permission: KarmicReport.edit");
		}
	}

	private void removeComment(CommandSender sender, String[] args) {
		if (perm.checkPermission(sender, "KarmicReport.edit"))
		{
			//Check if comment id is given
			if(args.length > 1)
			{
				try
				{
					int id = Integer.parseInt(args[1]);
					final String name = sender.getName();
					if (lookup.containsKey(name))
					{

						lookup.get(name).removeReport(id);
					}
					else
					{
						sender.sendMessage(ChatColor.RED
								+ prefix
								+ " No report to attach to. Look up a player's report first.");
					}
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " Given comment id is not an integer");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + prefix
						+ " No comment number given");
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + prefix
					+ " Lack permission: KarmicReport.edit");
		}
	}

	private void addComment(CommandSender sender, String[] args) {
		if (perm.checkPermission(sender, "KarmicReport.edit"))
		{
			// Intend to add a comment
			final String name = sender.getName();
			if (lookup.containsKey(name))
			{
				// Sender has a previous lookup, attach to that player's profile
				// Get comment string to add
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < args.length; i++)
				{
					sb.append(args[i] + " ");
				}
				String comment = sb.toString();
				comment = comment.replaceAll("\\s+$", "");
				if(comment.contains("\"") || comment.contains("'"))
				{
					sender.sendMessage(ChatColor.RED
							+ prefix
							+ " Illegal characters: \" or '");
				}
				else
				{
					lookup.get(name).addReport(name, comment);
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED
						+ prefix
						+ " No report to attach to. Look up a player's report first.");
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + prefix
					+ " Lack permission: KarmicReport.edit");
		}
	}

	private void viewReport(CommandSender sender, String[] args) {
		// TODO commands were given after the name?
		// Name given, potentially
		String name = args[0];
		// Attempt to expand name based on online players
		name = expandName(name);
		if (name == null)
		{
			// Expansion didn't find a name, revert
			name = args[0];
		}
		// See if name matches master list
		String query = "SELECT COUNT(*) FROM 'kr_masterlist' WHERE playername='"
				+ name + "';";
		ResultSet rs = kr.getLiteDB().select(query);
		try
		{
			boolean has = false;
			if (rs.next())
			{
				if (rs.getInt(1) == 1)
				{
					// Found exactly one person
					has = true;
				}
				else if (rs.getInt(1) > 1)
				{
					// Found one than more match
					// TODO handle more than one match in database
					// possibly implement like? IDK Or give list of names
					sender.sendMessage(ChatColor.YELLOW + prefix
							+ " TODO: more than one match in database");
				}
			}
			rs.close();
			if (has)
			{
				lookup.put(sender.getName(), new PlayerReport(kr, sender, name));
			}
			else
			{
				// Player not in database
				sender.sendMessage(ChatColor.RED + prefix + " Player '" +ChatColor.GOLD + name
						+ ChatColor.RED + "' is not in database. Note: Names are case-sensitive");
			}
		}
		catch (SQLException e)
		{
			kr.getLogger().warning(ChatColor.RED + prefix + " SQL Exception");
			e.printStackTrace();
		}
	}

	private void viewComment(CommandSender sender, String[] args) {
		// Permission check
		if (perm.checkPermission(sender, "KarmicReport.view"))
		{
			// Check if a report is already open
			if (lookup.containsKey(sender.getName()))
			{
				// Check if there is another argument
				if (args.length > 1)
				{
					// Parse entry number
					int num = 0;
					try
					{
						// Parse entry number
						num = Integer.parseInt(args[1]);
						lookup.get(sender.getName()).getEntry(num);
					}
					catch (NumberFormatException e)
					{
						sender.sendMessage(ChatColor.RED + prefix
								+ " Not a number.");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + prefix
							+ " Missing entry number.");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + prefix + " No report open.");
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + prefix
					+ " Lack permission: KarmicReport.view");
		}
	}

	private void showHelp(CommandSender sender) {
		// TODO Auto-generated method stub

	}

	private boolean adminCommand(CommandSender sender, String[] args) {
		// String com = args[1];
		// TODO admin commands
		return true;
	}

	private void debugTime(CommandSender sender, long time) {
		time = System.nanoTime() - time;
		sender.sendMessage("[Debug]" + prefix + "Process time: " + time);
	}

	/**
	 * Attempts to look up full name based on who's on the server Given a partial
	 * name
	 *
	 * @author Frigid, edited by Raphfrk and petteyg359
	 */
	public String expandName(String Name) {
		int m = 0;
		String Result = "";
		for (int n = 0; n < kr.getServer().getOnlinePlayers().length; n++)
		{
			String str = kr.getServer().getOnlinePlayers()[n].getName();
			if (str.matches("(?i).*" + Name + ".*"))
			{
				m++;
				Result = str;
				if (m == 2)
				{
					return null;
				}
			}
			if (str.equalsIgnoreCase(Name))
				return str;
		}
		if (m == 1)
			return Result;
		if (m > 1)
		{
			return null;
		}
		return Name;
	}
}
