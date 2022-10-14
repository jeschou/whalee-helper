package cn.whale.helper.action.experimental;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class GoBuildAction extends AnAction {

    static Notifier notifier = Notifier.getInstance("whgo_helper gbl");
    static boolean enabled = false;


    static {

        try {
            enabled = Utils.readFirstLine(new File(System.getProperty("user.home"), ".zshrc"), line -> line.startsWith("alias gbl=")) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        Presentation presentation = e.getPresentation();
        if (!enabled) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        VirtualFile moduleRoot = IDEUtils.getModuleRoot(virtualFile);
        if (moduleRoot == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setText("go build " + virtualFile.getParent().getName());
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
        VirtualFile mainDir = IDEUtils.getModuleRoot(virtualFile);
        String cmd = "go build -o /dev/null *.go";

        IDEUtils.executeInTerminal(project, mainDir.getPath(), cmd);
    }
}
