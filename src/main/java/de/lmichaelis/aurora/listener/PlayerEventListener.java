// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.Interactions;
import de.lmichaelis.aurora.Predicates;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import de.lmichaelis.aurora.model.User;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
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

	@EventHandler()
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
				player.sendMessage(plugin.config.messages.noPermission);
				event.setCancelled(true);
			}
		} else { // if (action == Action.RIGHT_CLICK_BLOCK)
			final var subjectType = subject.getType();

			if (subject.getState() instanceof Container) {
				// Rule: Inventories in claims may only be accessed by players with the STEAL permission
				// TODO: Special rule for lecterns: Viewing a book in a lectern should be allowed with
				//       the ACCESS permission
				if (!claim.isAllowed(player, Group.STEAL)) {
					player.sendMessage(plugin.config.messages.noPermission);
					event.setCancelled(true);
				}
			} else if (Predicates.isInteractBuildProtected(subjectType)) {
				// Rule: Build-protected blocks in claims may only be accessed by players with the BUILD permission
				if (!claim.isAllowed(player, Group.BUILD)) {
					player.sendMessage(plugin.config.messages.noPermission);
					event.setCancelled(true);
				}
			} else if (Predicates.isInteractAccessProtected(subjectType)) {
				// Rule: Interact-protected blocks in claims may only be accessed by players with the INTERACT permission
				if (!claim.isAllowed(player, Group.ACCESS)) {
					player.sendMessage(plugin.config.messages.noPermission);
					event.setCancelled(true);
				}
			} else if (hold != null && Predicates.isPlaceBuildProtected(holdType)) {
				// Rule: Entities may only be created or altered by players with the BUILD permission
				if (!claim.isAllowed(player, Group.BUILD)) {
					player.sendMessage(plugin.config.messages.noPermission);
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntity(final @NotNull PlayerInteractEntityEvent event) {
		final var player = event.getPlayer();
		final var entity = event.getRightClicked();
		final var claim = Claim.getClaim(entity.getLocation());

		// Rule: You can interact with all entities outside of claims without restriction
		if (claim == null) return;

		// Rule: Interacting with armor stands and item frames as well as chest, hopper and furnace
		//       minecarts and horses and mules requires the STEAL group. Additionally, picking up
		//       fishes and axolotls also requires the STEAL group.
		if (Predicates.hasEntityContainer(entity) || entity instanceof Fish || entity instanceof Axolotl) {
			if (claim.isAllowed(player, Group.STEAL)) return;
		}

		// Rule: Sitting in minecarts and boats requires the ACCESS group
		if (entity.getType() == EntityType.MINECART || entity.getType() == EntityType.BOAT) {
			if (claim.isAllowed(player, Group.ACCESS)) return;
		}

		// Rule: Dying sheep and milking cows and other animal interactions requires the ACCESS group.
		//       Note that this also includes trading with villagers.
		if (entity instanceof Animals) {
			if (claim.isAllowed(player, Group.ACCESS)) return;
		}

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerFish(final @NotNull PlayerFishEvent event) {
		final var subject = event.getCaught();
		final var player = event.getPlayer();

		// Rule: If nothing was caught, ignore the event
		if (subject == null) return;

		final var claim = Claim.getClaim(subject.getLocation());

		// Rule: Players can fish all entities outside of claims without restriction
		if (claim == null) return;

		// Rule: Only players with the ACCESS group can fish armor stands, animals and vehicles from inside claims
		if (subject.getType() == EntityType.ARMOR_STAND || subject instanceof Animals || subject instanceof Vehicle) {
			if (claim.isAllowed(player, Group.ACCESS)) return;
		}

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketEmpty(final @NotNull PlayerBucketEmptyEvent event) {
		final var player = event.getPlayer();
		var subject = event.getBlockClicked().getRelative(event.getBlockFace());

		// Quirk: If we're water-logging a block, the event actually applies to the
		//        clicked block, not the adjacent one
		if (event.getBlockClicked() instanceof Waterlogged && event.getBucket() == Material.WATER_BUCKET) {
			subject = event.getBlockClicked();
		}

		final var claim = Claim.getClaim(subject.getLocation());

		// Rule: Players can always empty buckets outside of claims
		if (claim == null) return;

		// Rule: Players can empty buckets only in claims where they have the BUILD group
		if (claim.isAllowed(player, Group.BUILD)) return;

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketFill(final @NotNull PlayerBucketFillEvent event) {
		final var player = event.getPlayer();
		final var subject = event.getBlockClicked();
		final var claim = Claim.getClaim(subject.getLocation());

		// Rule: Players can always fill buckets outside of claims
		if (claim == null) return;

		// Quirk: Milking a cow counts as a bucket fill event. We ignore it here because
		//        it is already handled by #onPlayerInteractEntity()
		if (subject.getType().isAir()) return;

		// Rule: Players can fill buckets only in claims where they have the BUILD group
		if (claim.isAllowed(player, Group.BUILD)) return;

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerTakeLecternBook(final @NotNull PlayerTakeLecternBookEvent event) {
		final var player = event.getPlayer();
		final var subject = event.getLectern();
		final var claim = Claim.getClaim(subject.getLocation());

		// Rule: You can remove books from all lecterns outside of claims
		if (claim == null) return;

		// Rule: Players remove books from lecterns only if they're in the STEAL group
		if (claim.isAllowed(player, Group.STEAL)) return;

		player.sendMessage(plugin.config.messages.noPermission);
		player.closeInventory();
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerTeleport(final @NotNull PlayerTeleportEvent event) {
		final var player = event.getPlayer();
		final var claim = Claim.getClaim(event.getTo());

		// Rule: You can teleport to anywhere in the wild
		if (claim == null) return;

		// Rule: You can only teleport into claims in which you have the ACCESS group using
		//       chorus fruit or ender pearls
		if (event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT ||
				event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			if (claim.isAllowed(player, Group.ACCESS)) return;

			// Quirk: If the player teleported using an ender pearl and is not in creative mode,
			//        we have to give it back to them
			if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL &&
					player.getGameMode() != GameMode.CREATIVE) {
				player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
			}

			player.sendMessage(plugin.config.messages.noPermission);
			event.setCancelled(true);
		}

		// Rule: Any other mode of teleportation is okay
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerEggThrow(final @NotNull PlayerEggThrowEvent event) {
		final var player = event.getPlayer();
		final var claim = Claim.getClaim(event.getEgg().getLocation());

		// Rule: Players can throw eggs anywhere in the wild
		if (claim == null) return;

		// Rule: Players can only throw eggs into claims that they have the ACCESS group in
		if (claim.isAllowed(player, Group.ACCESS)) return;

		// Quirk: We prevent the event by just disabling hatching
		event.setHatching(false);

		// Quirk: We have to give the egg back to the player only if they are in survival mode
		if (player.getGameMode() != GameMode.CREATIVE) {
			player.getInventory().addItem(event.getEgg().getItem());
		}

		player.sendMessage(plugin.config.messages.noPermission.formatted(player.getName()));
	}
}
