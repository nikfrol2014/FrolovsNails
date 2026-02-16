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