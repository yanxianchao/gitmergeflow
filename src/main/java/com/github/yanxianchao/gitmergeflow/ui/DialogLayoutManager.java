package com.github.yanxianchao.gitmergeflow.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 对话框布局管理器 - 负责在对话框中添加组件的布局策略
 */
public final class DialogLayoutManager {
    
    private static final int MIN_BUTTON_COUNT_FOR_PANEL = 2;
    
    private DialogLayoutManager() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 添加组件到容器中，使用以下策略（按优先级顺序）：
     * 1. 尝试添加到按钮上方区域（推送标签附近或按钮面板上方）
     * 2. 尝试添加到中间内容区域
     * 3. 尝试添加到容器底部
     * 
     * @param container 目标容器
     * @param component 要添加的组件
     * @return 是否成功添加
     */
    public static boolean addComponent(@NotNull Container container, @NotNull JPanel component) {
        return tryAddAboveButtons(container, component) ||
               tryAddToMiddleArea(container, component) ||
               tryAddToBottom(container, component);
    }
    
    /**
     * 策略1：尝试添加到按钮上方区域
     * 优先级：推送标签附近 > 按钮面板上方
     */
    private static boolean tryAddAboveButtons(@NotNull Container container, @NotNull JPanel component) {
        return tryAddNearPushTags(container, component) || 
               tryAddAboveButtonPanel(container, component);
    }
    
    /**
     * 尝试添加到推送标签附近（同一行）
     */
    private static boolean tryAddNearPushTags(@NotNull Container container, @NotNull JPanel component) {
        Component pushTagsComponent = findPushTagsComponent(container);
        if (pushTagsComponent == null || pushTagsComponent.getParent() == null) {
            return false;
        }
        
        Container parent = pushTagsComponent.getParent();
        return addToLayoutByType(parent, component, BorderLayout.EAST);
    }
    
    /**
     * 尝试添加到按钮面板上方
     */
    private static boolean tryAddAboveButtonPanel(@NotNull Container container, @NotNull JPanel component) {
        Component buttonPanel = findButtonPanel(container);
        if (buttonPanel == null || buttonPanel.getParent() == null) {
            return false;
        }
        
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
        return false;
    }
    
    /**
     * 递归查找推送标签相关组件
     */
    private static Component findPushTagsComponent(@NotNull Container container) {
        for (Component component : container.getComponents()) {
            if (isPushTagsComponent(component)) {
                return component;
            }
            
            // 递归查找子容器
            if (component instanceof Container) {
                Component found = findPushTagsComponent((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    /**
     * 判断是否为推送标签组件
     */
    private static boolean isPushTagsComponent(@NotNull Component component) {
        String text = null;
        
        if (component instanceof JCheckBox checkBox) {
            text = checkBox.getText();
        } else if (component instanceof JLabel label) {
            text = label.getText();
        }
        
        return text != null && (text.contains("Push tags") || text.contains("tags"));
    }
    
    /**
     * 策略2：尝试添加到中间内容区域
     */
    private static boolean tryAddToMiddleArea(@NotNull Container container, @NotNull JPanel component) {
        // 优先尝试BorderLayout的CENTER区域
        if (container.getLayout() instanceof BorderLayout layout) {
            Component center = layout.getLayoutComponent(BorderLayout.CENTER);
            if (center instanceof Container) {
                return addToContainerBottom((Container) center, component);
            }
        }
        
        // 查找非按钮面板的内容面板
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel panel && panel.getComponentCount() > 0 && !isButtonPanel(panel)) {
                return addToContainerBottom(panel, component);
            }
        }
        return false;
    }
    
    /**
     * 策略3：尝试添加到容器底部
     */
    private static boolean tryAddToBottom(@NotNull Container container, @NotNull JPanel component) {
        return addToLayoutByType(container, component, BorderLayout.SOUTH);
    }
    
    /**
     * 根据容器布局类型添加组件到底部
     */
    private static boolean addToContainerBottom(@NotNull Container container, @NotNull JPanel component) {
        return addToLayoutByType(container, component, BorderLayout.SOUTH);
    }
    
    /**
     * 根据布局管理器类型添加组件
     * 
     * @param container 目标容器
     * @param component 要添加的组件
     * @param borderConstraint BorderLayout约束（如果适用）
     * @return 是否成功添加
     */
    private static boolean addToLayoutByType(@NotNull Container container, 
                                           @NotNull JPanel component, 
                                           @NotNull String borderConstraint) {
        LayoutManager layout = container.getLayout();
        
        if (layout instanceof BorderLayout borderLayout) {
            if (borderLayout.getLayoutComponent(borderConstraint) == null) {
                container.add(component, borderConstraint);
                return true;
            }
        } else if (layout instanceof BoxLayout || layout instanceof FlowLayout) {
            container.add(component);
            return true;
        }
        return false;
    }
    
    /**
     * 递归查找按钮面板
     */
    private static Component findButtonPanel(@NotNull Container container) {
        for (Component component : container.getComponents()) {
            if (isButtonPanel(component)) {
                return component;
            }
            
            // 递归查找子容器
            if (component instanceof Container) {
                Component found = findButtonPanel((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    /**
     * 判断是否为按钮面板（包含至少2个按钮的容器）
     */
    private static boolean isButtonPanel(@NotNull Component component) {
        if (!(component instanceof Container container)) {
            return false;
        }
        
        int buttonCount = 0;
        for (Component child : container.getComponents()) {
            if (child instanceof JButton) {
                buttonCount++;
            }
        }
        return buttonCount >= MIN_BUTTON_COUNT_FOR_PANEL;
    }
    
    /**
     * 获取组件在父容器中的索引位置
     */
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