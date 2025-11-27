package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class PushDialogExtension implements ProjectActivity {

    private final PushDialogManager dialogManager = PushDialogManager.getInstance();

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        System.out.println("推送对话框扩展：为项目初始化活动 - " + project.getName());
        return Unit.INSTANCE;
    }

    public static boolean shouldPushToBranch() {
        Project currentProject = PushDialogManager.getInstance().getCurrentActiveProject();
        if (currentProject == null) {
            return false;
        }
        
        boolean currentState = getCurrentCheckboxState();
        if (currentState) {
            ProjectPushStateManager.getInstance().setPushToBranchEnabled(currentProject, true);
            return true;
        }
        
        return ProjectPushStateManager.getInstance().isPushToBranchEnabled(currentProject);
    }

    public static String getBranchName() {
        Project currentProject = PushDialogManager.getInstance().getCurrentActiveProject();
        if (currentProject == null) {
            return "";
        }
        
        String currentBranch = getCurrentBranchName();
        if (!currentBranch.trim().isEmpty()) {
            ProjectPushStateManager.getInstance().setLastSelectedBranch(currentProject, currentBranch);
            return currentBranch.trim();
        }
        
        return ProjectPushStateManager.getInstance().getLastSelectedBranch(currentProject);
    }

    private static boolean getCurrentCheckboxState() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog dialog && window.isVisible()) {
                String title = dialog.getTitle();
                if (title != null && title.contains("Push")) {
                    JCheckBox checkbox = PushDialogManager.getInstance().findCheckboxInDialog(dialog);
                    if (checkbox != null) {
                        return checkbox.isSelected();
                    }
                }
            }
        }
        return false;
    }

    private static String getCurrentBranchName() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog dialog && window.isVisible()) {
                String title = dialog.getTitle();
                if (title != null && title.contains("Push")) {
                    JComboBox<String> comboBox = PushDialogManager.getInstance().findComboBoxInDialog(dialog);
                    if (comboBox != null && comboBox.getSelectedItem() != null) {
                        String selectedItem = (String) comboBox.getSelectedItem();
                        return selectedItem != null ? selectedItem.trim() : "";
                    }
                }
            }
        }
        return "";
    }
}