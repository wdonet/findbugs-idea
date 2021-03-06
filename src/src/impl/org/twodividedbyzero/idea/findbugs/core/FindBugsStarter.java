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
package org.twodividedbyzero.idea.findbugs.core;


import com.intellij.compiler.impl.CompositeScope;
import com.intellij.compiler.impl.OneProjectItemCompileScope;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Consumer;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import edu.umd.cs.findbugs.config.UserPreferences;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.twodividedbyzero.idea.findbugs.common.EventDispatchThreadHelper;
import org.twodividedbyzero.idea.findbugs.common.FindBugsPluginConstants;
import org.twodividedbyzero.idea.findbugs.common.util.IdeaUtilImpl;
import org.twodividedbyzero.idea.findbugs.gui.PluginGuiCallback;
import org.twodividedbyzero.idea.findbugs.messages.AnalysisAbortingListener;
import org.twodividedbyzero.idea.findbugs.messages.MessageBusManager;
import org.twodividedbyzero.idea.findbugs.preferences.AnalysisEffort;
import org.twodividedbyzero.idea.findbugs.preferences.FindBugsPreferences;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author Reto Merz<reto.merz@gmail.com>
 * @since 0.9.995
 */
public abstract class FindBugsStarter implements AnalysisAbortingListener {

	private static final Logger LOGGER = Logger.getInstance(FindBugsStarter.class.getName());

	private final Project _project;
	private final String _title;
	private final FindBugsPreferences _preferences;
	private final Map<String, String> _bugCategories;
	private final boolean _startInBackground;
	private final FindBugsPlugin _findBugsPlugin;
	private final AtomicBoolean _cancellingByUser;


	public FindBugsStarter(@NotNull final Project project, @NotNull final String title, @NotNull final FindBugsPreferences preferences) {
		this(project, title, preferences, false);
	}


	public FindBugsStarter(@NotNull final Project project, @NotNull final String title, @NotNull final FindBugsPreferences preferences, boolean forceStartInBackground) {
		_project = project;
		_title = title;
		_preferences = preferences;
		_bugCategories = new HashMap<String, String>(preferences.getBugCategories());
		_startInBackground = preferences.getBooleanProperty(FindBugsPreferences.RUN_ANALYSIS_IN_BACKGROUND, false) || forceStartInBackground;
		_findBugsPlugin = IdeaUtilImpl.getPluginComponent(_project);
		_cancellingByUser = new AtomicBoolean();
		MessageBusManager.subscribe(project, this, AnalysisAbortingListener.TOPIC, this);
	}


	@SuppressWarnings("SimplifiableIfStatement") // debug style
	protected boolean isCompileBeforeAnalyze() {
		final String b = _preferences.getProperty(FindBugsPreferences.COMPILE_BEFORE_ANALYZE);
		if (StringUtil.isEmptyOrSpaces(b)) {
			return true; // default
		}
		return Boolean.parseBoolean(b);
	}


	public final void start() {
		EventDispatchThreadHelper.checkEDT();
		if (isCompileBeforeAnalyze()) {
			final boolean isAnalyzeAfterCompile = _preferences.isAnalyzeAfterCompile();
			final CompilerManager compilerManager = CompilerManager.getInstance(_project);
			createCompileScope(compilerManager, new Consumer<CompileScope>() {
				@Override
				public void consume(@Nullable final CompileScope compileScope) {
					if (compileScope != null) {
						compilerManager.make(compileScope, new CompileStatusNotification() {
							@Override
							public void finished(final boolean aborted, final int errors, final int warnings, final CompileContext compileContext) {
								if (!aborted && errors == 0 && !isAnalyzeAfterCompile) {
									EventDispatchThreadHelper.checkEDT(); // see javadoc of CompileStatusNotification
									startImpl();
								}
							}
						});
					}
				}
			});
		} else {
			startImpl();
		}
	}


	private void startImpl() {
		MessageBusManager.publishAnalysisStarted(_project);

		if (Boolean.valueOf(_preferences.getProperty(FindBugsPreferences.TOOLWINDOW_TO_FRONT))) {
			final ToolWindow toolWindow = ToolWindowManager.getInstance(_project).getToolWindow(FindBugsPluginConstants.TOOL_WINDOW_ID);
			IdeaUtilImpl.activateToolWindow(toolWindow);
		}

		new Task.Backgroundable(_project, _title, true) {
			@Override
			public void run(@NotNull final ProgressIndicator indicator) {
				indicator.setIndeterminate(true);
				indicator.setText("Configure FindBugs...");
				try {
					asyncStart(indicator);
				} catch (ProcessCanceledException e) {
					MessageBusManager.publishAnalysisAbortedToEDT(_project);
				}
			}
			@Override
			public boolean shouldStartInBackground() {
				return _startInBackground;
			}
		}.queue();
	}


	private void asyncStart(@NotNull final ProgressIndicator indicator) {

		final UserPreferences userPrefs = _preferences.getUserPreferences();
		{
			userPrefs.setEffort(_preferences.getProperty(FindBugsPreferences.ANALYSIS_EFFORT_LEVEL, AnalysisEffort.DEFAULT.getEffortLevel()));
			final ProjectFilterSettings projectFilterSettings = userPrefs.getFilterSettings();
			projectFilterSettings.setMinPriority(_preferences.getProperty(FindBugsPreferences.MIN_PRIORITY_TO_REPORT));
			configureSelectedCategories(_preferences, projectFilterSettings);
			userPrefs.setIncludeFilterFiles(_preferences.getIncludeFiltersMap());
			userPrefs.setExcludeBugsFiles(_preferences.getExcludeBaselineBugsMap());
			userPrefs.setExcludeFilterFiles(_preferences.getExcludeFiltersMap());
		}

		final FindBugsProject findBugsProject = new FindBugsProject();
		{
			findBugsProject.setProjectName(_project.getName());
			for (final Plugin plugin : Plugin.getAllPlugins()) {
				findBugsProject.setPluginStatusTrinary(plugin.getPluginId(), !_preferences.isPluginDisabled(plugin.getPluginId()));
			}
			findBugsProject.setGuiCallback(new PluginGuiCallback(_findBugsPlugin));
		}

		final SortedBugCollection bugCollection = new SortedBugCollection(findBugsProject);
		bugCollection.setDoNotUseCloud(true);

		ApplicationManager.getApplication().runReadAction(new Runnable() {
			@Override
			public void run() {
				configure(indicator, findBugsProject);
			}
		});

		final Reporter reporter = new Reporter(_project, bugCollection, _bugCategories, indicator, _cancellingByUser);
		reporter.setPriorityThreshold(userPrefs.getUserDetectorThreshold());

		final FindBugs2 engine = new FindBugs2();
		{
			engine.setNoClassOk(true);
			engine.setMergeSimilarWarnings(false);
			engine.setBugReporter(reporter);
			engine.setProject(findBugsProject);
			engine.setProgressCallback(reporter);
			configureFilter(engine, _project, userPrefs);

			final DetectorFactoryCollection factoryCollection = FindBugsPreferences.getDetectorFactorCollection();
			engine.setDetectorFactoryCollection(factoryCollection);
			engine.setUserPreferences(userPrefs);
		}

		indicator.setText("Start FindBugs...");
		Throwable error = null;
		try {
			engine.execute();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final Throwable e) {
			error = e;
		} finally {
			engine.dispose();
		}

		if (reporter.isCanceled()) {
			MessageBusManager.publishAnalysisAbortedToEDT(_project);
		} else {
			MessageBusManager.publishAnalysisFinishedToEDT(_project, reporter.getBugCollection(), findBugsProject, error);
		}

		bugCollection.setDoNotUseCloud(false);
		bugCollection.setTimestamp(System.currentTimeMillis());
		bugCollection.reinitializeCloud();
	}


	protected abstract void createCompileScope(@NotNull final CompilerManager compilerManager, @NotNull final  Consumer<CompileScope> consumer);


	// like CompilerManagerImpl#createFilesCompileScope but Collection based
	@NotNull
	protected final CompileScope createFilesCompileScope(@NotNull final Collection<VirtualFile> files) {
		final CompileScope[] scopes = new CompileScope[files.size()];
		int i = 0;
		for (final VirtualFile file : files){
			scopes[i++] = new OneProjectItemCompileScope(_project, file);
		}
		return new CompositeScope(scopes);
	}


	protected abstract void configure(@NotNull final ProgressIndicator indicator, @NotNull final FindBugsProject findBugsProject);


	@Override
	public final void analysisAborting() {
		_cancellingByUser.set(true);
	}


	private static void configureSelectedCategories(@NotNull final FindBugsPreferences preferences, @NotNull final ProjectFilterSettings filterSettings) {
		for (final Map.Entry<String, String> category : preferences.getBugCategories().entrySet()) {
			if ("true".equals(category.getValue())) {
				filterSettings.addCategory(category.getKey());
			} else {
				filterSettings.removeCategory(category.getKey());
			}
		}
	}


	private static void configureFilter(@NotNull final FindBugs2 engine, @NotNull final Project project, @NotNull final UserPreferences userPrefs) {
		final Map<String, Boolean> excludeFilterFiles = userPrefs.getExcludeFilterFiles();
		for (final Map.Entry<String, Boolean> excludeFileName : excludeFilterFiles.entrySet()) {
			try {
				engine.addFilter(IdeaUtilImpl.expandPathMacro(project, excludeFileName.getKey()), false);
			} catch (final IOException e) {
				LOGGER.error("ExcludeFilter configuration failed.", e);
			}
		}
		final Map<String, Boolean> includeFilterFiles = userPrefs.getIncludeFilterFiles();
		for (final Map.Entry<String, Boolean> includeFileName : includeFilterFiles.entrySet()) {
			try {
				engine.addFilter(IdeaUtilImpl.expandPathMacro(project, includeFileName.getKey()), true);
			} catch (final IOException e) {
				LOGGER.error("IncludeFilter configuration failed.", e);
			}
		}
		final Map<String, Boolean> excludeBugFiles = userPrefs.getExcludeBugsFiles();
		for (final Map.Entry<String, Boolean> excludeBugFile : excludeBugFiles.entrySet()) {
			try {
				engine.excludeBaselineBugs(IdeaUtilImpl.expandPathMacro(project, excludeBugFile.getKey()));
			} catch (final IOException e) {
				LOGGER.error("ExcludeBaseLineBug files configuration failed.", e);
			} catch (final DocumentException e) {
				LOGGER.error("ExcludeBaseLineBug files configuration failed.", e);
			}
		}
	}
}