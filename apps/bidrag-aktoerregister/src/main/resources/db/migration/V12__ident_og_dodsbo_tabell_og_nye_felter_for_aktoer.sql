CREATE TABLE IF NOT EXISTS aktoerregister.tidligere_identer
(
    id                     SERIAL PRIMARY KEY,
    tidligere_aktoer_ident INT,
    identtype              TEXT,
    aktoer_id              INT REFERENCES aktoerregister.aktoer
);

CREATE TABLE IF NOT EXISTS aktoerregister.dodsbo
(
    id               SERIAL PRIMARY KEY,
    kontaktperson    TEXT,
    adresselinje1    TEXT,
    adresselinje2    TEXT,
    adresselinje3    TEXT,
    leilighetsnummer TEXT,
    postnr           TEXT,
    poststed         TEXT,
    land             TEXT,
    aktoer_id        INT references aktoerregister.aktoer
);

ALTER TABLE aktoerregister.aktoer
    RENAME COLUMN navn TO etternavn;

ALTER TABLE aktoerregister.aktoer
    ADD COLUMN fornavn          TEXT,
    ADD COLUMN fodt_dato        DATE,
    ADD COLUMN dod_dato         DATE,
    ADD COLUMN leilighetsnummer TEXT,
    ADD COLUMN gradering        TEXT,
    ADD COLUMN sprakkode        TEXT;