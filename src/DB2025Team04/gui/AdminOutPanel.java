package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Vector;

/*
관리자 출고(대여신청 승인/취소) 패널 클래스
대여신청 상태의 대여 목록을 조회하고, 출고 완료/취소 처리를 할 수 있다.
 */
public class AdminOutPanel extends JPanel {
    private JTable itemTable; // 대여신청 목록을 보여주는 테이블
    private DefaultTableModel tableModel; // 테이블에 표시될 데이터 모델
    private JButton doneButton; // 출고 완료 버튼
    private JButton cancelButton; // 출고 취소 버튼

    /*
    패널 생성자. 레이아웃 설정 및 UI 컴포넌트 초기화.
    */
    public AdminOutPanel() {
        setLayout(new BorderLayout());
        initComponents(); // UI 컴포넌트 초기화
    }

    /*
    UI 컴포넌트(테이블, 버튼 등) 초기화 및 이벤트 리스너 등록
    */
    private void initComponents() {
        // 테이블 컬럼명 정의: 대여ID, 분류, 이름, 대여자, 대여일
        String[] columns = {"대여ID", "분류", "이름", "대여자", "대여일"};
        // 테이블 모델 생성(셀 편집 불가)
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 셀 편집 비활성화
            }
        };
        // 테이블 생성 및 단일 행 선택만 허용
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 테이블 스크롤 패널 생성
        JScrollPane scrollPane = new JScrollPane(itemTable);
        add(scrollPane, BorderLayout.CENTER);

        // 테이블 행 선택 시 버튼 활성화/비활성화 처리
        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {  // 이벤트가 조정 중이 아닐 때만 처리
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {  // 선택된 행이 있는 경우
                    doneButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                } else {  // 선택된 행이 없는 경우 모두 비활성화
                    doneButton.setEnabled(false); // 버튼 비활성화
                    cancelButton.setEnabled(false);
                }
            }
        });

        // 버튼 패널 생성 및 버튼 추가
        JPanel buttonPanel = new JPanel();
        doneButton = new JButton("대여 승인");
        cancelButton = new JButton("대여 취소");

        // 초기 상태에서는 버튼 비활성화
        doneButton.setEnabled(false);
        cancelButton.setEnabled(false);

        buttonPanel.add(doneButton);
        buttonPanel.add(cancelButton);

        // 대여 승인 버튼 클릭 시 이벤트 처리
        doneButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int rentId = (int) tableModel.getValueAt(selectedRow, 0);
                // 대여 승인
                if (DatabaseManager.getInstance().processOutDone(rentId)) {
                    JOptionPane.showMessageDialog(this, "대여 신청이 승인되었습니다.");
                    loadItemList(); // 목록 새로고침
                } else {
                    JOptionPane.showMessageDialog(this, "승인 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // 행이 선택되지 않은 경우 경고 메시지
                JOptionPane.showMessageDialog(this, "승인할 대여 신청을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
            }
        });

        // 대여 취소 버튼 클릭 시 이벤트 처리
        cancelButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int rentId = (int) tableModel.getValueAt(selectedRow, 0);
                // 대여 취소
                if (DatabaseManager.getInstance().processDeleteRent(rentId)) {
                    JOptionPane.showMessageDialog(this, "대여 신청이 취소되었습니다.");
                    loadItemList(); // 목록 새로고침
                } else {
                    JOptionPane.showMessageDialog(this, "취소 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // 행이 선택되지 않은 경우 경고 메시지
                JOptionPane.showMessageDialog(this, "취소할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
            }
        });

        // 버튼 패널을 하단에 추가
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /*
    대여신청 상태의 대여 목록을 DB에서 불러와 테이블에 표시
    (출고 완료/취소 후에도 호출하여 목록을 갱신)
    */
    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // DB 연결 객체 획득
            conn = DatabaseManager.getInstance().getConnection();

            // 대여신청 상태의 대여 목록 조회 쿼리
            // idx_rent_status 인덱스 사용
            String sql = "SELECT r.rent_id, category, item_name, borrow_date, u.user_name " +
                    "FROM DB2025_ITEMS i, DB2025_RENT r USE INDEX (idx_rent_status), DB2025_USER u " +
                    "WHERE i.item_id = r.item_id AND r.user_id = u.user_id AND r.rent_status='대여신청' " +
                    "ORDER BY borrow_date DESC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // 테이블 데이터 초기화
            tableModel.setRowCount(0);
            // 날짜 포맷 지정(yyyy-MM-dd)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            // 결과 집합을 한 행씩 읽어 테이블에 추가
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("rent_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(rs.getString("user_name")); // 대여일(yyyy-MM-dd)
                row.add(dateFormat.format(rs.getDate("borrow_date")));
                String returnDate;

                tableModel.addRow(row); // 테이블에 행 추가
            }
        } catch (SQLException e) {
            // DB 오류 발생 시 에러 메시지 출력
            JOptionPane.showMessageDialog(this, "Error loading my rent status: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            // DB 리소스 해제
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

}
