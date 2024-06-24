package cn.whale.helper.action.experimental;

import cn.whale.helper.action.Action0;
import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.I18nUtil;
import cn.whale.helper.utils.IconFontUtil;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class TsxShowVarValueAction extends Action0 {

    static Notifier notifier = Notifier.getInstance("whgo_helper tsx");

    Var var;

    @Override
    public void update(@NotNull AnActionEvent e) {
        var = null;
        super.update(e);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        Presentation presentation = e.getPresentation();
        if (virtualFile == null || !"tsx".equals(virtualFile.getExtension())) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        try {
            var = getTsxVar(e);
            if (var == null) {
                presentation.setEnabledAndVisible(false);
                return;
            }
            if (Utils.isNotEmpty(var.WhaleAliviaIcon)) {
                presentation.setText(var.WhaleAliviaIcon);
                presentation.setIcon(IconFontUtil.getSvgIcon(var.WhaleAliviaIcon));
            } else if (Utils.isNotEmpty(var.iconparkIcon)) {
                presentation.setText(var.iconparkIcon);
                presentation.setIcon(IconFontUtil.getSvgIcon(var.iconparkIcon));
            } else if (Utils.isNotEmpty(var.i18n)) {
                presentation.setText(StringUtils.defaultString(var.i18nTranslated, var.i18n));
                presentation.setIcon(null);
            }
        } catch (Exception ex) {
            presentation.setEnabledAndVisible(false);
            throw new RuntimeException(ex);
        }
        e.getPresentation().setEnabledAndVisible(true);

    }

    private Var getTsxVar(AnActionEvent e) {


        @Nullable Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return null;
        }
        @NotNull Document doc = editor.getDocument();
        @NotNull LogicalPosition caretPos = editor.getCaretModel().getCurrentCaret().getLogicalPosition();
        int lineStartOffset = doc.getLineStartOffset(caretPos.line);
        int lineEndOffset = doc.getLineEndOffset(caretPos.line);
        String text = doc.getText(new TextRange(lineStartOffset, lineEndOffset));
        text = text.replace("\"", "'");

        Var var = new Var();
        int idx = text.lastIndexOf("<WhaleAliviaIcon", caretPos.column);
        if (idx != -1) {
            var.WhaleAliviaIcon = StringUtils.substringBetween(text.substring(idx).replace(" ", ""), "name='", "'");
        }
        idx = text.lastIndexOf("<iconpark-icon", caretPos.column);
        if (idx != -1) {
            var.iconparkIcon = StringUtils.substringBetween(text.substring(idx).replace(" ", ""), "name='", "'");
            var.iconparkIcon = var.iconparkIcon.replace("alivia-", "");
        }

        idx = text.lastIndexOf("t(", caretPos.column);
        if (idx != -1) {
            var.i18n = StringUtils.substringBetween(text.substring(idx).replace(" ", ""), "t('", "'");
            try {
                var.i18nTranslated = I18nUtil.getTranslation(var.i18n);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        if (Utils.isAllEmpty(var.WhaleAliviaIcon, var.iconparkIcon, var.i18n)) {
            return null;
        }

        return var;
    }

    static class Var {

        String WhaleAliviaIcon;
        String iconparkIcon;
        String i18n;
        String i18nTranslated;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);

        if (project == null || virtualFile == null || var == null) {
            return;
        }
        if (Utils.isNotEmpty(var.WhaleAliviaIcon)) {

        } else if (Utils.isNotEmpty(var.iconparkIcon)) {

        } else if (Utils.isNotEmpty(var.i18n)) {
            if (var.i18nTranslated == null) {

            }
        }


        try {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
