package com.github.yanxianchao.gitmergeflow.launcher;

import com.github.yanxianchao.gitmergeflow.ui.GitPushDialogEnhancer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * GitMergeFlow 启动活动 - 初始化插件核心功能
 */
public final class GitMergeFlowLauncher implements StartupActivity {
    
    private static final Logger LOG = Logger.getInstance(GitMergeFlowLauncher.class);
    public void runActivity(@NotNull Project project) {
        GitPushDialogEnhancer.getInstance().enhance();
    }
}