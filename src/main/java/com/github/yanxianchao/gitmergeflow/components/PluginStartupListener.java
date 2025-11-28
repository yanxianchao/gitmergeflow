package com.github.yanxianchao.gitmergeflow.components;

import com.github.yanxianchao.gitmergeflow.ui.PushDialogEnhancer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * 插件启动监听器 - 初始化插件核心功能
 */
public class PluginStartupListener implements StartupActivity.DumbAware {
    
    @Override
    public void runActivity(@NotNull Project project) {
        PushDialogEnhancer.getInstance().enhance();
    }
}