ALTER TABLE aktoerregister.aktoer
    ADD COLUMN sist_endret timestamp DEFAULT current_timestamp;

ALTER TABLE aktoerregister.hendelse
    ADD COLUMN sist_endret timestamp DEFAULT current_timestamp;

ALTER TABLE aktoerregister.adresse
    ADD COLUMN sist_endret timestamp DEFAULT current_timestamp;

ALTER TABLE aktoerregister.kontonummer
    ADD COLUMN sist_endret timestamp DEFAULT current_timestamp;