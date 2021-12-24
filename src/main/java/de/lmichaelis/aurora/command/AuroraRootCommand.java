// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Aurora's base command implementation. This implementation Handles all subcommands, running them
 * and proxying tab completion for them.
 */
public class AuroraRootCommand extends AuroraBaseCommand {
	private final HashMap<String, AuroraBaseCommand> subCommands = new HashMap<>();
	private final ArrayList<String> subCommandNames = new ArrayList<>();

	public AuroraRootCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(final CommandSender sender) {
		return true;
	}

	/**
	 * Registers a sub command with this root command handler.
	 *
	 * @param name    The name of the command.
	 * @param command The command handler to register.
	 */
	public void addSubCommand(final String name, final AuroraBaseCommand command) {
		this.subCommands.put(name, command);
		this.subCommandNames.add(name);
	}

	/**
	 * Proxies the command invocation to the appropriate sub-command.
	 *
	 * @param sender  The sender of the command.
	 * @param command The command invoked.
	 * @param label   The label of the command invoked.
	 * @param args    The command arguments given.
	 * @return <tt>true</tt> if the given command is syntactically correct, <tt>false</tt> if not.
	 */
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
		if (args.length > 0) {
			final var cmd = subCommands.getOrDefault(args[0], null);

			if (cmd != null) {
				if (!cmd.hasPermission(sender)) {
					sender.sendMessage(Objects.requireNonNull(command.permissionMessage()));
					return true;
				} else {
					return cmd.onCommand(sender, command, label, args);
				}
			}
		}

		return false;
	}

	/**
	 * Proxies a tab completion event to the appropriate sub-command.
	 *
	 * @param sender  The sender of the command.
	 * @param command The command invoked.
	 * @param alias   The alias used to invoke the command.
	 * @param args    The command arguments given.
	 * @return A list of tab completion tokens.
	 */
	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
		if (args.length == 0) {
			return subCommandNames;
		} else {
			final var cmd = subCommands.getOrDefault(args[0], null);

			if (cmd != null) {
				if (!cmd.hasPermission(sender)) {
					return List.of();
				} else {
					return cmd.onTabComplete(sender, command, alias, args);
				}
			}

			return subCommandNames.stream().filter(s -> s.startsWith(args[0])).toList();
		}
	}
}
