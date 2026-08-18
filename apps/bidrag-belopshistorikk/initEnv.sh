#!/bin/bash
kubectx dev-gcp

deployment="deployment/bidrag-belopshistorikk-q2"
[ "$1" == "q1" ] && deployment="deployment/bidrag-belopshistorikk-q1"
echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --tty $deployment -- printenv | grep -E 'AZURE_|_URL|SCOPE|UNLEASH_' | grep -v -e 'BIDRAG_VEDTAK_URL' -e 'KODEVERK_URL' -e 'BIDRAG_SAMHANDLER_URL'> src/test/resources/application-lokal-nais-secrets.properties

echo "Henter email adresse som brukernavn mot database"
echo DB_USERNAME=`git config --get user.email` >> src/test/resources/application-lokal-nais-secrets.properties
