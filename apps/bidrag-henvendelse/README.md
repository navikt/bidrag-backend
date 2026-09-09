# bidrag-henvendelse

En tjeneste som utleverer henvendelsene (chat, meldingskjeder, samtalereferater) på en person til
brukeroversikten i bidrag-frontend. Oversetter lista fra **sf-henvendelse-api-proxy**
(namespace `teamnks`, proxy mot henvendelsesløsningen i Salesforce) til bidragsdomenet.

Erstatter `HenvendelselisteProvider` og `HenvendelseRsConsumer` i BiSys (Favro NAV-30542).
