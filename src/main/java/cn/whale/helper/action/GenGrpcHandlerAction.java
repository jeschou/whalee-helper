package cn.whale.helper.action;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.GoUtils;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.ProtoMeta;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenGrpcHandlerAction extends AnAction {

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
        e.getPresentation().setText("generate GRPC Handler for " + rpcSchema.serviceElement.getName());
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
        for (ServiceElement se : pf.getServices()) {
            if (se.getLocation().getLine() == caretLine) {
                serviceElement = se;
                break;
            }
        }
        if (serviceElement == null) {
            return null;
        }
        RpcSchema rs = new RpcSchema();
        rs.protoMeta = pm;
        rs.protoFileElement = pf;
        rs.serviceElement = serviceElement;
        return rs;

    }

    static class RpcSchema {

        ProtoMeta protoMeta;
        ProtoFileElement protoFileElement;
        ServiceElement serviceElement;

        String[] importAlias;

        Map<String, String[]> externalImport = new HashMap<>();

        void init() {
            for (RpcElement re : serviceElement.getRpcs()) {
                computeExternal(re.getRequestType());
                computeExternal(re.getResponseType());
            }
        }

        void computeExternal(String types) {
            if (types.equals("google.protobuf.Empty")) {
                externalImport.put(types, new String[]{"google.golang.org/protobuf/types/known/emptypb"});
            } else if (types.equals("oauth.proto.Token")) {
                // special treat
                computeExternal("whale.oauth.proto.Token");
                externalImport.put(types, externalImport.get("whale.oauth.proto.Token"));
            } else if (types.contains(".") && !externalImport.containsKey(types)) {
                ProtoFileElement pfile = protoMeta.elementFileMaps.get(types);
                if (pfile != null) {
                    OptionElement opt = pfile.getOptions().stream().filter(o -> o.getName().equals("go_package")).findFirst().get();
                    externalImport.put(types, opt.getValue().toString().split(";"));
                } else {
                    throw new RuntimeException(types);
                }

            }
        }

        List<String[]> getGoImport() {
            init();
            List<String[]> imps = new ArrayList<>();
            imps.add(new String[]{"context"});
            imps.add(getImportAlias());
            Set<String> set = new HashSet<>();
            externalImport.forEach((k, v) -> {
                if (set.add(v[0])) {
                    imps.add(v);
                }
            });
            GoUtils.sortGoImportWithAlias(imps);
            return imps;
        }

        String getGoType(String protoType) {
            String[] imps = externalImport.get(protoType);
            if (imps != null) {
                if (imps.length == 1) {
                    return Utils.substringAfterLast(imps[0], "/") + "." + Utils.substringAfterLast(protoType, ".");
                }
                return imps[1] + "." + Utils.substringAfterLast(protoType, ".");
            }

            return getImportAlias()[1] + "." + protoType;
        }

        String[] getImportAlias() {
            if (importAlias == null) {
                for (OptionElement ele : protoFileElement.getOptions()) {
                    if (ele.getName().equals("go_package")) {
                        importAlias = ele.getValue().toString().split(";");
                    }
                }
            }
            return importAlias;
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);

        if (project == null || virtualFile == null || rpcSchema == null) {
            return;
        }
        FileChooserDescriptor folderFd = FileChooserDescriptorFactory.createSingleFolderDescriptor();

        @NotNull PathChooserDialog fc = FileChooserFactory.getInstance().createPathChooser(folderFd, project, null);

        String serviceName = rpcSchema.serviceElement.getName();
        serviceName = Utils.substringBeforeLast(serviceName, "Service").toLowerCase();
        String fileName = serviceName + "-handler.go";
        StringBuilder sb = new StringBuilder();
        fc.choose(null, (f) -> {
            if (f != null && f.size() > 0) {
                List<String[]> imps = rpcSchema.getGoImport();

                try {
                    String package0 = GoUtils.getGoPackage(f.get(0));
                    sb.append("package ").append(package0).append("\n\n");

                    sb.append("import (\n");
                    for (String[] imp : imps) {
                        if (imp.length == 1) {
                            sb.append("\t\"").append(imp[0]).append("\"\n");
                        } else {
                            sb.append("\t").append(imp[1]).append(" \"").append(imp[0]).append("\"\n");
                        }
                    }
                    sb.append(")\n\n");
                    String handlerName = rpcSchema.serviceElement.getName() + "Handler";
                    sb.append("type ").append(handlerName).append(" struct {\n");
                    sb.append("\t").append(rpcSchema.getImportAlias()[1]).append(".").append(rpcSchema.serviceElement.getName()).append("Server").append("\n");
                    sb.append("}\n\n");

                    for (RpcElement re : rpcSchema.serviceElement.getRpcs()) {
                        if (!StringUtils.isBlank(re.getDocumentation())) {
                            // comment maybe multi line
                            String comment = re.getDocumentation().trim().replace("\n", "\n// ");
                            sb.append("// ").append(comment).append("\n");
                        }
                        sb.append("func (h *").append(handlerName).append(") ").append(re.getName()).append("(ctx context.Context, req *").append(rpcSchema.getGoType(re.getRequestType())).append(") ");
                        sb.append("(").append("*").append(rpcSchema.getGoType(re.getResponseType())).append(", error) {\n");
                        sb.append("\tresp := &").append(rpcSchema.getGoType(re.getResponseType())).append("{}\n");
                        sb.append("\treturn resp, nil\n");
                        sb.append("}\n\n");
                    }
                    IDEUtils.createAndOpenVirtualFile(project, f.get(0), fileName, sb.toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }
}
