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
    private final boolean deployEnabled;
    private final String goodsId;
    private final String appName;

    private PushConfiguration(boolean enableAutoPush, @Nullable String targetBranch,
                              boolean deployEnabled, @Nullable String goodsId,
                              @Nullable String appName) {
        this.enableAutoPush = enableAutoPush;
        this.targetBranch = targetBranch != null ? targetBranch.trim() : "";
        this.deployEnabled = deployEnabled;
        this.goodsId = goodsId != null ? goodsId.trim() : "";
        this.appName = appName != null ? appName.trim() : "";
    }

    public static PushConfiguration disabled(String targetBranch) {
        return new PushConfiguration(false, targetBranch, false, "", "");
    }

    public static PushConfiguration enabled(@NotNull String targetBranch) {
        if (targetBranch.trim().isEmpty()) {
            throw new IllegalArgumentException("Target branch cannot be empty");
        }
        return new PushConfiguration(true, targetBranch, false, "", "");
    }

    public PushConfiguration withDeploy(boolean deployEnabled, @Nullable String goodsId) {
        return new PushConfiguration(this.enableAutoPush, this.targetBranch, deployEnabled, goodsId, this.appName);
    }

    public PushConfiguration withAppName(@Nullable String appName) {
        return new PushConfiguration(this.enableAutoPush, this.targetBranch, this.deployEnabled, this.goodsId, appName);
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

    public boolean isDeployEnabled() {
        return deployEnabled;
    }

    @NotNull
    public String getGoodsId() {
        return goodsId;
    }

    public boolean hasValidDeploy() {
        return deployEnabled && !goodsId.isEmpty();
    }

    @NotNull
    public String getAppName() {
        return appName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PushConfiguration that = (PushConfiguration) obj;
        return enableAutoPush == that.enableAutoPush
                && deployEnabled == that.deployEnabled
                && targetBranch.equals(that.targetBranch)
                && goodsId.equals(that.goodsId)
                && appName.equals(that.appName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(enableAutoPush, targetBranch, deployEnabled, goodsId, appName);
    }

    @Override
    public String toString() {
        return String.format("PushConfiguration{enabled=%s, targetBranch='%s', deployEnabled=%s, goodsId='%s', appName='%s'}",
                enableAutoPush, targetBranch, deployEnabled, goodsId, appName);
    }
}