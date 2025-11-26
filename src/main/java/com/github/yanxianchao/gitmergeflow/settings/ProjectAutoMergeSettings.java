package com.github.yanxianchao.gitmergeflow.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 项目级别的自动合并设置类
 * 用于存储和管理每个项目的合并配置信息
 */
@Service(Service.Level.PROJECT)
@State(
        name = "com.github.yanxianchao.gitmergeflow.settings.ProjectAutoMergeSettings",
        storages = @Storage("ProjectAutoMergeSettings.xml")
)
public final class ProjectAutoMergeSettings implements PersistentStateComponent<ProjectAutoMergeSettings> {

    /**
     * 上次使用的目标分支名称
     */
    public String lastUsedBranch = "main";
    

    /**
     * 获取指定项目的设置实例
     *
     * @param project IDEA项目实例
     * @return 项目设置实例
     */
    public static ProjectAutoMergeSettings getInstance(Project project) {
        return project.getService(ProjectAutoMergeSettings.class);
    }

    @Nullable
    @Override
    public ProjectAutoMergeSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ProjectAutoMergeSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 获取上次使用的分支名称
     *
     * @return 分支名称
     */
    public String getLastUsedBranch() {
        return lastUsedBranch;
    }

    /**
     * 设置上次使用的分支名称
     *
     * @param lastUsedBranch 分支名称
     */
    public void setLastUsedBranch(String lastUsedBranch) {
        this.lastUsedBranch = lastUsedBranch;
    }

    
}