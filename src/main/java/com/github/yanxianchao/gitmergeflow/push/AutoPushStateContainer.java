package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.project.Project;

import java.util.concurrent.ConcurrentHashMap;

public class AutoPushStateContainer {

    private static class AutoPushState {
        volatile boolean lastPushToBranchState = false;
        volatile String lastSelectedBranch = "";
    }

    private static final AutoPushStateContainer INSTANCE = new AutoPushStateContainer();

    private final ConcurrentHashMap<String, AutoPushState> projectStates = new ConcurrentHashMap<>();

    private AutoPushStateContainer() {
    }

    public static AutoPushStateContainer getInstance() {
        return INSTANCE;
    }

    private AutoPushState getProjectState(Project project) {
        if (project == null) {
            return new AutoPushState();
        }
        String projectKey = project.getLocationHash();
        return projectStates.computeIfAbsent(projectKey, k -> new AutoPushState());
    }

    public boolean isPushToBranchEnabled(Project project) {
        return getProjectState(project).lastPushToBranchState;
    }

    public boolean isPushToBranchEnabled() {
        Project currentProject = ProjectHelper.getCurrentActiveProject();
        if (currentProject == null)
            return false;
        return isPushToBranchEnabled(currentProject);
    }

    public String getBranchName() {
        Project currentProject = ProjectHelper.getCurrentActiveProject();
        if (currentProject == null)
            return "";
        return getLastSelectedBranch(currentProject);
    }

    public void setPushToBranchEnabled(Project project, boolean enabled) {
        getProjectState(project).lastPushToBranchState = enabled;
    }

    public String getLastSelectedBranch(Project project) {
        return getProjectState(project).lastSelectedBranch;
    }

    public void setLastSelectedBranch(Project project, String branch) {
        if (branch != null) {
            getProjectState(project).lastSelectedBranch = branch.trim();
        }
    }
}