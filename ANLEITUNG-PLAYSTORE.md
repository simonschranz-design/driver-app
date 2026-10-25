# Fahrtenplan-App in den Google Play Store bringen

Version 4.0 ist für den Play Store vorbereitet:

- **Android 16 (API 36):** von Google seit 31.08.2026 für neue Apps und Updates verlangt. Dafür Wechsel auf Capacitor 8.
- **App-ID `com.sscbusiness.fahrtenplan`:** passend zur neuen Domain. Die bisherige APK hatte `online.sscbusiness.fahrtenplan`. Beide können parallel installiert sein; nach dem Umstieg die alte App löschen.
- **Hinweis-Dialog vor der Standort-Abfrage:** von Google verlangt («prominent disclosure»), in 7 Sprachen.
- **Akku-Hinweis:** führt jetzt zu den App-Einstellungen. Die eingeschränkte Berechtigung «Akku-Optimierung direkt ausschalten» ist entfernt, weil Google sie oft ablehnt.
- **Eigenes App-Symbol und Startbild** statt des Capacitor-Logos.
- **Kein Google-Backup** von Anmeldung und Standortdaten mehr.
- **Signiertes App-Bundle (.aab):** wird bei jedem Lauf automatisch gebaut, die Versionsnummer zählt selbst hoch.
- **Test-APK «Fahrtenplan Test»:** zum direkten Installieren, läuft neben der Play-Store-App.
- Nicht mehr benötigte Plugins (altes Hintergrund-GPS, Geolocation) sind entfernt; das Tracking läuft über den eigenen Dienst.

---

## Schritt 1 – Dateien ins GitHub-Repository laden

Repository `driver-app` öffnen, dann **Add file → Upload files**.
Den **Inhalt** des entpackten Ordners hineinziehen, also die Ordner `.github`, `android`, `play-store`, `www` und die Dateien.
Danach auf **Commit changes** klicken.

## Schritt 2 – Upload-Schlüssel als «Secrets» hinterlegen (einmalig)

`Settings → Secrets and variables → Actions → New repository secret`, viermal.
Namen und Werte stehen in **Play-Store-Schluessel-GEHEIM.txt**:

| Name | Wert |
|---|---|
| `FP_KEY_ALIAS` | aus der Datei |
| `FP_KEYSTORE_PASSWORD` | aus der Datei |
| `FP_KEY_PASSWORD` | aus der Datei |
| `FP_UPLOAD_KEYSTORE_BASE64` | die lange Zeile aus der Datei |

Danach: `Actions → Android-App bauen → Run workflow`.
Nach ca. 5 Minuten stehen unten bei **Artifacts** zwei Downloads:

- **Fahrtenplan-Test-APK:** zum Ausprobieren auf dem eigenen Handy.
- **Fahrtenplan-PlayStore:** enthält die `.aab`-Datei für Google.

Schlüssel-Datei und Passwörter sicher aufbewahren, z. B. im Passwort-Manager, und die TXT-Datei danach aus «Downloads» löschen.

**Tipp:** Das Repository auf **privat** stellen: `Settings → General → Danger Zone → Change visibility`.

## Schritt 3 – Test-APK ausprobieren

Installieren, als Fahrer anmelden und prüfen:

- Hinweis-Dialog erscheint, dann «Zustimmen», dann Standort «Immer zulassen».
- Die Benachrichtigung «Standort wird übertragen» ist sichtbar.
- Ein Foto in der Checkliste hochladen funktioniert.
- Oben und unten ist nichts von der Status- oder Navigationsleiste verdeckt.

## Schritt 4 – Google-Play-Entwicklerkonto

Auf https://play.google.com/console registrieren. Einmalig 25 USD, mit Ausweis-Prüfung.

- **Als Privatperson:** Neue private Konten müssen die App zuerst **14 Tage lang mit mindestens 12 Testern** im «geschlossenen Test» haben, bevor sie öffentlich («Produktion») gehen darf. Für Münger passt das gut: Die Fahrer sind die Tester.
- **Als Organisation:** z. B. Firma; braucht eine D-U-N-S-Nummer, gratis bei Dun & Bradstreet, dauert einige Tage. Dafür gilt die 12-Tester-Regel nicht.

## Schritt 5 – Testzugang für die Prüfer von Google

Google prüft die App von Hand und braucht dafür einen Login.

1. Im Fahrtenplan einen Fahrer anlegen, z. B. Benutzername `playtest`.
2. Ihm 1–2 Beispiel-Aufträge zuteilen.
3. Benutzername und Passwort in der Play Console unter **App-Inhalte → App-Zugriff** eintragen.
4. Nach der Prüfung den Zugang deaktivieren.

## Schritt 6 – App in der Play Console anlegen

1. **App erstellen** → Name «Fahrtenplan – Fahrer-App», Sprache Deutsch, App, kostenlos.
2. **App-Inhalte** ausfüllen: Antworten in `play-store/STORE-EINTRAG.md`.
3. **Store-Eintrag** ausfüllen: Texte und Grafiken aus `play-store/` sowie Screenshots vom Handy.
4. **Testen → Interner Test** → neuen Release erstellen → die `.aab`-Datei hochladen.
   - Bei der ersten Frage **«Play App-Signatur» akzeptieren**.
   - Danach erscheinen die Berechtigungserklärungen (Standort im Hintergrund, Vordergrunddienst). Texte stehen in `STORE-EINTRAG.md`; dazu ein kurzes Video aufnehmen und auf YouTube «nicht gelistet» hochladen.
5. **Testen → Geschlossener Test** → Testerliste mit den Gmail-Adressen der Fahrer → Release übernehmen.
   - Die Fahrer bekommen einen Link und installieren die App normal über den Play Store.
6. Nach 14 Tagen mit mindestens 12 Testern: **Produktion beantragen** (nur bei privatem Konto nötig).

## Neue Versionen

Etwas an den Dateien ändern und hochladen. GitHub baut automatisch, die Versionsnummer zählt selbst hoch. Danach die neue `.aab` in der Play Console als neuen Release hochladen.

Änderungen an der Web-App auf dem Server brauchen **keinen** neuen App-Build. Die App lädt die Seite live.
