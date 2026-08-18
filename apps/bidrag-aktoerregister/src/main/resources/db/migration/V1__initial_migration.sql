CREATE SCHEMA IF NOT EXISTS aktoerregister;

CREATE TABLE IF NOT EXISTS aktoerregister.adresse
(
    id            SERIAL UNIQUE,
    navn          TEXT,
    adresselinje1 TEXT,
    adresselinje2 TEXT,
    adresselinje3 TEXT,
    postnr        TEXT,
    poststed      TEXT,
    land          TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS aktoerregister.kontonummer
(
    id           SERIAL UNIQUE,
    norskKontonr TEXT,
    iban         TEXT,
    swift        TEXT,
    bankNavn     TEXT,
    bankLandkode TEXT,
    valutaKode   TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS aktoerregister.aktoer
(
    aktoerId      TEXT UNIQUE NOT NULL,
    aktoerType    TEXT        NOT NULL,
    adresseId     INT,
    kontonummerId INT,
    PRIMARY KEY (aktoerId),
    CONSTRAINT adresse_id FOREIGN KEY (adresseId) REFERENCES aktoerregister.adresse (id),
    CONSTRAINT kontonummer_id FOREIGN KEY (kontonummerId) REFERENCES aktoerregister.kontonummer (id)
);

CREATE TABLE IF NOT EXISTS aktoerregister.hendelse
(
    sekvensnummer SERIAL UNIQUE,
    aktoerId      TEXT,
    PRIMARY KEY (sekvensnummer),
    CONSTRAINT aktoerId FOREIGN KEY (aktoerId) REFERENCES aktoerregister.aktoer (aktoerId)
);