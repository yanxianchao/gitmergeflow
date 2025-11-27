package com.github.yanxianchao.gitmergeflow.push;

import com.github.yanxianchao.gitmergeflow.services.GitMergeService;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.push.GitPushRepoResult;
import git4idea.repo.GitRepository;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class PushInterceptor implements git4idea.push.GitPushListener {

    @Override
    public void onCompleted(@NotNull GitRepository repository, @NotNull GitPushRepoResult result) {
        if (result.getType() != GitPushRepoResult.Type.SUCCESS)
            return;
        // 检查是否需要自动合并
        boolean shouldPush = AutoPushStateContainer.getInstance().isPushToBranchEnabled();
        String branchName = AutoPushStateContainer.getInstance().getBranchName();
        System.out.println("自动合并检查 - shouldPush: " + shouldPush + ", branchName: " + branchName);
        if (!shouldPush || StringUtils.isBlank(branchName))
            return;
        System.out.println("开始执行自动合并到分支: " + branchName.trim());
        ApplicationManager.getApplication().invokeLater(() -> {
            GitMergeService.performAutoMerge(repository.getProject(), branchName.trim());
            // 可选的智能合并调用
            // GitMergeService.performSmartMerge(project, branchName.trim());
        });

    }

}