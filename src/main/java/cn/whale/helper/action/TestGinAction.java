package cn.whale.helper.action;

import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.ConfigUtil;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestGinAction extends Action0 {

    static Notifier notifier = Notifier.getInstance("whgo_helper gbl");

    GinDoc ginDoc;

    @Override
    public void update(@NotNull AnActionEvent e) {
        ginDoc = null;
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        Presentation presentation = e.getPresentation();
        if (virtualFile == null || !IDEUtils.isGoFile(virtualFile)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        try {
            ginDoc = getGinDoc(e);
            if (ginDoc == null || !ginDoc.isValid()) {
                presentation.setEnabledAndVisible(false);
                return;
            }
        } catch (Exception ex) {
            presentation.setEnabledAndVisible(false);
            throw new RuntimeException(ex);
        }
        e.getPresentation().setText("test " + ginDoc.path);
        e.getPresentation().setEnabledAndVisible(true);

    }

    private String getLineText(Document doc, int lineIdx) {
        int startOffset = doc.getLineStartOffset(lineIdx);
        int endOffset = doc.getLineEndOffset(lineIdx);
        return doc.getText(new TextRange(startOffset, endOffset));
    }

    static class GinDoc {
        Map<String, String> docs = new HashMap<>();
        String path;
        boolean needAuthorization;
        String bodyDefine;

        boolean isValid() {
            return !StringUtils.isBlank(path) && !docs.isEmpty();
        }
    }

    private GinDoc getGinDoc(AnActionEvent e) {

        @Nullable Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }
        // start from 0
        int caretLine = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line;

        GinDoc docs = new GinDoc();

        @NotNull Document doc = editor.getDocument();
        for (; caretLine > 0; caretLine--) {
            String text = getLineText(doc, caretLine);
            if (text == null || "".equals(text) || "}".equals(text) || ")".equals(text)) {
                break;
            }
            text = text.replace('\t', ' ').trim();
            String[] segs = StringUtils.split(text, " ");
            if (segs.length > 2 && segs[1].startsWith("@")) {
                String tagName = StringUtils.trim(segs[1].substring(1));
                if (StringUtils.isEmpty(tagName)) {
                    continue;
                }
                String tagValue = StringUtils.join(Arrays.copyOfRange(segs, 2, segs.length), " ");
                docs.docs.put(tagName, tagValue);
                if ("Authorization".equals(segs[2])) {
                    docs.needAuthorization = true;
                }
                if ("Router".equals(tagName)) {
                    docs.path = segs[2];
                }
                if ("Param".equals(tagName) && StringUtils.startsWith(tagValue, "Request body")) {
                    docs.bodyDefine = segs[4];
                }
            }
        }
        return docs;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);

        if (project == null || virtualFile == null || ginDoc == null || !ginDoc.isValid()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("### " + StringUtils.defaultString(ginDoc.docs.get("Summary"), ginDoc.docs.get("Description")) + "\n");
        String method = StringUtils.upperCase(StringUtils.substringBetween(ginDoc.docs.get("Router"), "[", "]"));
        sb.append(method + " http://127.0.0.1:3005").append(ginDoc.path).append("\n");
        if (StringUtils.contains(ginDoc.docs.get("Accept"), "json")) {
            sb.append("Content-Type: application/json\n");
        }
        if (ginDoc.needAuthorization) {
            sb.append("authorization: xxx\n");
        }
        sb.append("\n");
        Map<String, Object> bodySample = new HashMap<>();
        if (StringUtils.isNoneBlank(ginDoc.bodyDefine)) {
            bodySample = buildBody(loadSwaggerJson(virtualFile), ginDoc.bodyDefine);
        }

        try {
            sb.append(Utils.mapper.writeValueAsString(bodySample));
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

    static Map<String, Object> loadSwaggerJson(VirtualFile virtualFile) {
        VirtualFile root = IDEUtils.getModuleRoot(virtualFile);
        if (root == null) {
            return null;
        }
        @Nullable VirtualFile docs = root.findChild("docs");
        if (docs == null || !docs.isDirectory()) {
            return null;
        }
        @Nullable VirtualFile jsonFile = docs.findChild("swagger.json");
        if (jsonFile == null || jsonFile.isDirectory()) {
            return null;
        }
        try (@NotNull InputStream is = jsonFile.getInputStream()) {
            return Utils.mapper.readValue(is, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Map<String, Object> getMap(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object v = map.get(key);
        if (!(v instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) v;
    }

    static Map<String, Object> buildBody(Map<String, Object> swagger, String typeName) {
        Map<String, Object> properties = getMap(getMap(getMap(swagger, "definitions"), typeName), "properties");
        if (properties == null) {
            return Collections.emptyMap();
        }
        ConfigUtil.loadDefaultValueCfg();
        Map<String, Object> map = new HashMap<>();
        properties.forEach((k, v) -> {
            if (v instanceof Map) {
                Map<String, Object> vMap = (Map<String, Object>) v;
                String pTypeName = toStrClean(vMap.get("type"));
                switch (pTypeName) {
                    case "integer":
                    case "string":
                    case "boolean":
                        map.put(k, ConfigUtil.getDefaultVal(pTypeName, k));
                        break;
                    case "array":
                        Map<String, Object> itemsMap = getMap(vMap, "items");
                        if (itemsMap != null) {
                            String arrayType = toStrClean(itemsMap.get("type"));
                            if (StringUtils.isNoneBlank(arrayType)) {
                                map.put(k, Collections.singletonList(ConfigUtil.getDefaultVal(arrayType, "")));
                            } else {
                                String ref = String.valueOf(itemsMap.get("$ref"));
                                String refTypeName = StringUtils.substringAfter(ref, "#/definitions/");
                                map.put(k, Collections.singletonList(buildBody(swagger, refTypeName)));
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        return map;
    }

    static String toStrClean(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }
}
