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

    static Notifier notifier = Notifier.getInstance("whgo_helper protoc");

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
                if (cmdDir.getAbsolutePath().contains("/proto/")) {
                    while (!cmdDir.getName().equals("proto")) {
                        cmdDir = cmdDir.getParentFile();
                    }
                }
                arg = protoFile.getAbsolutePath().substring(cmdDir.getAbsolutePath().length() + 1);

                outPathRelative = relative_parent;
                for (File f = cmdDir; isSubOrEqual(projectRoot, f) && !firstDir.equals(f.getName()); f = f.getParentFile()) {
                    outPathRelative += relative_parent;
                }
            }


            System.out.println("generate proto: " + relativePath);

            String cmd = "protoc %s -I. -I../  -I../../  -I../../../  -I../../../../  --go_out=plugins=grpc:%s";

            if (Utils.isWindows()) {
                cmd = "protoc.exe %s -I. -I..\\  -I..\\..\\  -I..\\..\\..\\  -I..\\..\\..\\..\\  --go_out=plugins=grpc:%s";
            }

            cmd = String.format(cmd, arg, outPathRelative);

            String[] cmdOutput = Utils.executeShell(cmdDir, cmd);

            if (Utils.isNotEmpty(cmdOutput[0])) {
                //notifier.info(project, normalContent);
            }
            if (Utils.isNotEmpty(cmdOutput[1])) {
                notifier.error(project, String.format("working dir:%s\ncmd:%s\n%s\n%s", cmdDir, cmd, cmdOutput[1], suggestMsg(cmdOutput[1])));
            } else {
                notifier.info(project, String.format("working dir:%s\ncmd:%s\ncompile %s success", cmdDir, cmd, arg));
            }

        } catch (IOException e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
        }
    }

    private String suggestMsg(String errMsg) {
        if (Utils.isWindows()) {
            return "";
        }
        String sh = Utils.getLoginShell();
        sh = Utils.substringAfterLast(sh, "/");
        if (Utils.isEmpty(sh)) {
            return "";
        }
        String command = "";
        if (errMsg.contains("command not found: protoc")) {
            command = "protoc";
        } else if (errMsg.contains("protoc-gen-go: program not found")) {
            command = "protoc-gen-go";
        }
        if (Utils.isEmpty(command)) {
            return "";
        }
        return "You must config PATH in ~/." + sh + "rc, make sure " + command + " can be found in this PATH";
    }
}
