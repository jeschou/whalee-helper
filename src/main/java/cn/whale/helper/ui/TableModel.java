package cn.whale.helper.ui;

import cn.whale.helper.ui.table.ReorderableTableModel;
import cn.whale.helper.utils.DB;
import cn.whale.helper.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class TableModel extends ReorderableTableModel {
    List<DB.Column> dbColumns;
    ColumnConfig[] tableColumns;

    Vector columnVector;

    public TableModel() {
        tableColumns = new ColumnConfig[]{
                new ColumnConfig("", "checked", 35, Boolean.class, true),
                new ColumnConfig("Name", "name", 175, String.class, false),
                new ColumnConfig("PgType", "typeName", 75, String.class, false),
                new ColumnConfig("FieldName", "fieldName", 150, String.class, true),
                new ColumnConfig("GoType", "goType", 75, String.class, true),
                new ColumnConfig("Tag", "tag", 0, String.class, true),
        };
        columnVector = new Vector();
        for (int i = 0; i < tableColumns.length; i++) {
            columnVector.add(tableColumns[i].title);
        }
        setDataVector(new Vector(), columnVector);
    }

    public List<TableRowData> getTableData() {
        List<TableRowData> list = new ArrayList<>();
        Vector<Vector> dataVector = getDataVector();
        for (Vector row : dataVector) {
            TableRowData tr = new TableRowData();
            for (int i = 0; i < row.size(); i++) {
                Utils.setFieldValue(tr, tableColumns[i].key, row.get(i));
            }
            if (tr.checked)
                list.add(tr);
        }
        return list;
    }

    public ColumnConfig[] getColumnConfig() {
        return tableColumns;
    }

    public void setDbColumns(List<DB.Column> dbColumns) {
        this.dbColumns = dbColumns;
        List<TableRowData> rowDataList = new ArrayList<>(dbColumns.size());
        Vector dataVector = new Vector();
        for (DB.Column c : dbColumns) {
            TableRowData rd = new TableRowData();
            Utils.copyByField(rd, c);
            rd.checked = true;
            rd.fieldName = Utils.toTitleCamelCase(c.name);
            rd.goType = DB.pgToGoType(c.typeName);
            rd.tag = createTag(c);
            rowDataList.add(rd);
            Vector vectorRow = new Vector();
            for (int i = 0; i < tableColumns.length; i++) {
                vectorRow.add(Utils.getFieldValue(rd, tableColumns[i].key));
            }
            dataVector.add(vectorRow);
        }

        setDataVector(dataVector, columnVector);
    }

    private String createTag(DB.Column c) {
        StringBuilder sb=new StringBuilder();
        sb.append("`gorm:\"column:").append(c.name);
        if( c.isPk ){
            sb.append(";primary_key");
        }
        if ("created_time".equals(c.name)||"create_time".equals(c.name)) {
            sb.append(";autoCreateTime");
        }
        if ("updated_time".equals(c.name)||"update_time".equals(c.name)) {
            sb.append(";autoCreateTime;autoUpdateTime");
        }
        sb.append("\" json:\"").append(c.name).append("\"`");
        return sb.toString();
    }


    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return tableColumns[columnIndex].type;
    }
}


class ColumnConfig {
    String title;
    String key;
    int width;
    Class<?> type;
    boolean editable;

    public ColumnConfig(String title, String key, int width, Class<?> type, boolean editable) {
        this.title = title;
        this.key = key;
        this.width = width;
        this.type = type;
        this.editable = editable;
    }
}