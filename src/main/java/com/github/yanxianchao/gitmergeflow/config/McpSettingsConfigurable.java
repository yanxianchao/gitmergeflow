package com.github.yanxianchao.gitmergeflow.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * MCP配置面板 - 在 Settings → Tools → GitMergeFlow MCP 中展示
 */
public final class McpSettingsConfigurable implements Configurable {

    private JBTextField mcpUrlField;
    private JBTextField authTokenField;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitMergeFlow";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mcpUrlField = new JBTextField();
        authTokenField = new JBTextField();

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("URL（Streamable HTTP）:"), mcpUrlField, 1, false)
                .addLabeledComponent(new JBLabel("Auth Token:"), authTokenField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        McpSettings settings = McpSettings.getInstance();
        return !mcpUrlField.getText().trim().equals(settings.getMcpUrl())
                || !authTokenField.getText().trim().equals(settings.getAuthToken());
    }

    @Override
    public void apply() throws ConfigurationException {
        String url = mcpUrlField.getText().trim();
        String token = authTokenField.getText().trim();

        if (url.isEmpty()) {
            throw new ConfigurationException("MCP URL 不能为空");
        }
        if (token.isEmpty()) {
            throw new ConfigurationException("Auth Token 不能为空");
        }

        McpSettings settings = McpSettings.getInstance();
        settings.setMcpUrl(url);
        settings.setAuthToken(token);
    }

    @Override
    public void reset() {
        McpSettings settings = McpSettings.getInstance();
        mcpUrlField.setText(settings.getMcpUrl());
        authTokenField.setText(settings.getAuthToken());
    }
}
