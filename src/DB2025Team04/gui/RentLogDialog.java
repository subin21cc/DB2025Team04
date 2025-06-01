package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class RentLogDialog extends JDialog {
    private JTable logTable;
    private DefaultTableModel tableModel;
    private JComboBox<Integer> pageComboBox;
    private JButton firstPageButton;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JButton lastPageButton;
    
    private int currentPage = 1;
    private int totalPages = 1;
    private final int ROWS_PER_PAGE = 20;
    
    public RentLogDialog(Frame owner, String title) {
        super(owner, title, true);
        initComponents();
        loadRentLogs(1); // 첫 페이지 로드
        setSize(800, 600);
        setLocationRelativeTo(owner);
    }
    
    private void initComponents() {
        // 패널 설정
        setLayout(new BorderLayout());
        
        // 테이블 모델 생성
        String[] columns = {"로그ID", "물품명", "분류", "사용자", "학과", "변경상태", "로그일시", "대여일", "반납예정일", "반납일", "연체일수", "비고"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 편집 불가능하게 설정
            }
        };
        
        // 테이블 생성
        logTable = new JTable(tableModel);
        
        // 가로 스크롤을 활성화하는 스크롤 패널 설정
        JScrollPane scrollPane = new JScrollPane(logTable, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // 테이블 열 너비 설정
        setColumnWidths(logTable);
        
        // 테이블의 자동 리사이징 비활성화 (가로 스크롤 사용을 위해)
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 네비게이션 패널 생성
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        firstPageButton = new JButton("<<");
        prevPageButton = new JButton("<");
        pageComboBox = new JComboBox<>();
        nextPageButton = new JButton(">");
        lastPageButton = new JButton(">>");
        
        // 버튼 액션 리스너 추가
        firstPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 1) {
                    currentPage = 1;
                    loadRentLogs(currentPage);
                }
            }
        });
        
        prevPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 1) {
                    currentPage--;
                    loadRentLogs(currentPage);
                }
            }
        });
        
        pageComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pageComboBox.getSelectedItem() != null) {
                    int selectedPage = (Integer) pageComboBox.getSelectedItem();
                    if (selectedPage != currentPage) {
                        currentPage = selectedPage;
                        loadRentLogs(currentPage);
                    }
                }
            }
        });
        
        nextPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage < totalPages) {
                    currentPage++;
                    loadRentLogs(currentPage);
                }
            }
        });
        
        lastPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage < totalPages) {
                    currentPage = totalPages;
                    loadRentLogs(currentPage);
                }
            }
        });
        
        // 네비게이션 패널에 컴포넌트 추가
        navigationPanel.add(firstPageButton);
        navigationPanel.add(prevPageButton);
        navigationPanel.add(pageComboBox);
        navigationPanel.add(nextPageButton);
        navigationPanel.add(lastPageButton);
        
        // 네비게이션 패널을 다이얼로그 상단에 추가
        add(navigationPanel, BorderLayout.NORTH);
        
        // 닫기 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 테이블 열 너비를 설정하는 메서드
     */
    private void setColumnWidths(JTable table) {
        // 각 열에 대한 적절한 너비 설정
        int[] columnWidths = {60, 150, 100, 100, 120, 80, 150, 90, 90, 90, 70, 200};
        
        for (int i = 0; i < columnWidths.length; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(columnWidths[i]);
            
            // 최소 너비 설정
            column.setMinWidth(60);
        }
    }
    
    private void loadRentLogs(int page) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getInstance().getConnection();
            
            // 총 로그 수 조회 (페이징 처리용)
            String countSql;
            if (SessionManager.getInstance().isAdmin()) {
                // 관리자는 모든 로그 조회
                countSql = "SELECT COUNT(*) FROM DB2025_RENT_LOG";
                stmt = conn.prepareStatement(countSql);
            } else {
                // 일반 사용자는 자신의 로그만 조회
                countSql = "SELECT COUNT(*) FROM DB2025_RENT_LOG WHERE user_id = ?";

                stmt = conn.prepareStatement(countSql);
                stmt.setInt(1, SessionManager.getInstance().getUserId());
            }
            
            rs = stmt.executeQuery();
            int totalRecords = 0;
            if (rs.next()) {
                totalRecords = rs.getInt(1);
            }
            
            // 총 페이지 수 계산
            totalPages = (int) Math.ceil((double) totalRecords / ROWS_PER_PAGE);
            if (totalPages == 0) totalPages = 1;
            
            // 페이지 콤보박스 업데이트
            updatePageComboBox();
            
            // 페이지 버튼 활성화/비활성화 설정
            updateNavigationButtons();
            
            // 로그 데이터 조회
            String sql;
            int offset = (page - 1) * ROWS_PER_PAGE;
            
            if (SessionManager.getInstance().isAdmin()) {
                // 관리자는 모든 로그 조회
                sql = "SELECT log_id, item_name, item_category, user_name, user_dep, " +
                        "current_status, log_date, borrow_date, due_date, return_date, " +
                        "overdue_days, note " +
                        "FROM DB2025_RENT_LOG " +
                        "ORDER BY log_date DESC " +
                        "LIMIT ? OFFSET ?";

                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, ROWS_PER_PAGE);
                stmt.setInt(2, offset);
            } else {
                // 일반 사용자는 자신의 로그만 조회
                sql = "SELECT log_id, item_name, item_category, user_name, user_dep, " +
                        "current_status, log_date, borrow_date, due_date, return_date, " +
                        "overdue_days, note " +
                        "FROM DB2025_RENT_LOG " +
                        "WHERE user_id = ? " +
                        "ORDER BY log_id DESC " +
                        "LIMIT ? OFFSET ?";



                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, SessionManager.getInstance().getUserId());
                stmt.setInt(2, ROWS_PER_PAGE);
                stmt.setInt(3, offset);
            }
            
            rs = stmt.executeQuery();
            
            // 테이블 모델 초기화
            tableModel.setRowCount(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("log_id"));
                row.add(rs.getString("item_name"));
                row.add(rs.getString("item_category"));
                row.add(rs.getString("user_name"));
                row.add(rs.getString("user_dep"));
                row.add(rs.getString("current_status"));
                
                // 날짜 형식 처리
                java.sql.Timestamp logDate = rs.getTimestamp("log_date");
                row.add(logDate != null ? dateTimeFormat.format(logDate) : "-");
                
                java.sql.Date borrowDate = rs.getDate("borrow_date");
                row.add(borrowDate != null ? dateFormat.format(borrowDate) : "-");
                
                java.sql.Date dueDate = rs.getDate("due_date");
                row.add(dueDate != null ? dateFormat.format(dueDate) : "-");
                
                java.sql.Date returnDate = rs.getDate("return_date");
                row.add(returnDate != null ? dateFormat.format(returnDate) : "-");
                
                Integer overdueDays = rs.getInt("overdue_days");
                row.add(rs.wasNull() ? "-" : overdueDays);
                
                row.add(rs.getString("note"));
                
                tableModel.addRow(row);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "대여 로그 조회 중 오류가 발생했습니다: " + e.getMessage(),
                "데이터베이스 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }
    
    private void updatePageComboBox() {
        pageComboBox.removeAllItems();
        for (int i = 1; i <= totalPages; i++) {
            pageComboBox.addItem(i);
        }
        pageComboBox.setSelectedItem(currentPage);
    }
    
    private void updateNavigationButtons() {
        firstPageButton.setEnabled(currentPage > 1);
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
        lastPageButton.setEnabled(currentPage < totalPages);
    }
}