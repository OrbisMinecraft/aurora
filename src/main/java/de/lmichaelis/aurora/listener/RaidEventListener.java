// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.event.EventHandler;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event handlers for raid events.
 */
public final class RaidEventListener extends BaseListener {
	public RaidEventListener(final Aurora plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onRaidTrigger(final @NotNull RaidTriggerEvent event) {
		final var player = event.getPlayer();
		final var claim = Claim.getClaim(player.getLocation());

		// Rule: Players can always trigger raid outside of claims
		if (claim == null) return;

		// Rule: Triggering raids inside claims requires the player to be in the BUILD group
		if (claim.isAllowed(player, Group.BUILD)) return;

		player.sendMessage(plugin.config.messages.raidPrevented);
		event.setCancelled(true);
	}
}
