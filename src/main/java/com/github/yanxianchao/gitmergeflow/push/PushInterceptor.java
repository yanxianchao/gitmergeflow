package com.github.yanxianchao.gitmergeflow.push;

import com.github.yanxianchao.gitmergeflow.services.GitMergeService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.push.GitPushRepoResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

public class PushInterceptor implements git4idea.push.GitPushListener {

    @Override
    public void onCompleted(@NotNull GitRepository repository, @NotNull GitPushRepoResult result) {
        if (result.getType() == GitPushRepoResult.Type.SUCCESS) {
            Project project = repository.getProject();
            System.out.println("推送拦截器：推送操作成功完成");
            // 检查是否需要自动合并
            boolean shouldPush = PushDialogExtension.shouldPushToBranch();
            String branchName = PushDialogExtension.getBranchName();

            System.out.println("自动合并检查 - shouldPush: " + shouldPush + ", branchName: " + branchName);

            if (shouldPush) {
                if (branchName != null && !branchName.trim().isEmpty()) {
                    System.out.println("开始执行自动合并到分支: " + branchName.trim());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        GitMergeService.performAutoMerge(project, branchName.trim());
                        // 可选的智能合并调用
                        // GitMergeService.performSmartMerge(project, branchName.trim());
                    });
                } else {
                    System.out.println("分支名为空，跳过自动合并");
                }
            } else {
                System.out.println("自动合并条件不满足，跳过执行");
            }
        }
    }


}