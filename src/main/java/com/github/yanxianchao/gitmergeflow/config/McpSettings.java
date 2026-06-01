package com.github.yanxianchao.gitmergeflow.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MCP连接配置 - Application级别持久化存储
 */
@State(name = "GitMergeFlowMcpSettings", storages = @Storage("GitMergeFlowMcpSettings.xml"))
public final class McpSettings implements PersistentStateComponent<McpSettings.State> {

    private State myState = new State();

    public static McpSettings getInstance() {
        return ApplicationManager.getApplication().getService(McpSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public String getMcpUrl() {
        return myState.mcpUrl;
    }

    public void setMcpUrl(String mcpUrl) {
        myState.mcpUrl = mcpUrl != null ? mcpUrl.trim() : "";
    }

    public String getAuthToken() {
        return myState.authToken;
    }

    public void setAuthToken(String authToken) {
        myState.authToken = authToken != null ? authToken.trim() : "";
    }

    public static class State {
        public String mcpUrl = "";
        public String authToken = "";
    }
}
