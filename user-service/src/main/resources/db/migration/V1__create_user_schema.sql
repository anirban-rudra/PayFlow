CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX uk_app_user_email ON app_user (email);
