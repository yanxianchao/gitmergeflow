package com.github.yanxianchao.gitmergeflow.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分支下拉框管理器 - 负责分支下拉框的数据填充和渲染
 */
public final class BranchComboBoxManager {
    
    private static final Logger LOG = Logger.getInstance(BranchComboBoxManager.class);
    
    private BranchComboBoxManager() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static void setupBranchComboBox(@NotNull JComboBox<String> comboBox, 
                                          @NotNull Project project, 
                                          @Nullable String selectedBranch) {
        setupRenderer(comboBox);
        populateBranches(comboBox, project);
        setSelectedBranch(comboBox, selectedBranch);
    }
    
    private static void setupRenderer(@NotNull JComboBox<String> comboBox) {
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (value != null && !value.toString().isEmpty()) {
                    setIcon(com.intellij.icons.AllIcons.Vcs.Branch);
                    setToolTipText(value.toString());
                }
                
                return this;
            }
        });
    }
    
    private static void populateBranches(@NotNull JComboBox<String> comboBox, @NotNull Project project) {
        comboBox.removeAllItems();
        comboBox.addItem(""); // 空选项
        
        try {
            Set<String> branches = collectLocalBranches(project);
            branches.stream().sorted().forEach(comboBox::addItem);
            LOG.info("成功加载了 " + branches.size() + " 个本地分支");
        } catch (Exception e) {
            LOG.error("加载分支列表失败", e);
        }
        
        comboBox.hidePopup();
    }
    
    @NotNull
    private static Set<String> collectLocalBranches(@NotNull Project project) {
        Set<String> branches = new HashSet<>();
        
        if (project.isDisposed()) return branches;
        
        GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
        List<GitRepository> repositories = manager.getRepositories();
        
        if (!repositories.isEmpty()) {
            GitRepository repository = repositories.get(0);
            repository.getBranches().getLocalBranches().forEach(branch -> {
                String branchName = branch.getName();
                if (!branchName.isEmpty()) {
                    branches.add(branchName);
                }
            });
        }
        
        return branches;
    }
    
    private static void setSelectedBranch(@NotNull JComboBox<String> comboBox, @Nullable String selectedBranch) {
        if (selectedBranch != null && !selectedBranch.trim().isEmpty()) {
            comboBox.setSelectedItem(selectedBranch.trim());
        } else {
            comboBox.setSelectedIndex(0);
        }
    }
}