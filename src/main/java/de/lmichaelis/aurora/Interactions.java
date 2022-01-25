// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.User;
import de.lmichaelis.aurora.task.ClaimVisualizationTask;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Interactions {
	public static void showClaimBoundaries(final @NotNull Aurora plugin, final @NotNull Player player,
										   final @NotNull Claim claim) {
		final var user = Objects.requireNonNull(User.fromMetadata(player));

		// Only run at most three visualization tasks
		if (user.runningVisualizationTasks >= 3) return;

		final var visTask = Bukkit.getScheduler().runTaskTimer(
				plugin,
				new ClaimVisualizationTask(claim, player, Color.RED),
				0, 5
		);

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			Bukkit.getScheduler().cancelTask(visTask.getTaskId());
			user.runningVisualizationTasks -= 1;
		}, 20 * 20);

		user.runningVisualizationTasks += 1;
	}

	public static void onAuroraInteract(final Aurora plugin, final @NotNull Player player, final @NotNull Material tool) {
		final var rayTraceResult = player.rayTraceBlocks(100, FluidCollisionMode.SOURCE_ONLY);

		if (rayTraceResult == null) {
			player.sendMessage(plugin.config.messages.tooFarAway);
			return;
		}

		final var targetedLocation = rayTraceResult.getHitPosition().toLocation(player.getWorld());
		final var targetedClaim = Claim.getClaim(targetedLocation);

		if (tool == plugin.config.claimCreationTool) {
			if (!player.hasPermission("aurora.claims")) {
				player.sendMessage(plugin.config.messages.noClaimCreationPermission);
				return;
			}

			// Feature: Create a claim.
			if (targetedClaim != null) {
				player.sendMessage(plugin.config.messages.alreadyClaimed);
				return;
			}

			final var previousLocationMeta = player.getMetadata("aurora.claimBlockSelection");
			final var user = Objects.requireNonNull(User.fromMetadata(player));
			user.refresh();

			final var isAdminClaiming = user.adminMode && player.hasPermission("aurora.admin.claims");
			if (isAdminClaiming) player.sendMessage(plugin.config.messages.creatingAdminClaim);


			if (plugin.config.totalClaimsLimit > 0 && user.totalClaimsUsed >= plugin.config.totalClaimsLimit && !isAdminClaiming) {
				player.sendMessage(plugin.config.messages.tooManyClaims);
				return;
			}

			if (previousLocationMeta.size() == 0) {
				player.sendMessage(plugin.config.messages.claimCornerSet.formatted(
						targetedLocation.getBlockX(),
						targetedLocation.getBlockY(),
						targetedLocation.getBlockZ()
				));
				player.setMetadata("aurora.claimBlockSelection", new FixedMetadataValue(plugin, targetedLocation));
				return;
			}

			final var previousLocation = (Location) previousLocationMeta.get(0).value();
			final var remainingClaimBlocks = user.totalClaimBlocks - user.usedClaimBlocks;

			final var sizeX = Math.abs(previousLocation.getBlockX() - targetedLocation.getBlockX()) + 1;
			final var sizeZ = Math.abs(previousLocation.getBlockZ() - targetedLocation.getBlockZ()) + 1;

			if (sizeX * sizeZ > remainingClaimBlocks && !isAdminClaiming) {
				player.sendMessage(plugin.config.messages.needMoreClaimBlocks.formatted(sizeX * sizeZ - remainingClaimBlocks));
				return;
			}

			if (Claim.intersects(previousLocation, targetedLocation, true)) {
				player.sendMessage(plugin.config.messages.wouldOverlapAnotherClaim);
				return;
			}

			player.removeMetadata("aurora.claimBlockSelection", plugin);

			if (!isAdminClaiming) {
				player.sendMessage(plugin.config.messages.claimCreated.formatted(sizeX, sizeZ, remainingClaimBlocks - (sizeX * sizeZ)));

				user.refresh();
				user.usedClaimBlocks += sizeX * sizeZ;
				user.totalClaimsUsed += 1;
				user.update();
			} else {
				player.sendMessage(plugin.config.messages.claimCreated.formatted(sizeX, sizeZ, 0));
			}

			previousLocation.setY(previousLocation.getWorld().getMinHeight());
			targetedLocation.setY(targetedLocation.getWorld().getMaxHeight());

			final var claim = new Claim(player.getUniqueId(), "(unnamed)", previousLocation, targetedLocation);
			claim.isAdmin = isAdminClaiming;
			claim.save();

			showClaimBoundaries(plugin, player, claim);
		} else {
			// Feature: Inspect a claim.
			if (targetedClaim == null) {
				player.sendMessage(plugin.config.messages.notAClaim);
				return;
			}

			player.sendMessage(plugin.config.messages.blockClaimedBy.formatted(
					targetedClaim.isAdmin ? "an Admin" : Bukkit.getOfflinePlayer(targetedClaim.owner).getName()
			));

			showClaimBoundaries(plugin, player, targetedClaim);
		}
	}
}
