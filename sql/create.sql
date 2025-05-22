-- 데이터베이스 사용 선언
USE DB2025Team04;

-- 테이블 생성
-- 사용자 테이블블
CREATE TABLE DB2025_USER (
                             user_id     INT PRIMARY KEY,
                             user_pw     VARCHAR(64)          NOT NULL,
                             user_name   VARCHAR(50)          NOT NULL,
                             user_dep    VARCHAR(50)          NOT NULL CHECK (user_dep REGEXP '학과$'),
    user_phone  VARCHAR(20) UNIQUE   NOT NULL,
    user_status ENUM ('대여가능','대여불가') NOT NULL DEFAULT '대여가능'
)ENGINE = InnoDB
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
        WHEN I.available_quantity < 1
            THEN '예약불가(재고없음)'
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
