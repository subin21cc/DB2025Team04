-- 데이터베이스 사용 선언
USE DB2025Team04;

-- 기존 뷰 삭제
DROP VIEW IF EXISTS VIEW_RESERVATION_STATUS;
DROP VIEW IF EXISTS VIEW_RENT_DETAIL;

-- 기존 테이블 삭제 (외래 키 참조 순서를 고려하여 삭제)
DROP TABLE IF EXISTS DB2025_OVERDUES;
DROP TABLE IF EXISTS DB2025_RESERVATION;
DROP TABLE IF EXISTS DB2025_RENT;
DROP TABLE IF EXISTS DB2025_ADMIN;
DROP TABLE IF EXISTS DB2025_ITEMS;
DROP TABLE IF EXISTS DB2025_USER;

-- 테이블 생성
-- 사용자 테이블
CREATE TABLE DB2025_USER
(
    user_id     INT PRIMARY KEY,
    user_pw     VARCHAR(64)          NOT NULL,
    user_name   VARCHAR(50)          NOT NULL,
    user_dep    VARCHAR(50)          NOT NULL,
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
    rent_status ENUM ('대여신청','대여중','반납완료','연체중', '연체완료') NOT NULL DEFAULT '대여신청',
    FOREIGN KEY (item_id) REFERENCES DB2025_ITEMS (item_id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES DB2025_USER (user_id) ON DELETE RESTRICT,
    UNIQUE (item_id, user_id)
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



-- 대여 내역 + 사용자 정보 + 물품 정보를 통합 조회하는 뷰입니다.
CREATE VIEW VIEW_RENT_DETAIL AS
SELECT R.rent_id,
       U.user_id,
       U.user_name,
       U.user_dep,
       I.item_name,
       R.borrow_date,
       R.due_date,
       R.return_date,
       R.rent_status
FROM DB2025_RENT R
         JOIN DB2025_USER U ON R.user_id = U.user_id
         JOIN DB2025_ITEMS I ON R.item_id = I.item_id;


-- 설명: 예약 정보를 예약자 이름, 물품 이름, 예약 가능 여부와 함께 조회하는 뷰입니다. 예약 시 연체 여부 및 물품 재고 상황 등을 판단하기 위한 정보를 한눈에 보여줍니다.
CREATE VIEW VIEW_RESERVATION_STATUS AS
SELECT R.reservation_id,
       U.user_id,
       U.user_name,
       I.item_name,
       R.reserve_date,
       R.restriction_end,
       CASE
           WHEN O.restriction_end IS NOT NULL AND O.restriction_end > CURRENT_DATE THEN '예약불가(연체중)'
           WHEN I.available_quantity < 1 THEN '예약불가(재고없음)'
           ELSE '예약가능'
           END AS reservation_status
FROM DB2025_RESERVATION R
         JOIN DB2025_USER U ON R.user_id = U.user_id
         JOIN DB2025_ITEMS I ON R.item_id = I.item_id
         LEFT JOIN DB2025_OVERDUES O
                   ON R.user_id = O.user_id
                       AND R.item_id = O.item_id;









-- 샘플데이터
--

-- 사용자 샘플 데이터 with 7-digit user_id and sha2 hashed passwords
INSERT INTO DB2025_USER (user_id, user_pw, user_name, user_dep, user_phone, user_status)
VALUES (2025001, SHA2('pw1234', 256), '김철수', '컴퓨터공학과', '010-1111-2222', '대여가능'),
       (2025002, SHA2('pw5678', 256), '이영희', '전자공학과', '010-2222-3333', '대여가능'),
       (2025003, SHA2('pwabcd', 256), '박민수', '기계공학과', '010-3333-4444', '대여불가'),
       (2025004, SHA2('pwefgh', 256), '최지은', '경영학과', '010-4444-5555', '대여가능');

-- 관리자 샘플 데이터 with auto increment starting from 1
INSERT INTO DB2025_ADMIN (admin_id, user_id)
VALUES (1, 2025001),
       (2, 2025004);

-- 물품 샘플 데이터
INSERT INTO DB2025_ITEMS (item_id, item_name, quantity, available_quantity, category)
VALUES (2001, '노트북', 10, 7, '전자기기'),
       (2002, '빔프로젝터', 5, 2, '전자기기'),
       (2003, '공학용계산기', 20, 15, '학습기기'),
       (2004, '회의용마이크', 8, 0, '음향기기');

-- 대여 샘플 데이터 with auto increment starting from 1
INSERT INTO DB2025_RENT (rent_id, item_id, user_id, borrow_date, return_date, rent_status)
VALUES (1, 2001, 2025001, '2025-05-01', NULL, '대여중'),
       (2, 2002, 2025002, '2025-04-25', '2025-05-02', '반납완료'),
       (3, 2004, 2025003, '2025-04-20', NULL, '연체중');

-- 예약 샘플 데이터 with auto increment starting from 1
INSERT INTO DB2025_RESERVATION (reservation_id, user_id, item_id, reserve_date, restriction_end)
VALUES (1, 2025004, 2001, '2025-05-03', NULL),
       (2, 2025002, 2004, '2025-05-04', '2025-05-10'),
       (3, 2025003, 2003, '2025-05-05', NULL);

-- 연체 샘플 데이터 with auto increment starting from 1
INSERT INTO DB2025_OVERDUES (overdue_id, user_id, item_id, original_due, overdue_days, restriction_end)
VALUES (1, 2025003, 2004, '2025-04-27', 9, '2025-05-10'),
       (2, 2025002, 2004, '2025-04-30', 5, '2025-05-08');
