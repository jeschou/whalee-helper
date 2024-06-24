package cn.whale.helper.action;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class SwagInitAction extends Action0 {

    static Notifier notifier = Notifier.getInstance("whgo_helper swag");

    static String[] activeSuffix = new String[]{"api-server", "-bff", "-agi"};

    static String relative_parent = "../";

    static {
        if (Utils.isWindows()) {
            relative_parent = "..\\";
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        Presentation presentation = e.getPresentation();

        VirtualFile moduleRoot = IDEUtils.getModuleRoot(virtualFile);
        if (moduleRoot == null || !StringUtils.endsWithAny(moduleRoot.getName(), activeSuffix)) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setText("swag init (" + moduleRoot.getName() + ")");
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
        String cmd = "swag init";
        if (virtualFile.getPath().contains("whale-bapi-server")) {
            cmd = "swag init --exclude=./controllers/jingyingbao --output=./docs";
        } else if (virtualFile.getPath().contains("whale-agi")) {
            cmd = "swag init --pd";
        }
        VirtualFile moduleRoot = IDEUtils.getModuleRoot(virtualFile);
        if (moduleRoot == null) {
            notifier.error(project, "invalid file path:" + virtualFile.getPath());
            return;
        }

        IDEUtils.executeInTerminal(project, moduleRoot.getPath(), cmd);
    }
}
