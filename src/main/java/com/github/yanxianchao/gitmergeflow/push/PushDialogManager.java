package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

public class PushDialogManager {

    private static final PushDialogManager INSTANCE = new PushDialogManager();

    public static final String CUSTOM_COMPONENT_NAME = "AutoMergePushOptions";

    private volatile boolean initialized = false;

    private PushDialogManager() {
    }

    public static PushDialogManager getInstance() {
        if (!INSTANCE.initialized)
            INSTANCE.initializeGlobalListener();
        return INSTANCE;
    }

    private void initializeGlobalListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof WindowEvent windowEvent))
                return;
            if (windowEvent.getID() != WindowEvent.WINDOW_OPENED)
                return;
            Window window = windowEvent.getWindow();
            if (!(window instanceof JDialog dialog))
                return;
            String title = dialog.getTitle();
            if (title == null || !title.startsWith("Push Commits to "))
                return;
            SwingUtilities.invokeLater(() -> {
                Project currentProject = ProjectHelper.getCurrentActiveProject(dialog);
                if (currentProject != null && !currentProject.isDisposed() && !hasCustomComponent(dialog)) {
                    System.out.println("发现推送对话框：" + title + "，项目：" + currentProject.getName());
                    addCustomComponentToDialog(dialog, currentProject);
                }
            });
        }, AWTEvent.WINDOW_EVENT_MASK);
        initialized = true;
        System.out.println("已初始化全局AWT事件监听器");
    }

    private boolean hasCustomComponent(JDialog dialog) {
        return (findComponentByName(dialog, CUSTOM_COMPONENT_NAME) != null);
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

            JPanel customPanel = createCustomPanel(project);
            customPanel.setName(CUSTOM_COMPONENT_NAME);

            Container contentPane = dialog.getContentPane();
            DialogComponentAdder adder = new DialogComponentAdder();

            boolean added = adder.addToDialog(contentPane, customPanel);

            if (added) {
                dialog.revalidate();
                dialog.repaint();
                System.out.println("成功向推送对话框添加自定义组件");
            } else {
                System.out.println("未能找到适合的容器来放置自定义组件");
            }

        } catch (Exception e) {
            System.err.println("向推送对话框添加自定义组件失败：" + e.getMessage());
        }
    }

    private JPanel createCustomPanel(Project project) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBorder(JBUI.Borders.empty(0, 5));
        panel.setOpaque(false);

        JCheckBox pushToBranchCheckBox = new NonFocusableCheckBox("推送到分支：");
        JComboBox<String> branchComboBox = new ComboBox<>();

        setupComponents(pushToBranchCheckBox, branchComboBox, project);

        panel.add(pushToBranchCheckBox);
        panel.add(branchComboBox);

        return panel;
    }

    private void setupComponents(JCheckBox checkBox, JComboBox<String> comboBox, Project project) {
        AutoPushStateContainer stateManager = AutoPushStateContainer.getInstance();

        // 初始化状态
        checkBox.setSelected(stateManager.isPushToBranchEnabled(project));
        comboBox.setEnabled(checkBox.isSelected());
        comboBox.setEditable(false);
        comboBox.setPreferredSize(new Dimension(200, comboBox.getPreferredSize().height));

        // 设置渲染器和填充分支
        BranchComboBoxHelper.setupBranchComboBoxRenderer(comboBox);
        BranchComboBoxHelper.populateLocalBranches(project, comboBox);

        // 添加事件监听器
        checkBox.addActionListener(e -> {
            comboBox.setEnabled(checkBox.isSelected());
            stateManager.setPushToBranchEnabled(project, checkBox.isSelected());
            if (checkBox.isSelected()) {
                String selectedBranch = (String) comboBox.getSelectedItem();
                if (selectedBranch != null && !selectedBranch.trim().isEmpty()) {
                    stateManager.setLastSelectedBranch(project, selectedBranch);
                }
            }
        });

        comboBox.addActionListener(e -> {
            if (comboBox.getSelectedItem() != null) {
                String selectedBranch = (String) comboBox.getSelectedItem();
                if (!selectedBranch.trim().isEmpty()) {
                    stateManager.setLastSelectedBranch(project, selectedBranch);
                }
            }
        });
    }
}