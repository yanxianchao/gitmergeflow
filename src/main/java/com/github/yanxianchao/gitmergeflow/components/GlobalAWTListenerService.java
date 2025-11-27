package com.github.yanxianchao.gitmergeflow.components;

import com.github.yanxianchao.gitmergeflow.push.GMFApplication;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;

@Service
public final class GlobalAWTListenerService {

    public GlobalAWTListenerService() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                GMFApplication.run();
                System.out.println("全局AWT事件监听器已在应用级别初始化");
            } catch (Exception e) {
                System.err.println("初始化全局AWT事件监听器失败");
            }
        });
    }
}