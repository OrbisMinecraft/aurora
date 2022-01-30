// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.interactions;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.Interactions;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Handles a player resizing a claim.
 */
public class ClaimResizeHandler implements InteractionHandler {
	private final Location initialLocation;
	private final Claim claim;
	private final User user;

	public ClaimResizeHandler(final @NotNull Location initialLocation, final @NotNull User user, final @NotNull Claim claim) {
		this.initialLocation = initialLocation;
		this.user = user;
		this.claim = claim;
	}


	/**
	 * Checks that the given claim is inside the area marked by the given corner locations.
	 *
	 * @param newLocationMin The minimum corner of the area.
	 * @param newLocationMax The maximum corner of the area.
	 * @param sub The claim to check.
	 * @return <tt>true</tt> if the claim is inside the given area and <tt>false</tt> if not.
	 */
	private static boolean containsSubdivision(final @NotNull Location newLocationMin, final @NotNull Location newLocationMax, final @NotNull Claim sub) {
		return sub.minX >= newLocationMin.getBlockX() &&
				sub.maxX <= newLocationMax.getBlockX() &&
				sub.minY >= newLocationMin.getBlockY() &&
				sub.maxY <= newLocationMax.getBlockY() &&
				sub.minZ >= newLocationMin.getBlockZ() &&
				sub.maxZ <= newLocationMax.getBlockZ() &&
				Objects.equals(sub.world, newLocationMin.getWorld().getName());
	}

	/**
	 * Checks that <i>all</i> subdivisions of the given claim are inside the area marked by the given corner locations.
	 *
	 * @param claim The claim of which to check the subdivisions.
	 * @param newLocationMin The minimum corner of the area.
	 * @param newLocationMax The maximum corner of the area.
	 * @return <tt>true</tt> if all subdivisions of the claim are inside the new area and <tt>false</tt> if not.
	 */
	private static boolean checkSubdivisionsStillInside(final @NotNull Claim claim, final @NotNull Location newLocationMin,
														final @NotNull Location newLocationMax) {
		for (final var subclaim : claim.getSubClaims()) {
			if (!containsSubdivision(newLocationMin, newLocationMax, subclaim))
				return false;
		}

		return true;
	}

	@Override
	public void handle(final @NotNull Player player, final @NotNull Location location) {
		User owner = user;

		if (!claim.owner.equals(user.id)) {
			// If the user resizing the claim is not actually the owner, we need to make sure
			// to bill the owner for the resize
			final var onlineOwner = Bukkit.getPlayer(claim.owner);

			if (onlineOwner != null) {
				owner = Objects.requireNonNull(User.fromMetadata(onlineOwner));
			} else {
				owner = Objects.requireNonNull(User.get(claim.owner));
			}
		}

		if (initialLocation.equals(location)) {
			// Special case: if the user clicks the same block twice, reset.
			user.currentInteraction = null;
			return;
		}

		// Find the new claim extent
		int newMinX = claim.minX, newMaxX = claim.maxX, newMinY = claim.minY, newMaxY = claim.maxY, newMinZ = claim.minZ, newMaxZ = claim.maxZ;
		if (initialLocation.getBlockX() == claim.minX) {
			newMinX = location.getBlockX();
		} else {
			newMaxX = location.getBlockX();
		}

		if (initialLocation.getBlockZ() == claim.minZ) {
			newMinZ = location.getBlockZ();
		} else {
			newMaxZ = location.getBlockZ();
		}

		// Make sure we're up-to-date on claim block balance
		owner.refresh();

		final var newSizeX = Math.abs(newMaxX - newMinX) + 1;
		final var newSizeZ = Math.abs(newMaxZ - newMinZ) + 1;
		final var additionalBlocks = (newSizeX * newSizeZ) - claim.size();
		final var remainingClaimBlocks = owner.totalClaimBlocks - owner.usedClaimBlocks;

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

				owner.refresh();
				owner.usedClaimBlocks += additionalBlocks;
				owner.update();
			}

			// Reset the selected claim corner
			owner.currentInteraction = null;

			// Create the new claim and show its boundaries
			claim.maxX = newMaxX;
			claim.maxY = newMaxY;
			claim.maxZ = newMaxZ;
			claim.minX = newMinX;
			claim.minY = newMinY;
			claim.minZ = newMinZ;
			claim.update();

			Interactions.showClaimBoundaries(player, claim);
		}
	}
}
