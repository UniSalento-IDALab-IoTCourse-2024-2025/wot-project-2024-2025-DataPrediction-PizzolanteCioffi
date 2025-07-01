
Il microservizio espone le seguenti funzionalità tramite API RESTful:
elaborazione dei dati giornalieri dell’utente per la costruzione dell’input al modello predittivo;
interazione con il microservizio AIModel per l’inferenza del comportamento;
salvataggio dei risultati dell’inferenza nel database;
aggiornamento dello stato comportamentale dell’utente nel sistema;
monitoraggio dei comportamenti critici e generazione di notifiche in caso di anomalie;
consultazione delle predizioni archiviate.
DataPrediction interagisce con i seguenti componenti:
con User, per validare l’identità del paziente prima di associare i dati al modello e garantire l’integrità del flusso;
con DataCollector, da cui prende i dati da elaborare;
con AIModel, a cui invia il record di input da classificare e da cui riceve la predizione del behaviour;
con il Frontend, per consentire la visualizzazione dei risultati dell’analisi da parte dell’utente o del medico.
La comunicazione con il Frontend e con gli altri microservizi avviene tramite API RESTful oppure, in alcuni casi, attraverso messaggi MQTT veicolati dal message broker RabbitMQ. Questo consente sia l'interazione sincrona con l'interfaccia utente, sia una comunicazione asincrona e decentrata tra alcuni moduli interni al sistema.
