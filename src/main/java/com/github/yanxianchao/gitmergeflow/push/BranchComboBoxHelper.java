package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BranchComboBoxHelper {
    
    public static void populateLocalBranches(Project project, JComboBox<String> branchComboBox) {
        try {
            if (project == null || project.isDisposed()) {
                System.out.println("项目为空或已被销毁，跳过分支列表获取");
                addEmptyOption(branchComboBox);
                return;
            }

            GitRepositoryManager repositoryManager = getRepositoryManager(project);
            if (repositoryManager == null) {
                addEmptyOption(branchComboBox);
                return;
            }

            List<GitRepository> repositories = repositoryManager.getRepositories();
            if (repositories.isEmpty()) {
                System.out.println("未找到Git仓库");
                addEmptyOption(branchComboBox);
                return;
            }

            GitRepository repository = repositories.get(0);
            if (repository == null) {
                System.out.println("Git仓库为空，无法获取分支列表");
                addEmptyOption(branchComboBox);
                return;
            }

            System.out.println("开始获取本地分支列表，仓库路径：" + repository.getRoot().getPath());
            
            Set<String> localBranches = collectLocalBranches(repository);
            populateComboBox(branchComboBox, localBranches);
            
            setDefaultSelection(project, branchComboBox);
            branchComboBox.hidePopup();

        } catch (Exception e) {
            System.err.println("填充本地分支列表失败：" + e.getMessage());
            addEmptyOption(branchComboBox);
        }
    }
    
    public static void setupBranchComboBoxRenderer(JComboBox<String> branchComboBox) {
        if (branchComboBox == null) return;

        branchComboBox.setRenderer(new DefaultListCellRenderer() {
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
    
    private static GitRepositoryManager getRepositoryManager(Project project) {
        try {
            return GitRepositoryManager.getInstance(project);
        } catch (Exception e) {
            System.out.println("无法获取Git仓库管理器：" + e.getMessage());
            return null;
        }
    }
    
    private static Set<String> collectLocalBranches(GitRepository repository) {
        Set<String> localBranches = new HashSet<>();
        
        repository.getBranches().getLocalBranches().forEach(branch -> {
            String branchName = branch.getName();
            System.out.println("发现本地分支：" + branchName);
            if (!branchName.isEmpty()) {
                localBranches.add(branchName);
            }
        });
        
        System.out.println("总共找到 " + localBranches.size() + " 个本地分支");
        return localBranches;
    }
    
    private static void populateComboBox(JComboBox<String> branchComboBox, Set<String> branches) {
        branchComboBox.removeAllItems();
        branchComboBox.addItem(""); // 添加空选项

        branches.stream().sorted().forEach(branch -> {
            System.out.println("添加分支到下拉框：" + branch);
            branchComboBox.addItem(branch);
        });

        System.out.println("下拉框总共有 " + branchComboBox.getItemCount() + " 个选项");
    }
    
    private static void setDefaultSelection(Project project, JComboBox<String> branchComboBox) {
        AutoPushStateContainer stateManager = AutoPushStateContainer.getInstance();
        String lastSelectedBranch = stateManager.getLastSelectedBranch(project);
        
        if (lastSelectedBranch != null && !lastSelectedBranch.trim().isEmpty()) {
            branchComboBox.setSelectedItem(lastSelectedBranch.trim());
            System.out.println("设置默认选中分支：" + lastSelectedBranch.trim());
        } else {
            branchComboBox.setSelectedIndex(0);
            System.out.println("设置默认选中空选项");
        }
    }
    
    private static void addEmptyOption(JComboBox<String> branchComboBox) {
        branchComboBox.removeAllItems();
        branchComboBox.addItem("");
        branchComboBox.setSelectedIndex(0);
        branchComboBox.hidePopup();
    }
}