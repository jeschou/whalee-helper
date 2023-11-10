package cn.whale.helper.ui;

import cn.whale.helper.action.RepoGenCtx;
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

    RepoGenCtx ctx;
    private List<DB.Index> indexList;

    public TableModel(RepoGenCtx ctx) {
        this.ctx = ctx;
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
            rd.goType = ctx.isGormV2() ? DB.pgToGoTypeV2(c.typeName) : DB.pgToGoType(c.typeName);
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
        StringBuilder sb = new StringBuilder();
        sb.append("`gorm:\"column:").append(c.name);
        if (c.isPk) {
            if (ctx.isGormV2()) {
                sb.append(";primaryKey");
            } else {
                sb.append(";primary_key");
            }
        }
        if ("created_time".equals(c.name) || "create_time".equals(c.name)) {
            sb.append(";autoCreateTime");
        }
        if ("updated_time".equals(c.name) || "update_time".equals(c.name)) {
            sb.append(";autoCreateTime;autoUpdateTime");
        }
        // for gorm v2: array type must declare type
        if (c.typeName.startsWith("_")) {
            sb.append(";type:").append(c.typeName.substring(1)).append("[]");
        } else if (c.typeName.equalsIgnoreCase("numeric")) {
            sb.append(";type:").append(c.typeName).append("(").append(c.size).append(",").append(c.precision).append(")");
        }
        if (ctx.isGormV2()) {
            // only create index tag for gorm v2 (v1 can not guarantee index field order)
            for (DB.Index idx : indexList) {
                int idxIdx = idx.columns.indexOf(c.name);
                if (idxIdx >= 0) {
                    sb.append(";").append(idx.isUnique ? (ctx.isGormV2() ? "uniqueIndex" : "unique_index") : "index").append(":");
                    sb.append(idx.name);
                    if (!"btree".equalsIgnoreCase(idx.indexType)) {
                        sb.append(",type:").append(idx.indexType);
                    }
                    if (idx.columns.size() > 1) {
                        sb.append(",priority:").append(idxIdx + 1);
                    }
                }
            }
        }

        sb.append('"');// end of gorm tag

        sb.append(" json:\"").append(c.name).append("\"`");
        return sb.toString();
    }


    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return tableColumns[columnIndex].type;
    }

    public void setIndex(List<DB.Index> indexList) {
        this.indexList = indexList;
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