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
		final var subdivideMode = event.getItem().getType() == Aurora.instance.config.subclaimCreationTool;

		if (!player.hasPermission("aurora.claims")) {
			// The player is not allowed to create or modify claims
			player.sendMessage(Aurora.instance.config.messages.noClaimCreationPermission);
			return;
		}

		if (claim == null || (user.lastSelectedClaim != null && claim.id == user.lastSelectedClaim.id)) {
			// The player is creating a new claim or setting the second corner for a claim resize
			if (user.lastSelectedClaim != null) {
				if (subdivideMode && !isAdminClaiming) {
					if (claim == null) {
						// The other corner of a subdivision claim is outside the main claim!
						player.sendMessage(Aurora.instance.config.messages.invalidSubclaimLocation);
						return;
					}

					// The player is creating a new sub-claim
					onClaimCreate(player, user, target, user.lastToolLocation, isAdminClaiming, user.lastSelectedClaim);
				} else {
					// The player is resizing a claim. Try to do that.
					onClaimResize(player, user, target);
				}
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
				onClaimCreate(player, user, target, user.lastToolLocation, isAdminClaiming, null);
			}
		} else if (claim.isAllowed(player, Group.OWNER) && user.lastToolLocation == null && user.lastSelectedClaim == null) {
			if (isClaimCorner(claim, target)) {
				// The player is selecting a corner for resizing the claim

				if (claim.parent != null) {
					// We currently do not support sub-claim resizing
					player.sendMessage("§cResizing subdivisions is currently not supported");
					return;
				}

				user.lastSelectedClaim = claim;
				user.lastToolLocation = target;
				player.sendMessage(Aurora.instance.config.messages.resizingClaim);
			} else if (subdivideMode && claim.parent == null) {
				// The player is creating a new sub-claim
				user.lastToolLocation = target;
				user.lastSelectedClaim = claim;
				player.sendMessage(Aurora.instance.config.messages.claimCornerSet.formatted(
						target.getBlockX(), target.getBlockY(), target.getBlockZ()
				));
			} else {
				// The player is not in subdivision mode, or they tried to create a nested sub-claim
				player.sendMessage(subdivideMode ? Aurora.instance.config.messages.noNestedSubclaims
						: Aurora.instance.config.messages.sublaimsRequireMode.formatted(
								Aurora.instance.config.subclaimCreationTool
				));
			}
		} else {
			// The player is clicking into an already existing claim and no action can be performed with it
			player.sendMessage(Aurora.instance.config.messages.alreadyClaimed);
		}
	}

	private static void onClaimCreate(final @NotNull Player player, final @NotNull User user,
									  final @NotNull Location cornerA, final @NotNull Location cornerB, boolean admin,
									  final Claim parent) {
		final var remainingClaimBlocks = user.totalClaimBlocks - user.usedClaimBlocks;
		final var sizeX = Math.abs(cornerA.getBlockX() - cornerB.getBlockX()) + 1;
		final var sizeZ = Math.abs(cornerA.getBlockZ() - cornerB.getBlockZ()) + 1;

		if (sizeX * sizeZ > remainingClaimBlocks && !admin && parent == null) {
			// The player does not have enough claim blocks to claim the area they selected
			player.sendMessage(Aurora.instance.config.messages.needMoreClaimBlocks.formatted(
					sizeX * sizeZ - remainingClaimBlocks
			));
		} else if (parent == null && Claim.intersects(cornerA, cornerB, true)) {
			// The area selected overlaps another claim
			player.sendMessage(Aurora.instance.config.messages.wouldOverlapAnotherClaim);
		} else if (parent != null && Claim.intersects(cornerA, cornerB, false, user.lastSelectedClaim)) {
			// The area selected overlaps another sub-claim
			player.sendMessage(Aurora.instance.config.messages.wouldOverlapAnotherClaim);
		} else {
			// The claim is good to go
			if (admin || parent != null) {
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
			if (parent == null) {
				cornerA.setY(cornerA.getWorld().getMinHeight());
				cornerB.setY(cornerB.getWorld().getMaxHeight());
			}

			// Reset the selected claim corner
			user.lastToolLocation = null;
			user.lastSelectedClaim = null;

			// Create the new claim and show its boundaries
			final var claim = new Claim(parent == null ? player.getUniqueId() : parent.owner, "(unnamed)", cornerA, cornerB);
			claim.isAdmin = admin;
			claim.parent = parent;
			claim.save();

			showClaimBoundaries(player, claim);
		}
	}

	private static void onClaimResize(final @NotNull Player player, @NotNull User user,
									  final @NotNull Location newCorner) {
		final var claim = user.lastSelectedClaim;
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
			user.lastSelectedClaim = null;
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

		final var newSizeX = Math.abs(newMaxX - newMinX) + 1;
		final var newSizeZ = Math.abs(newMaxZ - newMinZ) + 1;
		final var additionalBlocks = (newSizeX * newSizeZ) - claim.size();
		final var remainingClaimBlocks = user.totalClaimBlocks - user.usedClaimBlocks;

		final var newLocationMax = new Location(player.getWorld(), newMaxX, newMaxY, newMaxZ);
		final var newLocationMin = new Location(player.getWorld(), newMinX, newMinY, newMinZ);

		if (additionalBlocks > remainingClaimBlocks && !claim.isAdmin && claim.parent == null) {
			// The player does not have enough claim blocks
			player.sendMessage(Aurora.instance.config.messages.needMoreClaimBlocks.formatted(
					additionalBlocks - remainingClaimBlocks
			));
		} else if (Claim.intersects(newLocationMax, newLocationMin, claim.parent == null, claim,
				claim.parent == null)) {
			// The area selected overlaps another claim
			player.sendMessage(Aurora.instance.config.messages.wouldOverlapAnotherClaim);
		} else if (!checkSubdivisionsStillInside(claim, newLocationMin, newLocationMax)) {
			// The resized area would not include some sub-claims
			player.sendMessage("§cCannot resize because some of your subclaims would not be contained within the claim.");
		} else {
			// The claim is good to go
			if (claim.isAdmin || claim.parent != null) {
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
			user.lastSelectedClaim = null;

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

	private static boolean containsSubdivision(Location newLocationMin, Location newLocationMax, Claim sub) {
		return sub.minX >= newLocationMin.getBlockX() &&
				sub.maxX <= newLocationMax.getBlockX() &&
				sub.minY >= newLocationMin.getBlockY() &&
				sub.maxY <= newLocationMax.getBlockY() &&
				sub.minZ >= newLocationMin.getBlockZ() &&
				sub.maxZ <= newLocationMax.getBlockZ() &&
				Objects.equals(sub.world, newLocationMin.getWorld().getName());
	}

	private static boolean checkSubdivisionsStillInside(Claim claim, Location newLocationMin, Location newLocationMax) {
		for (final var subclaim : claim.getSubClaims()) {
			if (!containsSubdivision(newLocationMin, newLocationMax, subclaim))
				return false;
		}

		return true;
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

		// If there is already a task for this claim, cancel it
		if (user.visualizationTasks.containsKey(claim.id)) {
			user.visualizationTasks.remove(claim.id).cancel();
		}

		user.visualizationTasks.put(claim.id, ClaimVisualizationTask.spawn(
				claim,
				player,
				claim.isAdmin ? Color.RED : (claim.parent == null ? Color.BLUE : Color.GREEN),
				20 * 20
		));
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

		final var target = rayTraceResult.getHitBlock();
		if (target == null) {
			player.sendMessage(Aurora.instance.config.messages.tooFarAway);
			return;
		}

		if (tool == Aurora.instance.config.claimCreationTool || tool == Aurora.instance.config.subclaimCreationTool) {
			onCreationToolUse(event, target.getLocation());
		} else {
			onInvestigationToolUse(event, target.getLocation());
		}
	}
}
