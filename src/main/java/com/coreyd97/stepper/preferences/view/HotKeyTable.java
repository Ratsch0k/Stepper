package com.coreyd97.stepper.preferences.view;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

import com.coreyd97.BurpExtenderUtilities.Preferences;

/**
 * Table to configure hotkeys.
 */
public class HotKeyTable extends JTable {
    Row[] data;
    final Preferences preferences;

    public HotKeyTable(Preferences preferences, Row[] data) {
        super(new HotKeyModel(preferences, data, new String[]{"Description", "Hotkey"}));

        HotKeyModel hotKeyModel = (HotKeyModel) this.getModel();
        this.preferences = preferences;
        this.data = data;

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();

                int row = table.rowAtPoint(point);

                if (e.getClickCount() == 2 && table.getSelectedRow() != -1 && row != -1) {
                    int selectedRow = table.convertRowIndexToModel(row);

                    HotKeyDialog dialog = new HotKeyDialog();
                    HotKeyDialog.Result newHotKey = dialog.prompt();

                    if (newHotKey instanceof HotKeyDialog.ApplyResult) {
                        hotKeyModel.updateHotKey(selectedRow, ((HotKeyDialog.ApplyResult)newHotKey).getValue());
                    }
                }
            }
        });

        InputMap inputMap = this.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = this.getActionMap();

        actionMap.put("ChangeRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = HotKeyTable.this.getSelectedRow();

                if (selectedRow < 0) {
                    return;
                }

                HotKeyDialog dialog = new HotKeyDialog();
                HotKeyDialog.Result newHotKey = dialog.prompt();

                if (newHotKey instanceof HotKeyDialog.ApplyResult) {
                    hotKeyModel.updateHotKey(selectedRow, ((HotKeyDialog.ApplyResult)newHotKey).getValue());
                }
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ChangeRow");
    }

    /**
     * Representation of one table row.
     */
    public static class Row {
        private String name;
        private String preferenceName;

        public Row(String name, String preferenceName) {
            this.name = name;
            this.preferenceName = preferenceName;
        }
    }

    /**
     * Custom table model thata works with the custom row representation and
     * allows working with preferences.
     */
    public static class HotKeyModel extends DefaultTableModel {
        Row[] data;
        String[] columnNames;
        final Preferences preferences;

        public HotKeyModel(Preferences preferences, Row[] data, String[] columnNames) {
            this.preferences = preferences;
            this.data = data;
            this.columnNames = columnNames;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            Row rowElement = this.data[row];

            switch (column) {
                case 0:
                    return rowElement.name;
                case 1:
                    return this.preferences.getSetting(rowElement.preferenceName);
                default:
                    return "Unknown value";
            }
        }

        @Override
        public String getColumnName(int column) {
            return this.columnNames[column];
        }

        @Override
        public int getRowCount() {
            if (this.data != null) {
                return this.data.length;
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        public Row getRow(int row) {
            return this.data[row];
        }

        public void updateHotKey(int row, String newValue) {
            String name = this.data[row].preferenceName;

            preferences.setSetting(name, newValue);

            this.fireTableDataChanged();
        }
    }
}
