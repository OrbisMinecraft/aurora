// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.AuroraUtil;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.block.data.type.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;

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

				if (Objects.equals(claimOwner, relativeOwner)) continue;

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

	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(final @NotNull BlockFromToEvent event) {
		final var to = event.getToBlock();
		final var from = event.getBlock();

		final var toClaim = Claim.getClaim(to.getLocation());

		// Rule: Blocks can always move into unclaimed land
		if (toClaim == null) return;

		final var fromClaim = Claim.getClaim(from.getLocation());

		// Rule: Blocks can move into claims of the same owner
		if (fromClaim != null && Objects.equals(fromClaim.owner, toClaim.owner)) return;

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(final @NotNull BlockExplodeEvent event) {
		AuroraUtil.neutralizeExplosion(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonRetract(final @NotNull BlockPistonRetractEvent event) {
		final var affected = event.getBlocks();
		final var piston = event.getBlock();

		// Rule: Pistons that are not pulling any blocks can always retract
		if (affected.isEmpty()) return;

		final var claim = Claim.getClaim(piston.getLocation());

		// Rule: If all blocks are in the same claim as the piston it is allowed to retract
		if (claim != null) {
			if (affected.stream().allMatch(b -> claim.contains(b.getLocation()))) return;
		}

		// Rule: Pistons can move any block within claims of the same owner and they can
		//       move blocks outside of claims
		if (affected.stream().allMatch(b -> {
			if (claim != null && claim.contains(b.getLocation())) return true;

			final var otherClaim = Claim.getClaim(b.getLocation());
			return otherClaim == null || (claim != null && Objects.equals(claim.owner, otherClaim.owner));
		})) return;

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonExtend(final @NotNull BlockPistonExtendEvent event) {
		final var affected = event.getBlocks();
		final var piston = event.getBlock();
		final var claim = Claim.getClaim(piston.getLocation());

		// Rule: Pistons cannot push into a claim (even without attached blocks).
		if (affected.isEmpty()) {
			final var relativeBlock = piston.getRelative(event.getDirection());
			final var relativeClaim = Claim.getClaim(relativeBlock.getLocation());

			if ((claim == null && relativeClaim != null) ||
					(relativeClaim != null && !Objects.equals(claim.owner, relativeClaim.owner)))
				event.setCancelled(true);
			return;
		}

		// Rule: If all blocks are in the same claim as the piston it is allowed to extend
		if (claim != null) {
			if (affected.stream().allMatch(b -> claim.contains(b.getRelative(event.getDirection()).getLocation()))) return;
		}

		// Rule: Pistons can move any block within claims of the same owner, and they can
		//       move blocks outside of claims
		if (affected.stream().allMatch(b -> {
			final var relative = b.getRelative(event.getDirection());
			if (claim != null && claim.contains(relative.getLocation())) return true;

			final var otherClaim = Claim.getClaim(relative.getLocation());
			return otherClaim == null || (claim != null && Objects.equals(claim.owner, otherClaim.owner));
		})) return;

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockSpread(final @NotNull BlockSpreadEvent event) {
		final var block = event.getBlock();
		final var source = event.getSource();

		// We only care about fire here
		if (source.getType() != Material.FIRE) return;

		final var targetClaim = Claim.getClaim(block.getLocation());

		// Rule: Fire can always spread outside of claims
		if (targetClaim == null) return;

		// Rule: Fire can only spread within the same claim
		if (targetClaim.contains(source.getLocation())) return;

		final var sourceClaim = Claim.getClaim(source.getLocation());
		if (sourceClaim != null && Objects.equals(sourceClaim.owner, targetClaim.owner)) return;

		// Extinguish fire that is not placed on netherrack. This behaviour is copied from GriefPrevention
		if (source.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) source.setType(Material.AIR);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlaceMulti(final @NotNull BlockMultiPlaceEvent event) {
		final var player = event.getPlayer();

		if (event.getReplacedBlockStates().stream().allMatch(b -> {
			final var claim = Claim.getClaim(b.getLocation());
			return claim == null || claim.isAllowed(player, Group.BUILD);
		})) return;

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(final @NotNull BlockBurnEvent event) {
		final var claim = Claim.getClaim(event.getBlock().getLocation());

		// Rule: Don't allow any blocks to be destroyed by fire inside claims.
		// TODO: Add claim config option?
		if (claim == null) return;
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDispense(final @NotNull BlockDispenseEvent event) {
		final var source = event.getBlock();

		// Ignore everything but dispensers
		if (!(source.getBlockData() instanceof final Dispenser dispenser)) return;

		final var target = source.getRelative(dispenser.getFacing());
		final var targetClaim = Claim.getClaim(target.getLocation());

		// Rule: Dispensing into the wild is always allowed
		if (targetClaim == null) return;

		// Rule: Dispensing into the same claim is allowed
		if (targetClaim.contains(source.getLocation())) return;

		final var sourceClaim = Claim.getClaim(source.getLocation());
		if (sourceClaim != null && Objects.equals(sourceClaim.owner, targetClaim.owner)) return;

		event.setCancelled(true);
	}
}
