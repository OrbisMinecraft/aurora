// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.User;
import de.lmichaelis.aurora.model.UserGroup;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Aurora's database access manager.
 */
public class Database {
	public final Dao<Claim, Integer> claims;
	public final Dao<User, UUID> users;
	public final Dao<UserGroup, Integer> userGroups;

	private final JdbcConnectionSource source;

	public Database(final String uri) throws SQLException {
		this.source = new JdbcConnectionSource(uri);
		this.claims = DaoManager.createDao(this.source, Claim.class);
		this.users = DaoManager.createDao(this.source, User.class);
		this.userGroups = DaoManager.createDao(this.source, UserGroup.class);

		// Create the tables if needed
		TableUtils.createTableIfNotExists(source, Claim.class);
		TableUtils.createTableIfNotExists(source, User.class);
		TableUtils.createTableIfNotExists(source, UserGroup.class);
	}

	public void onDisable() throws IOException {
		this.source.close();
	}
}
