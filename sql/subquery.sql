-- 대여 상태가 '대여중'인 물품의 이름과 대여자의 이름 조회
SELECT
    I.item_name AS 물품명,
    U.user_name AS 대여자명
FROM
    DB2025_RENT R
        JOIN
    DB2025_ITEMS I ON R.item_id = I.item_id
        JOIN
    DB2025_USER U ON R.user_id = U.user_id
WHERE
    R.rent_id IN (
        SELECT rent_id
        FROM DB2025_RENT
        WHERE rent_status = '대여중'
    );
