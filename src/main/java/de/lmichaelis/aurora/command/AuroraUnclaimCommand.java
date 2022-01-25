// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
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

		if (claim == null) {
			player.sendMessage(plugin.config.messages.notAClaim);
		} else if (!Objects.equals(player.getUniqueId(), claim.owner)) {
			player.sendMessage(plugin.config.messages.notClaimOwner);
		} else {
			claim.delete();

			final var actualOwner = User.fromMetadata(player);
			assert actualOwner != null;

			actualOwner.usedClaimBlocks -= claim.size();
			actualOwner.totalClaimsUsed -= 1;
			actualOwner.update();

			player.sendMessage(plugin.config.messages.claimDeleted.formatted(
					actualOwner.totalClaimBlocks - actualOwner.usedClaimBlocks
			));
		}

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return List.of();
	}
}
