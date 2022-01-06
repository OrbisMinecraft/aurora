// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

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
}
