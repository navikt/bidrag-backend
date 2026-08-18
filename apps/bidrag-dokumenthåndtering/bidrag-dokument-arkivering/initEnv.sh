kubectx dev-fss
kubectl exec --tty deployment/bidrag-dokument-arkivering printenv | grep -E 'AZURE_|TOKEN_X|_URL|SCOPE|CLIENT_ID' > src/test/resources/application-lokal-nais-secrets.properties
