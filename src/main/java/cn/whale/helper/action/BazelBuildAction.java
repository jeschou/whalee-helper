package cn.whale.helper.action;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.IDEUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class BazelBuildAction extends Action0 {

    static Notifier notifier = Notifier.getInstance("whgo_helper bazel");

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        Presentation presentation = e.getPresentation();
        VirtualFile moduleRoot = IDEUtils.getModuleRoot(virtualFile);
        if (moduleRoot == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setText(String.format("bazel build //%s/%s", moduleRoot.getParent().getName(), moduleRoot.getName()));
        presentation.setEnabledAndVisible(true);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null || project.getBasePath() == null) {
            return;
        }
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            return;
        }
        VirtualFile moduleRoot = IDEUtils.getModuleRoot(virtualFile);
        if (moduleRoot == null) {
            notifier.error(project, "invalid file path:" + virtualFile.getPath());
            return;
        }
        String cmd = String.format("bazel build //%s/%s", moduleRoot.getParent().getName(), moduleRoot.getName());
        IDEUtils.executeInTerminal(project, moduleRoot.getPath(), cmd);
    }

}
