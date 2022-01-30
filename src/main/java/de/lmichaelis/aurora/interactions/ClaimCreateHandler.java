// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.interactions;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.Interactions;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.User;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles a player creating a claim or a sub-claim.
 */
public class ClaimCreateHandler implements InteractionHandler {
	private final Location initialLocation;
	private final Claim parent;
	private final User user;

	public ClaimCreateHandler(final @NotNull Location initialLocation, final @NotNull User user, final @Nullable Claim parent) {
		this.initialLocation = initialLocation;
		this.user = user;
		this.parent = parent;
	}

	@Override
	public void handle(final @NotNull Player player, final @NotNull Location location) {
		final var admin = user.adminMode && player.hasPermission("aurora.admin.claims");
		final var remainingClaimBlocks = user.totalClaimBlocks - user.usedClaimBlocks;
		final var sizeX = Math.abs(initialLocation.getBlockX() - location.getBlockX()) + 1;
		final var sizeZ = Math.abs(initialLocation.getBlockZ() - location.getBlockZ()) + 1;

		if (sizeX * sizeZ > remainingClaimBlocks && !admin && parent == null) {
			// The player does not have enough claim blocks to claim the area they selected
			player.sendMessage(Aurora.instance.config.messages.needMoreClaimBlocks.formatted(
					sizeX * sizeZ - remainingClaimBlocks
			));
		} else if (parent == null && Claim.intersects(initialLocation, location, true)) {
			// The area selected overlaps another claim
			player.sendMessage(Aurora.instance.config.messages.wouldOverlapAnotherClaim);
		} else if (parent != null && Claim.intersects(initialLocation, location, false, parent)) {
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
				initialLocation.setY(initialLocation.getWorld().getMinHeight());
				location.setY(location.getWorld().getMaxHeight());
			}

			// Reset the selected claim corner
			user.currentInteraction = null;

			// Create the new claim and show its boundaries
			final var claim = new Claim(parent == null ? player.getUniqueId() : parent.owner, "(unnamed)", initialLocation, location);
			claim.isAdmin = admin;
			claim.parent = parent;
			claim.save();

			Interactions.showClaimBoundaries(player, claim);
		}
	}
}
