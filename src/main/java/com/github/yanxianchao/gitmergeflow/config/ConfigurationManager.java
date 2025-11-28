package com.github.yanxianchao.gitmergeflow.config;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器 - 负责管理项目级别的推送配置
 */
public final class ConfigurationManager {

    private static final ConfigurationManager INSTANCE = new ConfigurationManager();
    private final ConcurrentHashMap<String, PushConfiguration> projectConfigurations = new ConcurrentHashMap<>();

    private ConfigurationManager() {
        // 私有构造函数，防止外部实例化
    }

    public static ConfigurationManager getInstance() {
        return INSTANCE;
    }

    public void updateConfiguration(@NotNull Project project, @NotNull PushConfiguration configuration) {
        String projectKey = getProjectKey(project);
        projectConfigurations.put(projectKey, configuration);
    }

    @NotNull
    public PushConfiguration getConfiguration(@NotNull Project project) {
        String projectKey = getProjectKey(project);
        return projectConfigurations.getOrDefault(projectKey, PushConfiguration.disabled(null));
    }

    public void enableAutoPush(@NotNull Project project, @NotNull String targetBranch) {
        updateConfiguration(project, PushConfiguration.enabled(targetBranch));
    }

    public void disableAutoPush(@NotNull Project project) {
        updateConfiguration(project, PushConfiguration.disabled(getConfiguration(project).getTargetBranch()));
    }

    private String getProjectKey(@NotNull Project project) {
        return project.getLocationHash();
    }
}