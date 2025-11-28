package com.github.yanxianchao.gitmergeflow.listeners;

import com.github.yanxianchao.gitmergeflow.config.ConfigurationManager;
import com.github.yanxianchao.gitmergeflow.config.PushConfiguration;
import com.github.yanxianchao.gitmergeflow.git.GitMergeOperations;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import git4idea.push.GitPushRepoResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

/**
 * 推送拦截器 - 监听Git推送完成事件，执行自动合并
 */
public class PushInterceptor implements git4idea.push.GitPushListener {

    private static final Logger LOG = Logger.getInstance(PushInterceptor.class);

    @Override
    public void onCompleted(@NotNull GitRepository repository, @NotNull GitPushRepoResult result) {
        if (result.getType() != GitPushRepoResult.Type.SUCCESS) return;

        ConfigurationManager configManager = ApplicationManager.getApplication().getService(ConfigurationManager.class);
        PushConfiguration config = configManager.getConfiguration(repository.getProject());

        if (!config.hasValidTargetBranch()) {
            LOG.info("自动合并检查 - 未启用或未选择分支");
            return;
        }

        String currentBranch = repository.getCurrentBranchName();
        String targetBranch = config.getTargetBranch();
        LOG.info("开始执行自动合并到分支, 当前分支: " + currentBranch + ", 目标分支: " + targetBranch);

        ApplicationManager.getApplication().invokeLater(() -> {
            GitMergeOperations.performAutoMerge(repository.getProject(), targetBranch);
        });
    }
}