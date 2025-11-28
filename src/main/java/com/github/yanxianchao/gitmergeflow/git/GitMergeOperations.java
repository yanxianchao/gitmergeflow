package com.github.yanxianchao.gitmergeflow.git;

import com.github.yanxianchao.gitmergeflow.config.ConfigurationManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

public class GitMergeOperations {
    
    private static final Logger LOG = Logger.getInstance(GitMergeOperations.class);
    private static final String NOTIFICATION_GROUP_ID = "GitMergeFlow通知";
    
    public static void performAutoMerge(@NotNull Project project, @NotNull String targetBranch) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                GitRepository repository = GitUtil.getRepositoryManager(project).getRepositories().get(0);
                if (repository == null) {
                    showNotification(project, "未找到Git仓库", NotificationType.ERROR);
                    return;
                }
                
                String currentBranch = repository.getCurrentBranchName();
                if (currentBranch == null) {
                    showNotification(project, "无法获取当前分支", NotificationType.ERROR);
                    return;
                }
                
                if (currentBranch.equals(targetBranch)) {
                    showNotification(project, "当前分支已经是目标分支，跳过合并操作", NotificationType.INFORMATION);
                    return;
                }
                
                Git git = Git.getInstance();
                
                // 为了安全起见，使用本地合并方式
                boolean needsCheckoutBack = false;
                try {
                    // 切换到目标分支
                    GitLineHandler checkoutHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.CHECKOUT);
                    checkoutHandler.addParameters(targetBranch);
                    git.runCommand(checkoutHandler).throwOnError();
                    needsCheckoutBack = true;
                    
                    // 拉取目标分支的最新更改
                    try {
                        GitLineHandler pullHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.PULL);
                        pullHandler.addParameters("origin", targetBranch);
                        git.runCommand(pullHandler).throwOnError();
                    } catch (Exception pullException) {
                        needsCheckoutBack = false; // 发生冲突时不自动切换回原分支
                        showNotification(project, 
                            String.format("拉取目标分支 '%s' 时发生冲突，请手动解决冲突后继续操作", targetBranch), 
                            NotificationType.WARNING);
                        throw pullException;
                    }
                    
                    // 将当前分支合并到目标分支
                    try {
                        GitLineHandler mergeHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE);
                        mergeHandler.addParameters(currentBranch);
                        git.runCommand(mergeHandler).throwOnError();
                    } catch (Exception mergeException) {
                        needsCheckoutBack = false; // 发生冲突时不自动切换回原分支
                        showNotification(project, 
                            String.format("合并分支 '%s' 到 '%s' 时发生冲突，请手动解决冲突后继续操作", currentBranch, targetBranch), 
                            NotificationType.WARNING);
                        throw mergeException;
                    }
                    
                    // 推送目标分支
                    GitLineHandler pushHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
                    pushHandler.addParameters("origin", targetBranch);
                    git.runCommand(pushHandler).throwOnError();
                    
                    showNotification(project, 
                        String.format("成功将分支 '%s' 合并到 '%s' 并推送到远程仓库", currentBranch, targetBranch), 
                        NotificationType.INFORMATION);
                    
                } finally {
                    // 确保切换回原始分支
                    if (needsCheckoutBack) {
                        try {
                            GitLineHandler checkoutBackHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.CHECKOUT);
                            checkoutBackHandler.addParameters(currentBranch);
                            git.runCommand(checkoutBackHandler).throwOnError();
                            
                            showNotification(project, 
                                String.format("已切换回原始分支 '%s'", currentBranch), 
                                NotificationType.INFORMATION);
                        } catch (Exception checkoutBackException) {
                            showNotification(project, 
                                "合并成功，但切换回原始分支失败：" + checkoutBackException.getMessage(), 
                                NotificationType.WARNING);
                        }
                    }
                    ApplicationManager.getApplication().getService(ConfigurationManager.class).disableAutoPush(project);
                }
                
            } catch (Exception e) {
                showNotification(project, 
                    "自动合并失败：" + e.getMessage(), 
                    NotificationType.ERROR);
            }
        });
    }
    
    /**
     * 检查是否可以进行快进合并
     * 快进合并的条件：目标分支是当前分支的祖先（即目标分支没有新的提交）
     *
     * @param project 项目实例
     * @param currentBranch 当前分支名
     * @param targetBranch 目标分支名
     * @return true 如果可以快进合并，false 否则
     */
    private static boolean canFastForwardMerge(@NotNull Project project, @NotNull String currentBranch, @NotNull String targetBranch) {
        try {
            GitRepository repository = GitUtil.getRepositoryManager(project).getRepositories().get(0);
            if (repository == null) {
                return false;
            }
            
            Git git = Git.getInstance();
            
            // 获取当前分支的最新提交哈希
            GitLineHandler currentCommitHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.REV_PARSE);
            currentCommitHandler.addParameters(currentBranch);
            String currentCommit = git.runCommand(currentCommitHandler).getOutputOrThrow().trim();
            
            // 获取目标分支的最新提交哈希
            GitLineHandler targetCommitHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.REV_PARSE);
            targetCommitHandler.addParameters(targetBranch);
            String targetCommit = git.runCommand(targetCommitHandler).getOutputOrThrow().trim();
            
            // 获取两个分支的共同祖先
            GitLineHandler mergeBaseHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE_BASE);
            mergeBaseHandler.addParameters(currentBranch, targetBranch);
            String mergeBase = git.runCommand(mergeBaseHandler).getOutputOrThrow().trim();
            
            // 如果目标分支的最新提交就是共同祖先，说明目标分支没有新提交，可以快进合并
            boolean canFastForward = targetCommit.equals(mergeBase);
            
            LOG.info(String.format("快进合并检查 - 当前分支: %s (%s), 目标分支: %s (%s), 共同祖先: %s, 可快进: %s", 
                currentBranch, currentCommit.substring(0, 8), 
                targetBranch, targetCommit.substring(0, 8), 
                mergeBase.substring(0, 8), canFastForward));
            
            return canFastForward;
            
        } catch (Exception e) {
            LOG.warn("快进合并检查失败", e);
            return false;
        }
    }
    
    /**
     * 执行快进合并
     * 直接将当前分支推送到目标分支，不需要本地切换分支
     *
     * @param project 项目实例
     * @param currentBranch 当前分支名
     * @param targetBranch 目标分支名
     */
    public static void performFastForwardMerge(@NotNull Project project, @NotNull String currentBranch, @NotNull String targetBranch) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                GitRepository repository = GitUtil.getRepositoryManager(project).getRepositories().get(0);
                if (repository == null) {
                    showNotification(project, "未找到Git仓库", NotificationType.ERROR);
                    return;
                }
                
                if (currentBranch.equals(targetBranch)) {
                    showNotification(project, "当前分支已经是目标分支，跳过合并操作", NotificationType.INFORMATION);
                    return;
                }
                
                // 检查是否可以快进合并
                if (!canFastForwardMerge(project, currentBranch, targetBranch)) {
                    showNotification(project, 
                        String.format("无法进行快进合并：目标分支 '%s' 有新提交，请使用常规合并方式", targetBranch), 
                        NotificationType.WARNING);
                    return;
                }
                
                Git git = Git.getInstance();
                
                // 首先获取最新的远程引用
                try {
                    GitLineHandler fetchHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.FETCH);
                    fetchHandler.addParameters("origin");
                    git.runCommand(fetchHandler);
                } catch (Exception fetchException) {
                    LOG.warn("获取远程引用失败，继续执行", fetchException);
                }
                
                // 执行快进合并：直接推送当前分支到目标分支
                GitLineHandler pushHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.PUSH);
                pushHandler.addParameters("origin", currentBranch + ":" + targetBranch);
                git.runCommand(pushHandler).throwOnError();
                
                showNotification(project, 
                    String.format("快进合并成功：已将分支 '%s' 快进合并到 '%s'", currentBranch, targetBranch), 
                    NotificationType.INFORMATION);
                
            } catch (Exception e) {
                showNotification(project, 
                    "快进合并失败：" + e.getMessage(), 
                    NotificationType.ERROR);
            }
        });
    }
    
    /**
     * 智能合并方法：优先尝试快进合并，失败时使用常规合并
     * 注意：此方法仅供参考，当前仍使用 performAutoMerge 作为主要入口
     *
     * @param project 项目实例
     * @param targetBranch 目标分支名
     */
    public static void performSmartMerge(@NotNull Project project, @NotNull String targetBranch) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                GitRepository repository = GitUtil.getRepositoryManager(project).getRepositories().get(0);
                if (repository == null) {
                    showNotification(project, "未找到Git仓库", NotificationType.ERROR);
                    return;
                }
                
                String currentBranch = repository.getCurrentBranchName();
                if (currentBranch == null) {
                    showNotification(project, "无法获取当前分支", NotificationType.ERROR);
                    return;
                }
                
                // 尝试快进合并
                if (canFastForwardMerge(project, currentBranch, targetBranch)) {
                    LOG.info("使用快进合并策略");
                    performFastForwardMerge(project, currentBranch, targetBranch);
                } else {
                    LOG.info("快进合并不可用，使用常规合并策略");
                    performAutoMerge(project, targetBranch);
                }
                
            } catch (Exception e) {
                LOG.warn("智能合并策略选择失败，回退到常规合并：" + e.getMessage());
                performAutoMerge(project, targetBranch);
            }
        });
    }
    
    private static void showNotification(@NotNull Project project, @NotNull String message, @NotNull NotificationType type) {
        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(message, type);
        notification.notify(project);
    }
}