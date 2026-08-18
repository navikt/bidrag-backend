CREATE SEQUENCE IF NOT EXISTS ident_seq INCREMENT BY 1 START WITH 85000000000;

CREATE TABLE IF NOT EXISTS samhandlere
(
    id                  integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    ident               text NOT NULL UNIQUE DEFAULT nextval('ident_seq'),
    navn                text NOT NULL,
    offentlig_id        text,
    offentlig_id_type   text,
    sprak               text,
    omradekode          text,
    norskkontonr        text,
    iban                text,
    swift               text,
    banknavn            text,
    banklandkode        text,
    valutakode          text,
    bankcode            text,
    adresselinje1       text,
    adresselinje2       text,
    adresselinje3       text,
    postnr              text,
    poststed            text,
    land                text,
    kontaktperson       text,
    kontakt_epost       text,
    kontakt_telefon     text,
    notat               text,
    er_opphort          boolean DEFAULT false,
    endret_tidspunkt    timestamp,
    opprettet_tidspunkt timestamp DEFAULT current_timestamp
);

CREATE INDEX IF NOT EXISTS navn_index ON samhandlere (navn);
CREATE INDEX IF NOT EXISTS ident_index ON samhandlere (ident);