package com.github.yanxianchao.gitmergeflow.ui;

import com.github.yanxianchao.gitmergeflow.core.ConfigurationManager;
import com.github.yanxianchao.gitmergeflow.domain.PushConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
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
    private final BranchComboBoxManager branchManager;
    
    public PushPanelFactory(@NotNull ConfigurationManager configManager) {
        this.configManager = configManager;
        this.branchManager = new BranchComboBoxManager();
    }
    
    @NotNull
    public JPanel createPushPanel(@NotNull Project project) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBorder(JBUI.Borders.empty(0, 5));
        panel.setOpaque(false);
        
        PushConfiguration config = configManager.getConfiguration(project);
        
        JCheckBox enableCheckBox = new NonFocusableCheckBox("推送到分支：");
        JComboBox<String> branchComboBox = new ComboBox<>();
        
        setupComponents(enableCheckBox, branchComboBox, project, config);
        
        panel.add(enableCheckBox);
        panel.add(branchComboBox);
        
        return panel;
    }
    
    private void setupComponents(@NotNull JCheckBox checkBox, 
                               @NotNull JComboBox<String> comboBox,
                               @NotNull Project project,
                               @NotNull PushConfiguration config) {
        // 初始化状态
        checkBox.setSelected(config.isEnabled());
        comboBox.setEnabled(config.isEnabled());
        comboBox.setEditable(false);
        comboBox.setPreferredSize(new Dimension(200, comboBox.getPreferredSize().height));
        
        // 设置分支数据
        branchManager.setupBranchComboBox(comboBox, project, config.getTargetBranch());
        
        // 绑定事件
        checkBox.addActionListener(e -> handleCheckBoxChange(checkBox, comboBox, project));
        comboBox.addActionListener(e -> handleBranchSelection(checkBox, comboBox, project));
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