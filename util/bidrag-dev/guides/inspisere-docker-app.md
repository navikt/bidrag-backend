# Hvordan inspisere en docker app

## Inspisere bygget til den siste docker app fra github packages

1. `docker run <siste fra github packages>` - den skal da feile
2. `docker ps -a` - finn containerId til den feila docker appen
3. `docker commit <containerId> stopme`
4. `docker run -ti entrypoint=sh stopme`
5. `jar tf app.jar`

## Undersøk deploya docker app med kubernetes

1. `kubectl exec -it -n <namespace> <podname> -- bash` - for å bruke bash til docker-containeren på en pod <br> * For at denne kommandoen skal gå gjennom må .nais.io legges til env-variabel `NO_PROXY`: `NO_PROXY=<endre hosts>,.nais.io`
2. `cd /var/run/secrets/nais.io/vault` - gå til vault sine mounts (se eventuelt tilsvarende paths
3. `cat resources.env` - lister ut fila med env-variable som har blitt mountet
4. `env` - lister ut miljøvariablene til docker image <br> * verdier fra vault.adeo.no kan sjekkes under `/var/run/secrets/nais.io/`
5. Endre appens deploy-yaml: `kubectl edit deployment/<app-navn>` 

## Undersøk oppstartsloggen til nais-app

`kubectl logs <pod-navn>  -c vks-init -n <namespace or defult>`
