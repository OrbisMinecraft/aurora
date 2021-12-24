// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import org.bukkit.event.Listener;

/**
 * The base class of all listeners.
 */
public abstract class BaseListener implements Listener {
	protected final Aurora plugin;

	protected BaseListener(Aurora plugin) {
		this.plugin = plugin;
	}
}
