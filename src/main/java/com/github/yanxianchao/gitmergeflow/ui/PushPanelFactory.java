package com.github.yanxianchao.gitmergeflow.ui;

import com.github.yanxianchao.gitmergeflow.config.ConfigurationManager;
import com.github.yanxianchao.gitmergeflow.config.PushConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 推送面板工厂 - 负责创建推送配置UI组件
 */
public final class PushPanelFactory {

    private final ConfigurationManager configManager;

    public PushPanelFactory(@NotNull ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @NotNull
    public JPanel createPushPanel(@NotNull Project project, @NotNull String componentName) {
        PushConfiguration config = configManager.getConfiguration(project);
        // 创建组件
        JCheckBox enableCheckBox = new NonFocusableCheckBox("推送到分支：");
        JComboBox<String> branchComboBox = BranchComboBoxFactory.createBranchComboBox(project, config.getTargetBranch());
        // 初始化状态
        enableCheckBox.setSelected(config.isEnabled());
        branchComboBox.setEnabled(config.isEnabled());
        // 绑定事件
        enableCheckBox.addActionListener(e -> handleCheckBoxChange(enableCheckBox, branchComboBox, project));
        branchComboBox.addActionListener(e -> handleBranchSelection(enableCheckBox, branchComboBox, project));
        // 创建面板
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBorder(JBUI.Borders.empty(0, 5));
        panel.setOpaque(false);
        panel.setName(componentName);
        panel.add(enableCheckBox);
        panel.add(branchComboBox);
        return panel;
    }

    private void handleCheckBoxChange(@NotNull JCheckBox checkBox,
                                      @NotNull JComboBox<String> comboBox,
                                      @NotNull Project project) {
        boolean enabled = checkBox.isSelected();
        comboBox.setEnabled(enabled);

        if (enabled) {
            String selectedBranch = (String) comboBox.getSelectedItem();
            if (selectedBranch != null && !selectedBranch.trim().isEmpty()) {
                configManager.enableAutoPush(project, selectedBranch.trim());
            }
        } else {
            configManager.disableAutoPush(project);
        }
    }

    private void handleBranchSelection(@NotNull JCheckBox checkBox,
                                       @NotNull JComboBox<String> comboBox,
                                       @NotNull Project project) {
        if (!checkBox.isSelected()) return;

        String selectedBranch = (String) comboBox.getSelectedItem();
        if (selectedBranch != null && !selectedBranch.trim().isEmpty()) {
            configManager.enableAutoPush(project, selectedBranch.trim());
        }
    }
}