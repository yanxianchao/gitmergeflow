package com.github.yanxianchao.gitmergeflow.deploy;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * MCP部署服务 - 通过MCP HTTP Streamable协议调用重制制品并部署到预发环境
 */
public final class McpDeployService {

    private static final Logger LOG = Logger.getInstance(McpDeployService.class);

    private McpDeployService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 重制制品并部署到预发环境
     *
     * @param project 当前项目
     * @param goodsId 制品ID
     * @return 部署请求是否发送成功
     */
    public static boolean resetAndDeployToPreEnv(@NotNull Project project, @NotNull String goodsId) {
        try {
            String result = McpClient.callTool("reset_app_goods",
                    "\"goods_id\":" + goodsId + ",\"tag\":\"pre-i0\"");
            LOG.info("MCP调用结果: " + result);

            notifyInfo(project, "制品(" + goodsId + ")重制部署请求已发送，请关注运维平台状态。");
            return true;
        } catch (Exception e) {
            LOG.error("重制制品部署失败, goodsId=" + goodsId, e);
            notifyError(project, "重制制品部署失败: " + e.getMessage());
            return false;
        }
    }

    private static void notifyInfo(@NotNull Project project, @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("GitMergeFlow通知")
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project);
    }

    private static void notifyError(@NotNull Project project, @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("GitMergeFlow通知")
                .createNotification(message, NotificationType.ERROR)
                .notify(project);
    }
}
