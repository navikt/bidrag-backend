# bidrag-domene
Domenetyper for team bidrag. 

Spesifikke domenetyper gjør at regler kan sentraliseres på tvers av prosjekter og sikrer 
at typer ikke forveksles i parametere innad eller mellom systemer.

Domenetypene har også spesifisert Convertes for jdbc og jpa, slik at de kan brukes i databaseobjekter og 
automatisk konverteres ved persistering til databasen.

Mappen fileTemplates inneholder templates til IntelliJ for å støtte opprettelse av nye domenetyper. 
Disse importeres via "File" -> "Manage IDE Settings" -> "Import Settings..."
Etter at disse er importert kan man når man velger "File" -> "New" velge  "DomainBoolean", "DomainString", "DomainLocalDate" 
eller "DomainLocalDateTime" for å opprette nye domeneobjekter inkludert Convertes for de respektive typene.