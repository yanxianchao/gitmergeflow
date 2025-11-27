package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import javax.swing.*;
import java.awt.*;

public class ProjectHelper {

    private ProjectHelper() {
    }

    public static Project getCurrentActiveProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            return openProjects[openProjects.length - 1];
        }
        return null;
    }

    public static Project getCurrentActiveProject(JDialog dialog) {
        Window owner = dialog.getOwner();
        if (owner != null) {
            if (owner instanceof Frame frame) {
                String title = frame.getTitle();
                if (title != null) {
                    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                        if (title.contains(project.getName())) {
                            return project;
                        }
                    }
                }
            }
        }
        return getCurrentActiveProject();
    }

}
