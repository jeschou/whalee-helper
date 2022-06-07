package cn.whale.helper.action.experimental;

import cn.whale.helper.utils.IDEUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class TransGrpcAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        Presentation presentation = e.getPresentation();
        if (1 == 1) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        VirtualFile moduleRoot = IDEUtils.getModuleRoot(virtualFile);
        if (moduleRoot == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        if (virtualFile.getName().contains("-handler.go")) {
            presentation.setEnabledAndVisible(true);
            return;
        }
        presentation.setEnabledAndVisible(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(virtualFile.getInputStream(), "UTF-8"))) {
            String line = null;
            Arg resp = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("func ")) {
                    resp = null;
                    FuncDec fd = parseFunc(line);
                    if (fd == null || Character.isLowerCase(fd.name.charAt(0)) || fd.inputArgs.size() != 3 || fd.outputArgs.size() != 1 || !fd.outputArgs.get(0).type.equals("error")) {
                        pw.println(line);
                        continue;
                    }
                    resp = fd.inputArgs.remove(2);
                    fd.outputArgs.add(0, resp);
                    pw.println(fd + " {");
                    pw.println("\t" + resp.name + " := &" + resp.type.substring(1) + "{}");
                } else if (line.trim().startsWith("//")) {
                    pw.println(line);
                } else if (resp != null && line.contains("return")) {
                    if (line.trim().equals("return")) {
                        pw.println(line);
                        continue;
                    }
                    line = line.replace("return", "return " + resp.name + ",");
                    pw.println(line);
                } else {
                    pw.println(line);
                }
            }
            virtualFile.setBinaryContent(sw.toString().getBytes("UTF-8"));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    boolean containWord(String line, String word) {
        return Arrays.asList(StringUtils.split(line.trim(), "().*,& =")).contains(word);
    }

    class FuncDec {
        String r;
        String rType;
        String name;
        ArrayList<Arg> inputArgs;
        ArrayList<Arg> outputArgs;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("func (").append(r).append(" ").append(rType).append(")");
            stringBuilder.append(" ").append(name).append("(");
            for (Arg a : inputArgs) {
                stringBuilder.append(a.name).append(" ").append(a.type).append(", ");
            }
            if (inputArgs.size() > 0) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            stringBuilder.append(") (");
            for (Arg a : outputArgs) {
                stringBuilder.append(a.type).append(", ");
            }
            if (outputArgs.size() > 0) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            stringBuilder.append(")");


            return stringBuilder.toString();
        }
    }


    class Arg {
        String name;
        String type;

        Arg(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    FuncDec parseFunc(String line) {
        FuncDec fd = new FuncDec();
        String[] segs = StringUtils.split(line, ",() {");
        if (segs.length < 11) {
            return null;
        }
        fd.r = segs[1];
        fd.rType = segs[2];
        fd.name = segs[3];
        fd.inputArgs = new ArrayList<>(Arrays.asList(new Arg(segs[4], segs[5]), new Arg(segs[6], segs[7]), new Arg(segs[8], segs[9])));
        if (segs.length == 11) {
            fd.outputArgs = new ArrayList<>(Arrays.asList(new Arg("", segs[10])));
        } else {
            fd.outputArgs = new ArrayList<>(Arrays.asList(new Arg(segs[10], segs[11])));
        }
        return fd;
    }
}
