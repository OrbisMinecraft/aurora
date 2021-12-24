// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

/**
 * Contains permissions of users in claims. Every user has a certain group in a claim which can
 * be assigned by the claim owner.
 */
@DatabaseTable(tableName = "user_group")
public final class UserGroup {
	@DatabaseField(generatedId = true)
	public int id;

	@DatabaseField(canBeNull = false, foreign = true)
	public Claim claim;

	@DatabaseField(canBeNull = false)
	public UUID player;

	@DatabaseField(canBeNull = false)
	public Group group;

	UserGroup(final Claim claim, final UUID player, final Group group) {
		this.claim = claim;
		this.player = player;
		this.group = group;
	}

	@SuppressWarnings("ProtectedMemberInFinalClass")
	protected UserGroup() {
	}
}
