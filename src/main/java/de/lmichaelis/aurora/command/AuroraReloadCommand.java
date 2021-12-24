// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A command to reload Aurora's configuration.
 */
public class AuroraReloadCommand extends AuroraBaseCommand {
	public AuroraReloadCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(final CommandSender sender) {
		return sender.hasPermission("aurora.reload");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		// Do the reloading
		plugin.onReload();
		sender.sendMessage("§aReloaded.");
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return List.of();
	}
}
