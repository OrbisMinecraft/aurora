// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.Predicates;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import de.lmichaelis.aurora.model.User;
import de.lmichaelis.aurora.task.ClaimVisualizationTask;
import org.bukkit.*;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

/**
 * Event handlers for player events.
 */
public final class PlayerEventListener extends BaseListener {
	public PlayerEventListener(final Aurora plugin) {
		super(plugin);
	}

	private void onAuroraInteract(final @NotNull Player player, final @NotNull Material tool) {
		final var rayTraceResult = player.rayTraceBlocks(100, FluidCollisionMode.SOURCE_ONLY);

		if (rayTraceResult == null) {
			player.sendMessage(plugin.config.messages.tooFarAway);
			return;
		}

		final var targetedLocation = rayTraceResult.getHitPosition().toLocation(player.getWorld());
		final var targetedClaim = Claim.getClaim(targetedLocation);

		if (tool == plugin.config.claimCreationTool) {
			// Feature: Create a claim.
			if (targetedClaim != null) {
				player.sendMessage(plugin.config.messages.alreadyClaimed);
				return;
			}

			final var previousLocationMeta = player.getMetadata("aurora.claimBlockSelection");
			final var user = User.fromMetadata(player);

			if (previousLocationMeta.size() == 0) {
				player.sendMessage(plugin.config.messages.claimCornerSet);
				player.setMetadata("aurora.claimBlockSelection", new FixedMetadataValue(plugin, targetedLocation));
				return;
			}

			final var previousLocation = (Location) previousLocationMeta.get(0).value();
			final var remainingClaimBlocks = user.totalClaimBlocks - user.usedClaimBlocks;

			final var sizeX = Math.abs(previousLocation.getBlockX() - targetedLocation.getBlockX()) + 1;
			final var sizeZ = Math.abs(previousLocation.getBlockZ() - targetedLocation.getBlockZ()) + 1;

			if (sizeX * sizeZ > remainingClaimBlocks) {
				player.sendMessage(plugin.config.messages.needMoreClaimBlocks.formatted(sizeX * sizeZ - remainingClaimBlocks));
				return;
			}

			if (Claim.intersects(previousLocation, targetedLocation, true)) {
				player.sendMessage(plugin.config.messages.wouldOverlapAnotherClaim);
				return;
			}

			player.sendMessage(plugin.config.messages.claimCreated.formatted(sizeX, sizeZ));
			player.removeMetadata("aurora.claimBlockSelection", plugin);

			user.usedClaimBlocks += sizeX * sizeZ;
			user.update();

			new Claim(player.getUniqueId(), "test", previousLocation, targetedLocation).save();
		} else {
			// Feature: Inspect a claim.
			if (targetedClaim == null) {
				player.sendMessage(plugin.config.messages.notAClaim);
				return;
			}

			player.sendMessage(plugin.config.messages.blockClaimedBy.formatted(
					Bukkit.getOfflinePlayer(targetedClaim.owner).getName())
			);

			// TODO: prevent multiple tasks per claim per player
			final var visTask = Bukkit.getScheduler().runTaskTimer(
					plugin,
					new ClaimVisualizationTask(targetedClaim, player, Color.RED),
					0, 5
			);

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				Bukkit.getScheduler().cancelTask(visTask.getTaskId());
			}, 20 * 20);
		}
	}

	/**
	 * Meta event handler for creating or retrieving the user object associated with the joining player.
	 * This is required to make sure newly joining players are properly set up with their initial claim
	 * count, for example.
	 *
	 * @param event The event to process.
	 */
	@EventHandler
	public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
		final var player = event.getPlayer();
		var user = User.get(player.getUniqueId());

		if (user == null) {
			// This user has logged in for the first time
			user = new User(player.getUniqueId(), plugin.config.initialClaimBlocks);
			user.save();
		} else if (user.totalClaimBlocks < plugin.config.initialClaimBlocks) {
			// The user has fewer claims than users who would log in for the
			// first time; let's bring them up to speed
			user.totalClaimBlocks = plugin.config.initialClaimBlocks;
			user.update();
		}

		// Save the user object as metadata on the player
		player.setMetadata(User.METADATA_KEY, new FixedMetadataValue(plugin, user));
	}

	@EventHandler(ignoreCancelled = false)
	public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
		final var action = event.getAction();
		final var subject = event.getClickedBlock();
		final var player = event.getPlayer();
		final var hold = event.getItem();
		final var holdType = hold == null ? null : hold.getType();

		// Ignore left-clicking. It will trigger another event later.
		if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) return;

		// Rule: If the player is holding the claim creation or investigation tool, perform
		//       the required action and cancel the event
		if (holdType == plugin.config.claimCreationTool || holdType == plugin.config.claimInvestigationTool) {
			onAuroraInteract(player, holdType);
			event.setCancelled(true);
			return;
		}

		// Rule: Ignore right-clicks in air.
		if (action == Action.RIGHT_CLICK_AIR) return;

		assert subject != null;
		final var location = subject.getLocation();
		final var claim = Claim.getClaim(location);

		// Rule: You can interact with all blocks outside of claims without restriction
		if (claim == null) return;

		// Physical interactions in claims (e.g. trampling turtle eggs) require a permission
		// in that claim.
		if (action == Action.PHYSICAL) {
			// Rule: Only people with build permissions may execute physical actions in a claim.
			if (claim.isAllowed(player, Group.BUILD)) return;
		} else { // if (action == Action.RIGHT_CLICK_BLOCK)
			final var subjectType = subject.getType();

			if (subject.getState() instanceof Container) {
				// Rule: Inventories in claims may only be accessed by players with the STEAL permission
				// TODO: Special rule for lecterns: Viewing a book in a lectern should be allowed with
				//       the ACCESS permission
				if (claim.isAllowed(player, Group.STEAL)) return;
			} else if (Predicates.isInteractBuildProtected(subjectType)) {
				// Rule: Build-protected blocks in claims may only be accessed by players with the BUILD permission
				if (claim.isAllowed(player, Group.BUILD)) return;
			} else if (Predicates.isInteractInteractProtected(subjectType)) {
				// Rule: Interact-protected blocks in claims may only be accessed by players with the INTERACT permission
				if (claim.isAllowed(player, Group.INTERACT)) return;
			} else if (hold != null && Predicates.isUseBuildProtected(holdType)) {
				// Rule: Entities may only be created or altered by players with the BUILD permission
				if (claim.isAllowed(player, Group.BUILD)) return;
			}
		}

		Aurora.logger.warn("Unhandled PlayerInteractEvent(item: {}, action: {}, blockClicked: {}, hand: {})",
				event.getItem(),
				event.getAction(),
				event.getClickedBlock() == null
						? "null"
						: event.getClickedBlock().getType(),
				event.getHand());
		event.setCancelled(true);
	}
}
