package com.github.yanxianchao.gitmergeflow.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 对话框布局管理器 - 负责在对话框中添加组件的布局策略
 */
public final class DialogLayoutManager {
    
    private DialogLayoutManager() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static boolean addComponent(@NotNull Container container, @NotNull JPanel component) {
        return tryAddAboveButtons(container, component) ||
               tryAddToMiddleArea(container, component) ||
               tryAddToBottom(container, component);
    }
    
    private static boolean tryAddAboveButtons(@NotNull Container container, @NotNull JPanel component) {
        // 首先尝试查找推送标签相关组件并在同一行添加
        Component pushTagsComponent = findPushTagsComponent(container);
        if (pushTagsComponent != null && pushTagsComponent.getParent() != null) {
            Container parent = pushTagsComponent.getParent();
            LayoutManager layout = parent.getLayout();

            if (layout instanceof FlowLayout || layout instanceof BoxLayout) {
                parent.add(component);
                return true;
            } else if (layout instanceof BorderLayout borderLayout) {
                if (borderLayout.getLayoutComponent(BorderLayout.EAST) == null) {
                    parent.add(component, BorderLayout.EAST);
                    return true;
                }
            }
        }

        // 如果没找到推送标签，查找包含按钮的面板
        Component buttonPanel = findButtonPanel(container);
        if (buttonPanel != null && buttonPanel.getParent() != null) {
            Container parent = buttonPanel.getParent();
            LayoutManager layout = parent.getLayout();
            
            if (layout instanceof BorderLayout borderLayout) {
                Component center = borderLayout.getLayoutComponent(BorderLayout.CENTER);
                if (center instanceof Container) {
                    return addToContainerBottom((Container) center, component);
                }
            } else if (layout instanceof BoxLayout) {
                int index = getComponentIndex(parent, buttonPanel);
                if (index >= 0) {
                    parent.add(component, index);
                    return true;
                }
            }
        }
        return false;
    }
    
    private static Component findPushTagsComponent(@NotNull Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox checkBox) {
                String text = checkBox.getText();
                if (text != null && (text.contains("Push tags") || text.contains("tags"))) {
                    return component;
                }
            } else if (component instanceof JLabel label) {
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
    
    private static boolean tryAddToMiddleArea(@NotNull Container container, @NotNull JPanel component) {
        if (container.getLayout() instanceof BorderLayout layout) {
            Component center = layout.getLayoutComponent(BorderLayout.CENTER);
            if (center instanceof Container) {
                return addToContainerBottom((Container) center, component);
            }
        }
        
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel panel && panel.getComponentCount() > 0 && !isButtonPanel(panel)) {
                return addToContainerBottom(panel, component);
            }
        }
        return false;
    }
    
    private static boolean tryAddToBottom(@NotNull Container container, @NotNull JPanel component) {
        if (container.getLayout() instanceof BorderLayout layout) {
            if (layout.getLayoutComponent(BorderLayout.SOUTH) == null) {
                container.add(component, BorderLayout.SOUTH);
                return true;
            }
        }
        return false;
    }
    
    private static boolean addToContainerBottom(@NotNull Container container, @NotNull JPanel component) {
        LayoutManager layout = container.getLayout();
        
        if (layout instanceof BorderLayout borderLayout) {
            if (borderLayout.getLayoutComponent(BorderLayout.SOUTH) == null) {
                container.add(component, BorderLayout.SOUTH);
                return true;
            }
        } else if (layout instanceof BoxLayout || layout instanceof FlowLayout) {
            container.add(component);
            return true;
        }
        return false;
    }
    
    private static Component findButtonPanel(@NotNull Container container) {
        for (Component component : container.getComponents()) {
            if (isButtonPanel(component)) {
                return component;
            }
            if (component instanceof Container) {
                Component found = findButtonPanel((Container) component);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private static boolean isButtonPanel(@NotNull Component component) {
        if (!(component instanceof Container container)) return false;
        
        int buttonCount = 0;
        for (Component child : container.getComponents()) {
            if (child instanceof JButton) {
                buttonCount++;
            }
        }
        return buttonCount >= 2;
    }
    
    private static int getComponentIndex(@NotNull Container parent, @NotNull Component component) {
        Component[] components = parent.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] == component) {
                return i;
            }
        }
        return -1;
    }
}