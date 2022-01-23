// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event handlers for hanging entity events.
 */
public final class HangingEventListener extends BaseListener {
	public HangingEventListener(final Aurora plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onHangingBreak(final @NotNull HangingBreakEvent event) {
		final var cause = event.getCause();
		final var subject = event.getEntity();

		// Rule: Hanging entities can always break due to physics (like the supporting block being removed)
		if (cause == HangingBreakEvent.RemoveCause.PHYSICS) return;

		final var claim = Claim.getClaim(subject.getLocation());

		// Rule: Hanging entities can always be destroyed outside of claims
		if (claim == null) return;

		// Rule: Hanging entities in claims can only be destroyed by players
		if (event instanceof HangingBreakByEntityEvent) {
			var remover = ((HangingBreakByEntityEvent) event).getRemover();

			// TODO: We intentionally disallow projectiles here for now, since they can be used to shoot out the
			//       item inside and item frame. If we can check for that specific event, we can allow breaking
			//       hanging entities with projectiles.
			// if (remover instanceof Projectile) remover = (Entity) ((Projectile) remover).getShooter();

			if (remover instanceof final Player player) {
				if (claim.isAllowed(player, Group.BUILD)) return;
				player.sendMessage(plugin.config.messages.noPermission);
			}
		}

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onHangingPlace(final @NotNull HangingPlaceEvent event) {
		final var player = event.getPlayer();
		final var subject = event.getEntity();
		final var claim = Claim.getClaim(subject.getLocation());

		// Rule: Hanging entities can always be placed outside of claims
		if (claim == null) return;

		// TODO: Can `player` actually be null?
		assert player != null;

		// TODO: We can't detect if a player is removing an item from an item frame yet.
		//       Maybe this is handled by another listener but I don't currently see how.
		// Rule: Only players with the BUILD group can place hanging entities inside claims
		if (claim.isAllowed(player, Group.BUILD)) return;

		player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}
}
