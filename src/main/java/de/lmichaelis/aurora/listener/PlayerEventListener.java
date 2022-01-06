// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
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

import java.util.Arrays;
import java.util.Set;

/**
 * Event handlers for player events.
 */
public final class PlayerEventListener extends BaseListener {
	// Note: This set of blocks was adapted from GriefPrevention
	private static final Set<Material> INTERACT_PROTECTED_BLOCKS = Set.of(
			Material.ANVIL,
			Material.BEACON,
			Material.BEE_NEST,
			Material.BEEHIVE,
			Material.BELL,
			Material.CAKE,
			Material.CAULDRON,
			Material.COMPOSTER,
			Material.CAVE_VINES,
			Material.CAVE_VINES_PLANT,
			Material.CHIPPED_ANVIL,
			Material.DAMAGED_ANVIL,
			Material.JUKEBOX,
			Material.PUMPKIN,
			Material.RESPAWN_ANCHOR,
			Material.ROOTED_DIRT,
			Material.SWEET_BERRY_BUSH
	);

	// Note: This set of blocks was adapted from GriefPrevention
	private static final Set<Material> BUILD_PROTECTED_BLOCKS = Set.of(
			Material.NOTE_BLOCK,
			Material.REPEATER,
			Material.DRAGON_EGG,
			Material.DAYLIGHT_DETECTOR,
			Material.COMPARATOR,
			Material.REDSTONE_WIRE
	);

	// Note: This set of blocks was adapted from GriefPrevention
	private static final Set<Material> BUILD_PROTECTED_ITEMS = Set.of(
			Material.BONE_MEAL,
			Material.ARMOR_STAND,
			Material.END_CRYSTAL,
			Material.FLINT_AND_STEEL,
			Material.INK_SAC,
			Material.GLOW_INK_SAC,
			Material.MINECART,
			Material.CHEST_MINECART,
			Material.TNT_MINECART,
			Material.HOPPER_MINECART,
			Material.FURNACE_MINECART
	);

	private static final Set<Material> SPAWN_EGGS;
	private static final Set<Material> DYES;

	static {
		SPAWN_EGGS = Set.copyOf(Arrays.stream(Material.values()).filter(material -> material.name().endsWith("_SPAWN_EGG")).toList());
		DYES = Set.copyOf(Arrays.stream(Material.values()).filter(material -> material.name().endsWith("_DYE")).toList());
	}

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
		} else if (action == Action.RIGHT_CLICK_BLOCK && hold != null) {
			if (BUILD_PROTECTED_ITEMS.contains(holdType) || DYES.contains(holdType) || SPAWN_EGGS.contains(holdType) || Tag.ITEMS_BOATS.isTagged(holdType)) {
				// Rule: Entities may only be created or altered by players with the BUILD permission
				if (claim.isAllowed(player, Group.BUILD)) return;
			}
		} else {
			final var subjectType = subject.getType();

			if (subject.getState() instanceof Container) {
				// Rule: Inventories in claims may only be accessed by players with the STEAL permission
				// TODO: Special rule for lecterns: Viewing a book in a lectern should be allowed with the ACCESS permission
				// TODO: Special rule for placing
				if (claim.isAllowed(player, Group.STEAL)) return;
			} else if (BUILD_PROTECTED_BLOCKS.contains(subjectType) || Tag.FLOWER_POTS.isTagged(subjectType)) {
				// Rule: Build-protected blocks in claims may only be accessed by players with the BUILD permission
				if (claim.isAllowed(player, Group.BUILD)) return;
			} else if (INTERACT_PROTECTED_BLOCKS.contains(subjectType) ||
					Tag.CANDLES.isTagged(subjectType) ||
					Tag.CANDLE_CAKES.isTagged(subjectType) ||
					Tag.WOODEN_TRAPDOORS.isTagged(subjectType) ||
					Tag.WOODEN_DOORS.isTagged(subjectType) ||
					Tag.FENCE_GATES.isTagged(subjectType) ||
					Tag.BUTTONS.isTagged(subjectType) ||
					subjectType == Material.LEVER) {
				// Rule: Interact-protected blocks in claims may only be accessed by players with the INTERACT permission
				if (claim.isAllowed(player, Group.INTERACT)) return;
			}
		}

		Aurora.logger.warn("Unhandled PlayerInteractEvent(item: {}, action: {}, blockClicked: {}, hand: {})",
				event.getItem(), event.getAction(), event.getClickedBlock() == null ? "null" : event.getClickedBlock().getType(),
				event.getHand());
		event.setCancelled(true);
	}
}
