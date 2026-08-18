
-- Legger til ny ID som senere skal bli PK.
ALTER TABLE aktoerregister.aktoer
    ADD COLUMN id SERIAL;

-- Oppretter en sekvens som populerer ID tabellen. Denne blir senere droppet da postgres håndterer videre inkrementer av PK.
CREATE SEQUENCE IF NOT EXISTS aktoerregister.aktoer_id_temp_seq;
UPDATE aktoerregister.aktoer SET id = nextval('aktoerregister.aktoer_id_temp_seq');

-- Renamer id til ident for å være litt mer spesifikk.
ALTER TABLE aktoerregister.hendelse
    RENAME COLUMN aktoerid TO aktoer_ident;

-- Legger til kolonne for å referer til nye IDen til aktør og fjerner FK.
ALTER TABLE aktoerregister.hendelse
    ADD COLUMN aktoer_id INT,
    DROP CONSTRAINT aktoerid;

-- Fjerner gamle PK og legger til ID som nye.
ALTER TABLE aktoerregister.aktoer
    DROP CONSTRAINT aktoer_pkey,
    ADD PRIMARY KEY (id);

-- Renamer for å skille litt tydligere på id og ident.
ALTER TABLE aktoerregister.aktoer
    RENAME COLUMN aktoerId TO aktoer_ident;

-- Gjør oppdatering og ny FK på alle eksisterende hendelser slik at referanse til aktør opprettholdes.
UPDATE aktoerregister.hendelse
SET aktoer_id = aktoerregister.aktoer.id
FROM aktoerregister.aktoer
WHERE aktoerregister.aktoer.aktoer_ident = aktoerregister.hendelse.aktoer_ident;

ALTER TABLE aktoerregister.hendelse
    ALTER aktoer_ident SET NOT NULL;

ALTER TABLE aktoerregister.hendelse
    ADD CONSTRAINT aktoerid FOREIGN KEY (aktoer_id) REFERENCES aktoerregister.aktoer (id);

-- Dropper sekvensen da denne nå er overflødig.
DROP SEQUENCE IF EXISTS aktoerregister.aktoer_id_temp_seq;

-- Legger til index for oppslag på aktørs ident da denne brukes til å hente opp aktører.
CREATE INDEX aktoer_ident_index ON aktoerregister.aktoer (aktoer_ident);

-- Sekvensene er trolig manuelt opprettet i miljøer. Må legge til for at spring batch ikke skal feile lokalt etter hver gang flyway kjører.
CREATE SEQUENCE IF NOT EXISTS aktoerregister.BATCH_STEP_EXECUTION_SEQ;
CREATE SEQUENCE IF NOT EXISTS aktoerregister.BATCH_JOB_EXECUTION_SEQ;
CREATE SEQUENCE IF NOT EXISTS aktoerregister.BATCH_JOB_SEQ;

-- Legger til privilegier på alt i aktoerregister schemaet og for flyway schemaet
GRANT ALL PRIVILEGES ON SCHEMA aktoerregister TO cloudsqliamuser;
GRANT ALL PRIVILEGES ON SCHEMA migrations TO cloudsqliamuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA aktoerregister GRANT ALL ON SEQUENCES TO cloudsqliamuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA aktoerregister GRANT ALL ON TABLES TO cloudsqliamuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA migrations GRANT ALL ON SEQUENCES TO cloudsqliamuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA migrations GRANT ALL ON TABLES TO cloudsqliamuser;