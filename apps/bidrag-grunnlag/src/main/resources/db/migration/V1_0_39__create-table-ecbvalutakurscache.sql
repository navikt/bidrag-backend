CREATE SEQUENCE ecbvalutakurscache_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ecbvalutakurscache
(
    id bigint NOT NULL PRIMARY KEY,
    valutakursdato date NOT NULL,
    valutakode varchar(255) NOT NULL,
    kurs numeric(38, 16) NOT NULL,
    CONSTRAINT ecbvalutakurscache_valutakode_dato_unique UNIQUE (valutakode, valutakursdato)
);
