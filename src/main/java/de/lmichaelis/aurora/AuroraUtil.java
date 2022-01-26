// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import de.lmichaelis.aurora.model.Claim;
import org.bukkit.block.Block;
import org.bukkit.metadata.Metadatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AuroraUtil {
	public static <T> @Nullable T getScalarMetadata(final @NotNull String name, final @NotNull Metadatable entity) {
		final var meta = entity.getMetadata(name);
		if (meta.isEmpty()) return null;

		// noinspection unchecked
		return (T) meta.get(0).value();
	}

	public static void neutralizeExplosion(final @NotNull Iterable<Block> affectedBlocks) {
		final var iter = affectedBlocks.iterator();
		Claim cached = null;

		while (iter.hasNext()) {
			final var block = iter.next();

			// Ignore air blocks
			if (block.getType().isAir()) continue;

			// TODO: Cache the claim for better efficiency
			cached = Claim.getClaimIfDifferent(cached, block.getLocation());

			// Rule: Explosions can affect all blocks outside of claims
			if (cached == null) continue;

			// Rule: If explosions are turned on in the claim, all blocks can be destroyed
			if (cached.allowsExplosions) continue;

			// Otherwise, prevent the block from breaking
			iter.remove();
		}
	}
}
