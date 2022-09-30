package cn.whale.helper.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jediterm.terminal.TerminalOutputStream;
import com.jediterm.terminal.ui.TerminalPanel;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class IDEUtils {
    public static boolean isGoFile(VirtualFile vf) {
        return !vf.isDirectory() && "go".equals(vf.getExtension());
    }

    public static boolean isProtoFile(VirtualFile vf) {
        return !vf.isDirectory() && "proto".equals(vf.getExtension());
    }

    public static List<VirtualFile> collectChild(VirtualFile vdir, Predicate<VirtualFile> predicate) {
        List<VirtualFile> list = new ArrayList<>();
        VirtualFile[] childs = vdir.getChildren();
        if (childs == null) {
            return list;
        }
        for (VirtualFile vf : childs) {
            if (predicate.test(vf)) {
                list.add(vf);
            }
        }
        return list;
    }

    public static List<VirtualFile> collectChildDeep(VirtualFile vdir, Predicate<VirtualFile> predicate) {
        List<VirtualFile> list = new ArrayList<>();
        VirtualFile[] childs = vdir.getChildren();
        if (childs == null) {
            return list;
        }
        for (VirtualFile vf : childs) {
            if (predicate.test(vf)) {
                list.add(vf);
            } else if (vf.isDirectory()) {
                list.addAll(collectChild(vf, predicate));
            }
        }
        return list;
    }

    public static File toFile(VirtualFile vf) {
        return new File(vf.getPath());
    }

    public static VirtualFile getModuleRoot(VirtualFile vf) {
        while (vf != null && vf.findChild("main.go") == null) {
            vf = vf.getParent();
        }
        return vf;
    }

    public static VirtualFile getGoMod(VirtualFile vf) {
        while (vf != null) {
            VirtualFile gomf = vf.findChild("go.mod");
            if (gomf != null) {
                return gomf;
            }
            vf = vf.getParent();
        }
        return null;
    }

    public static void executeInTerminal(Project project, String workingDir, String cmd) {
        ToolWindow terminal = ToolWindowManager.getInstance(project).getToolWindow("Terminal");
        JComponent root = terminal.getComponent();
        terminal.show(() -> {
            JComponent panel = root;
            while (!(panel instanceof TerminalPanel)) {
                panel = (JComponent) panel.getComponent(0);
            }
            TerminalPanel terminalPanel = (TerminalPanel) panel;
            SwingUtilities.invokeLater(() -> {
                TerminalOutputStream terminalOutputStream = terminalPanel.getTerminalOutputStream();
                if (terminalOutputStream != null) {
                    execute(terminalOutputStream, workingDir, cmd);
                    return;
                }

                new Thread(() -> {
                    for (int i = 0; i < 10; i++) {
                        // try wait initialization
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        TerminalOutputStream terminalOutputStream2 = terminalPanel.getTerminalOutputStream();
                        if (terminalOutputStream2 != null) {
                            execute(terminalOutputStream2, workingDir, cmd);
                            return;
                        }
                    }
                }).start();
            });
        });
    }

    private static void execute(TerminalOutputStream terminalOutputStream, String workingDir, String cmd) {
        if (!"".equals(workingDir) && !".".equals(workingDir)) {
            terminalOutputStream.sendString("cd " + workingDir + "\n");
        }
        terminalOutputStream.sendString(cmd + "\n");
    }

    public static void createVirtualFile(Project project, VirtualFile dir, String fileName, String content) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile vf = dir.createChildData(project, fileName);
                vf.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

    public static void createAndOpenVirtualFile(Project project, VirtualFile dir, String fileName, byte[] data) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile vf = dir.createChildData(project, fileName);
                vf.setBinaryContent(data);
                FileEditorManager.getInstance(project).openFile(vf, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

}