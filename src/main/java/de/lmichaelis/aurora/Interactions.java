// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import de.lmichaelis.aurora.interactions.ClaimCreateHandler;
import de.lmichaelis.aurora.interactions.ClaimResizeHandler;
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

		if (user.currentInteraction != null) {
			user.currentInteraction.handle(player, target);
			return;
		}

		if (claim == null) {
			if (user.totalClaimsUsed >= totalClaimsLimit && !isAdminClaiming) {
				// The player cannot create any more claims
				player.sendMessage(Aurora.instance.config.messages.tooManyClaims);
				return;
			}

			if (subdivideMode) {
				// The player cannot create any more claims
				player.sendMessage("Please use a %s to create new claims.".formatted(Aurora.instance.config.claimCreationTool));
				return;
			}

			// the user is creating a new top-level claim
			user.currentInteraction = new ClaimCreateHandler(target, user, null);

			player.sendMessage(Aurora.instance.config.messages.claimCornerSet.formatted(
					target.getBlockX(), target.getBlockY(), target.getBlockZ()
			));
		} else if (claim.isAllowed(player, Group.OWNER)) {
			if (isClaimCorner(claim, target)) {
				if (claim.parent != null) {
					// We currently do not support sub-claim resizing
					player.sendMessage("§cResizing subdivisions is currently not supported");
					return;
				}

				// the user is resizing a top-level claim
				user.currentInteraction = new ClaimResizeHandler(target, user, claim);
				player.sendMessage(Aurora.instance.config.messages.resizingClaim);
			} else if (subdivideMode && claim.parent == null) {
				// the user is creating a sub-claim
				user.currentInteraction = new ClaimCreateHandler(target, user, claim);

				player.sendMessage(Aurora.instance.config.messages.claimCornerSet.formatted(
						target.getBlockX(), target.getBlockY(), target.getBlockZ()
				));
			} else {
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
