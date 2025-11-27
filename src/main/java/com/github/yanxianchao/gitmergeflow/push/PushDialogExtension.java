package com.github.yanxianchao.gitmergeflow.push;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

public class PushDialogExtension implements ProjectActivity {

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        GMFApplication.run();
        System.out.println("推送对话框扩展：为项目初始化活动 - " + project.getName());
        return Unit.INSTANCE;
    }
}