package cn.whale.helper.action;

import cn.whale.helper.bazel.AbstractBazelGenerator;
import cn.whale.helper.bazel.BazelUtils;
import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BazelUpdateAction extends Action0 {

    static Notifier notifier = Notifier.getInstance("whgo_helper bazel");

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            return;
        }
        if (!virtualFile.isDirectory()) {
            virtualFile = virtualFile.getParent();
        }
        List<VirtualFile> goFiles = IDEUtils.collectChild(virtualFile, IDEUtils::isGoFile);

        e.getPresentation().setEnabledAndVisible(goFiles.size() > 0);

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

        if (!virtualFile.isDirectory()) {
            virtualFile = virtualFile.getParent();
        }
        try {
            AbstractBazelGenerator.collectStdLib(project);
            BazelUtils.updateBUILDbazel(project, virtualFile);
        } catch (Exception e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
        }
    }

}
