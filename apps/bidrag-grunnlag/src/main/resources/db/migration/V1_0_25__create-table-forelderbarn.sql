-- Table: forelderbarn

-- DROP TABLE forelderbarn;

CREATE TABLE IF NOT EXISTS forelderbarn
(
    forelder_id integer NOT NULL,
    barn_id integer NOT NULL,
    CONSTRAINT forelderbarn_pkey PRIMARY KEY (forelder_id, barn_id),
    CONSTRAINT fk_forelder_id FOREIGN KEY (forelder_id)
        REFERENCES forelder (forelder_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_barn_id FOREIGN KEY (barn_id)
        REFERENCES barn (barn_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    UNIQUE (forelder_id, barn_id)
)

    TABLESPACE pg_default;