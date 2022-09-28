package cn.whale.helper.ui;

import cn.whale.helper.utils.Utils;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class SimplePopupInput {

    String title = "";
    String regex = "";
    String placeholder = "";

    public SimplePopupInput(String title, String placeholder, String regex) {
        this.title = title;
        this.placeholder = placeholder;
        this.regex = regex;
    }

    boolean validate(String text) {
        if (Utils.isEmpty(regex)) {
            return !Utils.isEmpty(text);
        }
        return text.matches(regex);
    }

    public void showPopup(Project project, Consumer<String> onOk) {
        JPanel panel = new JPanel();

        panel.setLayout(new BorderLayout());
        EditorTextField textField = new EditorTextField();
        textField.setOneLineMode(true);
        textField.setPlaceholder(placeholder);
        textField.setShowPlaceholderWhenFocused(true);
        panel.add(textField, BorderLayout.NORTH);
        @NotNull ComponentPopupBuilder popbuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, textField);
        popbuilder.setTitle(title);
        popbuilder.setCancelOnClickOutside(true);
        popbuilder.setProject(project);
        popbuilder.setFocusable(true);
        popbuilder.setOkHandler(() -> {
            String text = textField.getText().trim();
            if (validate(text)) {
                onOk.accept(text);
            }
        });
        @NotNull JBPopup pop = popbuilder.createPopup();
        pop.pack(true, true);
        pop.setRequestFocus(true);
        pop.showCenteredInCurrentWindow(project);
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                @NotNull @NlsSafe String text = event.getDocument().getText();
                if (!validate(text)) {
                    textField.setBackground(JBUI.CurrentTheme.Validator.errorBackgroundColor());
                } else {
                    textField.setBackground(null);
                }
            }
        });

        textField.getFocusTarget().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    if (validate(textField.getText())) {
                        pop.closeOk(e);
                    }
                }
            }
        });
    }
}
