# Mini Tutorial für den Umgang mit SSL Zertifikaten in Java für HTTPS

## Intro
Wenn ein Webservice unter HTTPS läuft, so wird nicht nur die Kommunikation der beiden Parteien verschlüsselt, sondern der Cient überprüft am Anfang der Kommunikation ebenfalls, ob er dem Server vertrauen kann oder nicht. Dazu sendet der Server dem Client sein Zertifikat. 
Mit dem Zertifikat wendet sich der Client an eine Vertrauenswürdige Domäne im Internet und fragt diese, ob das Zertifikat vertrauenswürdig ist.

Hier wird der Prozess ganz gut beschrieben:
https://de.wikipedia.org/wiki/X.509

In manchen Fällen verfügt aber der Server nicht über ein Zertifikat, das in einem dieser certificate authority (CA) Server bekannt ist, da der Registrierungsprozess nicht ganz billig ist (z.B. eine Entwicklungsumgebung). 
Möchte man mit diesen Servern dennoch reden, muss man sie explizid für den Client bekannt machen.

Im Falle von Java Programmen gibt es hierfür mehr oder weniger gangbare Wege. In diesem Tutorial beschreibe ich wie man die Java Bordmittel verwendet, ohne in das Coding des Clients einzugreifen.

Die Java Runtime hat hierfür eine eigene Datei, um alle gültigen Zertifikate zu verwalten. Man muss also "lediglich" das Zertifikat des Servers der Zertifikatverwaltung hinzufügen, damit dieses als gültig akzeptiert wird.

Ich führe in diesem Tutorial nachvollziehbar durch die einzelnen Schritte vom Aufbau eines kleinen HTTPS Servers als Versuchsstation bis hin zum Verpacken des Clients in einem Docker Container.   

## Einen privaten Schlüssel erzeugen
Zuerst erzeugen wir uns einen neuen privaten Schlüssel, mit dem der HTTPS Demo Server geschützt werden soll. Hierfür liefert das Java Runtime Environment (jre) eine Eigene Applikation mit aus. Das keytool.

```
keytool -genkeypair -alias https_test -keyalg RSA -keysize 2048 -keystore http_test_key_store.jks -validity 3650 -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -storepass MeinPasswort24
```

Man wird von `keytool` nach einigen persönlichen Angaben befragt und muss die Erzeugung des Key Store mit `yes` bestätigen. 

Das Ergebnis ist die Datei `http_test_key_store.jks`. Sie ist ein Java key store mit dem neu erstellten Schlüssel und dem neuen Zertifikat. (Man findet die Datei im Verzeichnis `KryptoFiles`)



## Einen Demo HTTPS Server starten
Im Verzeichniss `HttpsServer` befindet sich ein rudimentärer REST Server, der per HTTPS geschützt ist. Um in Spring Boot HTTPS zu aktivieren ist eigentlich nicht viel notwendig.

1. Den Java Key Store `http_test_key_store.jks` in das Verzeichnis `HttpsServer\src\main\resources\keystore` kopieren
2. In der application.properties https aktivieren, den Pfad zum KeyStore und dessen Passwort eintragen.

Nun kann man den Server im Verzeichnis `HttpsServer` mit `mvn spring-boot:run`starten.

Öffnet man jetzt die Seite https:\\localhost:8081 sagt einem der Browser, dass siese Seite unsicher ist, da das Zertifikat nicht verifiziert werden kann.


## Einen Demo HTTS Client starten
Im Verzeichnis `HttpsClient` befindet sich eine rudimentäre REST Client Application. Die Application ruft einfach andere unterschiedliche Webseiten auf und wirft das Ergebnis im aus. Es ist quasi eine Art Proxy auf http basis. Im Coding und der Konfiguration wird kein Zertifikat verwendet. Sprich das Coding selbst weiß nicht, ob es einen HTTP oder HTTPS endpunkt aufruft und wie man mit Zertifikaten umgeht.  

Wenn man die Applikation im Verzeichnis `HttpsClient` mit `mvn spring-boot:run` startet und folgende Entpunkte im Browser öffnet, bekommt man eine valide Ergebnisse:

* http://localhost:8080/global-http ==> proxy zu http://www.magic-inside.de
* http://localhost:8080/global-https ==> proxy zu https://google.com

Ruft man hingegen den Proxy für https:\\localhost:8081 auf, bekommt man eine Fehlermeldung, die besagt, dass das Zertifikat nicht geprüft werden kann:

* http://localhost:8080/local-https ==> `PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target`


## Dem JRE "beibringen" unserem eingenen Zertifikat zu vertrauen
Wichtig ist an diesem Punkt, sich nocheinmal die Strategie vor Auge zu führen: Wir bringen nicht dem Code bei wie man ein Zertifikat prüft, sondern dem Java Runtime Environment (jre)

### Den KeyStore mit dem Zertifikat besorgen
Als erstes müssen wir uns das Zertifikat des HTTPS Servers besorgen. Eigentlich ist das keine große Sache. Man kann das zum Beispiel mit dem Browser machen. Allerdings brauchen wir das Zertifikat in einer Form, die man in den jre keystore importieren kann. Hier kommt ein kleines Tool zum Einsatz, der einem das Leben extrem vereinfacht:

https://github.com/escline/InstallCert

Das ist eine Java Klasse, die einem die wesentlichen Schritte zusammenfast. Im Verzeichnis InstallCert findet man sie bereits kompiliert. 

Im Verzeichnis `InstallCert` ruft folgender Aufruf das Zertifikat von unserer HTTPS Server Demo Applikation auf und speichert es als KeyStore:

```
java InstallCert localhost:8081
```

1. Zunächst sollte man beim Ersten Aufruf eine Exeption sehen, die beweist, dass die jre das Zertifikat noch nicht kennt. 
2. Darunter findet man eine nummerierte Liste mit den Zertifikaten des Servers. In unserem Falle ist das nur eins.
3. Durch eingabe der Nummer des entsprechenden Zertifikats wird dieses heruntergeladen. In unserem Falle ist das somit die 1
4. Dadurch wird die Datei `jssecacerts`erstellt (`Added certificate to keystore 'jssecacerts' using alias 'localhost-1'`). Diese habe ich in das Verzeichnis `KryptoFiles` verschoben. Man muss sich den Namen des alias merken. In unserem Falle `localhost-1`

### Das Zertifikat aus dem KeyStore extrahieren
Nun müssen wir aus dem erstellten KeyStore im Verzeichnis `KryptoFiles` das Zertifikat herauslösen. Dazu verwenden wir wieder das `keytool`:

```
keytool -exportcert -alias localhost-1 -keystore jssecacerts -storepass changeit -file https_test_certificat.cer
```

Als Ergebnis erhält man die Datei `https_test_certificat.cer` im Verzeichnis `KryptoFiles`


### Das Zertifikal im KeyStore des jre importieren
Im Letzten Schritt müssen wir zunächst herausfinden, wo der KeyStore des jre auf der Festplatte liegt. Unter Linux geht das einfach mit `which java`. Unter Windows sollte man einen Blick in die %PATH% Variable werfen `echo %PATH%`. Bei mir ist das `C:\Program Files\Java\jdk-11\`.
Der jre KeyStore liegt dann unter `[mein Pfad zum jre]\lib\security\cacerts`

Wenn man diesen Pfad kennt, kann man den Key Importieren *Man muss hierfür aber Admin sein!*

Im Verzeichnis `KryptoFiles` ruft man das `Keytool` mit folgenden Parmaetern auf:

```
keytool -importcert -alias localhost-1 -keystore "C:\Program Files\Java\jdk-11\lib\security\cacerts" -storepass changeit -file https_test_certificat.cer
```

1. Es wird einem der Inhalt des Zertifikats angezeigt
2. Man bestätigt den Import mit "yes"
3. Falls es bereits ein Zertifikat mit dem gleichen Namen im KeyStore geben solte, muss man dieses erst entfernen:
```
keytool -delete -alias localhost-1 -keystore "C:\Program Files\Java\jdk-11\lib\security\cacerts" -storepass changeit
```


### Der Test 
Im Verzeichnis `HttpsClient` 

1. Die Application HttpsClient stoppen falls nicht schon geschehen
2. Die Applikation mit `mvn spring-boot:run` neu starten
3. Die Seite http://localhost:8080/local-https öffnen ==> `This is the respons of a HTTPS Server`

