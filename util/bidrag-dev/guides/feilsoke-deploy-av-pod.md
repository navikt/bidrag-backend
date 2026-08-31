# Hvordan feilsøke ved deploy av nais applikasjon

Når en pod ikke vil deployes - når deploy feiler eller henger - så må man finne ut hvorfor dette skjer.
 
### Fremgangsmåter

* Kubernetes cli:
  * hent "poddene" for applikasjonen din: `kubectl get pods [-n <namespace>] -l app=<applikasjonsnavn>`
  * beskrivelse av en pod: `kubectl describe pod [-n <namespace>] <podnavn>`
  * loggene fra en pod: `kubectl logs [-n <namespace>] <pod-navn>`
  * init-loggene fra en pod: `kubectl logs [-n <namespace>] <podnavn> -c vks-init`
* Logger:
  * følg aktivitetsloggen fra `environments` på gitub (1ste punkt)
  * opprett et manuelt søk på https://console.cloud.google.com/logs for applikasjonen
    * sørg for å snevre søket til din applikasjon: `resource.labels.container_name="bidrag-applikasjonsnavn"` 
* [Inspisere docker app eller kubernetes pod](inspisere-docker-app.md)
