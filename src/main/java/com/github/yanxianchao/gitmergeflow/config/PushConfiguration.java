package com.github.yanxianchao.gitmergeflow.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 推送配置领域模型
 * 封装项目级别的推送设置
 */
public final class PushConfiguration {
    
    private final boolean enableAutoPush;
    private final String targetBranch;
    
    private PushConfiguration(boolean enableAutoPush, @Nullable String targetBranch) {
        this.enableAutoPush = enableAutoPush;
        this.targetBranch = targetBranch != null ? targetBranch.trim() : "";
    }
    
    public static PushConfiguration disabled() {
        return new PushConfiguration(false, null);
    }
    
    public static PushConfiguration enabled(@NotNull String targetBranch) {
        if (targetBranch.trim().isEmpty()) {
            throw new IllegalArgumentException("Target branch cannot be empty");
        }
        return new PushConfiguration(true, targetBranch);
    }
    
    public boolean isEnabled() {
        return enableAutoPush;
    }
    
    @NotNull
    public String getTargetBranch() {
        return targetBranch;
    }
    
    public boolean hasValidTargetBranch() {
        return enableAutoPush && !targetBranch.isEmpty();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PushConfiguration that = (PushConfiguration) obj;
        return enableAutoPush == that.enableAutoPush && 
               targetBranch.equals(that.targetBranch);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(enableAutoPush, targetBranch);
    }
    
    @Override
    public String toString() {
        return String.format("PushConfiguration{enabled=%s, targetBranch='%s'}", 
                           enableAutoPush, targetBranch);
    }
}