# sodata-api

## TODO

- Nachführung Datenbezug.md
- more tests
  * Momentan wird mehr oder weniger nur das Hochfahren (inkl. das Erzeugen von ilidata.xml und den STAC-Dateien) geprüft. Auch hier kann schon viel schief gehen aber sollte besser gestestet werden.

## Beschreibung

Das Repository verwaltet den Quellcode der dateibasierten Datenbezugs-APIs. Zum jetzigen Zeitpunkt ist es ein "Filelisting" via ilidata.xml exklusive für INTERLIS-Formate.

## Komponenten

Sodata-API besteht aus einer einzelnen Komponente (einer Spring Boot Anwendung). Sie wiederum ist Bestandteil der funktionalen Einheit "Datenbezug" (https://github.com/sogis/dok/blob/dok/dok_funktionale_einheiten/Documents/Datenbezug/Datenbezug.md). 

## Konfigurieren und Starten

Die Anwendung kann am einfachsten mittels Env-Variablen gesteuert werden. Es stehen aber auch die normalen Spring Boot Konfigurationsmöglichkeiten zur Verfügung (siehe "Externalized Configuration").

| Name | Beschreibung | Standard |
|-----|-----|-----|
| `CONFIG_FILE` | Vollständiger, absoluter Pfad der Themebereitstellungs-Konfigurations-XML-Datei. | `/config/datasearch.xml` |
| `ILI_ILIDATADIR` | Verzeichnis, in das die ilidata.xml-Datei gespeichert wird. | `#{systemProperties['java.io.tmpdir']}` (= Temp-Verzeichnis des OS) |
| `STAC_DIR` | Verzeichnis, in das STAC-Kataloge und -Dateien gespeichert werden. | `#{systemProperties['java.io.tmpdir']}` (= Temp-Verzeichnis des OS) |
| `STAC_ROOT_HREF` | Url, unter der die Anwendung bereitgestellt wird und die STAC-Dateien ausgeliefert werden. | `https://data.geo.so.ch/stac/` (= Temp-Verzeichnis des OS) |
| `STAC_VENV_EXE_PATH` | Verzeichnis, in das das _venv_-Verzeichnis beim Starten der Anwendung entpackt wird. Muss beim Entwickeln `null` sein. | `#{systemProperties['java.io.tmpdir']}` (= Temp-Verzeichnis des OS) |

**Achtung:** In Produktion sollte für das `STAC_DIR` nicht das Temp-Verzeichnis des OS (also die Standardeinstellung) verwendet werden. Es sind sind sämtliche Dateien und Verzeichnisse exponiert (wenn man die Namen kennt).

### Java

Falls die _datasearch.xml_-Datei im Verzeichnis _/config/_ vorliegt, reicht:
```
java -jar target/sodata-api-<X.Y.Z>.jar 
```

Sonst muss die Datei explizit angegeben werden:

```
java -jar target/sodata-api-<X.Y.Z>.jar --app.configFile=/path/to/datasearch.xml
```

### Docker

Die _datasearch.xml_-Datei kann direkt in das Image gebrannt werden. In diesem Fall sollte sie in den Ordner _/config/_ gebrannt werden, was zu folgendem Start-Befehl führt:

```
docker run -p 8080:8080 sogis/sodata-api:latest
```

Wird die Datei nicht in das Image gebrannt, ergibt sich folgender Befehl:

```
docker run -p 8080:8080 -v /path/to/datasearch.xml:/config/datasearch.xml  -e STAC_DIR=/stac/ [-v /path/to/stac:/stac] sogis/sodata-api:latest
```

## Externe Abhängigkeiten

Die ilisite.xml-Datei unserer INTERLIS-Modellablage (https://geo.so.ch/models) zeigt auf diese Anwendung als `subsidiarySite`. Die ilisite.xml-Datei dieser Anwendung verlinkt als `parentSite` zurück.

## Konfiguration und Betrieb in der GDI

@gdi: todo

## Interne Struktur

### ilidata
Die ilimodels.xml- und ilisite.xml-Datei werden statisch vorgehalten. Die ilimodels.xml-Datei ist bewusst vorhanden aber leer. Die ilitools suchen immer auch eine ilimodels.xml-Datei. Damit keine Fehler in den Logfiles (des API-Gateways) auftauchen, wird eine leere Datei erstellt.

Die ilidata.xml-Datei wird einmalig beim Hochfahren aus der datasearch.xml-Datei hergestellt (analog Datensuche) und auf dem Filesystem abgelegt.

Das Datenmodell (von ilidata.xml) erlaubt es nicht auf beliebige Dateien im Internet zu zeigen, sondern erlaubt nur relative Pfade. Aus diesem Grund aggiert die Anwendung als Proxy. D.h. in der ilidata.xml steht ein relativer Pfade, der auf der Serverseite aufgelöst wird. Die Daten wird heruntergeladen und entzippt an den Client geschickt. Das Entzippen ist notwendig, damit ili2db die Daten direkt anhand des Identifiers importieren kann ("Einzeiler").

### STAC
Inspiration ist https://medium.com/graalvm/supercharge-your-java-apps-with-python-ec5d30634d18.

TODO:
- pystac muss die Url kennen, unter der die statischen Json-Dateien bereitgestellt werden. Darum muss dies als Option exponiert werden (`STAC_ROOT_HREF`).

## Entwicklung

### Run

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Build

```
./mvnw clean package
```