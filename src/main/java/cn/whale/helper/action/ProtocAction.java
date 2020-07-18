package cn.whale.helper.action;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.ProtoUtil;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProtocAction extends AnAction {

    static Notifier notifier = new Notifier("whgo_helper protoc");

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

    /**
     * do this before call protoc
     *
     * @return
     */
    static String sourcePath() {

        File bashProfileFile = new File(System.getProperty("user.home"), ".bash_profile");
        if (!bashProfileFile.isFile()) {
            return "";
        }

        return "source " + bashProfileFile.getAbsolutePath() + " && ";
    }

    static boolean isSubOrEqual(File base, File f) {
        return f.toPath().startsWith(base.toPath());
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
        if (virtualFile.isDirectory()) {
            List<VirtualFile> list = getChildrenProtoFiles(virtualFile);
            if (list.size() == 0) {
                presentation.setEnabledAndVisible(false);
                return;
            }
            presentation.setEnabledAndVisible(true);
            presentation.setText("protoc all " + list.size() + " proto files");
            return;
        }

        boolean isProto = isProtoSourceFile(virtualFile);
        if (isProto) {
            presentation.setText("protoc " + virtualFile.getName());
        }
        presentation.setEnabledAndVisible(isProto);
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

        if (isProtoSourceFile(virtualFile)) {
            Editor editor = event.getData(PlatformDataKeys.EDITOR);
            if (editor != null) {
                FileDocumentManager.getInstance().saveDocument(editor.getDocument());
            }
            compileProto(project, virtualFile);
            RefreshQueue.getInstance().refresh(true, true, null, virtualFile.getParent());
        } else if (virtualFile.isDirectory()) {
            List<VirtualFile> list = getChildrenProtoFiles(virtualFile);
            for (VirtualFile vf : list) {
                compileProto(project, vf);
            }
            RefreshQueue.getInstance().refresh(true, true, null, virtualFile);
        }

    }

    void compileProto(Project project, VirtualFile virtualFile) {
        File projectRoot = Utils.getWhgoProjectRoot(project);
        File protoFile = new File(virtualFile.getPath());
        String relativePath = virtualFile.getPath().substring(projectRoot.getAbsolutePath().length());

        try {
            String[] goPackage = ProtoUtil.readGoPackage(protoFile);
            if (goPackage == null) {
                notifier.error(project, virtualFile.getName() + " has no go_package declare");
                return;
            }
            String arg = protoFile.getName();
            // 命令执行路径
            File cmdDir = protoFile.getParentFile();
            // 输出相对
            String outPathRelative = ".";
            if (!".".equals(goPackage[0])) {
                String firstDir = Utils.substringBefore(goPackage[0], "/");
                if (goPackage[0].startsWith("whgo/product/proto/source")) {
                    cmdDir = protoFile.getParentFile().getParentFile().getParentFile();
                }
                arg = protoFile.getAbsolutePath().substring(cmdDir.getAbsolutePath().length() + 1);

                outPathRelative = relative_parent;
                for (File f = cmdDir; isSubOrEqual(projectRoot, f) && !firstDir.equals(f.getName()); f = f.getParentFile()) {
                    outPathRelative += relative_parent;
                }
            }


            System.out.println("generate proto: " + relativePath);

            String cmd = "protoc %s -I/usr/local/include -I. -I../  -I../../  -I../../../  -I../../../../  --go_out=plugins=grpc:%s --micro_out=logtostderr=true:%s";

            if (Utils.isWindows()) {
                cmd = "protoc.exe %s -I. -I..\\  -I..\\..\\  -I..\\..\\..\\  -I..\\..\\..\\..\\  --go_out=plugins=grpc:%s --micro_out=logtostderr=true:%s";
            }

            cmd = String.format(cmd, arg, outPathRelative, outPathRelative);

            System.out.println("working dir: " + cmdDir);
            System.out.println("exec cmd: " + cmd);


            ProcessBuilder processBuilder = new ProcessBuilder();

            processBuilder.directory(cmdDir);
            if (Utils.isWindows()) {
                processBuilder.command("cmd.exe", "/C", cmd);
            } else {
                processBuilder.command("bash", "-c", sourcePath() + cmd);
            }
            Process process = processBuilder.start();
            String normalContent = Utils.readText(process.getInputStream());
            String errorContent = Utils.readText(process.getErrorStream());
            process.destroy();

            if (Utils.isNotEmpty(normalContent)) {
                //notifier.info(project, normalContent);
            }
            if (Utils.isNotEmpty(errorContent)) {
                notifier.error(project, errorContent);
            } else {
                notifier.info(project, "compile " + arg + " success");
            }

        } catch (IOException e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
        }
    }

}
