// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import de.lmichaelis.aurora.model.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AuroraUnclaimCommand extends AuroraBaseCommand {
	public AuroraUnclaimCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("aurora.claims");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof final Player player)) return false;
		final var claim = Claim.getClaim(player.getLocation());
		final var user = Objects.requireNonNull(User.fromMetadata(player));

		if (claim == null) {
			player.sendMessage(plugin.config.messages.notAClaim);
		} else if (!claim.isAllowed(player, Group.OWNER)) {
			player.sendMessage(plugin.config.messages.notClaimOwner);
		} else {
			claim.delete();

			final var ownerPlayer = plugin.getServer().getPlayer(claim.owner);
			User ownerUser;
			if (ownerPlayer != null) {
				ownerUser = Objects.requireNonNull(User.fromMetadata(ownerPlayer));
			} else {
				ownerUser = Objects.requireNonNull(User.get(claim.owner));
			}

			ownerUser.refresh();
			ownerUser.usedClaimBlocks -= claim.size();
			ownerUser.totalClaimsUsed -= 1;
			ownerUser.update();

			if (Objects.equals(user.id, claim.owner)) {
				// This is the actual owner of the claim
				player.sendMessage(plugin.config.messages.claimDeletedByOwner.formatted(
						ownerUser.totalClaimBlocks - ownerUser.usedClaimBlocks
				));
			} else {
				player.sendMessage(plugin.config.messages.claimDeleted);
			}
		}

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return List.of();
	}
}
