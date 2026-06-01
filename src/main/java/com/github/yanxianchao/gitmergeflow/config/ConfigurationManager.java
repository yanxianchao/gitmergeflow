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
        PushConfiguration current = getConfiguration(project);
        PushConfiguration updated = PushConfiguration.enabled(targetBranch)
                .withDeploy(current.isDeployEnabled(), current.getGoodsId())
                .withAppName(current.getAppName());
        updateConfiguration(project, updated);
    }

    public void disableAutoPush(@NotNull Project project) {
        PushConfiguration current = getConfiguration(project);
        PushConfiguration updated = PushConfiguration.disabled(current.getTargetBranch())
                .withDeploy(current.isDeployEnabled(), current.getGoodsId())
                .withAppName(current.getAppName());
        updateConfiguration(project, updated);
    }

    public void updateDeploy(@NotNull Project project, boolean deployEnabled, @NotNull String goodsId) {
        PushConfiguration current = getConfiguration(project);
        updateConfiguration(project, current.withDeploy(deployEnabled, goodsId));
    }

    public void updateAppName(@NotNull Project project, @NotNull String appName) {
        PushConfiguration current = getConfiguration(project);
        updateConfiguration(project, current.withAppName(appName));
    }

    private String getProjectKey(@NotNull Project project) {
        return project.getLocationHash();
    }
}