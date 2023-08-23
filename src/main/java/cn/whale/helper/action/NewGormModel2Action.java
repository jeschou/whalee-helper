package cn.whale.helper.action;

import cn.whale.helper.repo2.GoRepo2Generator;
import cn.whale.helper.ui.SelectTableDialogWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class NewGormModel2Action extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(true);
    }


    @Override
    public void actionPerformed(AnActionEvent event) {
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (virtualFile == null || project == null) {
            return;
        }
        RepoGenCtx ctx = new RepoGenCtx(virtualFile);
        ctx.gormVersion = 2;

        SelectTableDialogWrapper dialog = new SelectTableDialogWrapper(project, ctx);
        if (!dialog.showAndGet()) {
            return;
        }
        if (dialog.getDbConfig().port == 0) {
            return;
        }

        new GoRepo2Generator(project, virtualFile, dialog.getDbConfig(), dialog.getFileName(), dialog.getDatabase(), dialog.getTableName(), dialog.getStructName(), dialog.getColumnData()).generate();

    }
}
