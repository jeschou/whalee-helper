package cn.whale.helper.ui.table;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class FieldsTableModel extends DefaultTableModel implements Reorderable {
    @Override
    public void reorder(int fromIndex, int toIndex) {
        Vector vector = getDataVector();
        Object del = vector.remove(fromIndex);
        fireTableRowsDeleted(fromIndex, fromIndex);
        if (toIndex > fromIndex)
            toIndex--;
        vector.add(toIndex, del);
        fireTableRowsInserted(toIndex, toIndex);
    }
}
