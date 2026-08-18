kubectx dev-fss
kubectl exec --tty deployment/bidrag-organisasjon-feature -- printenv | grep -E 'AZURE_|URL|SCOPE|UNLEASH_' > src/test/resources/application-lokal-nais-secrets.properties
