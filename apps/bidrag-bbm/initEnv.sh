#!/bin/bash
kubectx dev-fss

deployment="deployment/bidrag-bbm"
[ "$1" == "q1" ] && deployment="deployment/bidrag-bbm-feature"
echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --tty $deployment -n 'bidrag' -- printenv | grep -E 'AZURE_|_URL|SCOPE|UNLEASH' | grep -v -e 'BIDRAG_FORSENDELSE_URL' -e 'KODEVERK_URL' -e 'BIDRAG_TILGANGSKONTROLL_URL' -e 'BIDRAG_GRUNNLAG_URL' -e 'BIDRAG_VEDTAK_SCOPE' -e 'BIDRAG_VEDTAK_URL' -e 'BIDRAG_BELOPSHISTORIKK_URL' > src/test/resources/application-lokal-nais-secrets.properties
kubectl exec --tty $deployment -n 'bidrag' -- printenv | grep -E 'AZURE_|_URL|SCOPE|UNLEASH|DB' | grep -v -e 'BIDRAG_FORSENDELSE_URL' -e 'KODEVERK_URL' -e 'BIDRAG_TILGANGSKONTROLL_URL' -e 'BIDRAG_GRUNNLAG_URL' -e 'BIDRAG_VEDTAK_SCOPE' -e 'BIDRAG_VEDTAK_URL' -e 'BIDRAG_BELOPSHISTORIKK_URL' > .env.file