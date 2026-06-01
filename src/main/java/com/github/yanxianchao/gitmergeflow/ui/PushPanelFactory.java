package com.github.yanxianchao.gitmergeflow.ui;

import com.github.yanxianchao.gitmergeflow.config.ConfigurationManager;
import com.github.yanxianchao.gitmergeflow.config.PushConfiguration;
import com.github.yanxianchao.gitmergeflow.deploy.McpAppService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 推送面板工厂 - 负责创建推送配置UI组件
 */
public final class PushPanelFactory {

    private static final Logger LOG = Logger.getInstance(PushPanelFactory.class);

    private final ConfigurationManager configManager;

    public PushPanelFactory(@NotNull ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @NotNull
    public JPanel createPushPanel(@NotNull Project project, @NotNull String componentName) {
        PushConfiguration config = configManager.getConfiguration(project);

        // 合并推送组件
        JCheckBox enableCheckBox = new NonFocusableCheckBox("合并推送至：");
        JComboBox<String> branchComboBox = BranchComboBoxFactory.createBranchComboBox(project, config.getTargetBranch());
        enableCheckBox.setSelected(config.isEnabled());
        branchComboBox.setEnabled(config.isEnabled());
        enableCheckBox.addActionListener(e -> handleCheckBoxChange(enableCheckBox, branchComboBox, project));
        branchComboBox.addActionListener(e -> handleBranchSelection(enableCheckBox, branchComboBox, project));

        // 部署到预发环境组件
        JCheckBox deployCheckBox = new NonFocusableCheckBox("部署预发：");

        // 可编辑的制品下拉框：既可选择也可手动输入
        JComboBox<String> goodsComboBox = new JComboBox<>();
        goodsComboBox.setEditable(true);
        goodsComboBox.setPreferredSize(new Dimension(200, goodsComboBox.getPreferredSize().height));
        goodsComboBox.setToolTipText("点击下拉自动加载制品列表，也可直接输入制品ID");

        // 初始化状态
        deployCheckBox.setSelected(config.isDeployEnabled());
        goodsComboBox.setEnabled(config.isDeployEnabled());

        // 恢复上次保存的goodsId
        String savedGoodsId = config.getGoodsId();
        if (!savedGoodsId.isEmpty()) {
            goodsComboBox.addItem(savedGoodsId);
            goodsComboBox.setSelectedItem(savedGoodsId);
        }

        // 初始化应用名（从Git仓库推断或使用已保存的值）
        String appName = config.getAppName();
        if (appName.isEmpty()) {
            appName = resolveAppNameFromGit(project);
            if (appName != null && !appName.isEmpty()) {
                configManager.updateAppName(project, appName);
            }
        }

        // 点击下拉框时自动加载制品列表（仅首次加载）
        String finalAppName = appName;
        goodsComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            private boolean loaded = false;

            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                if (!loaded) {
                    loaded = true;
                    loadGoodsList(project, finalAppName, goodsComboBox);
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });

        // 绑定事件
        deployCheckBox.addActionListener(e -> {
            boolean enabled = deployCheckBox.isSelected();
            goodsComboBox.setEnabled(enabled);
            saveGoodsId(deployCheckBox, goodsComboBox, project);
        });

        goodsComboBox.addActionListener(e -> saveGoodsId(deployCheckBox, goodsComboBox, project));

        // 编辑器内容变化时也保存
        JTextField editorField = (JTextField) goodsComboBox.getEditor().getEditorComponent();
        editorField.addActionListener(e -> saveGoodsId(deployCheckBox, goodsComboBox, project));

        // 创建面板
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBorder(JBUI.Borders.empty(0, 5));
        panel.setOpaque(false);
        panel.setName(componentName);
        panel.add(enableCheckBox);
        panel.add(branchComboBox);
        panel.add(deployCheckBox);
        panel.add(goodsComboBox);

        return panel;
    }

    /**
     * 从Git仓库远程URL推断应用名
     */
    @Nullable
    private String resolveAppNameFromGit(@NotNull Project project) {
        try {
            GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
            List<GitRepository> repositories = manager.getRepositories();
            if (repositories.isEmpty()) return null;

            GitRepository repository = repositories.get(0);
            var remotes = repository.getRemotes();
            for (var remote : remotes) {
                var urls = remote.getUrls();
                for (String url : urls) {
                    String repoName = extractRepoName(url);
                    if (repoName != null) {
                        LOG.info("从Git远程URL推断应用名: " + repoName);
                        return repoName;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("从Git仓库推断应用名失败", e);
        }
        return null;
    }

    /**
     * 从Git URL中提取仓库名
     * 支持格式：https://xxx/group/repo.git 或 git@xxx:group/repo.git
     */
    @Nullable
    private String extractRepoName(@NotNull String gitUrl) {
        String url = gitUrl.trim();
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        int lastSlash = url.lastIndexOf('/');
        int lastColon = url.lastIndexOf(':');
        int separator = Math.max(lastSlash, lastColon);
        if (separator >= 0 && separator < url.length() - 1) {
            return url.substring(separator + 1);
        }
        return null;
    }

    /**
     * 异步加载制品列表到ComboBox
     */
    private void loadGoodsList(@NotNull Project project, @Nullable String appName,
                               @NotNull JComboBox<String> goodsComboBox) {
        if (appName == null || appName.isEmpty()) {
            LOG.warn("应用名称为空，无法加载制品列表");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Integer appId = McpAppService.getAppIdByName(appName);
            if (appId == null) {
                LOG.warn("未找到应用: " + appName);
                return;
            }

            List<McpAppService.GoodsItem> goodsList = McpAppService.getGoodsList(appId);
            SwingUtilities.invokeLater(() -> {
                Object currentItem = goodsComboBox.getEditor().getItem();
                goodsComboBox.removeAllItems();
                goodsComboBox.addItem("");

                for (McpAppService.GoodsItem item : goodsList) {
                    goodsComboBox.addItem(item.toString());
                }

                if (currentItem != null && !currentItem.toString().trim().isEmpty()) {
                    goodsComboBox.setSelectedItem(currentItem);
                }

                LOG.info("制品列表加载完成，共 " + goodsList.size() + " 条");
            });
        });
    }

    /**
     * 保存当前选中/输入的制品ID
     */
    private void saveGoodsId(@NotNull JCheckBox deployCheckBox,
                             @NotNull JComboBox<String> goodsComboBox,
                             @NotNull Project project) {
        String selectedItem = getGoodsIdFromComboBox(goodsComboBox);
        configManager.updateDeploy(project, deployCheckBox.isSelected(), selectedItem);
    }

    /**
     * 从ComboBox中提取制品ID（支持 "id - branch - state" 格式和纯数字格式）
     */
    @NotNull
    private String getGoodsIdFromComboBox(@NotNull JComboBox<String> comboBox) {
        Object item = comboBox.getEditor().getItem();
        if (item == null) return "";

        String text = item.toString().trim();
        if (text.isEmpty()) return "";

        // 如果是 "id - branch - state" 格式，提取id部分
        if (text.contains(" - ")) {
            return text.substring(0, text.indexOf(" - ")).trim();
        }
        return text;
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