package cn.whale.helper.ui;

import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.table.JBTable;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SelectTableDialogWrapper extends DialogWrapper {

    Project project;

    TableSelector tableSelector;

    public SelectTableDialogWrapper(Project project) {
        super(true); // use current window as parent
        this.project = project;
        tableSelector = new TableSelector(project);
        init();
        setTitle("Create Dao From Table");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return tableSelector;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (Utils.isEmpty(tableSelector.daoNameInput.getText())) {
            return new ValidationInfo("please input filename", tableSelector.daoNameInput);
        }
        String db = (String) tableSelector.databaseCombox.getSelectedItem();
        if (Utils.isEmpty(db)) {
            return new ValidationInfo("please select database", tableSelector.databaseCombox);
        }
        String table = (String) tableSelector.tableCombox.getSelectedItem();
        if (Utils.isEmpty(table)) {
            return new ValidationInfo("please select table", tableSelector.tableCombox);
        }
        if (Utils.isEmpty(tableSelector.structNameInput.getText())) {
            return new ValidationInfo("please struct name", tableSelector.structNameInput);
        }
        if (getColumnData().size() == 0) {
            return new ValidationInfo("please select rows", tableSelector.fieldsTable);
        }
        return null;
    }

    public String getFileName() {
        return tableSelector.daoNameInput.getText().trim();
    }

    public String getTableName() {
        return (String) tableSelector.tableCombox.getSelectedItem();
    }

    public String getStructName() {
        return tableSelector.structNameInput.getText().trim();
    }

    public List<Object[]> getColumnData() {
        List<Object[]> selectedRows = new ArrayList<>();
        JBTable table = tableSelector.fieldsTable;
        int rows = table.getRowCount();
        int cols = table.getColumnCount();
        for (int i = 0; i < rows; i++) {
            Object v0 = table.getValueAt(i, 0);
            if (Boolean.FALSE.equals(v0)) {
                continue;
            }
            Object[] objs = new Object[cols];
            for (int j = 0; j < cols; j++) {
                objs[j] = table.getValueAt(i, j);
            }
            selectedRows.add(objs);
        }
        return selectedRows;
    }
}