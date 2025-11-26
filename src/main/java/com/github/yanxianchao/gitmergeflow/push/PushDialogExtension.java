package com.github.yanxianchao.gitmergeflow.push;

import com.github.yanxianchao.gitmergeflow.settings.ProjectAutoMergeSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.ui.JBUI;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;

public class PushDialogExtension implements StartupActivity {
    
    private JCheckBox myPushToBranchCheckBox;
    private JComboBox<String> myBranchComboBox;
    private ProjectAutoMergeSettings mySettings;
    private Timer dialogCheckTimer;
    private Project currentProject;
    
    // 静态变量用于保存推送时的状态
    private static volatile boolean lastPushToBranchState = false;
    private static volatile String lastSelectedBranch = "";
    
    @Override
    public void runActivity(@NotNull Project project) {
        this.currentProject = project;
        mySettings = ProjectAutoMergeSettings.getInstance(project);
        
        System.out.println("推送对话框扩展：为项目初始化活动 - " + project.getName());
        
        // 使用AWTEventListener监听所有窗口事件
        ApplicationManager.getApplication().invokeLater(() -> {
            monitorForPushDialog(project);
        });
    }
    
    private void monitorForPushDialog(Project project) {
        // 监听所有AWT事件，特别是窗口事件
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event instanceof WindowEvent) {
                    WindowEvent windowEvent = (WindowEvent) event;
                    if (windowEvent.getID() == WindowEvent.WINDOW_OPENED) {
                        Window window = windowEvent.getWindow();
                        if (window instanceof JDialog) {
                            JDialog dialog = (JDialog) window;
                            String title = dialog.getTitle();
                            System.out.println("对话框已打开：" + title);
                            
                            // 检查是否是推送对话框
                            if (title != null && title.contains("Push")) {
                                if (!hasCustomComponent(dialog)) {
                                    System.out.println("发现推送对话框：" + title);
                                    SwingUtilities.invokeLater(() -> {
                                        if (project != null && !project.isDisposed()) {
                                            // 再次检查，避免重复添加
                                            if (!hasCustomComponent(dialog)) {
                                                addCustomComponentToDialog(dialog, project);
                                            }
                                        } else {
                                            System.out.println("项目已销毁，跳过对话框组件添加");
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
        
        // 同时定期检查现有窗口，但避免重复检查
        if (dialogCheckTimer != null) {
            dialogCheckTimer.stop();
        }
        dialogCheckTimer = new Timer(1000, e -> {
            checkExistingWindows(project);
        });
        dialogCheckTimer.start();
    }
    
    private void checkExistingWindows(Project project) {
        // 检查项目状态
        if (project == null || project.isDisposed()) {
            System.out.println("项目已销毁，停止检查窗口");
            return;
        }
        
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog && window.isVisible()) {
                JDialog dialog = (JDialog) window;
                String title = dialog.getTitle();
                if (title != null && title.contains("Push")) {
                    // 只检查组件是否存在
                    if (!hasCustomComponent(dialog)) {
                        System.out.println("发现已存在的推送对话框：" + title);
                        SwingUtilities.invokeLater(() -> {
                            if (project != null && !project.isDisposed()) {
                                // 再次检查，避免重复添加
                                if (!hasCustomComponent(dialog)) {
                                    addCustomComponentToDialog(dialog, project);
                                }
                            } else {
                                System.out.println("项目已销毁，跳过对话框组件添加");
                            }
                        });
                    }
                }
            }
        }
        
        
    }
    
    private boolean hasCustomComponent(JDialog dialog) {
        // 检查对话框是否已包含我们的自定义组件（通过名称检查）
        Component found = findComponentByName(dialog, "AutoMergePushOptions");
        if (found != null) {
            System.out.println("对话框中已存在自定义组件（通过名称检测）");
            return true;
        }
        
        // 额外检查：查找我们的复选框文本
        Component textFound = findComponentWithText(dialog, "推送到分支：");
        if (textFound != null) {
            System.out.println("对话框中已存在自定义组件（通过文本检测）");
            return true;
        }
        
        System.out.println("对话框中未找到自定义组件，可以添加");
        return false;
    }
    
    private Component findComponentWithText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (text.equals(checkBox.getText())) {
                    return component;
                }
            }
            if (component instanceof Container) {
                Component found = findComponentWithText((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private Component findComponentByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container) {
                Component found = findComponentByName((Container) component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private void addCustomComponentToDialog(JDialog dialog, Project project) {
        try {
            System.out.println("尝试向对话框添加自定义组件：" + dialog.getTitle());
            
            // 创建自定义组件
            JPanel customPanel = createCustomPanel(project);
            customPanel.setName("AutoMergePushOptions"); // 设置名称用于检测
            
            Container contentPane = dialog.getContentPane();
            boolean added = false;
            
            // 策略1：查找按钮面板并在其上方添加
            added = addAboveButtonPanel(contentPane, customPanel);
            
            // 策略2：如果没找到按钮面板，尝试添加到中间区域
            if (!added) {
                added = addToMiddleArea(contentPane, customPanel);
            }
            
            // 策略3：最后尝试添加到底部（但避免覆盖重要组件）
            if (!added) {
                added = addToBottomSafely(contentPane, customPanel);
            }
            
            if (added) {
                // 重新布局和重绘
                dialog.revalidate();
                dialog.repaint();
                // 不使用pack()以避免改变对话框大小
                System.out.println("成功向推送对话框添加自定义组件");
            } else {
                System.out.println("未能找到适合的容器来放置自定义组件");
                // 输出对话框结构用于调试
                printComponentStructure(contentPane, 0);
            }
            
        } catch (Exception e) {
            System.err.println("向推送对话框添加自定义组件失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean addAboveButtonPanel(Container container, JPanel customPanel) {
        // 首先尝试查找推送标签相关组件并在同一行添加
        Component pushTagsComponent = findPushTagsComponent(container);
        if (pushTagsComponent != null && pushTagsComponent.getParent() instanceof Container) {
            Container parent = (Container) pushTagsComponent.getParent();
            LayoutManager layout = parent.getLayout();
            
            if (layout instanceof FlowLayout || layout instanceof BoxLayout) {
                // 在同一行添加我们的组件
                parent.add(customPanel);
                return true;
            } else if (layout instanceof BorderLayout) {
                // 如果是BorderLayout，尝试添加到EAST位置
                BorderLayout borderLayout = (BorderLayout) layout;
                if (borderLayout.getLayoutComponent(BorderLayout.EAST) == null) {
                    parent.add(customPanel, BorderLayout.EAST);
                    return true;
                }
            }
        }
        
        // 如果没找到推送标签，查找包含按钮的面板
        Component buttonPanel = findButtonPanel(container);
        if (buttonPanel != null && buttonPanel.getParent() instanceof Container) {
            Container parent = (Container) buttonPanel.getParent();
            LayoutManager layout = parent.getLayout();
            
            if (layout instanceof BorderLayout) {
                // 如果按钮面板在SOUTH，我们添加到CENTER的底部
                BorderLayout borderLayout = (BorderLayout) layout;
                Component center = borderLayout.getLayoutComponent(BorderLayout.CENTER);
                if (center instanceof Container) {
                    return addToContainerBottom((Container) center, customPanel);
                }
            } else if (layout instanceof BoxLayout) {
                // 在按钮面板前插入我们的组件
                int index = getComponentIndex(parent, buttonPanel);
                if (index >= 0) {
                    parent.add(customPanel, index);
                    return true;
                }
            }
        }
        return false;
    }
    
    private Component findPushTagsComponent(Container container) {
        // 查找包含"Push tags"或类似文本的组件
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                String text = checkBox.getText();
                if (text != null && (text.contains("Push tags") || text.contains("tags"))) {
                    return component;
                }
            } else if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                String text = label.getText();
                if (text != null && (text.contains("Push tags") || text.contains("tags"))) {
                    return component;
                }
            }
            
            if (component instanceof Container) {
                Component found = findPushTagsComponent((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private boolean addToMiddleArea(Container container, JPanel customPanel) {
        // 查找中间的主要内容区域
        if (container.getLayout() instanceof BorderLayout) {
            BorderLayout layout = (BorderLayout) container.getLayout();
            Component center = layout.getLayoutComponent(BorderLayout.CENTER);
            if (center instanceof Container) {
                return addToContainerBottom((Container) center, customPanel);
            }
        }
        
        // 查找合适的面板
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                if (panel.getComponentCount() > 0 && !isButtonPanel(panel)) {
                    return addToContainerBottom(panel, customPanel);
                }
            }
        }
        return false;
    }
    
    private boolean addToBottomSafely(Container container, JPanel customPanel) {
        if (container.getLayout() instanceof BorderLayout) {
            BorderLayout layout = (BorderLayout) container.getLayout();
            // 只在SOUTH位置为空时添加
            if (layout.getLayoutComponent(BorderLayout.SOUTH) == null) {
                container.add(customPanel, BorderLayout.SOUTH);
                return true;
            }
        }
        return false;
    }
    
    private boolean addToContainerBottom(Container container, JPanel customPanel) {
        LayoutManager layout = container.getLayout();
        
        if (layout instanceof BorderLayout) {
            BorderLayout borderLayout = (BorderLayout) layout;
            if (borderLayout.getLayoutComponent(BorderLayout.SOUTH) == null) {
                container.add(customPanel, BorderLayout.SOUTH);
                return true;
            }
        } else if (layout instanceof BoxLayout) {
            container.add(customPanel);
            return true;
        } else if (layout instanceof FlowLayout) {
            container.add(customPanel);
            return true;
        }
        return false;
    }
    
    private Component findButtonPanel(Container container) {
        for (Component component : container.getComponents()) {
            if (isButtonPanel(component)) {
                return component;
            }
            if (component instanceof Container) {
                Component found = findButtonPanel((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private boolean isButtonPanel(Component component) {
        if (!(component instanceof Container)) {
            return false;
        }
        
        Container container = (Container) component;
        int buttonCount = 0;
        
        for (Component child : container.getComponents()) {
            if (child instanceof JButton) {
                buttonCount++;
            }
        }
        
        // 如果包含2个或更多按钮，认为是按钮面板
        return buttonCount >= 2;
    }
    
    private int getComponentIndex(Container parent, Component component) {
        Component[] components = parent.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] == component) {
                return i;
            }
        }
        return -1;
    }
    
    
    
    private void printComponentStructure(Container container, int level) {
        String indent = "  ".repeat(level);
        System.out.println(indent + container.getClass().getSimpleName() + 
                          " [" + container.getLayout().getClass().getSimpleName() + "]");
        
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                printComponentStructure((Container) component, level + 1);
            } else {
                System.out.println(indent + "  " + component.getClass().getSimpleName());
            }
        }
    }
    
    private JPanel createCustomPanel(Project project) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBorder(JBUI.Borders.empty(0, 5, 0, 5)); // 移除分隔符，只保留左右边距
        panel.setOpaque(false); // 透明背景，更好地与原始界面集成
        
        // 使用更小的字体和更紧凑的布局
        myPushToBranchCheckBox = new NonFocusableCheckBox("推送到分支：");
        myPushToBranchCheckBox.setSelected(false); // 默认不选中
        
        // 创建分支选择下拉框
        myBranchComboBox = new JComboBox<>();
        myBranchComboBox.setEditable(false); // 不可编辑，只能选择
        myBranchComboBox.setEnabled(myPushToBranchCheckBox.isSelected());
        myBranchComboBox.setPreferredSize(new Dimension(200, myBranchComboBox.getPreferredSize().height));
        
        // 设置自定义渲染器
        setupBranchComboBoxRenderer();
        
        // 获取本地分支列表并填充下拉框
        if (project != null && !project.isDisposed()) {
            populateLocalBranches(project);
        } else {
            System.out.println("项目为空或已销毁，跳过分支列表填充");
            // 添加空选项作为默认
            myBranchComboBox.addItem("");
        }
        
        myPushToBranchCheckBox.addActionListener(e -> {
            myBranchComboBox.setEnabled(myPushToBranchCheckBox.isSelected());
            // 更新静态状态
            lastPushToBranchState = myPushToBranchCheckBox.isSelected();
            if (myPushToBranchCheckBox.isSelected()) {
                String selectedBranch = (String) myBranchComboBox.getSelectedItem();
                if (selectedBranch != null && !selectedBranch.trim().isEmpty()) {
                    mySettings.setLastUsedBranch(selectedBranch.trim());
                    lastSelectedBranch = selectedBranch.trim();
                }
            }
        });
        
        myBranchComboBox.addActionListener(e -> {
            if (myBranchComboBox.getSelectedItem() != null) {
                String selectedBranch = (String) myBranchComboBox.getSelectedItem();
                if (!selectedBranch.trim().isEmpty()) {
                    mySettings.setLastUsedBranch(selectedBranch.trim());
                    // 更新静态状态
                    lastSelectedBranch = selectedBranch.trim();
                }
            }
        });
        
        panel.add(myPushToBranchCheckBox);
        panel.add(myBranchComboBox);
        
        // 初始化静态状态
        lastPushToBranchState = myPushToBranchCheckBox.isSelected();
        if (myBranchComboBox.getSelectedItem() != null) {
            String selectedItem = (String) myBranchComboBox.getSelectedItem();
            lastSelectedBranch = selectedItem != null ? selectedItem.trim() : "";
        }
        
        return panel;
    }
    
    private void populateLocalBranches(Project project) {
        try {
            if (project == null) {
                System.out.println("项目为空，无法获取分支列表");
                return;
            }
            
            // 检查项目是否已被销毁
            if (project.isDisposed()) {
                System.out.println("项目已被销毁，跳过分支列表获取");
                return;
            }
            
            // 安全地获取Git仓库管理器
            git4idea.repo.GitRepositoryManager repositoryManager;
            try {
                repositoryManager = git4idea.repo.GitRepositoryManager.getInstance(project);
            } catch (Exception e) {
                System.out.println("无法获取Git仓库管理器：" + e.getMessage());
                return;
            }
            
            if (repositoryManager == null) {
                System.out.println("Git仓库管理器为空");
                return;
            }
            
            java.util.List<GitRepository> repositories = repositoryManager.getRepositories();
            if (repositories.isEmpty()) {
                System.out.println("未找到Git仓库");
                return;
            }
            
            GitRepository repository = repositories.get(0);
            if (repository == null) {
                System.out.println("Git仓库为空，无法获取分支列表");
                return;
            }
            
            System.out.println("开始获取本地分支列表，仓库路径：" + repository.getRoot().getPath());
            
            // Get local branches list
            java.util.Set<String> localBranches = new java.util.HashSet<>();
            
            // 获取所有本地分支
            repository.getBranches().getLocalBranches().forEach(branch -> {
                String branchName = branch.getName();
                System.out.println("发现本地分支：" + branchName);
                if (branchName != null && !branchName.isEmpty()) {
                    localBranches.add(branchName);
                }
            });
            
            System.out.println("总共找到 " + localBranches.size() + " 个本地分支");
            
            // 将所有分支保存到全局列表用于搜索
            allBranches.clear();
            allBranches.addAll(localBranches.stream().sorted().collect(java.util.stream.Collectors.toList()));
            
            // 清空并填充下拉框
            myBranchComboBox.removeAllItems();
            myBranchComboBox.addItem(""); // 添加空选项
            
            allBranches.forEach(branch -> {
                System.out.println("添加分支到下拉框：" + branch);
                myBranchComboBox.addItem(branch);
            });
            
            System.out.println("下拉框总共有 " + myBranchComboBox.getItemCount() + " 个选项");
            
            // 设置默认选中项（上次选择的分支）
            String lastUsedBranch = mySettings.getLastUsedBranch();
            if (lastUsedBranch != null && !lastUsedBranch.trim().isEmpty()) {
                myBranchComboBox.setSelectedItem(lastUsedBranch.trim());
                System.out.println("设置默认选中分支：" + lastUsedBranch.trim());
            } else {
                myBranchComboBox.setSelectedIndex(0); // 选择空选项
                System.out.println("设置默认选中空选项");
            }
            
            // 确保下拉框不会自动弹出
            myBranchComboBox.hidePopup();
            
        } catch (Exception e) {
            System.err.println("填充本地分支列表失败：" + e.getMessage());
            e.printStackTrace();
            // 如果获取失败，只显示空选项
            allBranches.clear();
            
            myBranchComboBox.removeAllItems();
            myBranchComboBox.addItem(""); // 发生错误时只添加空选项
            myBranchComboBox.setSelectedIndex(0);
            myBranchComboBox.hidePopup();
        }
    }
    
    public static boolean shouldPushToBranch() {
        // 首先尝试从当前活动的对话框获取状态
        boolean currentState = getCurrentCheckboxState();
        if (currentState) {
            // 如果找到了活动状态，保存并返回
            lastPushToBranchState = true;
            return true;
        }
        // 如果没有找到活动对话框，返回最后保存的状态
        return lastPushToBranchState;
    }
    
    public static String getBranchName() {
        // 首先尝试从当前活动的对话框获取分支名
        String currentBranch = getCurrentBranchName();
        if (currentBranch != null && !currentBranch.trim().isEmpty()) {
            // 如果找到了分支名，保存并返回
            lastSelectedBranch = currentBranch.trim();
            return lastSelectedBranch;
        }
        // 如果没有找到活动对话框，返回最后保存的分支名
        return lastSelectedBranch;
    }
    
    private static boolean getCurrentCheckboxState() {
        // 查找当前可见的推送对话框中的复选框
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog && window.isVisible()) {
                JDialog dialog = (JDialog) window;
                String title = dialog.getTitle();
                if (title != null && title.contains("Push")) {
                    JCheckBox checkbox = findCheckboxInDialog(dialog);
                    if (checkbox != null) {
                        return checkbox.isSelected();
                    }
                }
            }
        }
        return false;
    }
    
    private static String getCurrentBranchName() {
        // 查找当前可见的推送对话框中的下拉框
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog && window.isVisible()) {
                JDialog dialog = (JDialog) window;
                String title = dialog.getTitle();
                if (title != null && title.contains("Push")) {
                    JComboBox<String> comboBox = findComboBoxInDialog(dialog);
                    if (comboBox != null && comboBox.getSelectedItem() != null) {
                        String selectedItem = (String) comboBox.getSelectedItem();
                        return selectedItem != null ? selectedItem.trim() : "";
                    }
                }
            }
        }
        return "";
    }
    
    private static JCheckBox findCheckboxInDialog(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if ("推送到分支：".equals(checkBox.getText())) {
                    return checkBox;
                }
            }
            if (component instanceof Container) {
                JCheckBox found = findCheckboxInDialog((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private static JComboBox<String> findComboBoxInDialog(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox) {
                // 假设这是我们的分支选择下拉框
                @SuppressWarnings("unchecked")
                JComboBox<String> comboBox = (JComboBox<String>) component;
                // 简单检查：如果下拉框在包含我们复选框的面板中，就认为是我们的
                Container parent = comboBox.getParent();
                if (parent != null && findCheckboxInDialog(parent) != null) {
                    return comboBox;
                }
            }
            if (component instanceof Container) {
                JComboBox<String> found = findComboBoxInDialog((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private void setupBranchComboBoxRenderer() {
        if (myBranchComboBox == null) return;
        
        // 设置自定义渲染器以更好地显示分支名称
        myBranchComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (value != null && !value.toString().isEmpty()) {
                    // 设置图标
                    setIcon(com.intellij.icons.AllIcons.Vcs.Branch);
                    // 设置工具提示显示完整分支名称
                    setToolTipText(value.toString());
                }
                
                return this;
            }
        });
    }
    
    private final java.util.List<String> allBranches = new java.util.ArrayList<>();
}