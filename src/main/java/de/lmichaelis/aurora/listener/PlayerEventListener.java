// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.Interactions;
import de.lmichaelis.aurora.Predicates;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import de.lmichaelis.aurora.model.User;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
			Interactions.onAuroraInteract(plugin, player, holdType);
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
		// in that claim. All the checks are inverted here, because subsequent handlers will
		// take care of handling the events.
		if (action == Action.PHYSICAL) {
			// Rule: Only people with build permissions may execute physical actions in a claim.
			if (!claim.isAllowed(player, Group.BUILD)) {
				event.setCancelled(true);
				return;
			}
		} else { // if (action == Action.RIGHT_CLICK_BLOCK)
			final var subjectType = subject.getType();

			if (subject.getState() instanceof Container) {
				// Rule: Inventories in claims may only be accessed by players with the STEAL permission
				// TODO: Special rule for lecterns: Viewing a book in a lectern should be allowed with
				//       the ACCESS permission
				if (!claim.isAllowed(player, Group.STEAL)) {
					event.setCancelled(true);
					return;
				}
			} else if (Predicates.isInteractBuildProtected(subjectType)) {
				// Rule: Build-protected blocks in claims may only be accessed by players with the BUILD permission
				if (!claim.isAllowed(player, Group.BUILD))  {
					event.setCancelled(true);
					return;
				}
			} else if (Predicates.isInteractAccessProtected(subjectType)) {
				// Rule: Interact-protected blocks in claims may only be accessed by players with the INTERACT permission
				if (!claim.isAllowed(player, Group.ACCESS)) {
					event.setCancelled(true);
					return;
				}
			} else if (hold != null && Predicates.isPlaceBuildProtected(holdType)) {
				// Rule: Entities may only be created or altered by players with the BUILD permission
				if (!claim.isAllowed(player, Group.BUILD)) {
					event.setCancelled(true);
					return;
				}
			}
		}

		Aurora.logger.warning("Unhandled PlayerInteractEvent(item: %s, action: %s, blockClicked: %s, hand: %s)".formatted(
				event.getItem(),
				event.getAction(),
				event.getClickedBlock() == null
						? "null"
						: event.getClickedBlock().getType(),
				event.getHand()));
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntity(final @NotNull PlayerInteractEntityEvent event) {
		final var player = event.getPlayer();
		final var entity = event.getRightClicked();
		final var claim = Claim.getClaim(entity.getLocation());

		// Rule: You can interact with all entities outside of claims without restriction
		if (claim == null) return;

		// Rule: Interacting with armor stands and item frames as well as chest, hopper and furnace
		//       minecarts and horses and mules requires the STEAL group
		if (Predicates.hasEntityContainer(entity)) {
			if (claim.isAllowed(player, Group.STEAL)) return;
		}

		event.setCancelled(true);
	}
}
