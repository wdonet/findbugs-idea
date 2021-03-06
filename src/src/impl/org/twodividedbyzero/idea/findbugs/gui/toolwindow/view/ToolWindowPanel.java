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
package org.twodividedbyzero.idea.findbugs.gui.toolwindow.view;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.diagnostic.IdeMessagePanel;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiFile;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ProjectStats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.twodividedbyzero.idea.findbugs.common.EventDispatchThreadHelper;
import org.twodividedbyzero.idea.findbugs.common.ExtendedProblemDescriptor;
import org.twodividedbyzero.idea.findbugs.common.FindBugsPluginConstants;
import org.twodividedbyzero.idea.findbugs.common.VersionManager;
import org.twodividedbyzero.idea.findbugs.common.util.FindBugsUtil;
import org.twodividedbyzero.idea.findbugs.common.util.IdeaUtilImpl;
import org.twodividedbyzero.idea.findbugs.core.FindBugsPlugin;
import org.twodividedbyzero.idea.findbugs.core.FindBugsPluginImpl;
import org.twodividedbyzero.idea.findbugs.core.FindBugsProject;
import org.twodividedbyzero.idea.findbugs.gui.common.ActionToolbarContainer;
import org.twodividedbyzero.idea.findbugs.gui.common.AnalysisRunDetailsDialog;
import org.twodividedbyzero.idea.findbugs.gui.common.BalloonTipFactory;
import org.twodividedbyzero.idea.findbugs.gui.common.MultiSplitLayout;
import org.twodividedbyzero.idea.findbugs.gui.common.MultiSplitPane;
import org.twodividedbyzero.idea.findbugs.gui.common.NDockLayout;
import org.twodividedbyzero.idea.findbugs.gui.common.NotificationUtil;
import org.twodividedbyzero.idea.findbugs.messages.AnalysisStateListener;
import org.twodividedbyzero.idea.findbugs.messages.ClearListener;
import org.twodividedbyzero.idea.findbugs.messages.MessageBusManager;
import org.twodividedbyzero.idea.findbugs.messages.NewBugInstanceListener;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.List;
import java.util.Map;


/**
 * $Date$
 *
 * @author Andre Pfeiler<andrep@twodividedbyzero.org>
 * @since 0.0.1
 */
@SuppressWarnings({"HardCodedStringLiteral", "AnonymousInnerClass", "AnonymousInnerClassMayBeStatic"})
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"SE_BAD_FIELD"})
public final class ToolWindowPanel extends JPanel implements AnalysisStateListener {

	private static final Logger LOGGER = Logger.getInstance(ToolWindowPanel.class.getName());

	private static final String A_HREF_MORE_ANCHOR = "#more";
	private static final String A_HREF_DISABLE_ANCHOR = "#disable";
	private static final String A_HREF_ERROR_ANCHOR = "#error";

	///private static final String DEFAULT_LAYOUT_DEF = "(ROW (LEAF name=left weight=0.3) (COLUMN weight=0.3 right.top right.bottom) (LEAF name=right weight=0.4))";
	private static final String DEFAULT_LAYOUT_DEF = "(ROW (LEAF name=left weight=0.4) (LEAF name=right weight=0.6))";
	private static final String PREVIEW_LAYOUT_DEF = "(ROW (LEAF name=left weight=0.3) (LEAF name=middle weight=0.4) (LEAF weight=0.3 name=right))";
	///private static final String PREVIEW_LAYOUT_DEF = "(ROW (LEAF name=left weight=0.3) (LEAF name=middle weight=0.4) (COLUMN weight=0.3 right.top right.bottom))";
	//private static final String PREVIEW_LAYOUT_DEF = "(ROW (LEAF name=left weight=0.4) (COLUMN weight=0.6 right.top right.bottom)  (LEAF name=right))";

	private final Project _project;
	private BugTreePanel _bugTreePanel;
	private transient BugDetailsComponents _bugDetailsComponents;
	private ComponentListener _componentListener;
	private MultiSplitPane _multiSplitPane;

	private boolean _previewEnabled;
	private boolean _isPreviewLayoutEnabled;
	private transient PreviewPanel _previewPanel;
	private final transient ToolWindow _parent;
	private FindBugsProject _bugsProject;


	public ToolWindowPanel(@NotNull final Project project, final ToolWindow parent) {
		_project = project;
		_parent = parent;
		checkFindBugsPlugin();
		initGui();
		MessageBusManager.subscribeAnalysisState(project, this, this);
		MessageBusManager.subscribe(project, this, ClearListener.TOPIC, new ClearListener() {
			@Override
			public void clear() {
				ToolWindowPanel.this.clear();
				DaemonCodeAnalyzer.getInstance(_project).restart();
			}
		});
		MessageBusManager.subscribe(project, this, NewBugInstanceListener.TOPIC, new NewBugInstanceListener() {
			@Override
			public void newBugInstance(@NotNull final BugInstance bugInstance, @NotNull final ProjectStats projectStats) {
				_bugTreePanel.addNode(bugInstance);
				_bugTreePanel.updateRootNode(projectStats);
			}
		});
	}


	private void initGui() {
		setLayout(new NDockLayout());
		setBorder(new EmptyBorder(1, 1, 1, 1));

		// Create the toolbars
		//final DefaultActionGroup actionGroupLeft = new DefaultActionGroup();
		//actionGroupLeft.add(new AnalyzeCurrentEditorFile());
		final ActionGroup actionGroupLeft = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_LEFT);
		final ActionToolbar toolbarLeft1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupLeft, false);

		final ActionGroup actionGroupRight = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_RIGHT);
		final ActionToolbar toolbarRight1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupRight, false);

		final ActionGroup actionGroupNavigation = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_NAVIGATION);
		final ActionToolbar toolbarNavigation1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupNavigation, false);

		final ActionGroup actionGroupUtils = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_UTILS);
		final ActionToolbar toolbarUtils1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupUtils, false);


		final Component toolbarLeft = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Analysis...", SwingConstants.VERTICAL, toolbarLeft1, true);
		final Component toolbarRight = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Grouping...", SwingConstants.VERTICAL, toolbarRight1, true);
		final Component toolbarNavigation = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Navigation...", SwingConstants.VERTICAL, toolbarNavigation1, true);
		final Component toolbarUtils = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Utils...", SwingConstants.VERTICAL, toolbarUtils1, true);


		add(toolbarLeft, NDockLayout.WEST);
		add(toolbarRight, NDockLayout.WEST);
		add(toolbarNavigation, NDockLayout.WEST);
		add(toolbarUtils, NDockLayout.WEST);

		updateLayout(false);

		add(getMultiSplitPane(), NDockLayout.CENTER);
	}


	private MultiSplitPane getMultiSplitPane() {
		if (_multiSplitPane == null) {
			_multiSplitPane = new MultiSplitPane();
			_multiSplitPane.setContinuousLayout(true);
		}
		return _multiSplitPane;
	}


	private void _updateMultiSplitLayout(final String layoutDef) {
		final MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel(layoutDef);
		final MultiSplitLayout multiSplitLayout = getMultiSplitPane().getMultiSplitLayout();
		multiSplitLayout.setDividerSize(5);
		multiSplitLayout.setModel(modelRoot);
		multiSplitLayout.setFloatingDividers(true);
	}


	private void updateLayout(final boolean enablePreviewLayout) {
		EventDispatchThreadHelper.checkEDT();
		if (!_isPreviewLayoutEnabled && enablePreviewLayout) {
			_updateMultiSplitLayout(PREVIEW_LAYOUT_DEF);
			getMultiSplitPane().add(getBugTreePanel(), "left");
			_multiSplitPane.add(getPreviewPanel().getComponent(), "middle");
			getMultiSplitPane().add(getBugDetailsComponents().getTabbedPane(), "right");
			//getMultiSplitPane().add(getBugDetailsComponents().getBugExplanationPanel(), "right.bottom");
			_isPreviewLayoutEnabled = true;

		} else if (!enablePreviewLayout) {
			getPreviewPanel().release();

			_updateMultiSplitLayout(DEFAULT_LAYOUT_DEF);
			getMultiSplitPane().add(getBugTreePanel(), "left");
			getMultiSplitPane().add(getBugDetailsComponents().getTabbedPane(), "right");
			//getMultiSplitPane().add(getBugDetailsComponents().getBugExplanationPanel(), "right.bottom");
			////getMultiSplitPane().add(_bugDetailsComponents.getCloudCommentsPanel(), "right");

			if (getPreviewPanel().getEditor() != null) {
				resizeSplitNodes(ToolWindowPanel.this);
			}
			_previewPanel = null;
			_isPreviewLayoutEnabled = false;
		}
	}


	private PreviewPanel getPreviewPanel() {
		if (_previewPanel == null) {
			_previewPanel = new PreviewPanel();
		}
		return _previewPanel;
	}


	public void setPreviewEditor(@Nullable final Editor editor, final PsiFile psiFile) {
		updateLayout(true);
		if (editor != null) {
			getPreviewPanel().add(editor, psiFile);
		} else {
			getPreviewPanel().clear();
		}
		resizeSplitNodes(this);
	}


	public void setPreviewEnabled(final boolean enabled) {
		updateLayout(false);
		_previewEnabled = enabled;
		getBugTreePanel().setPreviewEnabled(enabled);
	}


	public boolean isPreviewEnabled() {
		return _previewEnabled;
	}


	private void checkFindBugsPlugin() {
		final FindBugsPlugin findBugsPlugin = IdeaUtilImpl.getPluginComponent(_project);
		installListeners();
		if (findBugsPlugin == null) {
			throw new IllegalStateException("Couldn't get " + FindBugsPluginConstants.TOOL_WINDOW_ID + " plugin");
		}
	}


	public Project getProject() {
		return _project;
	}


	public BugCollection getBugCollection() {
		return _bugTreePanel.getBugCollection();
	}


	public FindBugsProject getBugsProject() {
		return _bugsProject;
	}


	public Map<PsiFile, List<ExtendedProblemDescriptor>> getProblems() {
		return _bugTreePanel.getProblems();
	}


	private void installListeners() {
		if (_componentListener == null) {
			_componentListener = createComponentListener();
		}
		addComponentListener(_componentListener);
	}


	@Override
	public void analysisStarted() {
		EditorFactory.getInstance().refreshAllEditors();
		DaemonCodeAnalyzer.getInstance(_project).restart();
		updateLayout(false);
		clear();
	}


	@Override
	public void analysisAborting() {
	}


	@Override
	public void analysisAborted() {
		_bugTreePanel.setBugCollection(null);
		BalloonTipFactory.showToolWindowInfoNotifier(_project, "Analysis aborted");
	}


	@Override
	public void analysisFinished(@NotNull final BugCollection bugCollection, @Nullable final FindBugsProject findBugsProject, @Nullable final Throwable error) {
		_bugTreePanel.setBugCollection(bugCollection);
		final ProjectStats stats = bugCollection.getProjectStats();
		_bugTreePanel.updateRootNode(stats);
		_bugTreePanel.getBugTree().validate();
		final int numAnalysedClasses = stats != null ? stats.getNumClasses() : 0;

		final StringBuilder message = new StringBuilder()
				.append("Found ")
				.append(_bugTreePanel.getGroupModel().getBugCount())
				.append(" bugs in ")
				.append(numAnalysedClasses)
				.append(numAnalysedClasses > 1 ? " classes" : " class");

		_bugsProject = findBugsProject;

		final NotificationType notificationType;
		if (numAnalysedClasses == 0) {
			notificationType = NotificationType.WARNING;
			message.append("&nbsp; (no class files found <a href='").append(A_HREF_MORE_ANCHOR).append("'>more...</a>)<br/>");
		} else {
			notificationType = NotificationType.INFORMATION;
			message.append("&nbsp;<a href='").append(A_HREF_MORE_ANCHOR).append("'>more...</a><br/>");
		}
		message.append("<font size='10px'>using ").append(VersionManager.getFullVersion()).append(" with Findbugs version ").append(FindBugsUtil.getFindBugsFullVersion()).append("</font><br/><br/>");

		if (error != null) {
			final boolean findBugsError = FindBugsUtil.isFindBugsError(error);
			final String impl;
			if (findBugsError) {
				impl = "FindBugs";
			} else {
				impl = "FindBugs-IDEA Plugin";
			}
			String errorText = "An " + impl + " error occurred.";

			final StatusBar statusBar = WindowManager.getInstance().getIdeFrame(_project).getStatusBar();
			final StatusBarWidget widget = statusBar.getWidget(IdeMessagePanel.FATAL_ERROR);
			IdeMessagePanel ideMessagePanel = null; // openFatals like ErrorNotifier
			if (widget instanceof IdeMessagePanel) {
				ideMessagePanel = (IdeMessagePanel)widget;
				errorText = "<a href='" + A_HREF_ERROR_ANCHOR + "'>" + errorText + "</a>";
			}

			message.append(String.format("%s The results of the analysis might be incomplete.", errorText));
			LOGGER.error(error);
			// use balloon because error should never disabled
			BalloonTipFactory.showToolWindowErrorNotifier(_project, message.toString(), new BalloonErrorListenerImpl(ToolWindowPanel.this, _bugsProject, ideMessagePanel));
		} else {
			message.append("<a href='").append(A_HREF_DISABLE_ANCHOR).append("'>Disable notification").append("</a>");
			FindBugsPluginImpl.NOTIFICATION_GROUP_ANALYSIS_FINISHED.createNotification(
					VersionManager.getName() + ": Analysis Finished",
					message.toString(),
					notificationType,
					new NotificationListenerImpl(ToolWindowPanel.this, _bugsProject)
			).setImportant(false).notify(_project);
		}

		EditorFactory.getInstance().refreshAllEditors();
		DaemonCodeAnalyzer.getInstance(_project).restart();
	}


	private ComponentListener createComponentListener() {
		return new ToolWindowComponentAdapter(this);
	}


	private void resizeSplitNodes(final Component component) {
		final int width = component.getWidth();
		final int height = component.getHeight();
		if (isPreviewEnabled() && getPreviewPanel().getEditor() != null) {
			_bugDetailsComponents.adaptSize((int) (width * 0.3), height);
			_bugTreePanel.adaptSize((int) (width * 0.3), height);
		} else {
			_bugDetailsComponents.adaptSize((int) (width * 0.6), height);
			_bugTreePanel.adaptSize((int) (width * 0.4), height);
		}
		getPreviewPanel().adaptSize((int) (width * 0.4), height);
		_multiSplitPane.validate();
	}


	public BugTreePanel getBugTreePanel() {
		if (_bugTreePanel == null) {
			_bugTreePanel = new BugTreePanel(this, _project);
		}
		return _bugTreePanel;
	}


	public BugDetailsComponents getBugDetailsComponents() {
		if (_bugDetailsComponents == null) {
			_bugDetailsComponents = new BugDetailsComponents(this);
		}
		return _bugDetailsComponents;
	}


	private void clear() {
		_bugTreePanel.clear();
		_bugTreePanel.updateRootNode(null);
	}


	private static class ToolWindowComponentAdapter extends ComponentAdapter {

		private final ToolWindowPanel _toolWindowPanel;


		private ToolWindowComponentAdapter(final ToolWindowPanel toolWindowPanel) {
			_toolWindowPanel = toolWindowPanel;
		}


		@Override
		public void componentShown(final ComponentEvent e) {
			super.componentShown(e);
			final Component component = e.getComponent();
			_toolWindowPanel.resizeSplitNodes(component);
		}


		@Override
		public void componentResized(final ComponentEvent e) {
			super.componentResized(e);
			final Component component = e.getComponent();
			_toolWindowPanel.resizeSplitNodes(component);
		}


		@Override
		public void componentMoved(final ComponentEvent e) {
			super.componentMoved(e);
			final Component component = e.getComponent();
			_toolWindowPanel.resizeSplitNodes(component);
		}
	}


	private static abstract class AbstractListenerImpl {

		protected final FindBugsProject _bugsProject;
		protected final ToolWindowPanel _toolWindowPanel;


		private AbstractListenerImpl(@NotNull final ToolWindowPanel toolWindowPanel, final FindBugsProject bugsProject) {
			_bugsProject = bugsProject;
			_toolWindowPanel = toolWindowPanel;
		}


		final void openAnalysisRunDetailsDialog() {
			final int bugCount = _toolWindowPanel.getBugTreePanel().getGroupModel().getBugCount();
			final DialogBuilder dialog = AnalysisRunDetailsDialog.create(_toolWindowPanel.getProject(), bugCount, _toolWindowPanel.getBugCollection().getProjectStats(), _bugsProject);
			dialog.showModal(false);
		}
	}


	private static class NotificationListenerImpl extends AbstractListenerImpl implements NotificationListener {


		private NotificationListenerImpl(@NotNull final ToolWindowPanel toolWindowPanel, final FindBugsProject bugsProject) {
			super(toolWindowPanel, bugsProject);
		}


		@Override
		public void hyperlinkUpdate(@NotNull final Notification notification, @NotNull final HyperlinkEvent e) {
			if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
				final String desc = e.getDescription();
				if (desc.equals(A_HREF_MORE_ANCHOR)) {
					openAnalysisRunDetailsDialog();
				} else if (desc.equals(A_HREF_DISABLE_ANCHOR)) {
					final int result = Messages.showYesNoDialog(
							_toolWindowPanel.getProject(),
							"Notification will be disabled for all projects.\n\n" +
									"Settings | Appearance & Behavior | Notifications | " +
									FindBugsPluginImpl.NOTIFICATION_GROUP_ID_ANALYSIS_FINISHED +
									"\ncan be used to configure the notification.",
							"FindBugs Analysis Finished Notification",
							"Disable Notification", CommonBundle.getCancelButtonText(), Messages.getWarningIcon());
					if (result == Messages.YES) {
						NotificationUtil.getNotificationsConfigurationImpl().changeSettings(
								FindBugsPluginImpl.NOTIFICATION_GROUP_ID_ANALYSIS_FINISHED,
								NotificationDisplayType.NONE, false, false);
						notification.expire();
					} else {
						notification.hideBalloon();
					}
				}
			}
		}


		@Override
		public String toString() {
			return "NotifierHyperlinkListener" +
					"{_bugsProject=" + _bugsProject +
					", _toolWindowPanel=" + _toolWindowPanel +
					'}';
		}
	}


	private static class BalloonErrorListenerImpl extends AbstractListenerImpl implements HyperlinkListener {

		private final IdeMessagePanel _ideMessagePanel;


		private BalloonErrorListenerImpl(@NotNull final ToolWindowPanel toolWindowPanel, final FindBugsProject bugsProject, @Nullable final IdeMessagePanel ideMessagePanel) {
			super(toolWindowPanel, bugsProject);
			_ideMessagePanel = ideMessagePanel;
		}


		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
				final String desc = e.getDescription();
				if (desc.equals(A_HREF_MORE_ANCHOR)) {
					openAnalysisRunDetailsDialog();
				} else if (desc.equals(A_HREF_ERROR_ANCHOR)) {
					_ideMessagePanel.openFatals(null);
				}
			}
		}
	}
}
