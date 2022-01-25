// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Event handlers for block events.
 */
public final class BlockEventListener extends BaseListener {
	private static final EnumSet<BlockFace> HORIZONTAL_FACES = EnumSet.of(
			BlockFace.NORTH,
			BlockFace.EAST,
			BlockFace.SOUTH,
			BlockFace.WEST
	);

	public BlockEventListener(final Aurora plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
		final var block = event.getBlock();
		final var player = event.getPlayer();
		final var claim = Claim.getClaim(block.getLocation());

		// Quirk: Prevent chests from connecting across claim borders
		if (block.getBlockData() instanceof final Chest blockBD) {
			final var claimOwner = claim == null ? null : claim.owner;
			for (final var face : HORIZONTAL_FACES) {
				final var relative = block.getRelative(face);
				if (!(relative.getBlockData() instanceof final Chest relativeBD)) continue;

				final var relativeClaim = Claim.getClaim(relative.getLocation());
				final var relativeOwner = relativeClaim == null ? null : relativeClaim.owner;

				if (claimOwner == relativeOwner) continue;

				// Don't allow the chests to connect!
				relativeBD.setType(Chest.Type.SINGLE);
				relative.setBlockData(relativeBD);

				blockBD.setType(Chest.Type.SINGLE);
				block.setBlockData(blockBD);

				// Send a block update to the player to prevent a graphical glitch
				player.sendBlockChange(relative.getLocation(), relativeBD);
			}
		}

		// Rule: Blocks can be placed by anyone outside of claims
		if (claim == null) return;

		// Rule: If we're 'replacing' a lectern, this means a book was placed into one. This
		//       action requires only the STEAL permission.
		if (block.getType() == Material.LECTERN && event.getBlockReplacedState().getType() == Material.LECTERN) {
			if (claim.isAllowed(player, Group.STEAL)) return;
		}

		// Rule: Inside claims, players need the build permission to place blocks
		if (claim.isAllowed(player, Group.BUILD)) return;

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(final @NotNull BlockBreakEvent event) {
		final var player = event.getPlayer();
		final var block = event.getBlock();
		final var claim = Claim.getClaim(block.getLocation());

		// Rule: Blocks can be broken everywhere outside of claims
		if (claim == null) return;

		// Rule: Only players with the BUILD group can break blocks inside claims
		if (claim.isAllowed(player, Group.BUILD)) return;

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}
}
