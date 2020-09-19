package cn.whale.helper.action;

import cn.whale.helper.dao.GoDaoGenerator;
import cn.whale.helper.ui.SelectTableDialogWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class NewGormModelAction extends AnAction {

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
        SelectTableDialogWrapper dialog = new SelectTableDialogWrapper(project);
        if (!dialog.showAndGet()) {
            return;
        }

        new GoDaoGenerator(project, virtualFile, dialog.getDbConfig(), dialog.getFileName(), dialog.getDatabase(), dialog.getTableName(), dialog.getStructName(), dialog.getColumnData()).generate();

    }
}
