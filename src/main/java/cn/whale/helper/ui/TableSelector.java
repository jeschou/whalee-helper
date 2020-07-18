package cn.whale.helper.ui;

import cn.whale.helper.ui.table.FieldsTableModel;
import cn.whale.helper.ui.table.TableRowTransferHandler;
import cn.whale.helper.utils.DB;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TableSelector extends JPanel {
    public ComboBox<String> databaseCombox;
    public ComboBox<String> tableCombox;
    public JBTextField daoNameInput;
    public JBTable fieldsTable;
    public JBTextField structNameInput;

    private Project project;

    public TableSelector(Project project) {
        this.project = project;
        createUIComponents();
    }

    private void createUIComponents() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
        GridLayoutManager layout = new GridLayoutManager(5, 2, JBUI.emptyInsets(), -1, -1, false, false);
        setLayout(layout);

        add(new JBLabel("File Name:"), newCons(0, 0, 1, 1, 0, 0, 8, 0));
        daoNameInput = new JBTextField();
        add(daoNameInput, newCons(0, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Database:"), newCons(1, 0, 1, 1, 0, 0, 8, 0));
        databaseCombox = new ComboBox<>();
        new ComboboxSpeedSearch(databaseCombox);
        add(databaseCombox, newCons(1, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Table:"), newCons(2, 0, 1, 1, 0, 0, 8, 0));
        tableCombox = new ComboBox<>();
        new ComboboxSpeedSearch(tableCombox);
        add(tableCombox, newCons(2, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Struct Name:"), newCons(3, 0, 1, 1, 0, 0, 8, 0));
        structNameInput = new JBTextField();
        add(structNameInput, newCons(3, 1, 1, 1, 3, 3, 0, 1));

        JBScrollPane scrollPane = new JBScrollPane();
        add(scrollPane, newCons(4, 1, 1, 1, 3, 3, 0, 3));
        fieldsTable = new JBTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 1 || column == 2) {
                    return false;
                }
                return super.isCellEditable(row, column);
            }
        };
        fieldsTable.setDragEnabled(true);
        fieldsTable.setDropMode(DropMode.INSERT_ROWS);
        fieldsTable.setTransferHandler(new TableRowTransferHandler(fieldsTable));
        fieldsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane.setViewportView(fieldsTable);


        add(new Spacer(), newCons(4, 0, 1, 1, 6, 1, 0, 2));

        SwingUtilities.invokeLater(() -> {
            List<String> dbs = DB.getDatabases();
            dbs.add(0, "");
            databaseCombox.setModel(new DefaultComboBoxModel(dbs.toArray()));
        });


        databaseCombox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }
            final String db = e.getItem().toString();
            SwingUtilities.invokeLater(() -> {
                List<String> tbls = Utils.isNotEmpty(db) ? DB.getTables(db) : new ArrayList<>();
                tbls.add(0, "");
                tbls = tbls.stream().filter(s -> !s.matches(".+\\d+")).collect(Collectors.toList());
                tableCombox.setModel(new DefaultComboBoxModel(tbls.toArray()));
            });

        });


        tableCombox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }
            String database = (String) databaseCombox.getSelectedItem();
            String table = (String) e.getItem();
            if (Utils.isNoneEmpty(database, table)) {
                daoNameInput.setText(table + "-dao");
                structNameInput.setText(Utils.toTitleCamelCase(table));
                List<DB.Column> list = DB.getColumns(database, table);
                updateFieldsTable(list);
            }
        });

    }

    private void updateFieldsTable(List<DB.Column> columns) {
        DefaultTableModel tableModel = new FieldsTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                return String.class;
            }
        };

        Object[][] columnConfig = new Object[][]{
                {"", 35},
                {"Name", 150},
                {"PgType", 75},
                // {"Comment", 50},
                {"FieldName", 150},
                {"GoType", 75},
                {"Tag", 0},
        };

        Object[][] data = new Object[columns.size()][];
        int r = 0;
        for (DB.Column c : columns) {
            String tag = String.format("`gorm:\"column:%s%s\" json:\"%s\"`", c.name, c.isPk ? ";primary_key" : "", c.name);
            String goType = DB.pgToGoType(c.typeName);
            String fieldName = Utils.toTitleCamelCase(c.name);
            data[r++] = new Object[]{true, c.name, c.typeName, /*c.comment,*/ fieldName, goType, tag};
        }
        tableModel.setDataVector(data, Utils.pluck(columnConfig, 0));
        fieldsTable.setModel(tableModel);
        TableColumnModel columnModel = fieldsTable.getColumnModel();


        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            Integer width = (Integer) columnConfig[i][1];
            if (width != null && width > 0) {
                columnModel.getColumn(i).setMaxWidth(width);
            }
        }

        Map<String, DB.Column> colMap = new HashMap<>();
        columns.forEach(c -> colMap.put(c.name, c));

        columnModel.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 1) {
                    DB.Column col = colMap.get(value);
                    if (col.isPk) {
                        comp.setForeground(JBColor.ORANGE);
                    } else {
                        comp.setForeground(JBColor.foreground());
                    }
                }
                return comp;
            }
        });
    }

    private GridConstraints newCons(int row, int column, int rowSpan, int colSpan, int vSizePolicy, int hSizePolicy, int anchor, int fill) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(column);
        constraints.setRowSpan(rowSpan);
        constraints.setColSpan(colSpan);
        constraints.setVSizePolicy(vSizePolicy);
        constraints.setHSizePolicy(hSizePolicy);
        constraints.setAnchor(anchor);
        constraints.setFill(fill);
        constraints.setIndent(0);
        constraints.setUseParentLayout(false);
        return constraints;
    }
}
