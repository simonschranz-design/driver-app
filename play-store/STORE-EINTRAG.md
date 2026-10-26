# Texte und Angaben für die Google Play Console

Alles hier kann 1:1 in die Play Console kopiert werden. Grafiken liegen in diesem Ordner:
`app-icon-512.png` (App-Symbol) und `feature-graphic-1024x500.png` (Vorstellungsgrafik).
Screenshots: mit der Test-App auf dem Handy 2–8 Bildschirmfotos machen (z. B. Auftragsliste, Checkliste, Auftrag-Details).
**Keine echten Kundennamen oder Personendaten auf den Screenshots** – am besten mit dem Testzugang aufnehmen.

---

## Store-Eintrag (Deutsch – Hauptsprache)

**App-Name** (max. 30 Zeichen)
```
Fahrtenplan – Fahrer-App
```

**Kurzbeschreibung** (max. 80 Zeichen)
```
Aufträge, Checklisten, Fotos und Fahrzeug-Standort für Fahrer im Transport
```

**Vollständige Beschreibung**
```
Fahrtenplan ist die Fahrer-App zur Dispositions-Software Fahrtenplan für Transportunternehmen. Die Zugänge richtet dein Arbeitgeber ein – mit diesen Anmeldedaten meldest du dich in der App an.

Für Fahrerinnen und Fahrer:
• Zugeteilte Aufträge mit Lade- und Abladestellen, Zeitfenstern und Hinweisen der Disposition
• Auftragsschritte mit einem Tipp melden: angekommen, geladen, abgeliefert
• Lieferscheine, CMR und Zollpapiere fotografieren und direkt übermitteln
• Fahrzeug-Übernahme und -Rückgabe mit Checkliste, Kilometerstand und Fotos
• Schäden mit Fotos melden
• Unterschrift des Empfängers auf dem Handy erfassen
• Nachrichten mit der Disposition austauschen, Dokumente öffnen
• Ferienanträge stellen
• Mehrsprachig: Deutsch, Englisch, Französisch, Italienisch, Polnisch, Rumänisch, Ungarisch

Fahrzeug-Standort während der Arbeit:
Solange du als Fahrer angemeldet bist, übermittelt die App den Standort deines Fahrzeugs an die Disposition – auch im Hintergrund. So kann die Disposition Touren planen, Kunden die Ankunftszeit mitteilen und gefahrene Strecken dokumentieren. Eine Benachrichtigung zeigt an, wenn der Standort übertragen wird; beim Abmelden endet die Erfassung.

Hinweis: Die App funktioniert nur mit einem Zugang, den dein Arbeitgeber in Fahrtenplan für dich angelegt hat.
```

**Kategorie:** Wirtschaft (Business) · **Tags:** Logistik, Transport, Disposition
**E-Mail-Adresse für Nutzer** (öffentlich sichtbar): eine Support-Adresse, z. B. support@ssc-business.com
**Website:** https://ssc-business.com
**Datenschutzerklärung:** https://ssc-business.com/datenschutz

## Store-Eintrag (Englisch – zusätzliche Sprache, empfohlen)

**App name**
```
Fahrtenplan – Driver App
```
**Short description**
```
Jobs, vehicle checklists, photos and vehicle location for transport drivers
```
**Full description**
```
Fahrtenplan is the driver app for the Fahrtenplan dispatch software for transport companies. Your employer creates your account – log in with those credentials.

For drivers:
• Assigned jobs with loading and unloading points, time windows and notes from dispatch
• Report job steps with one tap: arrived, loaded, delivered
• Photograph delivery notes, CMR and customs documents and send them right away
• Vehicle handover and return with checklist, mileage and photos
• Report damage with photos
• Capture the recipient's signature on the phone
• Chat with dispatch, open documents, request holidays
• Available in German, English, French, Italian, Polish, Romanian and Hungarian

Vehicle location during work:
While you are logged in as a driver, the app sends your vehicle's location to dispatch – also in the background – so dispatch can plan tours, tell customers the arrival time and document routes. A notification is shown while the location is being transmitted; logging out stops it.

Note: the app only works with an account your employer has set up for you in Fahrtenplan.
```

---

## App-Inhalte (Menü «Richtlinien → App-Inhalte»)

| Frage | Antwort |
|---|---|
| Datenschutzerklärung | `https://ssc-business.com/datenschutz` |
| App-Zugriff | «Alle oder einige Funktionen sind eingeschränkt» → neue Anleitung: Name «Demo-Fahrer», Benutzername `demo.fahrer`, Passwort (aus `tenant demo`), bei «Weitere Informationen» den Text unten einfügen |
| Werbung | Nein, die App enthält keine Werbung |
| Einstufung des Inhalts | Fragebogen: Kategorie «Dienstprogramm/Produktivität», alle Fragen zu Gewalt, Sex, Glücksspiel usw. mit **Nein** → Ergebnis «Alle/ab 3» |
| Zielgruppe | **18 und älter** (nur «18+» ankreuzen) |
| Nachrichten-App | Nein |
| Behörden-App | Nein |
| Finanzfunktionen | Keine |
| Gesundheit | Keine Gesundheitsfunktionen |
| Werbe-ID | **Nein**, die App verwendet keine Werbe-ID |

**Text für «Weitere Informationen» beim App-Zugriff** (Englisch, für die Prüfer):
```
1. Start the app. On the first screen enter the company code: demo  → tap "Continue".
2. Log in with the username and password above (demo driver account of a fictitious test company, no real data).
3. Before the jobs are shown, the app asks for the vehicle check ("Rundum-Kontrolle"): tick all items as OK and send.
4. The app then shows today's jobs. Location: the app explains location use first ("Agree"), then Android asks for the permission.
```

### Datensicherheit (Data safety)

| Frage | Antwort |
|---|---|
| Erhebt oder teilt die App Nutzerdaten? | **Ja** |
| Sind alle Daten bei der Übertragung verschlüsselt? | **Ja** |
| Können Nutzer das Löschen ihrer Daten beantragen? | **Ja** – Link: `https://ssc-business.com/datenschutz` |
| Können Nutzer in der App ein Konto erstellen? | **Nein** – Konten werden vom Arbeitgeber angelegt |
| Werden Daten mit Dritten **geteilt**? | **Nein** (nur Dienstleister, die im Auftrag arbeiten – zählt laut Google nicht als «Teilen») |

Erhobene Datentypen (jeweils «erhoben», nicht «geteilt», Zweck **App-Funktionalität**, verarbeitet nicht nur vorübergehend):

| Datentyp | Pflicht/optional | Zusätzlicher Zweck |
|---|---|---|
| Standort → **Genauer Standort** | Pflicht (für Fahrer) | – |
| Persönliche Daten → **Name** | Pflicht | Kontoverwaltung |
| Persönliche Daten → **Nutzer-IDs** (Benutzername) | Pflicht | Kontoverwaltung |
| Fotos und Videos → **Fotos** | Optional | – |
| Dateien und Dokumente | Optional | – |
| Nachrichten → **Andere In-App-Nachrichten** | Optional | – |
| App-Informationen und -Leistung → **Andere App-Leistungsdaten** (App-Version, Gerätemodell, GPS-/Akku-Status) | Pflicht | Analyse (Fehlersuche) |

### Berechtigungserklärungen (erscheinen nach dem ersten Upload)

**Standort im Hintergrund (ACCESS_BACKGROUND_LOCATION)**
- Hauptfunktion, die den Standort im Hintergrund braucht:
```
Fahrzeug-Ortung für die Disposition von Transportunternehmen. Solange ein Fahrer angemeldet ist, sendet die App den Standort seines Fahrzeugs an die Disposition seines Arbeitgebers, damit Touren geplant, Kunden über die Ankunftszeit informiert und gefahrene Strecken dokumentiert werden können. Fahrer haben die App während der Fahrt nicht im Vordergrund (Bildschirm aus, Navigation läuft), deshalb ist die Übertragung im Hintergrund nötig. Vor der Abfrage erklärt ein Hinweis-Dialog die Nutzung; während der Übertragung ist eine Benachrichtigung sichtbar; beim Abmelden endet die Erfassung.
```
- Video (YouTube, «nicht gelistet», 30–60 Sek.): Anmelden als Testfahrer → Hinweis-Dialog «Standortfreigabe während der Arbeit» → «Zustimmen» → Android-Abfrage → «Immer zulassen» → Benachrichtigung «Standort wird übertragen» → App schliessen, Benachrichtigung bleibt.

**Vordergrunddienst (FOREGROUND_SERVICE_LOCATION)**
- Typ: **Standort** · Beschreibung:
```
Der Vordergrunddienst überträgt während der Arbeitszeit den Fahrzeugstandort an die Disposition (Fahrzeug-Ortung). Er läuft nur, solange ein Fahrer angemeldet ist, und zeigt dauerhaft eine Benachrichtigung. Die Aufgabe kann nicht aufgeschoben werden, weil die Disposition den aktuellen Standort laufend braucht.
```
- Video: das gleiche Video wie oben.
