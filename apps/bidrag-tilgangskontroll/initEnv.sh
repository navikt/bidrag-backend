kubectx dev-gcp
kubectl exec --tty deployment/bidrag-tilgangskontroll -- printenv | grep -E 'AZURE_|URL|SCOPE' > src/test/resources/application-lokal-nais-secrets.properties
