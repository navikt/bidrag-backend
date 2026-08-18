#!/bin/bash
kubectx dev-gcp

deployment="deployment/bidrag-reskontro-q2"
[ "$1" == "q2" ] && deployment="deployment/bidrag-reskontro-q2"
echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --tty $deployment -- printenv | grep -E 'AZURE_|_URL|SCOPE|UNLEASH_'> src/test/resources/application-lokal-nais-secrets.properties