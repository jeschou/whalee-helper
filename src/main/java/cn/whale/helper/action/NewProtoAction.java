package cn.whale.helper.action;

import cn.whale.helper.template.SimpleTemplateRender;
import cn.whale.helper.ui.SimplePopupInput;
import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewProtoAction extends AnAction {

    static Notifier notifier = Notifier.getInstance("whgo_helper proto");

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
        VirtualFile vDir = virtualFile.isDirectory() ? virtualFile : virtualFile.getParent();
        VirtualFile gomodFile = IDEUtils.getGoMod(virtualFile);
        if (gomodFile == null) {
            return;
        }
        new SimplePopupInput("Input proto file name", "Name", "[\\w_]+").showPopup(project, (filename) -> {
            if (!filename.endsWith(".proto")) {
                filename += ".proto";
            }
            try {
                SimpleTemplateRender template = new SimpleTemplateRender();
                template.loadTemplate(this.getClass().getResourceAsStream("proto.txt"));
                Map<String, String> args = new HashMap<>();
                args.put("serviceName", vDir.getName());
                args.put("ServiceName", Utils.toTitle(vDir.getName()));
                List<String> lines = Utils.readLines(gomodFile.getInputStream());
                String module = null;
                for (String line : lines) {
                    if (line.startsWith("module")) {
                        module = Utils.substringAfter(line, "module").trim();
                        break;
                    }
                }
                args.put("module", module);
                String relativePath = vDir.getPath().substring(gomodFile.getParent().getPath().length() + 1);
                args.put("relativePath", relativePath);
                template.render(args);
                String content = template.getResult();
                IDEUtils.createAndOpenVirtualFile(project, vDir, filename, content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                notifier.error(project, Utils.getStackTrace(e));
                e.printStackTrace();
            }
        });

    }
}
