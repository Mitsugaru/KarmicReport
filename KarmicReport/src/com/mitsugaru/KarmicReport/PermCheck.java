package com.mitsugaru.KarmicReport;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * Class to handle permission node checks.
 * Mostly only to support PEX natively, due to
 * SuperPerm compatibility with PEX issues.
 *
 * @author Mitsugaru
 *
 */
public class PermCheck {
	private Permission perm;
	private boolean hasVault;

	/**
	 * Constructor
	 * May not really be needed. Had thought I needed it
	 * earlier, but now... meh.
	 */
	public PermCheck(KarmicReport kr)
	{
		if(kr.getServer().getPluginManager().getPlugin("Vault") != null)
		{
			hasVault = true;
			RegisteredServiceProvider<Permission> permissionProvider = kr
				.getServer()
				.getServicesManager()
				.getRegistration(net.milkbowl.vault.permission.Permission.class);
			if (permissionProvider != null)
			{
				perm = permissionProvider.getProvider();
			}
		}
		else
		{
			hasVault = false;
		}

	}

	/**
	 *
	 * @param CommandSender that sent command
	 * @param Permission node to check, as String
	 * @return true if sender has the node, else false
	 */
	public boolean checkPermission(CommandSender sender, String node)
	{
		if(hasVault)
		{
			return perm.has(sender, node);
		}
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("PermissionsEx"))
		{
			//Pex only supports player check, no CommandSender objects
			if(sender instanceof Player)
			{
				final Player p = (Player) sender;
				final PermissionManager permissions = PermissionsEx.getPermissionManager();
				//Handle pex check
				if(permissions.has(p, node))
				{
					return true;
				}
			}
		}
		//If not using PEX / Vault, OR if sender is not a player (in PEX only case)
		//Attempt to use SuperPerms
		if(sender.hasPermission(node))
		{
			return true;
		}
		//Else, they don't have permission
		return false;
	}
}
