// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum Group {
	OWNER(5),
	MANAGE(4),
	BUILD(3),
	STEAL(2),
	ACCESS(1),
	NONE(0);

	private final int no;

	Group(int no) {
		this.no = no;
	}

	@Contract(pure = true)
	public boolean encompasses(final @NotNull Group other) {
		return other.no <= this.no;
	}
}
