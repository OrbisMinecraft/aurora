// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import de.lmichaelis.aurora.model.User;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AuroraSetGroupCommand extends AuroraBaseCommand {
	private static final List<String> GROUP_NAMES = List.of(
			"none",
			"access",
			"containers",
			"build",
			"manage",
			"owner"
	);

	public AuroraSetGroupCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("aurora.claims");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof final Player player)) return false;
		if (args.length != 3) return false;
		final var claim = Claim.getClaim(player.getLocation());
		final var user = Objects.requireNonNull(User.fromMetadata(player));

		if (claim == null) {
			player.sendMessage(plugin.config.messages.notAClaim);
		} else if (!claim.isAllowed(player, Group.MANAGE)) {
			player.sendMessage(plugin.config.messages.noPermission);
		} else if (args[2].equals("manage") && !claim.isAllowed(player, Group.OWNER)) {
			player.sendMessage(plugin.config.messages.noPermission);
		} else if (args[2].equals("owner") && !Objects.equals(player.getUniqueId(), claim.owner) && !user.adminMode) {
			player.sendMessage(plugin.config.messages.noPermission);
		} else if (player.getName().equals(args[1])) {
			player.sendMessage(plugin.config.messages.cannotSetOwnGroup);
		} else {
			OfflinePlayer target = plugin.getServer().getPlayer(args[1]);
			if (target == null) target = plugin.getServer().getOfflinePlayerIfCached(args[1]);
			if (target == null) {
				player.sendMessage(plugin.config.messages.unknownPlayer);
				return false;
			}

			switch (args[2]) {
				case "none" -> claim.setGroup(target, Group.NONE);
				case "access" -> claim.setGroup(target, Group.ACCESS);
				case "containers" -> claim.setGroup(target, Group.CONTAINERS);
				case "build" -> claim.setGroup(target, Group.BUILD);
				case "manage" -> claim.setGroup(target, Group.MANAGE);
				case "owner" -> claim.setGroup(target, Group.OWNER);
				default -> {
					return false;
				}
			}

			player.sendMessage("§cGranted permission " + args[2] + " to " + args[1]);
		}

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return args.length < 3 ? null : GROUP_NAMES;
	}
}
