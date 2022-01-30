// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.interactions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for interactions with Aurora itself.
 */
public interface InteractionHandler {
	void handle(final @NotNull Player player, final @NotNull Location location);
}
