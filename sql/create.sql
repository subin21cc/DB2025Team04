-- 데이터베이스 사용 선언
USE DB2025Team04;

-- 기존 뷰 삭제
DROP VIEW IF EXISTS VIEW_RENTAL_ITEMS_STATUS;
DROP VIEW IF EXISTS VIEW_RESERVATION_OVERVIEW;
DROP VIEW IF EXISTS VIEW_USER_RENTAL_OVERVIEW;

-- 기존 인덱스 삭제
DROP INDEX IF EXISTS idx_rent_user_status_due ON DB2025_RENT;
DROP INDEX IF EXISTS idx_rent_status ON DB2025_RENT;
DROP INDEX IF EXISTS idx_overdues_user_item_restriction ON DB2025_OVERDUES;
DROP INDEX IF EXISTS idx_overdue_user_id ON DB2025_OVERDUES;
DROP INDEX IF EXISTS idx_item_category ON DB2025_ITEMS;

-- 기존 테이블 삭제 (외래 키 참조 순서를 고려하여 삭제)
DROP TABLE IF EXISTS DB2025_OVERDUES;
DROP TABLE IF EXISTS DB2025_RESERVATION;
DROP TABLE IF EXISTS DB2025_RENT;
DROP TABLE IF EXISTS DB2025_ADMIN;
DROP TABLE IF EXISTS DB2025_ITEMS;
DROP TABLE IF EXISTS DB2025_USER;


-- 테이블 생성
-- 사용자 테이블
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
    quantity           INT         NOT NULL CHECK (quanti들

-- 1. 대여 신청과 동시에 재고 수량 감소 (트랜잭션)
START TRANSACTION;
  INSERT INTO DB2025_RENT (item_id, user_id, borrow_date, rent_status)
  VALUES (3003, 2025009, CURRENT_DATE, '대여신청');

  UPDATE DB2025_ITEMS
  SET available_quantity = available_quantity - 1
  WHERE item_id = 3003;
COMMIT;

-- 2. 사용자 정보 수정 (트랜잭션 포함)
START TRANSACTION;
  UPDATE DB2025_USER
  SET user_phone = '010-1234-5678'
  WHERE user_id = 2025009;
COMMIT;

-- 3. 예약 취소 및 상태 복구 (트랜잭션)
START TRANSACTION;
  DELETE FROM DB2025_RESERVATION
  WHERE reservation_id = 1;

  UPDATE DB2025_ITEMS
  SET available_quantity = available_quantity + 1
  WHERE item_id = 3001;
COMMIT;

-- 4. VIEW_RENTAL_ITEMS_STATUS에서 대여중 수량이 많은 순으로 출력
SELECT * FROM VIEW_RENTAL_ITEMS_STATUS
ORDER BY rented_count DESC;

-- 5. 예약 불가 상태만 필터링
SELECT * FROM VIEW_RESERVATION_OVERVIEW
WHERE reservation_status != '예약가능';

-- 6. VIEW_USER_RENTAL_OVERVIEW에서 연체 건수가 1건 이상인 사용자 조회
SELECT * FROM VIEW_USER_RENTAL_OVERVIEW
WHERE overdue_count >= 1;

-- 7. 연체 사용자 검색 (idx_overdue_user_id 활용)
SELECT * FROM DB2025_OVERDUES
WHERE user_id = 2025007;

-- 8. 특정 카테고리의 물품 검색 (idx_item_category 활용)
SELECT * FROM DB2025_ITEMS
WHERE category = '전자기기';

-- 9. 특정 사용자에 대해 연체/대여중 상태와 반납 예정일 검색 (idx_rent_user_status_due 활용)
SELECT * FROM DB2025_RENT
WHERE user_id = 2025001 AND rent_status IN ('대여중', '연체중');

-- 10. 현재 연체 중인 사용자 ID 리스트 출력
SELECT DISTINCT user_id
FROM DB2025_RENT
WHERE rent_status = '연체중';

-- 11. 재고가 가장 부족한 물품 이름
SELECT item_name
FROM DB2025_ITEMS
WHERE available_quantity = (
  SELECT MIN(available_quantity)
  FROM DB2025_ITEMS
);

-- 12. 연체 일수가 평균 이상인 사용자
SELECT user_id, overdue_days
FROM DB2025_OVERDUES
WHERE overdue_days > (
  SELECT AVG(overdue_days) FROM DB2025_OVERDUES
);

-- 13. 모든 대여 기록과 해당 사용자 이름
SELECT R.*, U.user_name
FROM DB2025_RENT R
JOIN DB2025_USER U ON R.user_id = U.user_id;

-- 14. 예약 가능 상태의 예약자 목록
SELECT R.reservation_id, U.user_name, I.item_name
FROM DB2025_RESERVATION R
JOIN DB2025_USER U ON R.user_id = U.user_id
JOIN DB2025_ITEMS I ON R.item_id = I.item_id
LEFT JOIN DB2025_OVERDUES O ON R.user_id = O.user_id AND R.item_id = O.item_id
WHERE (
    O.restriction_end IS NULL OR O.restriction_end <= CURRENT_DATE
  ) AND I.available_quantity > 0;

-- 15. 연체 기록과 관련된 유저 및 물품명
SELECT O.*, U.user_name, I.item_name
FROM DB2025_OVERDUES O
JOIN DB2025_USER U ON O.user_id = U.user_id
JOIN DB2025_ITEMS I ON O.item_id = I.item_id;

-- 16. 특정 학과 학생 목록 (입력 값: '전자공학과')
SELECT * FROM DB2025_USER
WHERE user_dep = '전자공학과';

-- 17. 사용자 입력값으로 특정 물품 예약 (입력: 사용자 ID, 물품 ID)
-- 예: 2025005, 3002
INSERT INTO DB2025_RESERVATION (user_id, item_id, reserve_date)
VALUES (2025005, 3002, CURRENT_DATE);

-- 18. 특정 날짜 이후 반납된 물품 보기 (입력값: 날짜)
-- 예: '2025-05-01'
SELECT * FROM DB2025_RENT
WHERE return_date > '2025-05-01';

-- 19. 대여 기록 검색: 특정 사용자 이름 입력
-- 예: '박민수'
SELECT R.*, I.item_name
FROM DB2025_RENT R
JOIN DB2025_USER U ON R.user_id = U.user_id
JOIN DB2025_ITEMS I ON R.item_id = I.item_id
WHERE U.user_name = '박민수';

-- 20. 사용자 삭제 (입력: user_id)
-- 예: 2025010
DELETE FROM DB2025_USER
WHERE user_id = 2025010;
