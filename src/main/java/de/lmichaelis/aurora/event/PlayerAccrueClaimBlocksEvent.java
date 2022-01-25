// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * An event that is called whenever a player is about receive some claim blocks.
 */
public class PlayerAccrueClaimBlocksEvent extends PlayerEvent {
	private static final HandlerList handlers = new HandlerList();
	private final int total;
	private final int rate;
	private int count;

	public PlayerAccrueClaimBlocksEvent(final @NotNull Player who, int count, int total, int rate) {
		super(who);
		this.count = count;
		this.total = total;
		this.rate = rate;
	}

	/**
	 * @return The number of claim blocks accrued
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Set the number of claim blocks to add for the player. Set to 0 to not add any claim blocks.
	 * If set to a negative number, claim blocks are removed from the player instead.
	 *
	 * @param count The number of claim blocks to add to the player's balance.
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @return The configured number of claim blocks a player will receive per hour.
	 */
	public int getRate() {
		return rate;
	}

	/**
	 * @return The current total number of claim blocks of the player.
	 */
	public int getTotal() {
		return total;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
