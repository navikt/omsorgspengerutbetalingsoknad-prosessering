# omsorgspengerutbetalingsoknad-prosessering
![CI / CD](https://github.com/navikt/omsorgspengerutbetalingsoknad-prosessering/workflows/CI%20/%20CD/badge.svg)
![NAIS Alerts](https://github.com/navikt/omsorgspengerutbetalingsoknad-prosessering/workflows/Alerts/badge.svg)

Tjeneste som prosesserer søknader om omsorgspengerutbetaling.
Leser søknader fra Kafka topic `privat-omsorgspengerutbetaling-mottatt` som legges der av [omsorgspengerutbetalig-api](https://github.com/navikt/omsorgspengerutbetaling-api)

## Prosessering
- Genererer Søknad-PDF
- Oppretter Journalpost
- Sletter mellomlagrede dokumenter

## Feil i prosessering
Ved feil i en av streamene som håndterer prosesseringen vil streamen stoppe, og tjenesten gi 503 response på liveness etter 15 minutter.
Når tjenenesten restarter vil den forsøke å prosessere søknaden på ny og fortsette slik frem til den lykkes.

## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerterator.yml](nais/alerterator.yml).

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #sykdom-i-familien.
