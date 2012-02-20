/**
 * This class represents an individual report object
 *
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicReport;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

public class Report {
	//Class variables
	public String author, comment, date;
	public Location location;
	public boolean hasLocation;
	public int id;

	public Report(String a, String c, String d)
	{
		author = a;
		comment = c;
		date = d;
		hasLocation = false;
	}

	public Report(String a, String c, String d, World world, double locx, double locy, double locz)
	{
		author = a;
		comment = c;
		date = d;
		location = new Location(world, locx, locy, locz);
		hasLocation = true;
	}

	public int getID()
	{
		return id;
	}

	public void setID(int i)
	{
		id = i;
	}

	public void setLocation(Location l)
	{
		location = l;
		hasLocation = true;
	}

	/**
	 * Produces a summary of the report
	 * @return String of a limited view of the report
	 */
	public String summary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.RED + author);
		sb.append(ChatColor.BLUE + "-" + ChatColor.GOLD + date.substring(5, 10));
		sb.append(ChatColor.BLUE + "-" + ChatColor.GRAY + colorizeText(comment));
		if(sb.length() > 65)
		{
			return sb.toString().substring(0, 65) + "...";
		}
		return sb.toString();
	}

	public String getComment()
	{
		return colorizeText(this.comment);
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
