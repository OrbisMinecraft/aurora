// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.AuroraUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player in the Aurora database.
 */
@DatabaseTable(tableName = "users")
public final class User {
	public static final String METADATA_KEY = "aurora.user";
	// TODO: User params per world!

	@DatabaseField(id = true)
	public UUID id;

	@DatabaseField(canBeNull = false)
	public int totalClaimBlocks = 0;

	@DatabaseField(canBeNull = false)
	public int usedClaimBlocks = 0;

	@DatabaseField(canBeNull = false)
	public int totalClaimsUsed = 0;

	// Temporary, non-persistent data
	public int runningVisualizationTasks = 0;
	public boolean adminMode = false;
	public Location lastToolLocation = null;
	public Claim lastSelectedClaim = null;

	public User(final UUID id, int totalClaims) {
		this.id = id;
		this.totalClaimBlocks = totalClaims;
	}

	@SuppressWarnings("ProtectedMemberInFinalClass")
	protected User() {
	}

	public static @Nullable User get(final UUID id) {
		try {
			return Aurora.db.users.queryForId(id);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to get user (%s): %s".formatted(id, e));
			return null;
		}
	}

	public static @Nullable User fromMetadata(final @NotNull Player player) {
		return AuroraUtil.getScalarMetadata(METADATA_KEY, player);
	}

	public @NotNull List<Claim> getClaims() {
		try {
			return Aurora.db.claims.queryBuilder().where()
					.eq("owner", this.id)
					.query();
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to get user's claims (%s): %s".formatted(id, e));
			return List.of();
		}
	}

	public void update() {
		try {
			Aurora.db.users.update(this);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to update user (%s): %s".formatted(id, e));
		}
	}

	public void save() {
		try {
			Aurora.db.users.create(this);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to create user (%s): %s".formatted(id, e));
		}
	}

	public void refresh() {
		try {
			Aurora.db.users.refresh(this);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to refresh user (%s): %s".formatted(id, e));
		}
	}
}
