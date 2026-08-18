kubectx dev-fss
kubectl exec --tty deployment/bidrag-person printenv | grep -E 'AZURE_|TOKEN_X|_URL|SCOPE' > src/test/resources/application-lokal-nais-secrets.properties