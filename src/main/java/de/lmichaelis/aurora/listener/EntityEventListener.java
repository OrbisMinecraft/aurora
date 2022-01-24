// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event handlers for entity events.
 */
public final class EntityEventListener extends BaseListener {
	public EntityEventListener(final Aurora plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityBreakDoor(final @NotNull EntityBreakDoorEvent event) {
		final var claim = Claim.getClaim(event.getBlock().getLocation());

		// Rule: Zombies can always break doors outside of claims
		if (claim == null) return;

		// Rule: If mob griefing is enabled in the claim, zombies can break doors
		if (claim.mobGriefing) return;

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(final @NotNull EntityInteractEvent event) {
		final var block = event.getBlock();

		// We're only interested in farmland at the moment
		if (block.getType() == Material.FARMLAND) {
			final var claim = Claim.getClaim(block.getLocation());

			// Rule: Entities can always trample crops outside of claims
			if (claim == null) return;

			// Rule: Inside of claims, entities can only trample crops if mob griefing is enabled
			if (claim.mobGriefing) return;

			event.setCancelled(true);
		}
	}
}
