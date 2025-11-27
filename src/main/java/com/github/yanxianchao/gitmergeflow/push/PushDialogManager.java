package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

public class PushDialogManager {

    private static final PushDialogManager INSTANCE = new PushDialogManager();

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
                Project currentProject = getCurrentActiveProject(dialog);
                if (currentProject != null && !currentProject.isDisposed() && !hasCustomComponent(dialog)) {
                    System.out.println("发现推送对话框：" + title + "，项目：" + currentProject.getName());
                    addCustomComponentToDialog(dialog, currentProject);
                }
            });
        }, AWTEvent.WINDOW_EVENT_MASK);
        initialized = true;
        System.out.println("已初始化全局AWT事件监听器");
    }

    public Project getCurrentActiveProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            return openProjects[openProjects.length - 1];
        }
        return null;
    }

    private Project getCurrentActiveProject(JDialog dialog) {
        Window owner = dialog.getOwner();
        if (owner != null) {
            if (owner instanceof Frame frame) {
                String title = frame.getTitle();
                if (title != null) {
                    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                        if (title.contains(project.getName())) {
                            return project;
                        }
                    }
                }
            }
        }
        return getCurrentActiveProject();
    }

    private boolean hasCustomComponent(JDialog dialog) {
        return (findComponentByName(dialog, "AutoMergePushOptions") != null
                || findComponentWithText(dialog, "推送到分支：") != null);
    }

    private Component findComponentWithText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox checkBox) {
                if (text.equals(checkBox.getText()))
                    return component;
            }
            if (component instanceof Container) {
                Component found = findComponentWithText((Container) component, text);
                if (found != null)
                    return found;
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

            JPanel customPanel = createCustomPanel(project);
            customPanel.setName("AutoMergePushOptions");

            Container contentPane = dialog.getContentPane();
            DialogComponentAdder adder = new DialogComponentAdder();

            boolean added = adder.addToDialog(contentPane, customPanel);

            if (added) {
                dialog.revalidate();
                dialog.repaint();
                System.out.println("成功向推送对话框添加自定义组件");
            } else {
                System.out.println("未能找到适合的容器来放置自定义组件");
                printComponentStructure(contentPane, 0);
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
        JComboBox<String> branchComboBox = new JComboBox<>();

        setupComponents(pushToBranchCheckBox, branchComboBox, project);

        panel.add(pushToBranchCheckBox);
        panel.add(branchComboBox);

        return panel;
    }

    private void setupComponents(JCheckBox checkBox, JComboBox<String> comboBox, Project project) {
        ProjectPushStateManager stateManager = ProjectPushStateManager.getInstance();

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

    public JCheckBox findCheckboxInDialog(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox checkBox) {
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

    public JComboBox<String> findComboBoxInDialog(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox) {
                @SuppressWarnings("unchecked")
                JComboBox<String> comboBox = (JComboBox<String>) component;
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
}