package cn.whale.helper.ui;

import cn.whale.helper.ui.table.TableRowTransferHandler;
import cn.whale.helper.utils.DB;
import cn.whale.helper.utils.DbConfig;
import cn.whale.helper.utils.NacosUtil;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
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
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableSelector extends JPanel {
    static Notifier notifier = Notifier.getInstance("whgo_helper gorm-repo-gen");
    public ComboBox<String> serviceCombox;
    public ComboBox<String> databaseCombox;
    public ComboBox<DB.TableWithSchema> tableCombox;
    public JBTextField repoNameInput;
    public JBTable fieldsTable;
    public JBTextField structNameInput;
    public DbConfig dbConfig;

    private Project project;

    int gormVersion = 1;

    public TableSelector(Project project, int gormVersion) {
        this.project = project;
        this.gormVersion = gormVersion;
        createUIComponents();
    }

    private void createUIComponents() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
        GridLayoutManager layout = new GridLayoutManager(6, 2, JBUI.emptyInsets(), -1, -1, false, false);
        setLayout(layout);

        add(new JBLabel("File name:"), newCons(0, 0, 1, 1, 0, 0, 8, 0));
        repoNameInput = new JBTextField();
        add(repoNameInput, newCons(0, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Db config:"), newCons(1, 0, 1, 1, 0, 0, 8, 0));
        serviceCombox = new ComboBox<>();
        new ComboboxSpeedSearch(serviceCombox);
        add(serviceCombox, newCons(1, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Database:"), newCons(2, 0, 1, 1, 0, 0, 8, 0));
        databaseCombox = new ComboBox<>();
        new ComboboxSpeedSearch(databaseCombox);
        add(databaseCombox, newCons(2, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Table:"), newCons(3, 0, 1, 1, 0, 0, 8, 0));
        tableCombox = new ComboBox<>();
        new ComboboxSpeedSearch(tableCombox);
        add(tableCombox, newCons(3, 1, 1, 1, 3, 3, 0, 1));
        tableCombox.setRenderer(new ListCellRendererWithRightAlignedComponent<DB.TableWithSchema>() {
            @Override
            protected void customize(DB.TableWithSchema s) {
                if (s == null) {
                    return;
                }
                setLeftText(s.tableName);
                setRightText(s.schema);
                setRightForeground(Color.lightGray);
            }
        });

        add(new JBLabel("Struct name:"), newCons(4, 0, 1, 1, 0, 0, 8, 0));
        structNameInput = new JBTextField();
        add(structNameInput, newCons(4, 1, 1, 1, 3, 3, 0, 1));

        JBScrollPane scrollPane = new JBScrollPane();
        add(scrollPane, newCons(5, 1, 1, 1, 3, 3, 0, 3));
        fieldsTable = new JBTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                TableModel tableModel = (TableModel) getModel();
                return tableModel.getColumnConfig()[column].editable;
            }
        };
        fieldsTable.setDragEnabled(true);
        fieldsTable.setDropMode(DropMode.INSERT_ROWS);
        fieldsTable.setTransferHandler(new TableRowTransferHandler(fieldsTable));
        fieldsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane.setViewportView(fieldsTable);


        add(new Spacer(), newCons(5, 0, 1, 1, 6, 1, 0, 2));


        serviceCombox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }
            dbConfig = (DbConfig) e.getItem();
            if (dbConfig.serviceName.contains("add local config")) {
                try {
                    Desktop.getDesktop().browse(new URI("https://whales.feishu.cn/docx/P6tzdy6sNoyoEIxl4LSc7HEYn5b#SgQQdmiyWowa2Yx0BrncUW25nDs"));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                Window wind = SwingUtilities.getWindowAncestor(serviceCombox);
                if (wind != null) {
                    wind.dispose();
                }
                return;
            }
            setLoading(databaseCombox);
            setLoading(tableCombox);
            resetFields();
            new Thread(() -> {
                try {
                    List<String> dbs = DB.getDatabases(dbConfig);
                    SwingUtilities.invokeLater(() -> {
                        databaseCombox.setModel(new DefaultComboBoxModel(dbs.toArray()));
                        databaseCombox.setSelectedItem(dbConfig.database);
                    });
                } catch (Exception ex) {
                    notifier.error(project, Utils.getStackTrace(ex));
                }
            }).start();
        });


        databaseCombox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }
            dbConfig = (DbConfig) serviceCombox.getSelectedItem();
            final String db = e.getItem().toString();

            resetFields();
            setLoading(tableCombox, new DB.TableWithSchema());
            new Thread(() -> {
                try {
                    final List<DB.TableWithSchema> tbls = Utils.isNotEmpty(db) ? DB.getTables(dbConfig, db) : new ArrayList<>();
                    SwingUtilities.invokeLater(() -> {
                        List<DB.TableWithSchema> tbls0 = removeBackUpTable(tbls);
                        tbls0.add(0, new DB.TableWithSchema());
                        tableCombox.setModel(new DefaultComboBoxModel(tbls0.toArray()));
                        tableCombox.setSelectedIndex(0);
                    });
                } catch (Exception ex) {
                    notifier.error(project, Utils.getStackTrace(ex));
                }
            }).start();

        });


        tableCombox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                return;
            }
            dbConfig = (DbConfig) serviceCombox.getSelectedItem();
            String database = (String) databaseCombox.getSelectedItem();
            DB.TableWithSchema table = (DB.TableWithSchema) e.getItem();
            List<DB.Column> list = new ArrayList<>();
            if (Utils.isNoneEmpty(database, table.tableName)) {
                if (gormVersion == 2) {
                    repoNameInput.setText(table + "-repo2");
                } else {
                    repoNameInput.setText(table + "-repo");
                }
                structNameInput.setText(Utils.toTitleCamelCase(table.tableName));
                list = DB.getColumns(dbConfig, database, table);
            } else {
                repoNameInput.setText("");
                structNameInput.setText("");
            }
            updateFieldsTable(list);
        });

        setLoading(serviceCombox);
        setLoading(databaseCombox);
        new Thread(() -> {
            try {
                final List<DbConfig> cfgs = NacosUtil.getDbConfigList();
                cfgs.add(new DbConfig() {
                    {
                        this.serviceName = "➕add local config ...";
                    }
                });
                final List<String> dbs = DB.getDatabases(cfgs.get(0));

                SwingUtilities.invokeLater(() -> {
                    serviceCombox.setModel(new DefaultComboBoxModel(cfgs.toArray()));

                    databaseCombox.setModel(new DefaultComboBoxModel(dbs.toArray()));
                    databaseCombox.setSelectedItem(cfgs.get(0).database);
                });
            } catch (Exception e) {
                e.printStackTrace();
                notifier.error(project, Utils.getStackTrace(e));
            }
        }).start();
    }

    private <T> void setLoading(ComboBox<T> combox, T... values) {
        T[] vals = values;
        if (vals.length == 0) {
            vals = (T[]) new String[]{"loading..."};
        }
        combox.setModel(new DefaultComboBoxModel(vals));
        combox.setSelectedIndex(0);
    }

    private void resetFields() {
        repoNameInput.setText("");
        structNameInput.setText("");
        updateFieldsTable(new ArrayList<>());
    }

    /**
     * remove backup or partition table, e.g. edge_tracing_log_201908
     *
     * @param list
     * @return
     */
    private List<DB.TableWithSchema> removeBackUpTable(List<DB.TableWithSchema> list) {
        List<DB.TableWithSchema> list2 = new ArrayList<>(list.size());
        for (DB.TableWithSchema s : list) {
            if (!s.tableName.matches(".+\\d+(_partition)?")) {
                list2.add(s);
            }
        }
        return list2;
    }

    private void updateFieldsTable(List<DB.Column> columns) {
        TableModel tableModel = new TableModel(gormVersion);
        tableModel.setDbColumns(columns);

        fieldsTable.setModel(tableModel);
        TableColumnModel columnModel = fieldsTable.getColumnModel();


        ColumnConfig[] columnConfig = tableModel.getColumnConfig();
        for (int i = 0; i < columnConfig.length; i++) {
            int width = columnConfig[i].width;
            if (width > 0) {
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
