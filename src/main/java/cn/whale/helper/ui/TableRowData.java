package cn.whale.helper.ui;

import cn.whale.helper.utils.DB;

public class TableRowData extends DB.Column {
    /**
     * is checked in table
     */
    public boolean checked;
    /**
     * struct field name
     */
    public String fieldName;
    /**
     * struct field type
     */
    public String goType;
    /**
     * struct field tag
     */
    public String tag;
}
