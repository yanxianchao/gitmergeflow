package com.github.yanxianchao.gitmergeflow.push;

import javax.swing.*;
import java.awt.*;

public class DialogComponentAdder {
    
    public boolean addToDialog(Container container, JPanel customPanel) {
        return addAboveButtonPanel(container, customPanel) ||
               addToMiddleArea(container, customPanel) ||
               addToBottomSafely(container, customPanel);
    }
    
    private boolean addAboveButtonPanel(Container container, JPanel customPanel) {
        Component pushTagsComponent = findPushTagsComponent(container);
        if (pushTagsComponent != null && pushTagsComponent.getParent() != null) {
            Container parent = pushTagsComponent.getParent();
            LayoutManager layout = parent.getLayout();

            if (layout instanceof FlowLayout || layout instanceof BoxLayout) {
                parent.add(customPanel);
                return true;
            } else if (layout instanceof BorderLayout borderLayout) {
                if (borderLayout.getLayoutComponent(BorderLayout.EAST) == null) {
                    parent.add(customPanel, BorderLayout.EAST);
                    return true;
                }
            }
        }

        Component buttonPanel = findButtonPanel(container);
        if (buttonPanel != null && buttonPanel.getParent() != null) {
            Container parent = buttonPanel.getParent();
            LayoutManager layout = parent.getLayout();

            if (layout instanceof BorderLayout borderLayout) {
                Component center = borderLayout.getLayoutComponent(BorderLayout.CENTER);
                if (center instanceof Container) {
                    return addToContainerBottom((Container) center, customPanel);
                }
            } else if (layout instanceof BoxLayout) {
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

    private boolean addToMiddleArea(Container container, JPanel customPanel) {
        if (container.getLayout() instanceof BorderLayout layout) {
            Component center = layout.getLayoutComponent(BorderLayout.CENTER);
            if (center instanceof Container) {
                return addToContainerBottom((Container) center, customPanel);
            }
        }

        for (Component component : container.getComponents()) {
            if (component instanceof JPanel panel) {
                if (panel.getComponentCount() > 0 && !isButtonPanel(panel)) {
                    return addToContainerBottom(panel, customPanel);
                }
            }
        }
        return false;
    }

    private boolean addToBottomSafely(Container container, JPanel customPanel) {
        if (container.getLayout() instanceof BorderLayout layout) {
            if (layout.getLayoutComponent(BorderLayout.SOUTH) == null) {
                container.add(customPanel, BorderLayout.SOUTH);
                return true;
            }
        }
        return false;
    }

    private boolean addToContainerBottom(Container container, JPanel customPanel) {
        LayoutManager layout = container.getLayout();

        if (layout instanceof BorderLayout borderLayout) {
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
        if (!(component instanceof Container container))
            return false;
        int buttonCount = 0;

        for (Component child : container.getComponents()) {
            if (child instanceof JButton) {
                buttonCount++;
            }
        }

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
}