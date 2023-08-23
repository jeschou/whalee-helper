package cn.whale.helper.ui;

import cn.whale.helper.action.RepoGenCtx;
import cn.whale.helper.ui.table.TableRowTransferHandler;
import cn.whale.helper.utils.DB;
import cn.whale.helper.utils.DbConfig;
import cn.whale.helper.utils.NacosUtil;
import cn.whale.helper.utils.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    private JBLabel indexLabel;
    public DbConfig dbConfig;

    private Project project;

    RepoGenCtx ctx;

    public TableSelector(Project project, RepoGenCtx ctx) {
        this.project = project;
        this.ctx = ctx;
        createUIComponents();
    }

    String selectDefaultDatabase(List<String> dbs, DbConfig dbConfig) {
        if (Utils.isEmpty(ctx.defaultDatabase) || !dbs.contains(ctx.defaultDatabase)) {
            return dbConfig.database;
        }
        return ctx.defaultDatabase;
    }

    private void createUIComponents() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
        GridLayoutManager layout = new GridLayoutManager(7, 2, JBUI.emptyInsets(), -1, -1, false, false);
        setLayout(layout);
        int row = 0;

        add(new JBLabel("File name:"), newCons(row, 0, 1, 1, 0, 0, 8, 0));
        repoNameInput = new JBTextField();
        add(repoNameInput, newCons(row, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Db config:"), newCons(++row, 0, 1, 1, 0, 0, 8, 0));
        serviceCombox = new ComboBox<>();
        new ComboboxSpeedSearch(serviceCombox);
        add(serviceCombox, newCons(row, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Database:"), newCons(++row, 0, 1, 1, 0, 0, 8, 0));
        databaseCombox = new ComboBox<>();

        new ComboboxSpeedSearch(databaseCombox);
        add(databaseCombox, newCons(row, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Table:"), newCons(++row, 0, 1, 1, 0, 0, 8, 0));
        tableCombox = new ComboBox<>();
        new ComboboxSpeedSearch(tableCombox);
        add(tableCombox, newCons(row, 1, 1, 1, 3, 3, 0, 1));
        tableCombox.setRenderer(new ListCellRendererWithRightAlignedComponent<DB.TableWithSchema>() {
            @Override
            protected void customize(DB.TableWithSchema s) {
                if (s == null) {
                    return;
                }
                setLeftText(s.tableName);
                setRightText(s.schema);
                setRightForeground(JBUI.CurrentTheme.Label.disabledForeground());
            }
        });

        add(new JBLabel("Struct name:"), newCons(++row, 0, 1, 1, 0, 0, 8, 0));
        structNameInput = new JBTextField();
        add(structNameInput, newCons(row, 1, 1, 1, 3, 3, 0, 1));

        add(new JBLabel("Fields:"), newCons(++row, 0, 1, 1, 0, 0, 8, 0));
        JBScrollPane scrollPane = new JBScrollPane();
        add(scrollPane, newCons(row, 1, 1, 1, 6, 3, 0, 3));
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

        ContextHelpLabel label = new ContextHelpLabel("", "Index will be add to gorm struct tag, work with AutoMigrate.");
        label.setIcon(AllIcons.General.ContextHelp);

        JBPanel p = new JBPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(new JBLabel("Index:"));
        p.add(label);
        add(p, newCons(++row, 0, 1, 1, 0, 0, 8, 0));
        indexLabel = new JBLabel();


        add(indexLabel, newCons(row, 1, 1, 1, 3, 3, 0, 3));

        serviceCombox.addItemListener(new ItemListenerAdapter<DbConfig>() {
            @Override
            public void itemSelected(DbConfig val) {
                dbConfig = val;
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
                            setSource(databaseCombox, dbs, selectDefaultDatabase(dbs, dbConfig));
                        });
                    } catch (Exception ex) {
                        notifier.error(project, Utils.getStackTrace(ex));
                    }
                }).start();
            }
        });

        databaseCombox.addItemListener(new ItemListenerAdapter<String>() {
            @Override
            public void itemSelected(String db) {
                dbConfig = (DbConfig) serviceCombox.getSelectedItem();

                resetFields();
                setLoading(tableCombox);
                new Thread(() -> {
                    try {
                        final List<DB.TableWithSchema> tbls = Utils.isNotEmpty(db) ? DB.getTables(dbConfig, db) : new ArrayList<>();
                        SwingUtilities.invokeLater(() -> {
                            List<DB.TableWithSchema> tbls0 = removeBackUpTable(tbls);
                            tbls0.add(0, new DB.TableWithSchema());
                            setSource(tableCombox, tbls0, tbls0.get(0));
                        });
                    } catch (Exception ex) {
                        notifier.error(project, Utils.getStackTrace(ex));
                    }
                }).start();
            }
        });


        tableCombox.addItemListener(new ItemListenerAdapter<DB.TableWithSchema>() {
            @Override
            public void itemSelected(DB.TableWithSchema table) {
                dbConfig = (DbConfig) serviceCombox.getSelectedItem();
                String database = (String) databaseCombox.getSelectedItem();
                List<DB.Column> list = new ArrayList<>();
                List<DB.Index> indexList = new ArrayList<>();
                if (Utils.isNoneEmpty(database, table.tableName)) {
                    if (ctx.isGormV2()) {
                        repoNameInput.setText(table + "-repo2");
                    } else {
                        repoNameInput.setText(table + "-repo");
                    }
                    structNameInput.setText(Utils.toTitleCamelCase(table.tableName));
                    list = DB.getColumns(dbConfig, database, table);
                    indexList = DB.getIndexes(dbConfig, database, table);
                    updateIndexLabel(indexList);
                } else {
                    repoNameInput.setText("");
                    structNameInput.setText("");
                }
                updateFieldsTable(list, indexList);
            }
        });

        setLoading(serviceCombox);
        setLoading(databaseCombox);
        new Thread(() -> {
            try {
                final List<DbConfig> cfgs = NacosUtil.getDbConfigList();
                cfgs.add(new DbConfig() {
                    {
                        this.serviceName = "➕add local config";
                    }
                });

                SwingUtilities.invokeLater(() -> {
                    setSource(serviceCombox, cfgs, cfgs.get(0));
                });
            } catch (Exception e) {
                e.printStackTrace();
                notifier.error(project, Utils.getStackTrace(e));
            }
        }).start();
    }

    private void updateIndexLabel(List<DB.Index> indexList) {
        if (indexList == null || indexList.isEmpty()) {
            indexLabel.setForeground(JBColor.RED);
            indexLabel.setText(" ** NO INDEX ** ");
        } else {
            indexLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
            indexLabel.setText("<html>" + StringUtils.join(indexList, "<br>") + "</html>");
        }
    }

    private void setLoading(ComboBox combox) {
        Object[] vals = null;
        if (combox == tableCombox) {
            DB.TableWithSchema a = new DB.TableWithSchema();
            a.tableName = "loading...";
            vals = new Object[]{a};
        } else if (combox == serviceCombox) {
            DbConfig a = new DbConfig();
            a.serviceName = "loading...";
            vals = new Object[]{a};
        } else {
            vals = new Object[]{"loading..."};
        }

        combox.setModel(new DefaultComboBoxModel(vals));
        combox.setSelectedIndex(0);
    }

    private boolean isLoading(String text) {
        return Utils.isEmpty(text) || text.contains("...");
    }

    private boolean isValid(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof DbConfig) {
            return !isLoading(((DbConfig) obj).serviceName);
        }
        if (obj instanceof DB.TableWithSchema) {
            return !isLoading(((DB.TableWithSchema) obj).tableName);
        }
        return !isLoading(obj.toString());
    }

    private void setSource(ComboBox comboBox, List src, Object selection) {
        comboBox.setModel(new DefaultComboBoxModel(src.toArray()));
        comboBox.setSelectedItem(null);
        comboBox.setSelectedItem(selection);
    }

    private void resetFields() {
        repoNameInput.setText("");
        structNameInput.setText("");
        updateFieldsTable(new ArrayList<>(), new ArrayList<>());
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

    private void updateFieldsTable(List<DB.Column> columns, List<DB.Index> indexList) {
        TableModel tableModel = new TableModel(ctx);
        tableModel.setIndex(indexList);
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

    private class ItemListenerAdapter<T> implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Object item = e.getItem();
                if (isValid(item)) {
                    itemSelected((T) item);
                }
            }
        }

        public void itemSelected(T val) {

        }
    }
}
