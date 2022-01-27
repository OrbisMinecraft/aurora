// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event handlers for vehicle events.
 */
public final class VehicleEventListener extends BaseListener {
	public VehicleEventListener(final Aurora plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onVehicleDamage(final @NotNull VehicleDamageEvent event) {
		final var attacker = event.getAttacker();
		final var vehicle = event.getVehicle();

		Player player = null;
		if (attacker instanceof Player) {
			player = (Player) attacker;
		} else if (attacker instanceof Projectile) {
			final var shooter = ((Projectile) attacker).getShooter();
			if (shooter instanceof Player) player = (Player) shooter;
		}

		final var claim = Claim.getClaim(vehicle.getLocation());

		// Rule: Vehicles outside of claims can always be damaged
		if (claim == null) return;

		// Rule: If the event wasn't caused by a player mob-griefing is enabled, allow the damage to go through
		if (player == null && claim.mobGriefing) return;

		// Rule: If the event was caused by a player, check that they're in the CONTAINERS group
		if (player != null && claim.isAllowed(player, Group.CONTAINERS)) return;

		if (player != null) player.sendMessage(plugin.config.messages.noPermission);
		event.setCancelled(true);
	}
}
