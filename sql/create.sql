-- DB2025Team04 데이터베이스 생성 스크립트
DROP DATABASE IF EXISTS DB2025Team04;
DROP USER IF EXISTS 'DB2025Team04'@'localhost';
CREATE USER 'DB2025Team04'@'localhost' IDENTIFIED WITH mysql_native_password BY 'DB2025Team04';
CREATE DATABASE DB2025Team04;
GRANT ALL PRIVILEGES ON DB2025Team04.* TO 'DB2025Team04'@'localhost' WITH GRANT OPTION;
COMMIT;

-- 데이터베이스 사용 선언
USE DB2025Team04;

-- 테이블 생성
-- 사용자 테이블
CREATE TABLE DB2025_USER
(
    user_id            INT PRIMARY KEY,
    user_pw            VARCHAR(64)          NOT NULL,
    user_name          VARCHAR(50)          NOT NULL,
    user_dep           VARCHAR(50)          NOT NULL CHECK (user_dep REGEXP '학과$|^전공미진입$'),
    user_phone         VARCHAR(20) UNIQUE   NOT NULL,
    user_restrict_date DATE                          DEFAULT NULL, -- 대여 제한일 (NULL이면 대여가능)
    user_status        ENUM ('대여가능','대여불가') NOT NULL DEFAULT '대여가능'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 관리자 테이블
CREATE TABLE DB2025_ADMIN
(
    admin_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id  INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES DB2025_USER (user_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 물품 테이블
CREATE TABLE DB2025_ITEMS
(
    item_id            INT PRIMARY KEY,
    item_name          VARCHAR(20) NOT NULL,
    quantity           INT         NOT NULL CHECK (quantity >= 0),
    available_quantity INT         NOT NULL CHECK (available_quantity >= 0),
    category           VARCHAR(20)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 대여 테이블
CREATE TABLE DB2025_RENT
(
    rent_id     INT PRIMARY KEY AUTO_INCREMENT,
    item_id     INT                      NOT NULL,
    user_id     INT                      NOT NULL,
    borrow_date DATE                     NOT NULL DEFAULT (CURRENT_DATE),
    due_date    DATE GENERATED ALWAYS AS (borrow_date + INTERVAL 7 DAY) STORED,
    return_date DATE,
    rent_status ENUM ('대여신청','대여중','반납완료','연체중', '연체반납') NOT NULL DEFAULT '대여신청',
    FOREIGN KEY (item_id) REFERENCES DB2025_ITEMS (item_id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES DB2025_USER (user_id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 예약 테이블
CREATE TABLE DB2025_RESERVATION
(
    reservation_id  INT PRIMARY KEY AUTO_INCREMENT,
    user_id         INT  NOT NULL,
    item_id         INT  NOT NULL,
    reserve_date    DATE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES DB2025_USER (user_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES DB2025_ITEMS (item_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


-- 대여 로그 테이블 (외래키 제약조건 없음, 모든 데이터 직접 저장)
CREATE TABLE DB2025_RENT_LOG
(
    log_id             INT PRIMARY KEY AUTO_INCREMENT,
    rent_id            INT,                                                                                        -- 참조용 rent_id (외래키 없음, NULL 가능)
    item_id            INT,                                                                                        -- 참조용 item_id (외래키 없음, NULL 가능)
    user_id            INT                                                     NOT NULL,                           -- 참조용 ID (외래키 없음)
    admin_id           INT,                                                                                        -- 처리 관리자 ID (외래키 없음)
    previous_status    ENUM ('대여신청','대여중','반납완료','연체중','연체반납', '대여가능', '대여불가'),                                    -- 이전 상태
    current_status     ENUM ('대여신청','대여중','반납완료','연체중','연체반납', '대여가능', '대여불가', '예약취소') NOT NULL,                           -- 변경된 상태
    log_date           DATETIME                                                NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 로그 생성 시간
    borrow_date        DATE,                                                                                       -- 대여일
    due_date           DATE                                                    ,                           -- 반납예정일
    return_date        DATE,                                                                                       -- 실제 반납일
    overdue_days       INT,                                                                                        -- 연체일수
    note               VARCHAR(200),                                                                               -- 추가 설명
    client_ip          VARCHAR(45),                                                                                -- 요청 IP 주소
    item_name          VARCHAR(50)                                             ,                           -- 물품명 (직접 저장)
    item_category      VARCHAR(20),                                                                                -- 물품 분류 (직접 저장)
    user_name          VARCHAR(50)                                             NOT NULL,                           -- 사용자명 (직접 저장)
    user_dep           VARCHAR(50)                                             NOT NULL,                           -- 사용자 학과 (직접 저장)
    admin_name         VARCHAR(50),                                                                                -- 관리자명 (직접 저장)
    available_quantity INT,                                                                                        -- 처리 당시 물품 가용 수량
    total_quantity     INT,                                                                                        -- 처리 당시 물품 총 수량
    operation_type     ENUM ('생성','수정','삭제','시스템처리')                           NOT NULL,                           -- 작업 유형
    created_by         VARCHAR(50)                                             NOT NULL DEFAULT 'SYSTEM'           -- 로그 생성 주체
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


-- 뷰 생성

-- 대여 현황 뷰: 대여 상태별로 물품의 대여 현황을 집계
CREATE VIEW DB2025_VIEW_USER_RENT_STATUS AS
SELECT
    r.rent_id,
    r.item_id,
    r.user_id,
    i.category,
    i.item_name,
    r.borrow_date,
    r.return_date,
    r.rent_status,
    CASE
        WHEN r.rent_status = '연체중' THEN
            DATEDIFF(COALESCE(r.return_date, CURRENT_DATE), r.borrow_date)
        ELSE NULL
        END as elapsed_days
FROM DB2025_RENT r
         JOIN DB2025_ITEMS i ON i.item_id = r.item_id;

-- 사용자별 대여 현황 뷰: 각 사용자가 현재 몇 건을 대여 중인지, 연체 건수와 대여 물품명을 나열
CREATE VIEW DB2025_VIEW_USER_RENTAL_OVERVIEW AS
SELECT
    U.user_id,
    U.user_name,
    -- ‘대여중’ 또는 ‘연체중’인 건수
    COALESCE(SUM(
                     CASE WHEN R.rent_status IN ('대여중','연체중') THEN 1 ELSE 0 END
             ), 0) AS active_rental_count,
    -- 연체 중인 건수
    COALESCE(SUM(
                     CASE WHEN R.rent_status = '연체중' THEN 1 ELSE 0 END
             ), 0) AS overdue_count,
    -- 현재 대여 중인 물품명을 쉼표로 연결
    GROUP_CONCAT(
        DISTINCT I.item_name
      ORDER BY I.item_name
      SEPARATOR ', '
    ) AS rented_items
FROM DB2025_USER U,
     DB2025_RENT R,
     DB2025_ITEMS I
WHERE U.user_id = R.user_id
AND R.item_id = I.item_id
AND R.rent_status IN ('대여중', '연체중') -- 대여 중이거나 연체 중인 상태만 포함
GROUP BY
    U.user_id,
    U.user_name
;


-- 인덱스 생성
CREATE INDEX idx_rent_status ON DB2025_RENT (rent_status);
CREATE INDEX idx_item_user_status ON DB2025_RENT (item_id, user_id, rent_status);
CREATE INDEX idx_item_status ON DB2025_RENT (item_id, rent_status);
CREATE INDEX idx_item_user ON DB2025_RESERVATION (item_id, user_id);
CREATE INDEX idx_user_status ON DB2025_USER (user_id, user_status);
CREATE INDEX idx_user_phone ON DB2025_USER (user_phone);


-- 사용자 데이터
INSERT INTO DB2025_USER (user_id, user_pw, user_name, user_dep, user_phone, user_status) VALUES
    (2025001, SHA2('pw1234',256), '김철수', '컴퓨터공학과', '010-1111-2222', '대여가능'),
    (2025002, SHA2('pw5678',256), '이영희', '전자공학과', '010-2222-3333', '대여가능'),
    (2025003, SHA2('pwabcd',256), '박민수', '기계공학과', '010-3333-4444', '대여불가'),
    (2025004, SHA2('pwefgh',256), '최지은', '경영학과', '010-4444-5555', '대여가능'),
    (2025005, SHA2('pw0005',256), '박영희', '화학과',     '010-5555-6666', '대여가능'),
    (2025006, SHA2('pw0006',256), '최민호', '물리학과',   '010-6666-7777', '대여가능'),
    (2025007, SHA2('pw0007',256), '정수연', '수학과',     '010-7777-8888', '대여불가'),
    (2025008, SHA2('pw0008',256), '이준호', '경제학과',   '010-8888-9999', '대여가능'),
    (2025009, SHA2('pw0009',256), '김하나', '심리학과',   '010-9999-0000', '대여가능'),
    (2025010, SHA2('pw0010',256), '한지민', '디자인학과', '010-1010-2020', '대여가능');

-- 관리자 데이터
INSERT INTO DB2025_ADMIN (user_id) VALUES
    (2025001),
    (2025004),
    (2025006);

-- 대여물품 데이터
INSERT INTO DB2025_ITEMS (item_id, item_name, quantity, available_quantity, category) VALUES
    (3001, '노트북', 10, 7, '전자기기'),
    (3002, '빔프로젝터', 5, 2, '전자기기'),
    (3003, '공학용계산기', 20, 15, '학습기기'),
    (3004, '회의용마이크', 8,  0, '음향기기'),
    (3005, '태블릿', 7, 5, '전자기기'),
    (3006, '디지털카메라', 4, 0, '전자기기'),
    (3007, '볼펜', 3, 2, '필기구'),
    (3008, '지우개', 1, 0, '필기구'),
    (3009, '컴퓨터사인펜', 12,  12, '필기구');

-- 대여 데이터
-- 현재 날짜 기준으로 다양한 대여 상태의 데이터 삽입

-- 예약 데이터 삽입
INSERT INTO DB2025_RESERVATION (user_id, item_id, reserve_date) VALUES
    (2025009, 3008, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY)),
    -- [테스트 케이스 1] 연체된 사람에 대한 예약
    -- processAutoTask 실행 시 예약 취소되어야 함
    (2025001, 3004, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY));   -- 2025001: 회의용마이크 예약


-- 대여 데이터 삽입
INSERT INTO DB2025_RENT (item_id, user_id, borrow_date, return_date, rent_status) VALUES
  -- 노트북(3001): 총 10개, 가용 7개 -> 3개 대여중
  (3001, 2025001, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL, '연체중'),    -- 2025001: 연체중
  (3001, 2025002, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), NULL, '대여중'),
  (3001, 2025004, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), NULL, '대여중'),
  (3001, 2025005, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), '반납완료'),

  -- 빔프로젝터(3002): 총 5개, 가용 2개 -> 2개 대여중, 1개 연체중
  (3002, 2025001, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), NULL, '대여중'),    -- 2025001: 대여중
  -- [테스트 케이스 3] 심각한 연체 상태 (15일 전 대여, 아직 '대여중' 상태)
  -- processAutoTask 실행 시 '연체중'으로 변경되고, 사용자가 '대여불가' 상태로 변경되어야 함
  (3002, 2025005, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), NULL, '대여중'),
  (3002, 2025008, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL, '연체중'),
  (3002, 2025009, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), '연체반납'),

  -- 공학용계산기(3003): 총 20개, 가용 15개 -> 5개 대여중
  -- [테스트 케이스 4] 오늘 연체될 항목 (정확히 7일 지난 상태)
  -- processAutoTask에서 연체중으로 변경될 예정
  (3003, 2025001, DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), NULL, '대여중'),
  (3003, 2025003, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), NULL, '대여중'),
  (3003, 2025004, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), NULL, '대여중'),
  (3003, 2025005, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), NULL, '대여중'),
  (3003, 2025006, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), NULL, '대여중'),
  (3003, 2025007, DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), '반납완료'),

  -- 회의용마이크(3004): 총 8개, 가용 0개 -> 6개 대여중, 2개 대여신청
  (3004, 2025001, CURRENT_DATE, NULL, '대여신청'),    -- 2025001: 대여신청
  (3004, 2025007, DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY), NULL, '대여중'),
  (3004, 2025008, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), NULL, '대여중'),
  (3004, 2025009, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), NULL, '대여중'),
  (3004, 2025010, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), NULL, '대여중'),
  (3004, 2025002, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), NULL, '대여중'),
  (3004, 2025003, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), NULL, '대여중'),
  (3004, 2025005, CURRENT_DATE, NULL, '대여신청'),

  -- 태블릿(3005): 총 7개, 가용 5개 -> 1개 대여중, 1개 연체중
  -- [테스트 케이스 5] 연체로 처리될 항목 (8일 전 대여, 아직 '대여중' 상태)
  -- processAutoTask 실행 시 '연체중'으로 변경되어야 함
  (3005, 2025001, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL, '대여중'),
  (3005, 2025006, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL, '연체중'),
  (3005, 2025007, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), '반납완료'),

  -- 디지털카메라(3006): 총 4개, 가용 0개 -> 3개 대여중, 1개 연체중
  (3006, 2025008, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), NULL, '대여중'),
  (3006, 2025009, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), NULL, '대여중'),
  (3006, 2025010, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL, '연체중'),
  (3006, 2025007, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), NULL, '대여중'),

  -- 볼펜(3007): 총 3개, 가용 2개 -> 1개 대여중
  (3007, 2025002, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), NULL, '대여중'),
  (3007, 2025003, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), '반납완료'),

  -- 지우개(3008): 총 2개, 가용 1개 -> 1개 대여중
  (3008, 2025003, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), NULL, '대여중'),

  -- 컴퓨터사인펜(3009): 총 12개, 가용 12개 -> 모두 가용
  (3009, 2025004, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), '반납완료'),
  (3009, 2025005, DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), '반납완료');
