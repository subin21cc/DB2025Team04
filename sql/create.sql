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
    user_id     INT PRIMARY KEY,
    user_pw     VARCHAR(64)          NOT NULL,
    user_name   VARCHAR(50)          NOT NULL,
    user_dep    VARCHAR(50)          NOT NULL CHECK (user_dep REGEXP '학과$|^전공미진입$'
        ),
    user_phone  VARCHAR(20) UNIQUE   NOT NULL,
    user_status ENUM ('대여가능','대여불가') NOT NULL DEFAULT '대여가능'
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
    restriction_end DATE,
    FOREIGN KEY (user_id) REFERENCES DB2025_USER (user_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES DB2025_ITEMS (item_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 연체 테이블
CREATE TABLE DB2025_OVERDUES
(
    overdue_id      INT PRIMARY KEY AUTO_INCREMENT,
    user_id         INT  NOT NULL,
    item_id         INT  NOT NULL,
    original_due    DATE NOT NULL,
    overdue_days    INT  NOT NULL CHECK (overdue_days >= 0),
    restriction_end DATE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES DB2025_USER (user_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES DB2025_ITEMS (item_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


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

-- 예약 현황 뷰: 예약 정보와 함께 연체나 재고 부족으로 인한 예약 가능 여부를 표시
CREATE VIEW VIEW_RESERVATION_OVERVIEW AS
SELECT
    R.reservation_id,
    U.user_id,
    U.user_name,
    I.item_id,
    I.item_name,
    R.reserve_date,
    R.restriction_end,
    CASE
        WHEN O.restriction_end IS NOT NULL
            AND O.restriction_end > CURRENT_DATE
            THEN '예약불가(연체중)'
        WHEN I.available_quantity > 0
            THEN '대여가능'
        ELSE '예약가능'
        END AS reservation_status
FROM DB2025_RESERVATION R
         JOIN DB2025_USER U
              ON R.user_id = U.user_id
         JOIN DB2025_ITEMS I
              ON R.item_id = I.item_id
         LEFT JOIN DB2025_OVERDUES O
                   ON R.user_id = O.user_id
                       AND R.item_id = O.item_id
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
FROM DB2025_USER U
         LEFT JOIN DB2025_RENT R
                   ON U.user_id = R.user_id
         LEFT JOIN DB2025_ITEMS I
                   ON R.item_id = I.item_id
GROUP BY
    U.user_id,
    U.user_name
;


-- 인덱스 생성
CREATE INDEX idx_rent_status ON DB2025_RENT (rent_status);
CREATE INDEX idx_overdue_user_id ON DB2025_OVERDUES (user_id);
CREATE INDEX idx_item_category ON DB2025_ITEMS (category);
CREATE INDEX idx_overdues_user_item_restriction ON DB2025_OVERDUES (user_id, item_id, restriction_end);
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
INSERT INTO DB2025_RENT (item_id, user_id, borrow_date, return_date, rent_status) VALUES
    (3001, 2025001, '2025-05-01', NULL, '대여중'),
    (3002, 2025002, '2025-04-25', '2025-05-02', '반납완료'),
    (3004, 2025003, '2025-04-20', NULL, '연체중'),
    (3005, 2025005, '2025-05-10', NULL, '대여신청'),
    (3006, 2025006, '2025-05-05', '2025-05-12', '반납완료'),
    (3007, 2025007, '2025-05-12', NULL, '대여중'),
    (3008, 2025008, '2025-04-28', NULL, '연체중'),
    (3009, 2025009, '2025-05-15', NULL, '대여신청'),
    (3003, 2025010, '2025-05-10', '2025-05-18', '반납완료'),
    (3003, 2025004, '2025-05-14', NULL, '대여중'),
    (3002, 2025008, '2025-04-22', '2025-05-01', '연체반납'),
    (3006, 2025002, '2025-05-03', NULL, '대여중');

-- 예약 데이터
INSERT INTO DB2025_RESERVATION (user_id, item_id, reserve_date, restriction_end) VALUES
    (2025004, 3001, '2025-05-03', NULL),
    (2025002, 3004, '2025-05-04', '2025-05-25'),
    (2025003, 3003, '2025-05-05', NULL),
    (2025005, 3002, '2025-05-06', NULL),
    (2025006, 3008, '2025-05-07', '2025-05-20'),
    (2025007, 3009, '2025-05-08', NULL),
    (2025008, 3001, '2025-05-09', NULL),
    (2025009, 3005, '2025-05-10', NULL),
    (2025010, 3007, '2025-05-11', NULL),
    (2025001, 3006, '2025-05-12', '2025-05-22');

-- 연체 데이터
INSERT INTO DB2025_OVERDUES (user_id, item_id, original_due, overdue_days, restriction_end) VALUES
    (2025003, 3004, '2025-04-27',  9, '2025-05-10'),
    (2025002, 3002, '2025-04-30',  5, '2025-05-08'),
    (2025008, 3008, '2025-05-05', 16, '2025-05-21'),
    (2025007, 3009, '2025-04-25', 26, '2025-05-25'),
    (2025010, 3003, '2025-05-01', 10, '2025-05-20');
