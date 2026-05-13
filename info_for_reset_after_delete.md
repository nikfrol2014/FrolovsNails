# После дропа бд нужно восстановить админа напрямую в бд
## 1. Для этого нужно создать его и привязать к клиентскому профилю
```postgresql
-- для создания админа как юзера
INSERT INTO users (phone, password, role, created_at, enabled)
VALUES (
    '88888888888',
    '$2a$10$FXr5kzytdIxO5.fIKYWR5.c7hS0jzt6bDR3peqHQVGKKpanBrWcGy', -- qwe
    'ADMIN',
    CURRENT_TIMESTAMP,
    true
) ON CONFLICT (phone) DO NOTHING;
```

```postgresql
-- для привязки 
INSERT INTO clients (user_id, first_name, last_name, created_at)
SELECT
    u.id,
    'Алина',
    'Фролова',
    CURRENT_DATE
FROM users u
WHERE u.phone = '88888888888'
ON CONFLICT (user_id) DO NOTHING;
```

## ЭТО ДЕЛАЙ ЧЕРЕЗ СВАГГЕР -- важно также задавать notes и день рождения иначе будет косяк с профилем
```postgresql
-- Клиент 1
INSERT INTO users (phone, password, role, created_at, enabled)
VALUES (
'79161234567',
'$2a$10$FXr5kzytdIxO5.fIKYWR5.c7hS0jzt6bDR3peqHQVGKKpanBrWcGy', -- пароль: qwe
'CLIENT',
CURRENT_TIMESTAMP,
true
) ON CONFLICT (phone) DO NOTHING;
```
```postgresql
INSERT INTO clients (user_id, first_name, last_name, birth_date, created_at)
SELECT
u.id,
'Екатерина',
'Смирнова',
'1990-05-15',
CURRENT_DATE
FROM users u
WHERE u.phone = '79161234567'
ON CONFLICT (user_id) DO NOTHING;
```
```postgresql
-- Клиент 2
INSERT INTO users (phone, password, role, created_at, enabled)
VALUES (
'79261234567',
'$2a$10$FXr5kzytdIxO5.fIKYWR5.c7hS0jzt6bDR3peqHQVGKKpanBrWcGy',
'CLIENT',
CURRENT_TIMESTAMP,
true
) ON CONFLICT (phone) DO NOTHING;
```
```postgresql
INSERT INTO clients (user_id, first_name, last_name, birth_date, created_at)
SELECT
u.id,
'Ольга',
'Иванова',
'1988-10-20',
CURRENT_DATE
FROM users u
WHERE u.phone = '79261234567'
ON CONFLICT (user_id) DO NOTHING;
```
```postgresql
-- Клиент 3
INSERT INTO users (phone, password, role, created_at, enabled)
VALUES (
'79361234567',
'$2a$10$FXr5kzytdIxO5.fIKYWR5.c7hS0jzt6bDR3peqHQVGKKpanBrWcGy',
'CLIENT',
CURRENT_TIMESTAMP,
true
) ON CONFLICT (phone) DO NOTHING;
```
```postgresql
INSERT INTO clients (user_id, first_name, last_name, birth_date, created_at)
SELECT
u.id,
'Мария',
'Петрова',
'1995-03-25',
CURRENT_DATE
FROM users u
WHERE u.phone = '79361234567'
ON CONFLICT (user_id) DO NOTHING;
```