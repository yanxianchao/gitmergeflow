package com.github.yanxianchao.gitmergeflow.deploy;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP应用服务 - 查询应用详情和制品列表
 */
public final class McpAppService {

    private static final Logger LOG = Logger.getInstance(McpAppService.class);

    private McpAppService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 根据应用名称查询应用ID
     *
     * @param appName 应用名称
     * @return 应用ID，查询失败返回null
     */
    @Nullable
    public static Integer getAppIdByName(@NotNull String appName) {
        try {
            String result = McpClient.callTool("get_app_detail_by_name",
                    "\"name\":\"" + appName + "\"");
            LOG.info("查询应用详情结果: " + result);
            return parseAppId(result);
        } catch (Exception e) {
            LOG.error("查询应用详情失败, appName=" + appName, e);
            return null;
        }
    }

    /**
     * 根据应用ID查询制品列表
     *
     * @param appId 应用ID
     * @return 制品信息列表
     */
    @NotNull
    public static List<GoodsItem> getGoodsList(int appId) {
        try {
            String result = McpClient.callTool("get_app_goods_list",
                    "\"app_id\":" + appId + ",\"page_size\":20");
            LOG.info("查询制品列表结果: " + result);
            return parseGoodsList(result);
        } catch (Exception e) {
            LOG.error("查询制品列表失败, appId=" + appId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从MCP响应中提取内嵌的text内容
     * MCP响应格式：{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"实际内容"}]}}
     */
    @NotNull
    private static String extractTextContent(@NotNull String mcpResponse) {
        // 提取 "text":"..." 中的内容（处理转义换行符）
        Matcher textMatcher = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"\\s*}\\s*]", Pattern.DOTALL)
                .matcher(mcpResponse);
        if (textMatcher.find()) {
            return textMatcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return mcpResponse;
    }

    /**
     * 从MCP响应中解析应用ID
     */
    @Nullable
    private static Integer parseAppId(@NotNull String mcpResponse) {
        String textContent = extractTextContent(mcpResponse);
        // 在提取的text内容中匹配 "id":数字
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(textContent);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    /**
     * 从MCP响应中解析制品列表
     */
    @NotNull
    private static List<GoodsItem> parseGoodsList(@NotNull String mcpResponse) {
        List<GoodsItem> items = new ArrayList<>();

        String textContent = extractTextContent(mcpResponse);

        // 匹配每个制品的 goods_id, git, state
        Pattern goodsPattern = Pattern.compile(
                "\"goods_id\"\\s*:\\s*(\\d+).*?\"git\"\\s*:\\s*\"([^\"]+)\".*?\"state\"\\s*:\\s*\"([^\"]+)\"",
                Pattern.DOTALL);
        Matcher matcher = goodsPattern.matcher(textContent);

        while (matcher.find()) {
            int goodsId = Integer.parseInt(matcher.group(1));
            String gitBranch = matcher.group(2);
            String state = matcher.group(3);
            items.add(new GoodsItem(goodsId, gitBranch, state));
        }

        return items;
    }

    /**
     * 制品信息
     */
    public static final class GoodsItem {
        private final int goodsId;
        private final String gitBranch;
        private final String state;

        public GoodsItem(int goodsId, @NotNull String gitBranch, @NotNull String state) {
            this.goodsId = goodsId;
            this.gitBranch = gitBranch;
            this.state = state;
        }

        public int getGoodsId() {
            return goodsId;
        }

        public String getGitBranch() {
            return gitBranch;
        }

        public String getState() {
            return state;
        }

        @Override
        public String toString() {
            return goodsId + " - " + gitBranch + " - " + state;
        }
    }
}
