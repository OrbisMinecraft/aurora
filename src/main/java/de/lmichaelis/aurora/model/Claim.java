// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import de.lmichaelis.aurora.Aurora;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a claim or sub-claim owned by a player. Contains the claim's
 * configuration values.
 */
@DatabaseTable(tableName = "claims")
public final class Claim {
	@DatabaseField(generatedId = true)
	public int id;

	@DatabaseField
	public String name;

	@DatabaseField(canBeNull = false)
	public Date createdAt;

	@DatabaseField(canBeNull = false, columnName = "min_x", index = true, indexName = "claims_location_idx")
	public int minX;

	@DatabaseField(canBeNull = false, columnName = "min_y", index = true, indexName = "claims_location_idx")
	public int minY;

	@DatabaseField(canBeNull = false, columnName = "min_z", index = true, indexName = "claims_location_idx")
	public int minZ;

	@DatabaseField(canBeNull = false, columnName = "max_x", index = true, indexName = "claims_location_idx")
	public int maxX;

	@DatabaseField(canBeNull = false, columnName = "max_y", index = true, indexName = "claims_location_idx")
	public int maxY;

	@DatabaseField(canBeNull = false, columnName = "max_z", index = true, indexName = "claims_location_idx")
	public int maxZ;

	@DatabaseField(columnName = "parent_id", foreign = true, index = true, indexName = "claims_parent_idx")
	public Claim parent;

	@DatabaseField(canBeNull = false)
	public UUID owner;

	@DatabaseField(canBeNull = false, width = 64, index = true, indexName = "claims_location_idx")
	public String world;

	@DatabaseField(canBeNull = false, columnName = "mob_griefing", defaultValue = "false")
	public boolean mobGriefing;

	@DatabaseField(canBeNull = false, columnName = "pvp_enabled", defaultValue = "false")
	public boolean pvpEnabled;

	@DatabaseField(canBeNull = false, columnName = "allows_enabled", defaultValue = "false")
	public boolean allowsExplosions;

	@DatabaseField(canBeNull = false, columnName = "is_admin", defaultValue = "false")
	public boolean isAdmin;

	@DatabaseField(canBeNull = false, columnName = "is_restricted", defaultValue = "false")
	public boolean restricted;

	@ForeignCollectionField(foreignFieldName = "claim", eager = true)
	public ForeignCollection<UserGroup> userGroups;

	public Claim(final @NotNull UUID owner, final @NotNull String name,
				 final @NotNull Location cornerA, final @NotNull Location cornerB) {
		this.owner = owner;
		this.name = name;
		this.world = cornerA.getWorld().getName();
		this.createdAt = Date.from(Instant.now());
		this.mobGriefing = false;
		this.pvpEnabled = false;
		this.allowsExplosions = false;
		this.isAdmin = false;
		this.restricted = false;

		this.minX = Math.min(cornerA.getBlockX(), cornerB.getBlockX());
		this.minY = Math.min(cornerA.getBlockY(), cornerB.getBlockY());
		this.minZ = Math.min(cornerA.getBlockZ(), cornerB.getBlockZ());
		this.maxX = Math.max(cornerA.getBlockX(), cornerB.getBlockX());
		this.maxY = Math.max(cornerA.getBlockY(), cornerB.getBlockY());
		this.maxZ = Math.max(cornerA.getBlockZ(), cornerB.getBlockZ());
	}

	public Claim(final @NotNull Claim parent, final @NotNull Location cornerA, final @NotNull Location cornerB) {
		this.parent = parent;
		this.owner = parent.owner;
		this.world = parent.world;
		this.createdAt = Date.from(Instant.now());
		this.mobGriefing = false;
		this.pvpEnabled = false;
		this.allowsExplosions = false;
		this.isAdmin = false;
		this.restricted = false;

		this.minX = Math.min(cornerA.getBlockX(), cornerB.getBlockX());
		this.minY = Math.min(cornerA.getBlockY(), cornerB.getBlockY());
		this.minZ = Math.min(cornerA.getBlockZ(), cornerB.getBlockZ());
		this.maxX = Math.max(cornerA.getBlockX(), cornerB.getBlockX());
		this.maxY = Math.max(cornerA.getBlockY(), cornerB.getBlockY());
		this.maxZ = Math.max(cornerA.getBlockZ(), cornerB.getBlockZ());
	}

	@SuppressWarnings("ProtectedMemberInFinalClass")
	protected Claim() {
	}

	public List<Claim> getSubClaims() {
		try {
			return Aurora.db.claims.queryBuilder().where().eq("parent_id", this.id).query();
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to get subclaims of claim %d: %s".formatted(id, e));
			return List.of();
		}
	}

	/**
	 * Gets the claim at the given location.
	 *
	 * @param location The location to query a claim for.
	 * @return A claim if there is one at the given location and <tt>null</tt> if not.
	 */
	public static @Nullable Claim getClaim(final @NotNull Location location) {
		// TODO: When regions are loaded, query the database for claims in the region and cache them.
		//       Don't unload them from the cache. If we want to find a chunk, iterate through all chunks
		//       of the region of the interaction.
		try {
			return Aurora.db.claims.queryBuilder().where()
					.eq("world", location.getWorld().getName()).and()
					.le("min_x", location.getBlockX()).and()
					.ge("max_x", location.getBlockX()).and()
					.le("min_y", location.getBlockY()).and()
					.ge("max_y", location.getBlockY()).and()
					.le("min_z", location.getBlockZ()).and()
					.ge("max_z", location.getBlockZ())
					.queryBuilder()
					.orderByNullsLast("parent_id", false)
					.queryForFirst();
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to get claim at %s: %s".formatted(location, e));
			return null;
		}
	}

	public static @Nullable Claim getClaimIfDifferent(final @Nullable Claim other, final @NotNull Location location) {
		// TODO: I have to disable this optimization because it breaks the plugin's behaviour when combined with
		//  sub-claims. (ie. a block in a sub-claim is always also in the parent claim)
		// if (other != null && other.contains(location)) return other;
		return getClaim(location);
	}

	public static boolean intersects(final @NotNull Location areaCornerA, final @NotNull Location areaCornerB, boolean ignoreY) {
		return intersects(areaCornerA, areaCornerB, ignoreY, null);
	}

	public static boolean intersects(final @NotNull Location areaCornerA, final @NotNull Location areaCornerB, boolean ignoreY, Claim ignoredClaim) {
		return intersects(areaCornerA, areaCornerB, ignoreY, ignoredClaim, false);
	}

	public static boolean intersects(final @NotNull Location areaCornerA, final @NotNull Location areaCornerB, boolean ignoreY, Claim ignoredClaim, boolean ignoreSubclaims) {
		final var minX = Math.min(areaCornerA.getBlockX(), areaCornerB.getBlockX());
		final var minZ = Math.min(areaCornerA.getBlockZ(), areaCornerB.getBlockZ());
		final var maxX = Math.max(areaCornerA.getBlockX(), areaCornerB.getBlockX());
		final var maxZ = Math.max(areaCornerA.getBlockZ(), areaCornerB.getBlockZ());

		final var minY = Math.min(areaCornerA.getBlockY(), areaCornerB.getBlockY());
		final var maxY = Math.max(areaCornerA.getBlockY(), areaCornerB.getBlockY());

		try {
			final var query = Aurora.db.claims.queryBuilder().where()
					.eq("world", areaCornerA.getWorld().getName()).and()
					.le("min_x", maxX).and()
					.ge("max_x", minX).and()
					.le("min_z", maxZ).and()
					.ge("max_z", minZ);

			if (ignoredClaim != null) query.and().ne("id", ignoredClaim.id);
			if (ignoreSubclaims) query.and().isNull("parent_id");

			if (!ignoreY) {
				query.and()
						.le("min_y", maxY).and()
						.ge("max_y", minY);
			}

			return query.countOf() > 0;
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to get claim: %s".formatted(e));
			return true;
		}
	}

	public int size() {
		return ((maxX - minX) + 1) * ((maxZ - minZ) + 1);
	}

	/**
	 * Saves the claim into the database.
	 */
	public void save() {
		try {
			Aurora.db.claims.create(this);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to create a claim: %s".formatted(e));
		}
	}

	/**
	 * Updates the claim in the database.
	 */
	public void update() {
		try {
			Aurora.db.claims.update(this);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to update a claim: %s".formatted(e));
		}
	}

	/**
	 * Deletes the claim from the database.
	 */
	public void delete() {
		try {
			Aurora.db.claims.delete(this);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to delete a claim: %s".formatted(e));
		}
	}

	/**
	 * Sets the given group for the given player in the claim.
	 *
	 * @param player The player to set the group for.
	 * @param group  The group to set.
	 */
	public void setGroup(final @NotNull OfflinePlayer player, final Group group) {
		try {
			for (final var gr : this.userGroups) {
				if (gr.player.equals(player.getUniqueId())) {
					gr.group = group;
					Aurora.db.userGroups.update(gr);
				}
			}

			final var userGroup = new UserGroup(this, player.getUniqueId(), group);
			Aurora.db.userGroups.create(userGroup);
			Aurora.db.claims.refresh(this);
		} catch (SQLException e) {
			Aurora.logger.severe("Failed to set a player group: %s".formatted(e));
		}
	}

	/**
	 * Gets the group the given player is in.
	 *
	 * @param player The player to get the group for.
	 * @return The group the player is in.
	 */
	public Group getGroup(final @NotNull OfflinePlayer player) {
		if (Objects.equals(player.getUniqueId(), this.owner)) return Group.OWNER;
		if (this.userGroups == null) {
			// Oh god, this is inefficient
			try {
				Aurora.db.claims.refresh(this);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		for (final var group : this.userGroups) {
			if (group.player.equals(player.getUniqueId())) {
				return group.group;
			}
		}

		if (!restricted && parent != null) {
			return this.parent.getGroup(player);
		}

		return Group.NONE;
	}

	public boolean isAllowed(final @NotNull OfflinePlayer player, final Group group) {
		if (player instanceof final Player online) {
			final var user = Objects.requireNonNull(User.fromMetadata(online));

			if (this.isAdmin && online.hasPermission("aurora.admin.claims")) return true;
			if (user.adminMode && !isAdmin) return true;
		}

		return getGroup(player).encompasses(group);
	}

	/**
	 * Checks whether the given location is inside the claim.
	 *
	 * @param location The location to check.
	 * @return <tt>true</tt> if the location is in the claim and <tt>false</tt> if it is not.
	 */
	public boolean contains(final @NotNull Location location) {
		return location.getBlockX() >= minX &&
				location.getBlockX() <= maxX &&
				location.getBlockY() >= minY &&
				location.getBlockY() <= maxY &&
				location.getBlockZ() >= minZ &&
				location.getBlockZ() <= maxZ &&
				Objects.equals(location.getWorld().getName(), world);
	}
}
