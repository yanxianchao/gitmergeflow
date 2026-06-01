package com.github.yanxianchao.gitmergeflow.deploy;

import com.github.yanxianchao.gitmergeflow.config.McpSettings;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * MCP HTTP Streamable协议客户端 - 无状态工具类
 * 每次调用独立POST请求，从响应流中读取JSON-RPC结果
 */
public final class McpClient {

    private static final Logger LOG = Logger.getInstance(McpClient.class);

    private McpClient() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 调用MCP工具，POST发送JSON-RPC请求并从HTTP Streamable响应中读取结果
     *
     * @param toolName  工具名称
     * @param arguments JSON格式的参数体（不含外层花括号）
     * @return 工具调用的结果
     */
    public static String callTool(@NotNull String toolName, @NotNull String arguments) throws Exception {
        String jsonBody = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"" + toolName + "\",\"arguments\":{" + arguments + "}}}";

        McpSettings settings = McpSettings.getInstance();
        String mcpUrl = settings.getMcpUrl();
        String authToken = settings.getAuthToken();

        if (mcpUrl.isEmpty() || authToken.isEmpty()) {
            throw new IllegalStateException("MCP URL 或 Auth Token 未配置，请在 Settings → Tools → GitMergeFlow MCP 中配置");
        }

        HttpURLConnection connection = (HttpURLConnection) URI.create(mcpUrl).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", authToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json, text/event-stream");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        int responseCode = connection.getResponseCode();
        LOG.info("MCP POST响应码: " + responseCode);

        if (responseCode != 200 && responseCode != 202) {
            String errorBody = readResponseBody(connection, true);
            throw new RuntimeException("MCP请求失败, HTTP " + responseCode + ": " + errorBody);
        }

        String contentType = connection.getContentType();
        if (contentType != null && contentType.contains("text/event-stream")) {
            return readResultFromStream(connection);
        }
        return readResponseBody(connection, false);
    }

    /**
     * 从SSE格式的HTTP Streamable响应流中读取tool调用结果
     */
    private static String readResultFromStream(@NotNull HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String currentEvent = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event: ")) {
                    currentEvent = line.substring(7).trim();
                } else if (line.startsWith("data: ") && "message".equals(currentEvent)) {
                    String data = line.substring(6).trim();
                    LOG.info("收到message事件: " + data.substring(0, Math.min(data.length(), 200)));
                    return data;
                }
            }
        } finally {
            connection.disconnect();
        }
        throw new RuntimeException("响应流结束，未收到tool调用结果");
    }

    private static String readResponseBody(@NotNull HttpURLConnection connection, boolean errorStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                errorStream ? connection.getErrorStream() : connection.getInputStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "读取响应失败: " + e.getMessage();
        }
    }
}
