package DB2025Team04.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MyReservationPanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton cancelButton;

    public MyReservationPanel() {
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
        cancelButton = new JButton("예약 취소");
        cancelButton.setEnabled(false); // 초기 상태에서는 버튼 비활성화

        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {  // 이벤트가 조정 중이 아닐 때만 처리
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {  // 선택된 행이 있는 경우
                    cancelButton.setEnabled(true);
                } else {  // 선택된 행이 없는 경우 모두 비활성화
                    cancelButton.setEnabled(false);
                }
            }
        });
    }

    public void loadItemList() {
        // 이 메서드는 DB에서 예약 목록을 불러오는 로직을 구현해야 합니다.
        // 예시로 더미 데이터를 추가합니다.
        tableModel.setRowCount(0); // 기존 데이터 초기화
        Object[] rowData = {1, "도서", "Java Programming", "홍길동", "2023-10-01"};
        tableModel.addRow(rowData);
    }
}
