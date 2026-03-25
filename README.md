# Phyphox BLE Emulator (Android)

Produktionsnahes Kotlin/Compose-Tool, das ein phyphox-kompatibles BLE-Peripheral mit GATT-Server emuliert. Die App übernimmt die Rolle der phyphox-ESP32-Library (Advertising, Services/Characteristics, Read/Write/Notify, optionaler Experimenttransfer), aber alle Daten werden lokal in der UI erzeugt.

## MVP-Umfang

- BLE Peripheral/GATT Server via `BlePeripheralManager`
- Advertising start/stop in der UI
- Demo-Service mit:
  - `float32` little-endian Charakteristik (Slider)
  - `uInt8` Charakteristik (Toggle)
  - Write-Charakteristik für eingehende Daten von phyphox
  - Trigger/Event-Charakteristik (Button)
- Live-Logs für BLE-Ereignisse
- Grundgerüst für phyphox-Experimenttransfer (`cddf0001-...` Service)

## Projektstruktur

```text
app/src/main/java/com/example/phyphoxemulator/
  MainActivity.kt
  ble/
    BlePeripheralManager.kt
    PhyphoxExperimentTransfer.kt
  data/
    LogRepository.kt
  domain/
    Models.kt
  presentation/
    MainScreen.kt
    MainViewModel.kt
  profile/
    DemoProfiles.kt
  simulation/
    SimulationEngine.kt
```

## Build

1. Android Studio (aktuell, AGP 8.6+) öffnen.
2. Projekt synchronisieren.
3. Auf einem **echten Android-Gerät** starten (BLE Peripheral funktioniert Emulator-seitig meist nicht).

CLI (wenn Android SDK installiert):

```bash
./gradlew :app:assembleDebug
```

## phyphox-Testablauf

1. App starten und BLE-Rechte erlauben.
2. `Start Advertising` drücken.
3. In phyphox ein BLE-Experiment öffnen oder ein passendes bestehendes Setup nutzen.
4. Nach Gerät `phyphox-emu` suchen und verbinden.
5. Slider/Toggle/Trigger in der App ändern; in phyphox Read/Notify prüfen.
6. Write-Operationen aus phyphox senden und `Last Write` + Logs in der App kontrollieren.

## Bekannte Android/BLE-Einschränkungen

- BLE Peripheral wird nicht von allen Geräten/ROMs zuverlässig unterstützt.
- Advertising mit Gerätename kann je nach Hersteller gecacht/überschrieben sein.
- MTU/Chunking ist je nach Central unterschiedlich; Experimenttransfer ist im MVP als Basis implementiert, nicht als vollständiger phyphox-Produktivstack.

## Annahmen und TODOs

- UUIDs des Demo-Telemetrieservices sind frei gewählt (stabile Demo-ID).
- phyphox-Transferservice ist mit den gewünschten UUIDs angelegt; Protokolldetails für vollständigen Chunk-/Header-Handshake sind als nächste Ausbaustufe vorgesehen.
- Profilarchitektur ist bereits datengetrieben (`DeviceProfile`), aktuell mit hardcodiertem Profil.
