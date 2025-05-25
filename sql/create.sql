-- MySQL 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS DB2025Team04
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- MySQL 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'DB2025Team04'@'localhost' IDENTIFIED BY 'DB2025Team04';
CREATE USER IF NOT EXISTS 'DB2025Team04'@'%' IDENTIFIED BY 'DB2025Team04';

-- 로컬 호스트에서 접속하는 사용자에게 모든 권한 부여
GRANT ALL PRIVILEGES ON DB2025Team04.* TO 'DB2025Team04'@'localhost';

-- 원격 접속 사용자에게 모든 권한 부여
GRANT ALL PRIVILEGES ON DB2025Team04.* TO 'DB2025Team04'@'%';

-- 권한 설정 적용
FLUSH PRIVILEGES;

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

-- 대여 로그 인덱스 생성
CREATE INDEX idx_rent_log_rent_id ON DB2025_RENT_LOG (rent_id);
CREATE INDEX idx_rent_log_user_id ON DB2025_RENT_LOG (user_id);
CREATE INDEX idx_rent_log_item_id ON DB2025_RENT_LOG (item_id);
CREATE INDEX idx_rent_log_date ON DB2025_RENT_LOG (log_date);
CREATE INDEX idx_rent_log_status ON DB2025_RENT_LOG (previous_status, current_status);
CREATE INDEX idx_rent_log_op_type ON DB2025_RENT_LOG (operation_type);


-- 뷰 생성
-- 대여 물품 현황 뷰: 전체 수량, 사용 가능 수량과 현재 대여 중인 수를 함께 표시
CREATE VIEW VIEW_RENTAL_ITEMS_STATUS AS
SELECT
    I.item_id,
    I.item_name,
    I.category,
    I.quantity              AS total_quantity,
    I.available_quantity    AS available_quantity,
    -- 대여중·연체중인 건수 집계
    COALESCE(SUM(
                     CASE WHEN R.rent_status IN ('대여중','연체중') THEN 1 ELSE 0 END
             ), 0)                 AS rented_count
FROM DB2025_ITEMS I
         LEFT JOIN DB2025_RENT R
                   ON I.item_id = R.item_id
GROUP BY
    I.item_id,
    I.item_name,
    I.category,
    I.quantity,
    I.available_quantity
;

-- 예약 현황 뷰 수정: 예약 정보와 대여 현황을 함께 표시
CREATE VIEW VIEW_RESERVATION_OVERVIEW AS
SELECT
    R.reservation_id,
    R.user_id,
    U.user_name,
    U.user_dep,
    I.item_id,
    I.item_name,
    I.category,
    I.available_quantity,
    I.quantity,
    R.reserve_date,
    -- 예약 상태 정보
    CASE
        -- 물품 상태에 따른 처리 가능성
        WHEN I.available_quantity > 0 THEN '대여가능'
        -- 대기 순서 계산
        ELSE CONCAT('대기번호: ', (
            SELECT COUNT(*) + 1 FROM DB2025_RESERVATION RS
            WHERE RS.item_id = R.item_id
              AND RS.reserve_date < R.reserve_date
        ))
        END AS reservation_status,

    -- 대여 현황 정보 (대여 중인 물품의 총 수)
    (
        SELECT COUNT(*)
        FROM DB2025_RENT RT
        WHERE RT.item_id = I.item_id
          AND RT.rent_status IN ('대여중', '연체중')
    ) AS current_rentals,

    -- 다음 반납 예정일 (가장 빠른 반납 예정일)
    (
        SELECT MIN(RT.due_date)
        FROM DB2025_RENT RT
        WHERE RT.item_id = I.item_id
          AND RT.rent_status IN ('대여중', '연체중')
    ) AS next_due_date,

    -- 예약 경과일 계산
    DATEDIFF(CURRENT_DATE, R.reserve_date) AS days_since_reservation,

    -- 예약 우선순위
    (
        SELECT COUNT(*) + 1
        FROM DB2025_RESERVATION RS
        WHERE RS.item_id = R.item_id
          AND RS.reserve_date < R.reserve_date
    ) AS priority_order,

    -- 예약 대기 수
    (
        SELECT COUNT(*)
        FROM DB2025_RESERVATION RS
        WHERE RS.item_id = R.item_id
    ) AS total_reservations,

    -- 현재 대여자 목록 (최대 3명)
    (
        SELECT GROUP_CONCAT(RU.user_name ORDER BY RT.due_date SEPARATOR ', ')
        FROM DB2025_RENT RT
                 JOIN DB2025_USER RU ON RT.user_id = RU.user_id
        WHERE RT.item_id = I.item_id
          AND RT.rent_status IN ('대여중', '연체중')
        LIMIT 3
    ) AS current_renters

FROM
    DB2025_RESERVATION R
        JOIN
    DB2025_USER U ON R.user_id = U.user_id
        JOIN
    DB2025_ITEMS I ON R.item_id = I.item_id
ORDER BY
    I.category,           -- 물품 분류로 그룹화
    I.item_name,          -- 물품 이름으로 정렬
    R.reserve_date        -- 예약일 순서대로 정렬
;


-- 사용자별 대여 현황 뷰: 각 사용자가 현재 몇 건을 대여 중인지, 연체 건수와 대여 물품명을 나열
CREATE VIEW VIEW_USER_RENTAL_OVERVIEW AS
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
CREATE INDEX idx_item_category ON DB2025_ITEMS (category);
CREATE INDEX idx_rent_user_status_due ON DB2025_RENT (user_id, rent_status, due_date);

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
    (3006, '디지털카메라', 4, 1, '전자기기'),
    (3007, '볼펜', 3, 2, '필기구'),
    (3008, '지우개', 2, 1, '필기구'),
    (3009, '컴퓨터사인펜', 12,  12, '필기구');

-- 대여 데이터
-- 현재 날짜 기준으로 다양한 대여 상태의 데이터 삽입
INSERT INTO DB2025_RENT (item_id, user_id, borrow_date, return_date, rent_status) VALUES
    -- 1. 대여 기간 내 - 정상 대여중
    (3001, 2025001, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), NULL, '대여중'),

    -- 2. 정상 반납 완료
    (3002, 2025002, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), '반납완료'),

    -- 3. 연체중 상태 - 이미 처리된 상태
    (3004, 2025003, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL, '연체중'),

    -- 4. 대여신청 상태
    (3005, 2025005, CURRENT_DATE, NULL, '대여신청'),

    -- 5. 정상 반납 완료 - 예약자가 대여할 물품
    (3006, 2025006, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), '반납완료'),

    -- 6. 대여중 - 대여 불가 사용자
    (3007, 2025007, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), NULL, '대여중'),

    -- 7. 연체중 상태 - 이미 처리된 상태
    (3008, 2025008, DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), NULL, '연체중'),

    -- 8. 대여신청 상태 - 미래 날짜
    (3009, 2025009, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), NULL, '대여신청'),

    -- 9. 정상 반납 완료
    (3003, 2025010, DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), '반납완료'),

    -- 10. 정상 대여중
    (3003, 2025004, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), NULL, '대여중'),

    -- 11. 연체 후 반납 완료
    (3002, 2025008, DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), '연체반납'),

    -- 12. 정상 대여중
    (3006, 2025002, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), NULL, '대여중'),

    -- 13. [테스트 케이스 1] 오늘 연체될 항목 (정확히 7일 지난 상태)
    -- processAutoTask에서 연체중으로 변경될 예정
    (3001, 2025010, DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), NULL, '대여중'),

    -- 14. 대여신청 상태 - 오늘 신청
    (3004, 2025001, CURRENT_DATE, NULL, '대여신청'),

    -- 15. [테스트 케이스 2] 내일 연체 예정인 항목 - 이건 연체로 처리되지 않아야 함
    (3005, 2025004, DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY), NULL, '대여중'),

    -- 16. [테스트 케이스 3] 연체로 처리될 항목 (8일 전 대여, 아직 '대여중' 상태)
    -- processAutoTask 실행 시 '연체중'으로 변경되어야 함
    (3001, 2025002, DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), NULL, '대여중'),

    -- 17. [테스트 케이스 4] 심각한 연체 상태 (15일 전 대여, 아직 '대여중' 상태)
    -- processAutoTask 실행 시 '연체중'으로 변경되고, 사용자가 '대여불가' 상태로 변경되어야 함
    (3002, 2025005, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), NULL, '대여중');

-- 예약 데이터 추가 - 자동 처리 테스트를 위한 예약 상태
INSERT INTO DB2025_RESERVATION (user_id, item_id, reserve_date) VALUES
    -- 1. [테스트 케이스 5] 대여가능 수량이 0인 물품에 대한 예약 (물품 id 3004)
    -- 연체 처리될 사용자(2025005)의 예약 - processAutoTask 실행 시 취소되어야 함
    (2025005, 3004, DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY)),

    -- 2. [테스트 케이스 6] 다른 사용자의 예약 - 정상 유지되어야 함
    (2025006, 3004, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY)),

    -- 3. [테스트 케이스 7] 연체 처리될 사용자(2025005)의 다른 예약 - 모두 취소되어야 함
    (2025005, 3003, CURRENT_DATE),

    -- 4. [테스트 케이스 8] 대여가능 수량이 있는 물품에 대한 예약
    -- processAutoTask 실행 시 대여신청으로 전환되어야 함
    (2025009, 3001, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY)),

    -- 5. [테스트 케이스 9] 대여불가 사용자(2025007)의 예약
    -- processAutoTask 실행 시 취소되어야 함
    (2025007, 3005, DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY)),

    -- 6. [테스트 케이스 10] 대여가능 수량이 없지만 예약 순서가 뒤인 경우
    -- 유지되어야 함
    (2025001, 3004, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY));
