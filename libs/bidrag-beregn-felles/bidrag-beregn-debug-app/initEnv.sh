#!/bin/bash
kubectl config set-context dev-gcp

deployment="deployment/bidrag-behandling-q1"
[ "$1" == "q2" ] && deployment="deployment/bidrag-behandling-q2"
echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --cluster=dev-gcp -n bidrag --tty $deployment -- printenv | grep -E 'AZURE_|_URL|SCOPE|UNLEASH' | grep -v -e 'BIDRAG_FORSENDELSE_URL' -e 'KODEVERK_URL' -e 'BIDRAG_TILGANGSKONTROLL_URL' -e 'BIDRAG_GRUNNLAG_URL' -e 'BIDRAG_VEDTAK_SCOPE' -e 'BIDRAG_VEDTAK_URL' -e 'BIDRAG_BELOPSHISTORIKK_URL' > src/main/resources/application-lokal-nais-secrets.properties