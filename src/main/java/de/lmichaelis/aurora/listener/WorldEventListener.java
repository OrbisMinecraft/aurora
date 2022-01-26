// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event handlers for world events.
 */
public final class WorldEventListener extends BaseListener {
	public WorldEventListener(final Aurora plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onStructureGrow(final @NotNull StructureGrowEvent event) {
		final var root = event.getLocation();
		final var rootClaim = Claim.getClaim(root);

		// Rule: Trees can't grow into neighboring claims
		final var iter = event.getBlocks().iterator();
		while (iter.hasNext()) {
			final var block = iter.next();
			final var blockClaim = Claim.getClaimIfDifferent(rootClaim, block.getLocation());

			if (blockClaim != null && (rootClaim == null || rootClaim.owner != blockClaim.owner)) {
				iter.remove();
			}
		}
	}
}
