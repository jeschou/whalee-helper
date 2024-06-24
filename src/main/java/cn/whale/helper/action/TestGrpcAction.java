package cn.whale.helper.action;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.ProtoMeta;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class TestGrpcAction extends Action0 {

    static Notifier notifier = Notifier.getInstance("whgo_helper gbl");

    RpcSchema rpcSchema;

    @Override
    public void update(@NotNull AnActionEvent e) {
        rpcSchema = null;
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        Presentation presentation = e.getPresentation();
        if (virtualFile == null || !IDEUtils.isProtoFile(virtualFile)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        try {
            rpcSchema = getRpcElement(e);
            if (rpcSchema == null) {
                presentation.setEnabledAndVisible(false);
                return;
            }
        } catch (Exception ex) {
            presentation.setEnabledAndVisible(false);
            throw new RuntimeException(ex);
        }
        e.getPresentation().setText("test " + rpcSchema.rpcElement.getName());
        e.getPresentation().setEnabledAndVisible(true);

    }

    private RpcSchema getRpcElement(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);


        @Nullable Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }
        // start from 0
        int caretLine = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line + 1;


        File projectRoot = IDEUtils.toFile(project.getBaseDir());
        File protoFile = IDEUtils.toFile(virtualFile);
        ProtoMeta pm = new ProtoMeta(projectRoot.getAbsolutePath());
        ProtoFileElement pf = pm.parse(protoFile.getAbsolutePath().substring(projectRoot.getAbsolutePath().length() + 1));
        ServiceElement serviceElement = null;
        RpcElement rpcElement = null;
        for (ServiceElement se : pf.getServices()) {
            if (se.getLocation().getLine() > caretLine) {
                // out of range
                continue;
            }
            for (RpcElement re : se.getRpcs()) {
                if (re.getLocation().getLine() == caretLine) {
                    serviceElement = se;
                    rpcElement = re;
                    break;
                }
            }
            if (rpcElement != null) {
                break;
            }
        }
        if (rpcElement == null) {
            return null;
        }
        RpcSchema rs = new RpcSchema();
        rs.protoMeta = pm;
        rs.protoFileElement = pf;
        rs.serviceElement = serviceElement;
        rs.rpcElement = rpcElement;
        return rs;

    }

    static class RpcSchema {

        ProtoMeta protoMeta;
        ProtoFileElement protoFileElement;
        ServiceElement serviceElement;
        RpcElement rpcElement;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);

        if (project == null || virtualFile == null || rpcSchema == null) {
            return;
        }
        String fullName = rpcSchema.protoFileElement.getPackageName() + "." + rpcSchema.serviceElement.getName() + "/" + rpcSchema.rpcElement.getName();
        String reqType = rpcSchema.rpcElement.getRequestType();

        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(StringUtils.defaultString(rpcSchema.rpcElement.getDocumentation())).append("\n");
        sb.append("GRPC 127.0.0.1:50051/").append(fullName).append("\n");
        sb.append("lang: zh-CN\n"); // 暗示用户, 可以通过 header 指定 语言环境
        sb.append("\n");
        try {
            sb.append(Utils.mapper.writeValueAsString(rpcSchema.protoMeta.toJsonMap(rpcSchema.protoMeta.typeMaps.get(reqType))));
            sb.append("\n");
            String testFileName = virtualFile.getName() + "_test.http";
            String content = sb.toString();
            @Nullable VirtualFile testFile = virtualFile.getParent().findChild(testFileName);
            int line = 1;
            if (testFile != null) {
                // append
                String data = new String(testFile.contentsToByteArray(), StandardCharsets.UTF_8);
                line = data.split("\n").length + 3;
                content = data + "\n\n" + sb;
            }
            IDEUtils.createAndOpenVirtualFile(project, virtualFile.getParent(), testFileName, content.getBytes(StandardCharsets.UTF_8), line, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
