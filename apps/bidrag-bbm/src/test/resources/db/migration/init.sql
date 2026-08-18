create table T_SOKNADSTYPE
(
    KODE_SOKNADSTYPE  CHAR(2)                             not null,
    BESKR_SOKNADSTYPE CHAR(50)                            not null,
    DATO_GJELDER_TOM  DATE         default '9999-12-31',
    BRUKERID          CHAR(8)      default 'CURRENT USER' not null,
    TIDSPKT_ENDRET    TIMESTAMP(6) default CURRENT TIMESTAMP not null
);

create unique index IKOSO01U
    on T_SOKNADSTYPE (KODE_SOKNADSTYPE);

alter table T_SOKNADSTYPE
    add primary key (KODE_SOKNADSTYPE);



create table T_SOKNAD
(
    SAKSNR            CHAR(7)                             not null,
    DATO_SOKNAD       DATE                                not null,
    KODE_SOKNADSTYPE  CHAR(2)                             not null
        constraint KODEÅSOK
            references T_SOKNADSTYPE,
    DATO_SOKT_FOM     DATE                                not null,
    DATO_VIRKNING     DATE,
    UNDERGRUPPE       CHAR(2)                             not null,
    FULL_BIDREVNE     CHAR(1)                             not null,
    HAR_BTILL_FORSVAR CHAR(1)                             not null,
    DATO_BEREGNET     DATE,
    DATO_VEDTAK       DATE,
    KODE_AARSAK       CHAR(2),
    BRUKERID          CHAR(8)      default 'CURRENT USER' not null,
    TIDSPKT_ENDRET    TIMESTAMP(6) default CURRENT TIMESTAMP not null,
    NESTE_INDEX_AAR   CHAR(4)      default ''             not null,
    constraint KODE1SWV
        foreign key (KODE_SOKNADSTYPE, KODE_AARSAK) references T_SOKNADSTYPE (KODE_SOKNADSTYPE)
);

create unique index ISOKN01U
    on T_SOKNAD (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE);

alter table T_SOKNAD
    add primary key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE);



create unique index ISOKN01U
    on T_SOKNAD (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE);

alter table T_SOKNAD
    add primary key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE);



create table T_SOKNADSBARN
(
    SAKSNR            CHAR(7)  not null,
    DATO_SOKNAD       DATE     not null,
    KODE_SOKNADSTYPE  CHAR(2)  not null,
    PERSON_ID_BARN    CHAR(11) not null,
    BELOP_LOP_BIDRAG  DECIMAL(13, 2),
    INNKREVES         CHAR(1)  not null,
    MED_I_BEREGNING   CHAR(1)  not null,
    SOKT_TILLEGGSBIDR CHAR(1)  not null,
    constraint SAKS1PUB
        foreign key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE) references T_SOKNAD
);

create unique index ISOBA01U
    on T_SOKNADSBARN (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN);

create unique index ISOBA02
    on T_SOKNADSBARN (PERSON_ID_BARN);

alter table T_SOKNADSBARN
    add primary key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN);



create table T_BEREGNINGPERIODE
(
    SAKSNR           CHAR(7)        not null,
    DATO_SOKNAD      DATE           not null,
    KODE_SOKNADSTYPE CHAR(2)        not null,
    PERSON_ID_BARN   CHAR(11)       not null,
    DATO_PERIODE_FOM DATE           not null,
    KODE_RESULTAT    CHAR(3)        not null,
    BELOP_INNTEKT_BP DECIMAL(13, 2) not null,
    BELOP_INNTEKT_BM DECIMAL(13, 2) not null,
    BELOP_INNTEKT_BB DECIMAL(13, 2) not null,
    DATO_PERIODE_TOM DATE           not null,
    constraint SAKS1DNO
        foreign key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN) references T_SOKNADSBARN
);

create unique index IBERE01U
    on T_BEREGNINGPERIODE (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN, DATO_PERIODE_FOM);

create unique index IBERE02
    on T_BEREGNINGPERIODE (PERSON_ID_BARN);

alter table T_BEREGNINGPERIODE
    add primary key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN, DATO_PERIODE_FOM);



create table T_PERIODE_BIDRAG
(
    SAKSNR             CHAR(7)        not null,
    DATO_SOKNAD        DATE           not null,
    KODE_SOKNADSTYPE   CHAR(2)        not null,
    PERSON_ID_BARN     CHAR(11)       not null,
    DATO_PERIODE_FOM   DATE           not null,
    BELOP_BP_ANDEL_U   DECIMAL(13, 2) not null,
    ANDELSBROK_TELLER  SMALLINT       not null,
    ANDELSBROK_NEVNER  SMALLINT       not null,
    FORHOLDSTALL       DECIMAL(4, 3)  not null,
    BELOP_FOR_SAMVAR   DECIMAL(13, 2) not null,
    BELOP_BIDRAGSEVNE  DECIMAL(13, 2) not null,
    BELOP_BARNETILSYN  DECIMAL(13, 2) not null,
    BELOP_U            DECIMAL(13, 2) not null,
    FULL_BIDRAGSEVNE   CHAR(1)        not null,
    BELOP_BIDRAG_BEREG DECIMAL(13, 2) not null,
    BELOP_BIDRAG_TILL  DECIMAL(13, 2) not null,
    BELOP_BIDRAG_FAKT  DECIMAL(13, 2) not null,
    constraint SAKS1VMM
        foreign key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN,
                     DATO_PERIODE_FOM) references T_BEREGNINGPERIODE
);

create unique index IPEBI01U
    on T_PERIODE_BIDRAG (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN, DATO_PERIODE_FOM);

create unique index IPEBI02
    on T_PERIODE_BIDRAG (PERSON_ID_BARN);

alter table T_PERIODE_BIDRAG
    add primary key (SAKSNR, DATO_SOKNAD, KODE_SOKNADSTYPE, PERSON_ID_BARN, DATO_PERIODE_FOM);

create table T_SOKNAD_KNYT
(
    ID                   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    HOVED_SOKNAD_ID      INTEGER NOT NULL,
    REF_SOKNAD_ID        INTEGER NOT NULL,
    STATUS               VARCHAR(30) NOT NULL,
    SOKNAD_KNYTNINGSTYPE VARCHAR(20),
    OPPRETT_DATO         TIMESTAMP
);


