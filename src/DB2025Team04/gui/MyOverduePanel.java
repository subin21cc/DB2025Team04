package DB2025Team04.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MyOverduePanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton returnButton;

    public MyOverduePanel() {
        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        // Table
        String[] columns = {"ID", "분류", "이름", "대여자", "대여일"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent editing
            }
        };
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        returnButton = new JButton("반납");
        returnButton.setEnabled(false); // 초기 상태에서는 버튼 비활성화

        buttonPanel.add(returnButton);
        add(buttonPanel, BorderLayout.SOUTH);

        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {  // 이벤트가 조정 중이 아닐 때만 처리
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {  // 선택된 행이 있는 경우
                    returnButton.setEnabled(true);
                } else {  // 선택된 행이 없는 경우 모두 비활성화
                    returnButton.setEnabled(false);
                }
            }
        });
    }
}
