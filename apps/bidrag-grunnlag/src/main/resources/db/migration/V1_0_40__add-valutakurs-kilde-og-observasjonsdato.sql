ALTER TABLE valutakursgrunnlag
    ADD COLUMN kilde varchar(20),
    ADD COLUMN observasjonsdato date,
    ADD CONSTRAINT valutakursgrunnlag_kilde_check CHECK (kilde IN ('ECB', 'NORGES_BANK', 'MANUELL'));
