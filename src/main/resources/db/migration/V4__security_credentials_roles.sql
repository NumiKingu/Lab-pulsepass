CREATE TABLE user_credentials (
                                  user_id              BIGINT       PRIMARY KEY,
                                  password_hash        VARCHAR(100) NOT NULL,
                                  password_changed_at  TIMESTAMP,
                                  CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  CONSTRAINT ck_user_credentials_hash CHECK (char_length(password_hash) >= 60)
);

CREATE TABLE user_roles (
                            user_id  BIGINT      NOT NULL,
                            role     VARCHAR(20) NOT NULL,
                            CONSTRAINT pk_user_roles      PRIMARY KEY (user_id, role),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                            CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'USER'
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'USER');
