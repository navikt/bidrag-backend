# Hvordan remote debugge nais applikasjoner.

## Bakgrunn
Det er flere tilfeller hvor det er gunstig å kunne kjøre lokal debugging i applikasjoner som går mot reelle endepunkter. 
Denne guiden vil vise hvordan dette kan gjøres.

## Hvordan
### 1. Innlogging og prerequisite
For å kunne gjennomføre må du være logget inn i GCP med gcp auth login og kubectl må være installert.
### 2. Endre deployment på kjørende applikasjon for å legge til rette for remote debugging. 
Bruk kommandoen, bytt ut -regnskap-feature med applikasjonen du ønsker å remote debugge mot:
```
kubectl edit deployment/bidrag-regnskap-feature
```  
Det vil da åpne seg en .yaml fil i din favoritt tekst-editor. Under spec.template.spec.containers - env legg inn følgende:
```  
        - name: JAVA_OPTS
          value: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
``` 
<span style="color: red">NB: Spacingen er viktig at blir riktig. Om denne er feil vil filen åpnes på nytt etter du har lagret og lukket den.
Feilmeldingen står på toppen av filen.

Porten kan settes til noe annet om du ønsker så lenge resterende kommandoer også blir endret.

### 3. Start port-forward mot applikasjonen
Om applikasjonen kun kjører på 1 node så kan følgende kommando brukes for å koble opp mot applikasjonen:
``` 
kubectl port-forward deployment/bidrag-regnskap-feature 5005:5005
``` 
Om appliaksjonen kjører på 2 eller flere noder må følgende gjøres:
1. Hent ut podenes id med kommandoen 
``` kubectl get pods | grep regnskap-feature``` 
2. Kjør følgende kommando for hver pod, bytt ut med pod id'en for hvert kall og endre porten. Eks:
 ```  
kubectl port-forward pods/bidrag-regnskap-feature-xxxxxxxxx-xxx1 5005:5005
```
```  
kubectl port-forward pods/bidrag-regnskap-feature-xxxxxxxxx-xxx2 5006:5005
```
### 4. Koble opp intellij (eller noe annet om du ønsker)
Lag en per port og start dem.

![Remote JVM debug](media/remote_JVM_debug_intellij.png?raw=true "Remote JVM debug")

### 5. Kos deg med debugging!