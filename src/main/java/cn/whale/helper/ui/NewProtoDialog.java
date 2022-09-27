package cn.whale.helper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class NewProtoDialog extends DialogWrapper {
    private final Project project;
    public JTextField textField = new JTextField();
    private JButton okButton;

    public NewProtoDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        this.setTitle("Create new proto file");
        init();
        this.setUndecorated(true);

        this.setSize(260, 80);
        pack();


    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return textField;
    }

    @Override
    protected JComponent createCenterPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
//        JLabel title = new JLabel("Create new proto file");
//        title.setBackground(Gray._224);
//        title.setHorizontalAlignment(SwingConstants.CENTER);
//        panel.add(title, BorderLayout.NORTH);
        JLabel label = new JLabel("File name: ");
        panel.add(label, BorderLayout.WEST);

        panel.add(textField, BorderLayout.CENTER);
        JLabel sufLabel = new JLabel(".proto");
        panel.add(sufLabel, BorderLayout.EAST);
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (doValidate() == null) {
                        NewProtoDialog.this.close(0, true);
                    }
                }
            }
        });

        return panel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JComponent comp = super.createSouthPanel();
        //comp.setVisible(false);
        return comp;
    }

    public ValidationInfo doValidate() {
        String text = textField.getText();
        if (!text.matches("[\\w_]+")) {
            return new ValidationInfo("Invalid file name", textField);
        }
        return null;
    }


    public String getFilename() {
        String text = textField.getText();
        if (text.endsWith(".proto")) {
            return text;
        }
        return text + ".proto";
    }
}
