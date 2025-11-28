package com.github.yanxianchao.gitmergeflow.config;

import com.github.yanxianchao.gitmergeflow.config.PushConfiguration;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器 - 负责管理项目级别的推送配置
 */
@Service
public final class ConfigurationManager {
    
    private final ConcurrentHashMap<String, PushConfiguration> projectConfigurations = new ConcurrentHashMap<>();
    
    public void updateConfiguration(@NotNull Project project, @NotNull PushConfiguration configuration) {
        String projectKey = getProjectKey(project);
        projectConfigurations.put(projectKey, configuration);
    }
    
    @NotNull
    public PushConfiguration getConfiguration(@NotNull Project project) {
        String projectKey = getProjectKey(project);
        return projectConfigurations.getOrDefault(projectKey, PushConfiguration.disabled());
    }
    
    public void enableAutoPush(@NotNull Project project, @NotNull String targetBranch) {
        updateConfiguration(project, PushConfiguration.enabled(targetBranch));
    }
    
    public void disableAutoPush(@NotNull Project project) {
        updateConfiguration(project, PushConfiguration.disabled());
    }
    
    private String getProjectKey(@NotNull Project project) {
        return project.getLocationHash();
    }
}