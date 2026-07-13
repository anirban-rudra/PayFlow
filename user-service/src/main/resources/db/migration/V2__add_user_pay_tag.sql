ALTER TABLE app_user ADD COLUMN pay_tag VARCHAR(40);

UPDATE app_user
SET pay_tag = '@user' || id
WHERE pay_tag IS NULL;

ALTER TABLE app_user ALTER COLUMN pay_tag SET NOT NULL;

CREATE UNIQUE INDEX uk_app_user_pay_tag ON app_user (pay_tag);
