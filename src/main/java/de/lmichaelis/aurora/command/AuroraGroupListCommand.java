// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuroraGroupListCommand extends AuroraBaseCommand {
	public AuroraGroupListCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("aurora.claims");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof final Player player)) return false;
		if (args.length != 1) return false;
		final var claim = Claim.getClaim(player.getLocation());

		if (claim == null) {
			player.sendMessage(plugin.config.messages.notAClaim);
		} else if (claim.isAllowed(player, Group.MANAGE)) {
			final HashMap<Group, Set<String>> map = new HashMap<>();

			for (final var group : claim.userGroups) {
				map.computeIfAbsent(group.group, g -> new HashSet<>()).add(Bukkit.getOfflinePlayer(group.player).getName());
			}

			if (!claim.restricted && claim.parent != null) {
				try {
					Aurora.db.claims.refresh(claim.parent);
					for (final var group : claim.parent.userGroups) {
						map.computeIfAbsent(group.group, g -> new HashSet<>()).add(Bukkit.getOfflinePlayer(group.player).getName());
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}


			if (map.isEmpty()) {
				player.sendMessage("§aNobody has been granted a group yet.");
			}

			for (final var e : map.entrySet()) {
				switch (e.getKey()) {
					case OWNER -> player.sendMessage("§aOwner:");
					case MANAGE -> player.sendMessage("§aManage:");
					case BUILD -> player.sendMessage("§aBuild:");
					case STEAL -> player.sendMessage("§aSteal:");
					case ACCESS -> player.sendMessage("§aAccess:");
					case NONE -> {
					}
				}

				player.sendMessage(String.join(" ", e.getValue()));
			}
		}

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return List.of();
	}
}
