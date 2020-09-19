package cn.whale.helper.ui;

import cn.whale.helper.utils.DbConfig;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;

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
            return new ValidationInfo("Please input filename", tableSelector.daoNameInput);
        }
        String db = (String) tableSelector.databaseCombox.getSelectedItem();
        if (Utils.isEmpty(db)) {
            return new ValidationInfo("Please select database", tableSelector.databaseCombox);
        }
        String table = (String) tableSelector.tableCombox.getSelectedItem();
        if (Utils.isEmpty(table)) {
            return new ValidationInfo("Please select table", tableSelector.tableCombox);
        }
        if (Utils.isEmpty(tableSelector.structNameInput.getText())) {
            return new ValidationInfo("Please struct name", tableSelector.structNameInput);
        }
        if (getColumnData().size() == 0) {
            return new ValidationInfo("Please select rows", tableSelector.fieldsTable);
        }
        return null;
    }

    public String getFileName() {
        return tableSelector.daoNameInput.getText().trim();
    }

    public String getTableName() {
        return (String) tableSelector.tableCombox.getSelectedItem();
    }

    public String getDatabase() {
        return (String) tableSelector.databaseCombox.getSelectedItem();
    }

    public DbConfig getDbConfig() {
        return tableSelector.dbConfig;
    }

    public String getStructName() {
        return tableSelector.structNameInput.getText().trim();
    }

    public List<TableRowData> getColumnData() {
        TableModel tableModel = (TableModel) tableSelector.fieldsTable.getModel();
        return tableModel.getTableData();
    }
}