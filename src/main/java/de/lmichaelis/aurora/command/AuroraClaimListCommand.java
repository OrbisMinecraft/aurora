// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AuroraClaimListCommand extends AuroraBaseCommand {
	public AuroraClaimListCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("aurora.claims");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof final Player player)) return false;

		User user;
		if (args.length > 1) {
			if (!player.hasPermission("aurora.claims.other")) {
				player.sendMessage(Objects.requireNonNull(command.permissionMessage()));
				return true;
			}

			final var targetedPlayer = plugin.getServer().getOfflinePlayerIfCached(args[0]);
			if (targetedPlayer == null) {
				player.sendMessage(plugin.config.messages.unknownPlayer);
				return true;
			}

			user = User.get(player.getUniqueId());

		} else {
			user = User.fromMetadata(player);
		}

		assert user != null;
		user.refresh();
		final var claims = user.getClaims();

		player.sendMessage(plugin.config.messages.claimListHeader);

		if (claims.size() == 0) {
			player.sendMessage(plugin.config.messages.claimListEmpty);
		} else {
			for (final var claim : claims) {
				if (claim.isAdmin) {
					player.sendMessage(plugin.config.messages.claimListEntryAdmin.formatted(
							claim.name,
							claim.minX,
							claim.minZ,
							claim.size()
					));
				} else {
					player.sendMessage(plugin.config.messages.claimListEntry.formatted(
							claim.name,
							claim.minX,
							claim.minZ,
							claim.size()
					));
				}
			}
		}

		player.sendMessage(plugin.config.messages.claimListFooter.formatted(
				user.usedClaimBlocks,
				user.totalClaimBlocks,
				user.totalClaimBlocks - user.usedClaimBlocks,
				plugin.config.totalClaimsLimit == -1 ? "∞" : plugin.config.totalClaimsLimit - user.totalClaimsUsed
		));

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return null;
	}
}
