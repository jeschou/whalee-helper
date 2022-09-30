package cn.whale.helper.action.service;

import cn.whale.helper.template.SimpleTemplateRender;
import cn.whale.helper.ui.Notifier;
import cn.whale.helper.ui.SimplePopupInput;
import cn.whale.helper.utils.GoUtils;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NewServiceAction extends AnAction {

    static Notifier notifier = Notifier.getInstance("whgo_helper service");

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setEnabledAndVisible(virtualFile.isDirectory());
    }


    @Override
    public void actionPerformed(AnActionEvent event) {
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (virtualFile == null || project == null) {
            return;
        }
        new SimplePopupInput("Input service name", "Name", "[\\w\\-]+").showPopup(project, (serviceName) -> {
            if (virtualFile.findChild(serviceName) != null) {
                notifier.error(project, serviceName + " already exists");
                return;
            }
            Map<String, String> args = new HashMap<>();
            VirtualFile projectRoot = project.getBaseDir();
            String projectModule = GoUtils.getModuleFromGoMod(IDEUtils.getGoMod(projectRoot));
            args.put("projectModule", projectModule);
            String shortName = Utils.substringAfter(serviceName, "whale-");
            args.put("serviceModule", serviceName);
            args.put("serviceName", serviceName);
            args.put("shortName", shortName);
            args.put("ShortName", Utils.toTitle(shortName));
            String cleanName = shortName.replace("-", "");
            args.put("cleanName",cleanName);
            args.put("CleanName",Utils.toTitle(cleanName));



            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    VirtualFile serviceRoot = virtualFile.createChildDirectory(project, serviceName);
                    args.put("servicePath", serviceRoot.getPath().substring(projectRoot.getPath().length()+1));
                    VirtualFile grpc = serviceRoot.createChildDirectory(project, "grpc");
                    VirtualFile service = serviceRoot.createChildDirectory(project, "service");


                    if ("whgo".equals(projectModule)) {
                        args.put("whgo", "whgo");
                        String rootRelative = serviceRoot.getPath().substring(projectRoot.getPath().length() + 1);
                        args.put("protoPackage", "whgo/proto/" + shortName);
                        String whgoRelative = StringUtils.repeat("../", rootRelative.split("/").length);
                        args.put("whgoRelativePath", whgoRelative);
                        args.put("libraryRelativePath", whgoRelative + "library");
                        // create proto in /whgo/proto/xx
                        @NotNull VirtualFile protoDir = projectRoot.findChild("proto").createChildDirectory(project, shortName);
                        createVF(project, protoDir, shortName + ".proto", "proto.tpl", args);       //  proto/xx.proto
                    } else {
                        args.put("protoPackage", "whgo/proto/" + shortName);
                        VirtualFile proto = serviceRoot.createChildDirectory(project, "proto");
                        createVF(project, proto, shortName + ".proto", "proto.tpl", args);       //  proto/xx.proto
                        createVF(project, proto, "go.mod", "proto.go.mod.tpl", args);             //  proto/go.mod
                    }
                    createVF(project, grpc, shortName + "-handler.go", "handler.tpl", args); //  grpc/xx-handler.go

                    createVF(project, serviceRoot, "main.go", "main.go.tpl", args);           //  main.go
                    createVF(project, serviceRoot, "go.mod", "go.mod.tpl", args);             //  go.mod
                    VirtualFile readme = createVF(project, serviceRoot, "README.md", "readme.tpl", args);          //  README.md
                    FileEditorManager.getInstance(project).openFile(readme, true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    VirtualFile createVF(Project project, VirtualFile dir, String fileName, String tpl, Map<String, String> args) throws IOException {
        SimpleTemplateRender mainRender = new SimpleTemplateRender();
        mainRender.loadTemplate(getClass().getResourceAsStream(tpl));
        mainRender.render(args);
        VirtualFile vf = dir.createChildData(project, fileName);
        vf.setBinaryContent(mainRender.getResult().getBytes(StandardCharsets.UTF_8));
        return vf;
    }
}
