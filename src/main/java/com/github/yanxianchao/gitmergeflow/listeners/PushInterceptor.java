package com.github.yanxianchao.gitmergeflow.listeners;

import com.github.yanxianchao.gitmergeflow.config.ConfigurationManager;
import com.github.yanxianchao.gitmergeflow.config.PushConfiguration;
import com.github.yanxianchao.gitmergeflow.deploy.McpDeployService;
import com.github.yanxianchao.gitmergeflow.git.GitMergeOperations;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.push.GitPushRepoResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

/**
 * 推送拦截器 - 监听Git推送完成事件，执行自动合并和预发部署
 */
public class PushInterceptor implements git4idea.push.GitPushListener {

    private static final Logger LOG = Logger.getInstance(PushInterceptor.class);

    @Override
    public void onCompleted(@NotNull GitRepository repository, @NotNull GitPushRepoResult result) {
        if (result.getType() != GitPushRepoResult.Type.SUCCESS) return;

        Project project = repository.getProject();
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        PushConfiguration config = configManager.getConfiguration(project);

        boolean needMerge = config.hasValidTargetBranch();
        boolean needDeploy = config.hasValidDeploy();

        if (needMerge) {
            // 如果需要合并，在线程池中先执行合并，合并完成后再判断是否部署
            String currentBranch = repository.getCurrentBranchName();
            String targetBranch = config.getTargetBranch();
            String goodsId = config.getGoodsId();
            LOG.info(String.format("开始执行自动合并到分支, 当前分支: %s, 目标分支: %s", currentBranch, targetBranch));

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                GitMergeOperations.performAutoMerge(project, targetBranch);
                LOG.info(String.format("自动合并执行完成, 当前分支: %s, 目标分支: %s", currentBranch, targetBranch));

                if (needDeploy) {
                    LOG.info("合并完成，开始执行预发环境部署, goodsId=" + goodsId);
                    McpDeployService.resetAndDeployToPreEnv(project, goodsId);
                }
            });
        } else if (needDeploy) {
            // 不需要合并，直接部署
            String goodsId = config.getGoodsId();
            LOG.info("开始执行预发环境部署, goodsId=" + goodsId);

            ApplicationManager.getApplication().executeOnPooledThread(() ->
                    McpDeployService.resetAndDeployToPreEnv(project, goodsId)
            );
        }
    }
}