kubectl config use dev-gcp
kubectl exec -n=bidrag --tty deployment/bidrag-grunnlag -- printenv | grep -E 'AZURE_|_URL|SCOPE'  > src/test/resources/application-lokal-nais-secrets.properties
