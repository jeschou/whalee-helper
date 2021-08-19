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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SwagInitAction extends AnAction {

    static Notifier notifier = new Notifier("whgo_helper swag");

    static String relative_parent = "../";

    static {
        if (Utils.isWindows()) {
            relative_parent = "..\\";
        }
    }

    static List<VirtualFile> getChildrenProtoFiles(VirtualFile dir) {
        List<VirtualFile> list = new ArrayList<>();
        VirtualFile[] childs = dir.getChildren();
        if (childs == null) {
            return list;
        }
        for (VirtualFile vf : childs) {
            if (isProtoSourceFile(vf)) {
                list.add(vf);
            }
        }
        return list;
    }

    static boolean isProtoSourceFile(VirtualFile vf) {
        if (vf == null) return false;
        return "proto".equalsIgnoreCase(vf.getExtension());
    }

    static boolean isSubOrEqual(File base, File f) {
        return f.toPath().startsWith(base.toPath());
    }

    static File createTempShell(String cmd) throws IOException {
        File f = File.createTempFile("whalee_helper_protoc", ".sh");
        Utils.writeFile(f, cmd);
        return f;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        Presentation presentation = e.getPresentation();

        if (virtualFile == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setEnabledAndVisible(virtualFile.getPath().contains("api-server/"));
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
        }
        VirtualFile moduleRoot = IDEUtils.getModuleRoot(virtualFile);
        if (moduleRoot == null) {
            notifier.error(project, "invalid file path:" + virtualFile.getPath());
            return;
        }

        IDEUtils.executeInTerminal(project, moduleRoot.getPath(), cmd);
    }
}
