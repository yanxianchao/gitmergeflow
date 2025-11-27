package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.project.Project;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectPushStateManager {
    
    private static class ProjectPushState {
        volatile boolean lastPushToBranchState = false;
        volatile String lastSelectedBranch = "";
    }
    
    private static final ProjectPushStateManager INSTANCE = new ProjectPushStateManager();
    private final ConcurrentHashMap<String, ProjectPushState> projectStates = new ConcurrentHashMap<>();
    
    private ProjectPushStateManager() {}
    
    public static ProjectPushStateManager getInstance() {
        return INSTANCE;
    }
    
    private ProjectPushState getProjectState(Project project) {
        if (project == null) {
            return new ProjectPushState();
        }
        String projectKey = project.getLocationHash();
        return projectStates.computeIfAbsent(projectKey, k -> new ProjectPushState());
    }
    
    public boolean isPushToBranchEnabled(Project project) {
        return getProjectState(project).lastPushToBranchState;
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