CREATE TABLE MEDICINE
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(3000) NOT NULL,
    UNIQUE (name)
);

CREATE TABLE ACTIVE_INGREDIENT
(
    id          UUID PRIMARY KEY,
    medicine_id UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    FOREIGN KEY (medicine_id) REFERENCES MEDICINE (id) ON DELETE CASCADE,
    UNIQUE (name, medicine_id)
);

CREATE TABLE MEDICINE_ANALOGUE
(
    id          UUID PRIMARY KEY,
    medicine_id UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    FOREIGN KEY (medicine_id) REFERENCES MEDICINE (id) ON DELETE CASCADE,
    UNIQUE (name, medicine_id)
);

CREATE TABLE TELEGRAM_USER
(
    id                UUID PRIMARY KEY,
    telegram_username VARCHAR(255) NOT NULL UNIQUE,
    is_admin          BOOLEAN      NOT NULL
);

INSERT INTO TELEGRAM_USER (id, telegram_username, is_admin)
VALUES ('b1fdb7d3-f228-4bc8-98db-441d3e5224d6', 'shchuko', true);

INSERT INTO TELEGRAM_USER (id, telegram_username, is_admin)
VALUES ('480ed0c8-27e9-409d-937a-15a12eee0818', 'lerhon', true);
