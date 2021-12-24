// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Interface for declaring commands that support tab completion.
 */
public abstract class AuroraBaseCommand implements CommandExecutor, TabCompleter {
	protected final Aurora plugin;

	public AuroraBaseCommand(final Aurora plugin) {
		this.plugin = plugin;
	}

	/**
	 * Checks whether the given sender has permission to run the command.
	 *
	 * @param sender The sender to check.
	 * @return <tt>true</tt> if the sender can run the command, <tt>false</tt> if not.
	 */
	public abstract boolean hasPermission(final CommandSender sender);
}
