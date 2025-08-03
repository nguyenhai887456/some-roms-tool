package rom_editor.ui;



import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import rom_editor.FileHandler;
import rom_editor.HexTableModel;

public class MainFrame extends JFrame {
    private JTable hexTable;
    private HexTableModel tableModel;
    private FileHandler fileHandler = new FileHandler();

    private int hoveredAsciiRow = -1;
    private int hoveredAsciiChar = -1;
    private int selectedAsciiRow = -1;
    private int selectedAsciiChar = -1;

    private boolean isModified = false;
    private String currentFilePath = null;

    private JButton saveButton;

    public MainFrame() {
        setTitle("ROM/Hex Editor");
        setSize(1200, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayout(10, 1, 10, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton openButton = new JButton("Open File");
        saveButton = new JButton("Save");
        JButton exitButton = new JButton("Exit");

        saveButton.setEnabled(false);

        leftPanel.add(openButton);
        leftPanel.add(saveButton);
        leftPanel.add(exitButton);

        add(leftPanel, BorderLayout.WEST);

        tableModel = new HexTableModel(new ArrayList<>());
        hexTable = new JTable(tableModel);

        int tableWidth = 900;
        int asciiWidth = 180; // ASCII column
        int dataCols = 17;    // 1 address + 16 data
        int dataColWidth = (tableWidth - asciiWidth) / dataCols;

        for (int i = 0; i < dataCols; i++) {
            hexTable.getColumnModel().getColumn(i).setPreferredWidth(dataColWidth);
        }
        hexTable.getColumnModel().getColumn(17).setPreferredWidth(asciiWidth);

        JTextField hexEditorField = new JTextField();
        DefaultCellEditor hexEditor = new DefaultCellEditor(hexEditorField) {
            @Override
            public boolean stopCellEditing() {
                boolean stopped = super.stopCellEditing();
                if (stopped) {
                    int row = hexTable.getSelectedRow();
                    int col = hexTable.getSelectedColumn();
                    if (col >= 1 && col < 16) {
                        hexTable.changeSelection(row, col + 1, false, false);
                        hexTable.editCellAt(row, col + 1);
                        hexTable.getEditorComponent().requestFocus();
                    } else if (col == 16 && row < hexTable.getRowCount() - 1) {
                        hexTable.changeSelection(row + 1, 1, false, false);
                        hexTable.editCellAt(row + 1, 1);
                        hexTable.getEditorComponent().requestFocus();
                    }
                }
                return stopped;
            }
        };
        for (int i = 1; i <= 16; i++) {
            hexTable.getColumnModel().getColumn(i).setCellEditor(hexEditor);
        }

        hexTable.getInputMap().put(KeyStroke.getKeyStroke("ctrl V"), "PasteHex");
        hexTable.getActionMap().put("PasteHex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = hexTable.getSelectedRow();
                int col = hexTable.getSelectedColumn();
                if (row < 0 || col < 1 || col > 16) return;
                try {
                    String data = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    data = data.trim();
                    String[] tokens;
                    if (data.contains(" ")) {
                        tokens = data.split("\\s+");
                    } else {
                        tokens = data.split("(?<=\\G..)");
                    }
                    int r = row, c = col;
                    for (String token : tokens) {
                        if (token.isEmpty()) continue;
                        if (c > 16) {
                            c = 1;
                            r++;
                            if (r >= hexTable.getRowCount()) break;
                        }
                        tableModel.setValueAt(token, r, c);
                        c++;
                    }
                    if (c > 16) {
                        if (r + 1 < hexTable.getRowCount()) {
                            hexTable.changeSelection(r + 1, 1, false, false);
                        }
                    } else {
                        hexTable.changeSelection(r, c, false, false);
                    }
                } catch (Exception ex) {
                    // Ignore this shit
                }
            }
        });

        hexTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("SPACE"), "NextCell");
        hexTable.getActionMap().put("NextCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hexTable.isEditing()) {
                    hexTable.getCellEditor().stopCellEditing();
                }
            }
        });

        hexTable.setRowSelectionAllowed(false);
        hexTable.setColumnSelectionAllowed(true);
        hexTable.setCellSelectionEnabled(true);

        TableCellRenderer asciiRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                          boolean isSelected, boolean hasFocus,
                                                          int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
                label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setOpaque(true);
                label.setBackground(Color.WHITE);

                label = new JLabel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        String asciiStr = value.toString();
                        int x = 0;
                        FontMetrics fm = g.getFontMetrics();
                        for (int pos = 0, i = 0; pos < 16 && i < asciiStr.length(); pos++, i += 2) {
                            char ch = asciiStr.charAt(i);
                            int charWidth = fm.charWidth(ch);
                            int charX = x;
                            boolean drawOutline = (row == hoveredAsciiRow && pos == hoveredAsciiChar) ||
                                                  (row == selectedAsciiRow && pos == selectedAsciiChar);

                            boolean isHexSelected = hexTable.isCellSelected(row, pos + 1);
                            if (isHexSelected) {
                                g.setColor(new Color(200, 220, 255));
                                g.fillRect(charX - 2, 2, charWidth + 3, fm.getHeight());
                            }

                            g.setColor(Color.BLACK);
                            g.drawString(String.valueOf(ch), charX, fm.getAscent() + 2);
                            if (drawOutline) {
                                g.setColor(Color.BLUE);
                                g.drawRect(charX - 2, 2, charWidth + 3, fm.getHeight());
                            }
                            x += charWidth + fm.charWidth(' ');
                        }
                    }
                };
                label.setPreferredSize(super.getPreferredSize());
                label.setOpaque(true);
                label.setBackground(Color.WHITE);
                return label;
            }
        };
        hexTable.getColumnModel().getColumn(17).setCellRenderer(asciiRenderer);

        DefaultTableCellRenderer hexRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                          boolean isSelected, boolean hasFocus,
                                                          int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setOpaque(true);
                if ("00".equalsIgnoreCase(String.valueOf(value))) {
                    label.setBackground(Color.LIGHT_GRAY);
                } else {
                    label.setBackground(Color.WHITE);
                }
                if (row == selectedAsciiRow && column - 1 == selectedAsciiChar && column > 0 && column <= 16) {
                    label.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                } else if (table.isCellSelected(row, column)) {
                    label.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                } else {
                    label.setBorder(null);
                }
                return label;
            }
        };
        for (int i = 1; i <= 16; i++) {
            hexTable.getColumnModel().getColumn(i).setCellRenderer(hexRenderer);
        }

        hexTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = hexTable.rowAtPoint(e.getPoint());
                int col = hexTable.columnAtPoint(e.getPoint());
                int prevRow = hoveredAsciiRow;
                int prevChar = hoveredAsciiChar;
                if (col == 17 && row >= 0) {
                    Rectangle cellRect = hexTable.getCellRect(row, col, false);
                    String asciiStr = (String) tableModel.getValueAt(row, col);
                    FontMetrics fm = hexTable.getFontMetrics(hexTable.getFont());
                    int xInCell = e.getX() - cellRect.x;
                    int charWidth = fm.charWidth('A') + fm.charWidth(' ');
                    int letterIdx = xInCell / charWidth;
                    if (letterIdx >= 0 && letterIdx < 16) {
                        hoveredAsciiRow = row;
                        hoveredAsciiChar = letterIdx;
                        if (prevRow != hoveredAsciiRow || prevChar != hoveredAsciiChar) {
                            if (prevRow >= 0) hexTable.repaint(hexTable.getCellRect(prevRow, 17, false));
                            hexTable.repaint(cellRect);
                        }
                        return;
                    }
                }
                if (hoveredAsciiRow != -1 || hoveredAsciiChar != -1) {
                    int oldRow = hoveredAsciiRow;
                    hoveredAsciiRow = -1;
                    hoveredAsciiChar = -1;
                    if (oldRow >= 0) {
                        hexTable.repaint(hexTable.getCellRect(oldRow, 17, false));
                    }
                }
            }
        });

        hexTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = hexTable.rowAtPoint(e.getPoint());
                int col = hexTable.columnAtPoint(e.getPoint());

                int prevRow = selectedAsciiRow;
                int prevChar = selectedAsciiChar;

                selectedAsciiRow = -1;
                selectedAsciiChar = -1;
                hexTable.clearSelection();

                if (col == 17 && row >= 0) {
                    Rectangle cellRect = hexTable.getCellRect(row, col, false);
                    String asciiStr = (String) tableModel.getValueAt(row, col);
                    FontMetrics fm = hexTable.getFontMetrics(hexTable.getFont());
                    int xInCell = e.getX() - cellRect.x;
                    int charWidth = fm.charWidth('A') + fm.charWidth(' ');
                    int letterIdx = xInCell / charWidth;
                    if (letterIdx >= 0 && letterIdx < 16) {
                        selectedAsciiRow = row;
                        selectedAsciiChar = letterIdx;
                        hexTable.changeSelection(row, letterIdx + 1, false, false);
                    }
                } else if (col > 0 && col <= 16 && row >= 0) {
                    selectedAsciiRow = row;
                    selectedAsciiChar = col - 1;
                    hexTable.changeSelection(row, col, false, false);
                }

                if (prevRow >= 0 && prevChar >= 0) {
                    hexTable.repaint(hexTable.getCellRect(prevRow, prevChar + 1, false));
                }
                if (selectedAsciiRow >= 0 && selectedAsciiChar >= 0) {
                    hexTable.repaint(hexTable.getCellRect(selectedAsciiRow, selectedAsciiChar + 1, false));
                }
            }
        });

        hexTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("DELETE"), "DeleteHex");
        hexTable.getActionMap().put("DeleteHex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = hexTable.getSelectedRows();
                int[] selectedCols = hexTable.getSelectedColumns();

                java.util.List<int[]> cells = new ArrayList<>();
                java.util.List<String> oldValues = new ArrayList<>();

                for (int row : selectedRows) {
                    for (int col : selectedCols) {
                        if (col >= 1 && col <= 16) {
                            Object val = tableModel.getValueAt(row, col);
                            cells.add(new int[]{row, col});
                            oldValues.add(val != null ? val.toString() : "00");
                        }
                    }
                }

                if (!cells.isEmpty()) {
                    tableModel.getUndoManager().addEdit(new javax.swing.undo.AbstractUndoableEdit() {
                        @Override
                        public void undo() throws javax.swing.undo.CannotUndoException {
                            super.undo();
                            tableModel.setUndoableEdit(false);
                            for (int i = 0; i < cells.size(); i++) {
                                int[] cell = cells.get(i);
                                tableModel.setValueAt(oldValues.get(i), cell[0], cell[1]);
                            }
                            tableModel.setUndoableEdit(true);
                        }
                        @Override
                        public void redo() throws javax.swing.undo.CannotRedoException {
                            super.redo();
                            tableModel.setUndoableEdit(false);
                            for (int[] cell : cells) {
                                tableModel.setValueAt("00", cell[0], cell[1]);
                            }
                            tableModel.setUndoableEdit(true);
                        }
                    });
                }

                tableModel.setUndoableEdit(false);
                for (int[] cell : cells) {
                    tableModel.setValueAt("00", cell[0], cell[1]);
                }
                tableModel.setUndoableEdit(true);
            }
        });

        JScrollPane scrollPane = new JScrollPane(hexTable);
        add(scrollPane, BorderLayout.CENTER);

        hexTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "UndoHex");
        hexTable.getActionMap().put("UndoHex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tableModel.getUndoManager().canUndo()) {
                    tableModel.getUndoManager().undo();
                }
            }
        });

        hexTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "SaveHex");
        hexTable.getActionMap().put("SaveHex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentFile();
            }
        });

        tableModel.addTableModelListener(e -> {
            isModified = true;
            saveButton.setEnabled(true);
        });

        openButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                openFileDialogWithPath(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        saveButton.addActionListener(e -> saveCurrentFile());

        exitButton.addActionListener(e -> {
            attemptExit();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attemptExit();
            }
        });
    }

    private void attemptExit() {
        if (isModified) {
            int result = JOptionPane.showOptionDialog(this,
                "You have unsaved changes. Save before exit?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{"Save", "Don't Save", "Cancel"},
                "Save");
            if (result == JOptionPane.YES_OPTION) {
                saveCurrentFile();
                dispose();
            } else if (result == JOptionPane.NO_OPTION) {
                dispose();
            }
        } else {
            dispose();
        }
    }

    private void saveCurrentFile() {
        if (currentFilePath == null) {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                currentFilePath = chooser.getSelectedFile().getAbsolutePath();
            } else {
                return;
            }
        }
        try {
            List<byte[]> rows = ((HexTableModel)hexTable.getModel()).getHexData();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            for (byte[] row : rows) baos.write(row);
            fileHandler.saveFile(currentFilePath, baos.toByteArray());
            isModified = false;
            saveButton.setEnabled(false);
            setTitle("ROM/Hex Editor - " + new File(currentFilePath).getName());
            JOptionPane.showMessageDialog(this, "File saved successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    public void openFileDialogWithPath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try {
                byte[] data = fileHandler.readFile(file.getAbsolutePath());
                List<byte[]> rows = new ArrayList<>();
                for (int i = 0; i < data.length; i += 16) {
                    int len = Math.min(16, data.length - i);
                    byte[] row = new byte[len];
                    System.arraycopy(data, i, row, 0, len);
                    rows.add(row);
                }
                tableModel.setHexData(rows);
                setTitle("ROM/Hex Editor - " + file.getName());
                currentFilePath = filePath;
                isModified = false;
                saveButton.setEnabled(false);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to open file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}