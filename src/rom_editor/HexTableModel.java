package rom_editor;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.undo.*;

public class HexTableModel extends AbstractTableModel {
    List<byte[]> hexData;

    public List<byte[]> getHexData() {
        return hexData;
    }

    private final String[] columnNames;

    private UndoManager undoManager = new UndoManager();
    private boolean undoableEdit = true;

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void setUndoableEdit(boolean flag) {
        this.undoableEdit = flag;
    }

    public HexTableModel(List<byte[]> hexData) {
        this.hexData = hexData;
        columnNames = new String[18];
        columnNames[0] = "Address";
        for (int i = 1; i <= 16; i++) {
            columnNames[i] = String.format("%02X", i - 1);
        }
        columnNames[17] = "ASCII";
    }

    @Override
    public int getRowCount() {
        return hexData.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        byte[] row = hexData.get(rowIndex);
        if (columnIndex == 0) {
            return String.format("0x%04X", rowIndex * 16);
        } else if (columnIndex >= 1 && columnIndex <= 16) {
            int idx = columnIndex - 1;
            if (idx < row.length) {
                return String.format("%02X", row[idx]);
            } else {
                return "";
            }
        } else if (columnIndex == 17) {
            StringBuilder ascii = new StringBuilder();
            for (byte b : row) {
                if (b >= 32 && b <= 126) {
                    ascii.append((char) b);
                } else {
                    ascii.append('.');
                }
                ascii.append(' ');
            }
            return ascii.toString().trim();
        }
        return "";
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex > 0 && columnIndex <= 16;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex > 0 && columnIndex <= 16) {
            byte[] row = hexData.get(rowIndex);
            int idx = columnIndex - 1;
            if (idx < row.length) {
                try {
                    String str = aValue.toString().trim();
                    if (str.length() > 2) str = str.substring(0, 2);
                    int oldVal = row[idx] & 0xFF;
                    int val = Integer.parseInt(str, 16);
                    if (undoableEdit && oldVal != val) {
                        undoManager.addEdit(new AbstractUndoableEdit() {
                            public void undo() throws CannotUndoException {
                                super.undo();
                                undoableEdit = false;
                                row[idx] = (byte) oldVal;
                                fireTableCellUpdated(rowIndex, columnIndex);
                                fireTableCellUpdated(rowIndex, 17);
                                undoableEdit = true;
                            }
                            public void redo() throws CannotRedoException {
                                super.redo();
                                undoableEdit = false;
                                row[idx] = (byte) val;
                                fireTableCellUpdated(rowIndex, columnIndex);
                                fireTableCellUpdated(rowIndex, 17);
                                undoableEdit = true;
                            }
                        });
                    }
                    row[idx] = (byte) val;
                    fireTableCellUpdated(rowIndex, columnIndex);
                    fireTableCellUpdated(rowIndex, 17); 
                } catch (NumberFormatException e) {
                    // Ignore this shit
                }
            }
        }
    }

    public void setHexData(List<byte[]> hexData) {
        this.hexData = hexData;
        fireTableDataChanged();
    }
}