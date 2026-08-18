ALTER TABLE aktoerregister.aktoer
    ADD COLUMN norskKontonr TEXT,
    ADD COLUMN iban TEXT,
    ADD COLUMN swift TEXT,
    ADD COLUMN bankNavn TEXT,
    ADD COLUMN bankLandkode TEXT,
    ADD COLUMN valutaKode TEXT,
    ADD COLUMN bankCode TEXT,
    ADD COLUMN navn TEXT,
    ADD COLUMN adresselinje1 TEXT,
    ADD COLUMN adresselinje2 TEXT,
    ADD COLUMN adresselinje3 TEXT,
    ADD COLUMN postnr TEXT,
    ADD COLUMN poststed TEXT,
    ADD COLUMN land TEXT;

UPDATE aktoerregister.aktoer
    SET norskKontonr = kontonummer.norskKontonr,
        iban = kontonummer.iban,
        swift = kontonummer.swift,
        bankNavn = kontonummer.bankNavn,
        bankLandkode = kontonummer.bankLandkode,
        valutaKode = kontonummer.valutaKode,
        bankCode = kontonummer.bankCode
    FROM aktoerregister.kontonummer AS kontonummer
    WHERE aktoer.kontonummerid = kontonummer.id;

UPDATE aktoerregister.aktoer
    SET navn = adresse.navn,
        adresselinje1 = adresse.adresselinje1,
        adresselinje2 = adresse.adresselinje2,
        adresselinje3 = adresse.adresselinje3,
        postnr = adresse.postnr,
        poststed = adresse.poststed,
        land = adresse.land
    FROM aktoerregister.adresse AS adresse
    WHERE aktoer.adresseid = adresse.id;

ALTER TABLE aktoerregister.aktoer
    DROP COLUMN adresseid,
    DROP COLUMN kontonummerid;

DROP TABLE aktoerregister.adresse;

DROP TABLE aktoerregister.kontonummer;
