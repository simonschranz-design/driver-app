# Fahrtenplan als native Android-App

Dieses Paket baut automatisch (über GitHub) eine installierbare
Android-App, die deine bestehende Web-App (`https://ssc-business.online/fahrtenplan.html`)
in einer echten App-Hülle lädt – mit App-Symbol und Hintergrund-GPS
während der Fahrt (Standort wird in der Collection `driverLocations`
gespeichert). Push-Benachrichtigungen kommen in einem zweiten Schritt
dazu.

## Einmalige Einrichtung (ca. 5 Minuten)

1. Auf [github.com](https://github.com) einloggen (du hast schon einen Account).
2. Oben rechts auf das **„+"** klicken → **„New repository"**.
3. Name eingeben, z. B. `fahrtenplan-app`. **„Public" oder „Private"** – beides geht,
   Private ist etwas diskreter, kostet auf GitHub aber nichts extra.
   „Create repository" klicken.
4. Auf der nächsten Seite den Link **„uploading an existing file"** anklicken
   (steht im Fliesstext unter „…or push an existing repository").
5. **Alle Dateien und Ordner aus diesem Paket** (auch die versteckte
   `.github`-Ordnerstruktur und `.gitignore`!) per Drag & Drop in das
   Upload-Feld ziehen. Falls dein Rechner `.github` als „versteckten
   Ordner" ausblendet: im Datei-Explorer/Finder „versteckte Dateien
   anzeigen" aktivieren, oder das ganze Paket als ZIP anhängen –
   GitHub entpackt es beim Hochladen NICHT automatisch, also am besten
   den entpackten Ordnerinhalt direkt hineinziehen.
6. Unten „Commit changes" klicken.
7. Oben im Repository auf den Reiter **„Actions"** klicken – dort läuft
   jetzt automatisch „Android-App bauen" (dauert ca. 3–5 Minuten).
8. Ist der Lauf fertig (grüner Haken): draufklicken, ganz unten bei
   **„Artifacts"** erscheint **„Fahrtenplan-App"** – anklicken lädt eine
   ZIP-Datei mit der `app-debug.apk` herunter.

## App installieren

1. Die `app-debug.apk` aufs Android-Handy übertragen (z. B. per E-Mail
   an sich selbst, USB-Kabel, oder direkt auf dem Handy herunterladen).
2. Darauf tippen. Falls Android eine Warnung zeigt ("Unbekannte Quelle"):
   das ist normal für Apps ausserhalb des Play Stores – Installation
   trotzdem erlauben (einmalige Berechtigung „Apps aus dieser Quelle
   installieren" für den Browser/Dateimanager aktivieren, Android
   führt dich dabei durch).
3. App öffnen – lädt automatisch eure Fahrtenplan-Seite.

## Wichtig: fahrtenplan.html muss aktualisiert sein

Damit das Hintergrund-GPS funktioniert, muss die neueste `fahrtenplan.html`
mit dem Code-Abschnitt "NATIVE APP (Android): Hintergrund-GPS" auf dem
Server liegen (wurde separat besprochen/geliefert). Ohne diesen Code tut
die App nichts Zusätzliches – sie zeigt einfach eure normale Web-App an.

## Künftige Änderungen an der App selbst

Änderungen an `fahrtenplan.html` (Inhalt/Funktionen) brauchen **keinen**
neuen App-Build – die App lädt die Seite live von eurem Server, genau wie
ein Browser. Ein neuer App-Build ist nur nötig, wenn sich etwas an der
nativen Hülle selbst ändert (z. B. neue Berechtigungen, Push-Benachrichtigungen
werden ergänzt, App-Symbol wird ausgetauscht). In dem Fall: neue Dateien
hier ins gleiche GitHub-Repository hochladen (überschreiben), „Actions"
läuft automatisch neu, neue APK herunterladen und Fahrern zum Update
schicken.
