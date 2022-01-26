// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import de.lmichaelis.aurora.model.User;
import de.lmichaelis.aurora.task.ClaimVisualizationTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Interactions {

	/**
	 * Handles the player using the claim creation tool. The claim creation tool can be used to create and resize
	 * claims as well as to create sub-claims. This method will check for all of these conditions and will run the
	 * proper logic for them.
	 *
	 * @param event  The event that was triggered when the player used to the tool
	 * @param target The block targeted by the player.
	 */
	public static void onCreationToolUse(final @NotNull PlayerInteractEvent event, final @NotNull Location target) {
		final var claim = Claim.getClaim(target);
		final var player = event.getPlayer();
		final var user = Objects.requireNonNull(User.fromMetadata(player));
		final var totalClaimsLimit = Aurora.instance.config.totalClaimsLimit;
		final var isAdminClaiming = user.adminMode && player.hasPermission("aurora.admin.claims");

		if (!player.hasPermission("aurora.claims")) {
			// The player is not allowed to create or modify claims
			player.sendMessage(Aurora.instance.config.messages.noClaimCreationPermission);
			return;
		}

		if (claim == null || (user.currentResizeClaim != null && claim.owner.equals(user.currentResizeClaim.owner))) {
			// The player is creating a new claim or setting the second corner for a claim resize

			if (user.currentResizeClaim != null) {
				// The player is resizing a claim. Try to do that.
				onClaimResize(player, user, target);
			} else if (totalClaimsLimit >= 0 && user.totalClaimsUsed >= totalClaimsLimit && !isAdminClaiming) {
				// The player cannot create any more claims
				player.sendMessage(Aurora.instance.config.messages.tooManyClaims);
			} else if (user.lastToolLocation == null) {
				// The user is selecting the first corner of the new claim
				user.lastToolLocation = target;

				player.sendMessage(Aurora.instance.config.messages.claimCornerSet.formatted(
						target.getBlockX(), target.getBlockY(), target.getBlockZ()
				));
			} else {
				// The user is setting the second corner of the claim. Try to actually create it.
				onClaimCreate(player, user, target, user.lastToolLocation, isAdminClaiming);
			}
		} else if (claim.isAllowed(player, Group.OWNER) && user.lastToolLocation == null && user.currentResizeClaim == null) {
			if (isClaimCorner(claim, target)) {
				// The player is selecting a corner for resizing the claim
				user.currentResizeClaim = claim;
				user.lastToolLocation = target;
				player.sendMessage(Aurora.instance.config.messages.resizingClaim);
			} else if (user.subdivideMode && claim.parent == null) {
				// The player is creating a new sub-claim
				// TODO
			} else {
				// The player is not in subdivision mode, or they tried to create a nested sub-claim
				player.sendMessage(user.subdivideMode ? Aurora.instance.config.messages.noNestedSubclaims
						: Aurora.instance.config.messages.sublaimsRequireMode);
			}
		} else {
			// The player is clicking into an already existing claim and no action can be performed with it
			player.sendMessage(Aurora.instance.config.messages.alreadyClaimed);
		}
	}

	private static void onClaimCreate(final @NotNull Player player, final @NotNull User user,
									  final @NotNull Location cornerA, final @NotNull Location cornerB, boolean admin) {
		final var remainingClaimBlocks = user.totalClaimBlocks - user.usedClaimBlocks;
		final var sizeX = Math.abs(cornerA.getBlockX() - cornerB.getBlockX()) + 1;
		final var sizeZ = Math.abs(cornerA.getBlockZ() - cornerB.getBlockZ()) + 1;

		if (sizeX * sizeZ > remainingClaimBlocks && !admin) {
			// The player does not have enough claim blocks to claim the area they selected
			player.sendMessage(Aurora.instance.config.messages.needMoreClaimBlocks.formatted(
					sizeX * sizeZ - remainingClaimBlocks
			));
		} else if (Claim.intersects(cornerA, cornerB, true)) {
			// The area selected overlaps another claim
			player.sendMessage(Aurora.instance.config.messages.wouldOverlapAnotherClaim);
		} else {
			// The claim is good to go
			if (admin) {
				// We're creating an admin claim. The user's claim block balance is not touched.
				player.sendMessage(Aurora.instance.config.messages.claimCreated.formatted(sizeX, sizeZ, 0));
			} else {
				// The user is creating a claim for themselves
				player.sendMessage(Aurora.instance.config.messages.claimCreated.formatted(
						sizeX, sizeZ, remainingClaimBlocks - (sizeX * sizeZ)
				));

				user.refresh();
				user.usedClaimBlocks += sizeX * sizeZ;
				user.totalClaimsUsed += 1;
				user.update();
			}

			// Top level claims always range from the top of the world to the very bottom
			cornerA.setY(cornerA.getWorld().getMinHeight());
			cornerB.setY(cornerB.getWorld().getMaxHeight());

			// Reset the selected claim corner
			user.lastToolLocation = null;

			// Create the new claim and show its boundaries
			final var claim = new Claim(player.getUniqueId(), "(unnamed)", cornerA, cornerB);
			claim.isAdmin = admin;
			claim.save();

			showClaimBoundaries(player, claim);
		}
	}

	private static void onClaimResize(final @NotNull Player player, @NotNull User user,
									  final @NotNull Location newCorner) {
		final var claim = user.currentResizeClaim;
		final var oldCorner = user.lastToolLocation;

		if (!claim.owner.equals(user.id)) {
			// If the user resizing the claim is not actually the owner, we need to make sure
			// to bill the owner for the resize
			final var onlineOwner = Bukkit.getPlayer(claim.owner);

			if (onlineOwner != null) {
				user = Objects.requireNonNull(User.fromMetadata(onlineOwner));
			} else {
				user = Objects.requireNonNull(User.get(claim.owner));
			}
		}

		if (oldCorner.equals(newCorner)) {
			// Special case: if the user clicks the same block twice, reset.
			user.currentResizeClaim = null;
			user.lastToolLocation = null;
			return;
		}

		// Find the new claim extent
		int newMinX = claim.minX, newMaxX = claim.maxX, newMinY = claim.minY, newMaxY = claim.maxY, newMinZ = claim.minZ, newMaxZ = claim.maxZ;
		if (oldCorner.getBlockX() == claim.minX) {
			newMinX = newCorner.getBlockX();
		} else {
			newMaxX = newCorner.getBlockX();
		}

		if (oldCorner.getBlockZ() == claim.minZ) {
			newMinZ = newCorner.getBlockZ();
		} else {
			newMaxZ = newCorner.getBlockZ();
		}

		// Make sure we're up-to-date on claim block balance
		user.refresh();

		if (claim.parent != null) {
			// We're resizing a sub-claim. Respect Y-values as well.
			if (oldCorner.getBlockY() == claim.minY) {
				newMinY = newCorner.getBlockY();
			} else {
				newMaxY = newCorner.getBlockY();
			}

			// TODO
		} else {
			final var newSizeX = Math.abs(newMaxX - newMinX) + 1;
			final var newSizeZ = Math.abs(newMaxZ - newMinZ) + 1;
			final var additionalBlocks = (newSizeX * newSizeZ) - claim.size();
			final var remainingClaimBlocks = user.totalClaimBlocks - user.usedClaimBlocks;

			final var newLocationMax = new Location(player.getWorld(), newMaxX, newMaxY, newMaxZ);
			final var newLocationMin = new Location(player.getWorld(), newMinX, newMinY, newMinZ);

			if (additionalBlocks > remainingClaimBlocks && !claim.isAdmin) {
				// The player does not have enough claim blocks
				player.sendMessage(Aurora.instance.config.messages.needMoreClaimBlocks.formatted(
						additionalBlocks - remainingClaimBlocks
				));
			} else if (Claim.intersects(newLocationMax, newLocationMin, true, claim)) {
				// The area selected overlaps another claim
				player.sendMessage(Aurora.instance.config.messages.wouldOverlapAnotherClaim);
			} else {
				// The claim is good to go
				if (claim.isAdmin) {
					// We're resizing an admin claim. The user's claim block balance is not touched.
					player.sendMessage(Aurora.instance.config.messages.claimResized.formatted(newSizeX, newSizeZ));
				} else {
					// The user is resizing a non-admin claim
					player.sendMessage(Aurora.instance.config.messages.claimResized.formatted(newSizeX, newSizeZ));

					user.refresh();
					user.usedClaimBlocks += additionalBlocks;
					user.update();
				}

				// Reset the selected claim corner
				user.lastToolLocation = null;
				user.currentResizeClaim = null;

				// Create the new claim and show its boundaries
				claim.maxX = newMaxX;
				claim.maxY = newMaxY;
				claim.maxZ = newMaxZ;
				claim.minX = newMinX;
				claim.minY = newMinY;
				claim.minZ = newMinZ;
				claim.update();

				showClaimBoundaries(player, claim);
			}
		}
	}

	/**
	 * Handles player using the configured claim investigation tool (i.e. a stick) to find the owner and
	 * size of a claim. This method will check for a claim at the given location and show a bounding
	 * box around that claim.
	 *
	 * @param event  The event that was triggered when the player used to the tool
	 * @param target The block targeted by the player.
	 */
	public static void onInvestigationToolUse(final @NotNull PlayerInteractEvent event, final @NotNull Location target) {
		final var claim = Claim.getClaim(target);
		final var player = event.getPlayer();

		// Check that the player is actually targeting a claim
		if (claim == null) {
			player.sendMessage(Aurora.instance.config.messages.notAClaim);
			return;
		}

		// If they are, report back with the claim's owner and show the bounding box
		player.sendMessage(Aurora.instance.config.messages.blockClaimedBy.formatted(
				claim.isAdmin ? "an Admin" : Bukkit.getOfflinePlayer(claim.owner).getName()
		));

		showClaimBoundaries(player, claim);
	}


	public static void showClaimBoundaries(final @NotNull Player player,
										   final @NotNull Claim claim) {
		final var user = Objects.requireNonNull(User.fromMetadata(player));

		// Only run at most three visualization tasks
		if (user.runningVisualizationTasks >= 3) return;

		final var visTask = Bukkit.getScheduler().runTaskTimer(
				Aurora.instance,
				new ClaimVisualizationTask(claim, player, Color.RED),
				0, 5
		);

		Bukkit.getScheduler().runTaskLater(Aurora.instance, () -> {
			Bukkit.getScheduler().cancelTask(visTask.getTaskId());
			user.runningVisualizationTasks -= 1;
		}, 20 * 20);

		user.runningVisualizationTasks += 1;
	}

	public static boolean isClaimCorner(final @NotNull Claim claim, final @NotNull Location location) {
		return (location.getBlockX() == claim.minX || location.getBlockX() == claim.maxX) &&
				(location.getBlockZ() == claim.minZ || location.getBlockZ() == claim.maxZ) &&
				(claim.parent == null || (location.getBlockY() == claim.minY || location.getBlockY() == claim.maxY));
	}

	/**
	 * Handles interactions with Aurora itself. Basically a proxy to {@link #onCreationToolUse(PlayerInteractEvent, Location)}
	 * and {@link #onInvestigationToolUse(PlayerInteractEvent, Location)}
	 *
	 * @param event The event that triggered the interaction.
	 */
	public static void onAuroraInteract(final @NotNull PlayerInteractEvent event) {
		final var player = event.getPlayer();
		final var tool = Objects.requireNonNull(event.getItem()).getType();
		final var rayTraceResult = player.rayTraceBlocks(100, FluidCollisionMode.SOURCE_ONLY);

		if (rayTraceResult == null) {
			player.sendMessage(Aurora.instance.config.messages.tooFarAway);
			return;
		}

		final var target = rayTraceResult.getHitPosition().toLocation(player.getWorld());
		if (tool == Aurora.instance.config.claimCreationTool) {
			onCreationToolUse(event, target);
		} else {
			onInvestigationToolUse(event, target);
		}
	}
}
