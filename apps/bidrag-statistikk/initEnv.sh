kubectx dev-gcp
kubectl exec --tty deployment/bidrag-statistikk printenv | grep -E 'AZURE_|_URL|SCOPE'
