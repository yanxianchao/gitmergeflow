package com.github.yanxianchao.gitmergeflow.infrastructure;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 项目解析器 - 负责从UI上下文中解析当前项目
 */
public final class ProjectResolver {
    
    private ProjectResolver() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    @Nullable
    public static Project resolveProject(@NotNull JDialog dialog) {
        // 策略1: 通过对话框所有者窗口解析
        Project project = resolveFromOwnerWindow(dialog);
        if (project != null) return project;
        
        // 策略2: 返回当前活动项目
        return getCurrentActiveProject();
    }
    
    @Nullable
    public static Project getCurrentActiveProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        return openProjects.length > 0 ? openProjects[openProjects.length - 1] : null;
    }
    
    @Nullable
    private static Project resolveFromOwnerWindow(@NotNull JDialog dialog) {
        Window owner = dialog.getOwner();
        if (!(owner instanceof Frame frame)) return null;
        
        String title = frame.getTitle();
        if (title == null) return null;
        
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (title.contains(project.getName())) {
                return project;
            }
        }
        return null;
    }
}