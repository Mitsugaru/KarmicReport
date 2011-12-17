/**
 * This class represents an individual report object
 *
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicReport;

import org.bukkit.ChatColor;

public class Report {
	//Class variables
	public String author, comment, date;
	public double x, y, z;
	public boolean location;
	public int id;

	public Report(String a, String c, String d)
	{
		author = a;
		comment = c;
		date = d;
		location = false;
	}

	public Report(String a, String c, String d, double locx, double locy, double locz)
	{
		author = a;
		comment = c;
		date = d;
		x = locx;
		y = locy;
		z = locz;
		location = true;
	}

	public int getID()
	{
		return id;
	}

	public void setID(int i)
	{
		id = i;
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
            string = string.replace(String.format("&%x", color.getCode()), color.toString());
        }
        return string;
    }
}
