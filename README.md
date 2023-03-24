# sodata-api

## TODO

- Nachführung Datenbezug.md
- more tests
- stac api


## Beschreibung

Das Repository verwaltet den Quellcode der dateibasierten Datenbezugs-APIs. Zum jetzigen Zeitpunkt ist es ein "Filelisting" via ilidata.xml exklusive für INTERLIS-Formate.

## Komponenten

Sodata-API besteht aus einer einzelnen Komponente (einer Spring Boot Anwendung). Sie wiederum ist Bestandteil der funktionalen Einheit "Datenbezug" (https://github.com/sogis/dok/blob/dok/dok_funktionale_einheiten/Documents/Datenbezug/Datenbezug.md). 

## Konfigurieren und Starten

Die Anwendung kann am einfachsten mittels Env-Variablen gesteuert werden. Es stehen aber auch die normalen Spring Boot Konfigurationsmöglichkeiten zur Verfügung (siehe "Externalized Configuration").

| Name | Beschreibung | Standard |
|-----|-----|-----|
| `CONFIG_FILE` | Vollständiger, absoluter Pfad der Themebereitstellungs-Konfigurations-XML-Datei. | `/config/datasearch.xml` |
| `ILIDATA_DIR` | Verzeichnis, in das die ilidata.xml-Datei gespeichert wird. | `#{systemProperties['java.io.tmpdir']}` (= Temp-Verzeichnis des OS) |


### Java

Falls die _datasearch.xml_-Datei im Verzeichnis _/config/_ vorliegt, reicht:
```
java -jar build/libs/sodata-api-<X.Y.Z>-exec.jar 
```

Sonst muss die Datei explizit angegeben werden:

```
java -jar build/libs/sodata-api-<X.Y.Z>-exec.jar --app.configFile=/path/to/datasearch.xml
```

### Docker

Die _datasearch.xml_-Datei kann direkt in das Image gebrannt werden. In diesem Fall sollte sie in den Ordner _/config/_ gebrannt werden, was zu folgendem Start-Befehl führt:

```
docker run -p 8080:8080 sogis/sodata-api:latest
```

Wird die Datei nicht in das Image gebrannt, ergibt sich folgender Befehl:

```
docker run -p 8080:8080 -v /path/to/datasearch.xml:/config/datasearch.xml sogis/sodata-api:latest
```

## Externe Abhängigkeiten

Die ilisite.xml-Datei unserer INTERLIS-Modellablage (https://geo.so.ch/models) zeigt auf diese Anwendung als `subsidiarySite`.

## Konfiguration und Betrieb in der GDI

@gdi: todo

## Interne Struktur

Die ilimodels.xml- und ilisite.xml-Datei werden statisch vorgehalten. Die ilimodels.xml-Datei ist bewusst vorhanden aber leer. Die ilitools suchen immer auch eine ilimodels.xml-Datei. Damit keine Fehler in den Logfiles (des API-Gateways) auftauchen, wird eine leere Datei erstellt.

Die ilidata.xml-Datei wird einmalig beim Hochfahren aus der datasearch.xml-Datei hergestellt (analog Datensuche) und auf dem Filesystem abgelegt.

Das Datenmodell (von ilidata.xml) erlaubt es nicht auf beliebige Dateien im Internet zu zeigen, sondern erlaubt nur relative Pfade. Aus diesem Grund aggiert die Anwendung als Proxy. D.h. in der ilidata.xml steht ein relativer Pfade, der auf der Serverseite aufgelöst wird. Die Daten wird heruntergeladen und entzippt an den Client geschickt. Das Entzippen ist notwendig, damit ili2db die Daten direkt anhand des Identifiers importieren kann ("Einzeiler").

## Entwicklung

### Run

Starten mit Spring Tools (Eclipse). Unter "Run configurations - Arguments" `-Dspring.profiles.active=dev` hinzufügen.

### Build

```
./gradlew clean build
```