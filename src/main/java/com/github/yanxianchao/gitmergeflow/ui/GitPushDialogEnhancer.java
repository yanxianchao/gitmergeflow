package com.github.yanxianchao.gitmergeflow.ui;

import com.github.yanxianchao.gitmergeflow.config.ConfigurationManager;
import com.github.yanxianchao.gitmergeflow.utils.ProjectResolver;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * Git推送对话框增强器 - 负责在Git推送对话框中添加自定义功能
 */
public final class GitPushDialogEnhancer {

    private static final Logger LOG = Logger.getInstance(GitPushDialogEnhancer.class);
    private static final GitPushDialogEnhancer INSTANCE = new GitPushDialogEnhancer();
    private static final String COMPONENT_NAME = "GitMergeFlowPanel";

    private volatile boolean enhanced = false;
    private final PushPanelFactory panelFactory;

    private GitPushDialogEnhancer() {
        panelFactory = new PushPanelFactory(ConfigurationManager.getInstance());
    }

    public static GitPushDialogEnhancer getInstance() {
        return INSTANCE;
    }

    public void enhance() {
        if (enhanced) return;

        synchronized (this) {
            if (enhanced) return;

            try {
                Toolkit.getDefaultToolkit().addAWTEventListener(this::handleWindowEvent, AWTEvent.WINDOW_EVENT_MASK);
                enhanced = true;
                LOG.info("GitMergeFlow插件初始化成功");
            } catch (Exception e) {
                LOG.error("GitMergeFlow插件初始化失败", e);
                throw e;
            }
        }
    }

    private void handleWindowEvent(AWTEvent event) {
        if (!(event instanceof WindowEvent windowEvent) ||
                windowEvent.getID() != WindowEvent.WINDOW_OPENED)
            return;

        Window window = windowEvent.getWindow();
        if (!(window instanceof JDialog dialog) || !isPushDialog(dialog))
            return;

        SwingUtilities.invokeLater(() -> enhanceDialog(dialog));
    }

    private boolean isPushDialog(@NotNull JDialog dialog) {
        String title = dialog.getTitle();
        return title != null && title.startsWith("Push Commits to ");
    }

    private void enhanceDialog(@NotNull JDialog dialog) {
        Project project = ProjectResolver.resolveProject(dialog);
        if (project == null || project.isDisposed() || hasCustomComponent(dialog))
            return;

        try {
            JPanel customPanel = panelFactory.createPushPanel(project, COMPONENT_NAME);

            boolean added = DialogLayoutManager.addComponent(dialog.getContentPane(), customPanel);

            if (added) {
                dialog.revalidate();
                dialog.repaint();
            }
        } catch (Exception e) {
            LOG.error("增强推送对话框失败", e);
        }
    }

    private boolean hasCustomComponent(@NotNull JDialog dialog) {
        return findComponentByName(dialog.getContentPane(), COMPONENT_NAME) != null;
    }

    private Component findComponentByName(@NotNull Container container, @NotNull String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container) {
                Component found = findComponentByName((Container) component, name);
                if (found != null) return found;
            }
        }
        return null;
    }
}