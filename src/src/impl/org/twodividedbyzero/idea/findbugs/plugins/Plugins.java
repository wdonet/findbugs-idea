/*
 * Copyright 2008-2015 Andre Pfeiler
 *
 * This file is part of FindBugs-IDEA.
 *
 * FindBugs-IDEA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FindBugs-IDEA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FindBugs-IDEA.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.twodividedbyzero.idea.findbugs.plugins;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.twodividedbyzero.idea.findbugs.common.util.IoUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;


/**
 * $Date: 2014-06-01 23:40:00 +0100 (So, 01 June 2014) $
 *
 * @author Reto Merz<reto.merz@gmail.com>
 * @since 0.9.993
 */
public enum Plugins {

	// https://code.google.com/p/findbugs-for-android/
	AndroidFindbugs_0_5("AndroidFindbugs_0.5.jar", true),

	// http://fb-contrib.sourceforge.net/
	fb_contrib_6_2_1("fb-contrib-6.2.1.jar", false, "fb-contrib-6.0.0.jar", "fb-contrib-5.2.1.jar"),

	// http://h3xstream.github.io/find-sec-bugs/
	findsecbugs_plugin_1_4_1("findsecbugs-plugin-1.4.1.jar", false, "findsecbugs-plugin-1.4.0.jar", "findsecbugs-plugin-1.3.0.jar", "findsecbugs-plugin-1.2.1.jar", "findsecbugs-plugin-1.2.0.jar");

	public static final String PLUGIN_ID_ANDROID = "jp.f.dev.findbugs.detect.android";

	private static final Logger LOGGER = Logger.getInstance(Plugins.class.getName());

	private final String _jarName;
	private final boolean _needsJava7OrLater;
	private final String[] _legacyJarNames;


	Plugins(@NotNull final String jarName, boolean needsJava7OrLater, @NotNull final String... legacyJarNames) {
		_jarName = jarName;
		_needsJava7OrLater = needsJava7OrLater;
		_legacyJarNames = legacyJarNames;
	}


	@NotNull
	private static File findAccessibleFindBugsIdeaPluginPath(@NotNull final IdeaPluginDescriptor pluginDescriptor) {
		File ret = pluginDescriptor.getPath();
		if (!ret.isDirectory() || !ret.canWrite()) {
			// PathManager.getSystemPath() may return relative path
			final File pluginsPath = new File(PathManager.getPluginsPath()).getAbsoluteFile();
			if (!pluginsPath.isDirectory() || !pluginsPath.canWrite()) {
				throw new IllegalStateException("No accessible FindBugs-IDEA plugin directory found. Tested candidates:\n" +
						"'" + ret.getAbsolutePath() + "': isDirectory=" + ret.isDirectory() + ", canWrite=" + ret.canWrite() + "\n" +
						"'" + pluginsPath.getAbsolutePath() + "': isDirectory=" + pluginsPath.isDirectory() + ", canWrite=" + pluginsPath.canWrite());
			}
			ret = new File(pluginsPath, "FindBugs-IDEA");
			if (!ret.isDirectory()) {
				if (!ret.mkdirs()) {
					throw new IllegalStateException("Could not create alternative FindBugs-IDEA plugin directory: " + ret.getAbsolutePath());
				}
			}
		}
		return ret;
	}


	@NotNull
	public static File getDirectory(@NotNull final IdeaPluginDescriptor pluginDescriptor) {
		final File homeDir = findAccessibleFindBugsIdeaPluginPath(pluginDescriptor);
		final File dir = new File(homeDir, "customPlugins");
		if (!dir.isDirectory()) {
			if (!dir.mkdirs()) {
				throw new IllegalStateException("Could not create FindBugs-IDEA custom plugins directory: " + dir.getAbsolutePath());
			}
		}
		return dir;
	}


	public static void deploy(@NotNull final IdeaPluginDescriptor plugin) {
		final boolean isJava7OrLater = isJava7OrLater();
		final File dir = getDirectory(plugin);
		for (final Plugins customPlugin : values()) {
			for (final String legacyJarName : customPlugin._legacyJarNames) {
				final File legacyJar = new File(dir, legacyJarName);
				if (legacyJar.exists()) {
					if (!legacyJar.delete()) {
						LOGGER.warn("Could not delete legacy custom plugin: " + legacyJar.getAbsolutePath());
					}
				}
			}
			final File jar = new File(dir, customPlugin._jarName);
			if (customPlugin._needsJava7OrLater && !isJava7OrLater) {
				if (jar.exists()) {
					if (!jar.delete()) {
						LOGGER.error("Could not delete custom plugin (only supported with Java 7 or later): " + jar.getAbsolutePath());
					}
				}
			} else {
				if (!jar.isFile()) {
					deployImpl(jar, customPlugin);
				}
			}
		}
	}


	private static void deployImpl(@NotNull final File file, @NotNull final Plugins plugin) {
		final InputStream in = Plugins.class.getResourceAsStream(plugin._jarName);
		if (in == null) {
			throw new IllegalStateException("Can not find plugin: " + plugin);
		}
		try {
			final FileOutputStream out = new FileOutputStream(file);
			try {
				IoUtil.copy(in, out);
			} finally {
				IoUtil.safeClose(out);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			IoUtil.safeClose(in);
		}
	}


	private static boolean isJava7OrLater() {
		final BigDecimal current = new BigDecimal(System.getProperty("java.specification.version"));
		return current.compareTo(new BigDecimal("1.7")) >= 0;
	}
}
