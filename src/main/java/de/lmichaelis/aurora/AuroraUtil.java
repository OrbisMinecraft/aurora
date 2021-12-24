// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import de.lmichaelis.aurora.model.Claim;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AuroraUtil {
	public static @Nullable Claim getLookingAtClaim(final @NotNull Player player) {
		final var rayTraceResult = player.rayTraceBlocks(100, FluidCollisionMode.SOURCE_ONLY);

		if (rayTraceResult == null || rayTraceResult.getHitBlock() == null) {
			return null;
		}

		return Claim.getClaim(rayTraceResult.getHitBlock().getLocation());
	}
}
