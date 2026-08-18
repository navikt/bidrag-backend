CREATE TABLE IF NOT EXISTS audit_log
(
    id                serial PRIMARY KEY,
    tabell_navn       TEXT,
    tabell_id         INTEGER,
    operasjon         TEXT,
    endret_tidspunkt  TIMESTAMP DEFAULT now(),
    endret_av         TEXT,
    gamle_verdier     jsonb,
    nye_verdier       jsonb
);

CREATE OR REPLACE FUNCTION audit_trigger() RETURNS TRIGGER AS $$
DECLARE
    nye_data      jsonb;
    gamle_data    jsonb;
    key           text;
    nye_verdier   jsonb;
    gamle_verdier jsonb;
    user_id       text;
BEGIN

    user_id := current_setting('audit.user_id', true);

    IF user_id IS NULL THEN
        user_id := current_user;
    END IF;

    nye_verdier := '{}';
    gamle_verdier := '{}';

    IF TG_OP = 'INSERT' THEN
        nye_verdier := to_jsonb(NEW);

    ELSIF TG_OP = 'UPDATE' THEN
        nye_data := to_jsonb(NEW);
        gamle_data := to_jsonb(OLD);

        FOR key IN SELECT jsonb_object_keys(nye_data) UNION SELECT jsonb_object_keys(gamle_data)
            LOOP
                IF (nye_data ->> key IS DISTINCT FROM gamle_data ->> key) THEN
                    gamle_verdier := gamle_verdier || jsonb_build_object(key, gamle_data ->> key);
                    nye_verdier := nye_verdier || jsonb_build_object(key, nye_data ->> key);
                END IF;
            END LOOP;

    ELSIF TG_OP = 'DELETE' THEN
        gamle_data := to_jsonb(OLD);
        gamle_verdier := gamle_data;

        FOR key IN SELECT jsonb_object_keys(gamle_data)
            LOOP
                gamle_verdier := gamle_verdier || jsonb_build_object(key, gamle_data ->> key);
            END LOOP;

    END IF;

    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log (tabell_navn, tabell_id, operasjon, endret_av, gamle_verdier, nye_verdier)
        VALUES (TG_TABLE_NAME, NEW.id, TG_OP, user_id, gamle_verdier, nye_verdier);

        RETURN NEW;
    ELSE
        INSERT INTO audit_log (tabell_navn, tabell_id, operasjon, endret_av, gamle_verdier, nye_verdier)
        VALUES (TG_TABLE_NAME, OLD.id, TG_OP, user_id, gamle_verdier, nye_verdier);

        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_trigger
    BEFORE INSERT OR UPDATE OR DELETE
    ON samhandlere
    FOR EACH ROW
    EXECUTE FUNCTION audit_trigger();