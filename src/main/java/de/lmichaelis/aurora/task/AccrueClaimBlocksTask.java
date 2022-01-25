// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.task;

import de.lmichaelis.aurora.event.PlayerAccrueClaimBlocksEvent;
import de.lmichaelis.aurora.model.User;
import org.bukkit.Bukkit;

/**
 * A task which adds claim blocks for every player according to the given rate.
 */
public class AccrueClaimBlocksTask implements Runnable {
	private final int rate;
	private final int limit;
	private long lastRun;

	public AccrueClaimBlocksTask(int rate, int limit) {
		this.rate = rate;
		this.limit = limit;
		this.lastRun = System.currentTimeMillis();
	}

	@Override
	public void run() {
		final var now = System.currentTimeMillis();
		final var hoursPassed = (double) (now - lastRun) / (3600.0d * 1000.0d);
		final var claimBlocksAdded = (int) Math.ceil(rate * hoursPassed);

		lastRun = now;

		for (final var player : Bukkit.getOnlinePlayers()) {
			final var user = User.fromMetadata(player);
			assert user != null;

			final var event = new PlayerAccrueClaimBlocksEvent(player, claimBlocksAdded, user.totalClaimBlocks, rate);
			if (event.callEvent() && user.totalClaimBlocks + event.getCount() <= limit) {
				user.totalClaimBlocks += event.getCount();
				user.update();
			}
		}
	}
}
