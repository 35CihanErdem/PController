# PC Controller — Mimari Brief & Üretim Planı

> Amaç: Bu doküman mevcut sistemi, bilinen hataları ve “piyasaya sürülebilir” hedefi netleştirir.  
> Diğer AI / review’lerden mimari, UX, Bluetooth HID ve Android lifecycle konusunda fikir toplamak için yazılmıştır.  
> **Henüz refactor uygulanmadı** — karar ve tasarım aşaması.

---

## 1. Ürün Özeti

| Alan | Değer |
|------|--------|
| **Ürün adı (geçici)** | PC Controller / Bluetooth Keyboard |
| **Platform** | Android (minSdk 28 / Android 9+, targetSdk 34) |
| **Dil** | Kotlin |
| **UI** | XML + ViewBinding (açık ama kullanılmıyor; findViewById var) |
| **Temel vaat** | Android telefonu Bluetooth HID klavye olarak kullan; Windows PC’de ek yazılım gerekmez |
| **Hedef kullanım** | Medya kontrolü, sunum/navigasyon, basit uzaktan kumanda (D-Pad, Space, F, parlaklık) |
| **Hedef kalite** | Store’a çıkabilecek kadar stabil, öngörülebilir, bakımı kolay |

### Kullanıcı hikayesi (mutlu yol)

1. Kullanıcı Windows’ta Bluetooth açar, telefonda da Bluetooth açar.
2. İki cihaz klasik Bluetooth ile eşleşir (veya daha önce eşleşmiştir).
3. Uygulama açılır → izinler alınır → eşleşmiş cihazlar listelenir.
4. Kullanıcı PC’yi seçer → HID bağlantısı kurulur.
5. D-Pad / Space / F / parlaklık tuşları PC’de gerçek klavye/consumer HID event üretir.
6. Bildirimden (kilit ekranı dahil) temel yön tuşları kullanılabilir.
7. Bağlantı kesilebilir; servis temiz kapanır.

---

## 2. Mevcut Dosya Yapısı

```
PC Controller/
└── PC Controller/                          # Gradle root
    ├── README.md
    ├── ARCHITECTURE.md                   # Bu dosya
    ├── build.gradle.kts                  # AGP 8.5.1, Kotlin 1.9.20
    ├── settings.gradle.kts               # rootProject.name = "PC Controller"
    ├── gradle.properties
    ├── app/
    │   ├── build.gradle.kts
    │   ├── proguard-rules.pro
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       ├── java/com/example/bluetoothkeyboard/
    │       │   ├── MainActivity.kt       # UI, tarama, bağlantı orkestrasyonu
    │       │   └── BluetoothHidService.kt# HID register, connect, sendReport, FGS
    │       └── res/
    │           ├── layout/activity_main.xml
    │           ├── values/{strings,colors,themes}.xml
    │           ├── color/button_text_color.xml
    │           └── drawable/{button_*, status_card_*}.xml
    └── gradle/wrapper/
```

### Modül / sınıf sorumlulukları (bugün)

| Bileşen | Sorumluluk | Not |
|---------|------------|-----|
| `MainActivity` | İzinler, BLE tarama + paired list, ekran geçişleri, tuş tıklamaları | ~630 satır, adapter aynı dosyada |
| `DeviceAdapter` | Cihaz listesi RecyclerView | `simple_list_item_1` |
| `BluetoothHidService` | FGS, HID profile proxy, registerApp, connect/disconnect, sendKey, brightness, notification actions | Static companion ile global state |
| `KeyCode` enum | UP/DOWN/LEFT/RIGHT/SPACE/F/NONE | Enter yok; UI’da SPACE “enterButton” id’sinde |

---

## 3. Teknik Yığın

```
Android App (single module :app)
├── UI: AppCompat + Material + ConstraintLayout + RecyclerView
├── Bluetooth Classic HID Device API (BluetoothHidDevice, API 28+)
├── BLE Scanner (cihaz keşfi için — tartışmalı, bkz. §6)
├── Foreground Service (specialUse)
├── Notifications (NotificationCompat + action buttons)
└── Coroutines (lifecycleScope — sadece bağlantı polling için)
```

### Bağımlılıklar (`app/build.gradle.kts`)

- `androidx.core:core-ktx:1.12.0`
- `androidx.appcompat:appcompat:1.6.1`
- `com.google.android.material:material:1.11.0`
- `constraintlayout:2.1.4`
- `lifecycle-runtime-ktx:2.6.2`
- `recyclerview:1.3.2`
- `androidx.media:media:1.7.0` (MediaStyle yorum satırında; fiilen kullanılmıyor)

### Manifest izinleri

- `BLUETOOTH`, `BLUETOOTH_ADMIN` (eski API)
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`
- `POST_NOTIFICATIONS`
- Feature: `android.hardware.bluetooth` required=true
- Service: `foregroundServiceType="specialUse"` + PROPERTY_SPECIAL_USE_FGS_SUBTYPE

---

## 4. Mevcut Mimari (As-Is)

```
┌─────────────────┐         Intent(CONNECT/DISCONNECT/ACTION_*)        ┌──────────────────────┐
│  MainActivity   │ ──────────────────────────────────────────────────► │ BluetoothHidService  │
│                 │                                                     │                      │
│  - izinler      │ ◄── polling: BluetoothHidService.isConnected() ──── │  companion object:   │
│  - scan/paired  │     (500ms x 30, callback yok)                      │   hidDevice          │
│  - UI screens   │                                                     │   connectedDevice    │
│  - tuş click →  │ ── static call: sendKey() / sendBrightness*() ────► │   isServiceConnected │
│    Service.send │                                                     │                      │
└─────────────────┘                                                     │  HID registerApp     │
                                                                        │  sendReport(id,bytes)│
                                                                        │  FGS notification    │
                                                                        └──────────┬───────────┘
                                                                                   │
                                                                                   ▼
                                                                        ┌──────────────────────┐
                                                                        │ Windows PC (BT Host) │
                                                                        │ HID Keyboard +       │
                                                                        │ Consumer Control     │
                                                                        └──────────────────────┘
```

### Ekran akışı (tek Activity, visibility toggle)

1. **Device Selection** — Tara / cihaz listesi  
2. **Connection** — Bağlanıyor / Tekrar Dene  
3. **Input** — D-Pad, SPACE, F, Parlaklık ±, Bağlantıyı Kes  

### HID Report Descriptor (özet)

- **Report ID 1** — Standard keyboard (modifiers + reserved + 6 keycodes), LED output dahil  
- **Report ID 2** — Consumer Control, 1 byte usage (0x00–0xFF), brightness için 0x6F / 0x70 denemesi  

### Tuş mapping (bugün)

| UI | KeyCode / aksiyon | HID |
|----|-------------------|-----|
| UP/DOWN/LEFT/RIGHT | arrow | 0x52 / 0x51 / 0x50 / 0x4F |
| SPACE (id: enterButton) | SPACE | 0x2C |
| F (Tam Ekran) | F | 0x09 |
| Parlaklık + | Consumer 0x6F + gecikmeli F7 (0x40) | Report 2 + Report 1 |
| Parlaklık − | Consumer 0x70 + gecikmeli F6 (0x3F) | Report 2 + Report 1 |
| Bildirim Sol/Sağ/Yukarı/Aşağı | ACTION_* | aynı oklar |

Key press → 100–150ms sonra boş report (release).

---

## 5. Hedef Mimari (To-Be) — Üretim Kalitesi

> Bu bölüm “önerilen hedef”. Diğer AI’lardan alternatif / onay / red bekleniyor.

### 5.1 Katmanlar

```
app/
├── ui/
│   ├── MainActivity (veya tek Compose Activity)
│   ├── screens: PairingScreen, ConnectingScreen, RemoteScreen
│   └── RemoteViewModel
├── bluetooth/
│   ├── HidDeviceManager          # register/connect/disconnect/sendReport
│   ├── DeviceDiscovery           # bonded devices (+ optional classic discovery)
│   └── HidReports / Descriptor   # report builder, key map
├── service/
│   └── BluetoothHidService       # sadece FGS + binding/callback köprüsü
├── domain/
│   ├── ConnectionState           # Idle | Ready | Connecting | Connected | Failed
│   └── RemoteAction              # sealed class: Arrow, Space, F, Brightness...
└── util/
    └── PermissionHelper
```

### 5.2 İletişim modeli (kritik değişiklik)

**Kaldırılacak:** Activity’nin `isConnected()` polling’i ve Service companion static state’e bağımlılık.

**Önerilen:**

- `BluetoothHidService` ↔ UI: `LocalBroadcast` / `SharedFlow` / `Messenger` / **bound service + StateFlow**
- `ConnectionState` tek kaynak (single source of truth) serviste veya manager’da
- UI sadece state’e abone olur; tuşlar `RemoteAction` gönderir
- Process death / Activity recreate sonrası state restore

### 5.3 Keşif / eşleştirme politikası (karar gerekli)

**Öneri A (tercih adayı):** Sadece **bonded (eşleşmiş)** cihazları listele. “Sistem Bluetooth ayarlarından eşleştir” CTA’sı. BLE scan kaldır veya “gelişmiş” opsiyon yap.

**Öneri B:** Classic Bluetooth discovery (`startDiscovery`) — HID host’lar için BLE’den daha doğru olabilir; UX ve izin maliyeti var.

**Öneri C:** Mevcut BLE + bonded karışımı — basit ama semantik olarak hatalı; üretimde riskli.

### 5.4 HID / tuş politikası

- Tek, test edilmiş report descriptor
- Brightness: ya güvenilir Consumer Control **veya** bilinen media shortcut; **ikisini peş peşe gönderme**
- Press/release: tutma (hold/repeat) desteği opsiyonel (uzun basınca tekrar)
- Enter vs Space: ürün kararı netleştirilsin (medya play/pause için Space doğru olabilir)

### 5.5 Ürünleşme checklist (Store / “piyasaya sür”)

- [ ] `applicationId` / namespace: `com.example` → gerçek paket adı
- [ ] Uygulama adı, ikon, splash, marka
- [ ] Privacy policy (Bluetooth, konum gerekçesi)
- [ ] Play Console: specialUse FGS beyanı / gerekçe
- [ ] Android 13+ `POST_NOTIFICATIONS` akışı
- [ ] ProGuard/R8 minify açık + kurallar
- [ ] Crash-free: try/catch yerine structured error + kullanıcı mesajı
- [ ] Analytics / crash reporting (opsiyonel: Play Vitals yeterli olabilir)
- [ ] Dark/light tema, erişilebilirlik (büyük dokunma alanları zaten var)
- [ ] En az manuel test matrisi: Samsung / Pixel / Xiaomi × Windows 10/11
- [ ] Bağlantı kopunca UI anında “Bağlantı yok”
- [ ] Battery: gereksiz scan yok, FGS sadece bağlıyken veya bağlanırken

---

## 6. Bilinen Buglar, Riskler ve Teknik Borç

### Kritik / yüksek

| ID | Sorun | Etki |
|----|--------|------|
| B1 | Service state `companion object` static | Leak, yanlış “bağlı” durumu, process restart sonrası tutarsızlık |
| B2 | Bağlantı sonucu polling (15 sn timeout) | Geç / yanlış UI; race condition |
| B3 | BLE scan ile klasik HID host arama | PC görünmeyebilir veya alakasız BLE cihazlar listelenir |
| B4 | `registerApp` henüz bitmeden `connect` | “HID service not ready” / bağlantı başarısız |
| B5 | Brightness: Consumer + F6/F7 peş peşe | Yanlış tuş, çift event, güvenilmez davranış |
| B6 | `onCreate` içinde notification channel silinip yeniden yaratılıyor | Test artığı; üretimde anlamsız / zararlı |

### Orta

| ID | Sorun | Etki |
|----|--------|------|
| B7 | ViewBinding açık, kullanılmıyor | Boş yere build complexity |
| B8 | İzin kontrolü kopyala-yapıştır (birçok yer) | Bakım hatası, SecurityException riski |
| B9 | README “Enter”, UI “SPACE”, id `enterButton` | Kullanıcı / dokümantasyon karışıklığı |
| B10 | MediaStyle yorumda, media dependency ölü | Gereksiz bağımlılık |
| B11 | `DeviceAdapter` sistem list item | Marka / UX zayıf |
| B12 | Bağlantı kesilince UI’ya event yok (sadece onResume kontrolü) | Kullanıcı “bağlı” sanıp tuşa basabilir |
| B13 | Foreground service `specialUse` | Play inceleme riski; alternatif type araştırılmalı |
| B14 | Release minify kapalı | APK boyutu / tersine mühendislik |

### Düşük / kozmetik

| ID | Sorun |
|----|--------|
| B15 | Log spam (her report byte dump) |
| B16 | İngilizce/Türkçe karışık stringler |
| B17 | Drawable’lar layout’ta kısmen kullanılmıyor olabilir |
| B18 | `showConnectionScreen` var ama akış çoğu zaman seçimde direkt `connect()` |

### Platform riskleri (ürün seviyesinde)

- Bazı OEM’lerde `BluetoothHidDevice` kısıtlı / flaky (özellikle Çin markaları).
- Windows bazen cihazı “klavyesiz” veya “başka cihaz” olarak sınıflandırır; yeniden eşleştirme gerekir.
- HID descriptor değişince Windows cache’i eski descriptor’ı tutabilir → “unpair + pair” şart.
- Android 14+ FGS kısıtları ve Play “special use” gerekçesi.

---

## 7. Önerilen Refactor Sırası (uygulama planı)

1. **Stabil çekirdek**
   - HidDeviceManager + ConnectionState
   - Service binding / StateFlow
   - registerApp hazır olunca connect
   - Disconnect event → UI
2. **Keşif sadeleştirme**
   - Bonded-only (veya classic discovery kararı)
   - BLE’yi kaldır / opsiyonel yap
3. **HID temizliği**
   - Descriptor + report builder tek yerde
   - Brightness tek yöntem
   - Hold/repeat (opsiyonel)
4. **UI polish**
   - Tutarlı isimler (Space vs Enter)
   - ViewBinding veya Compose
   - Hata / boş / yükleniyor state’leri
5. **Ürünleşme**
   - Paket adı, ikon, store metinleri, minify, test matrisi

---

## 8. Diğer AI’lardan İstenen Geri Bildirim

Lütfen şu sorulara cevap verin (kısa, karar odaklı):

### Mimari
1. Bound Service + StateFlow mı, yoksa daha basit bir yaklaşım mı (ör. sadece Application-scoped manager)?
2. Clean Architecture / çok katman abartı mı bu ölçekte? Minimum doğru yapı nedir?
3. Compose’a geçmek şart mı, yoksa XML + ViewModel yeterli mi?

### Bluetooth / HID
4. Windows host için cihaz keşfi: bonded-only mu, classic discovery mı, BLE mi?
5. Consumer Control brightness report formatı (1-byte vs 2-byte usage) Windows 10/11’de hangisi güvenilir?
6. HID descriptor değişikliğinde kullanıcıya “yeniden eşleştir” zorunluluğu nasıl UX’e yedirilir?
7. `FOREGROUND_SERVICE_SPECIAL_USE` yerine Play-dostu alternatif var mı (connectedDevice vb.)?

### Ürün / UX
8. Minimal tuş seti nedir? (D-Pad + Space yeterli mi? Volume? Media play/pause? ESC?)
9. Bildirimden kontrol şart mı, yoksa sadece uygulama içi mi?
10. Marka adı: “PC Controller” mı, “Bluetooth Keyboard” mı, başka mı?

### Kalite
11. Hangi testler otomatize edilmeli (unit vs instrumented)? HID’i nasıl mock’larız?
12. Crash / ANR riskleri listesinde kaçırdığımız var mı?
13. minSdk 28’i yükseltmeli miyiz (ör. 29/31) bakım için?

---

## 9. Kabul Kriterleri (“mükemmel / piyasaya hazır” tanımı)

Aşağıdakilerin **hepsi** sağlanmadan “hazır” sayılmaz:

1. Eşleşmiş bir Windows PC’ye 3 denemeden ≤1’inde bağlantı kurulur (aynı telefon + PC).
2. Bağlıyken ok tuşları Not Defteri’nde doğru karakter/navigasyon üretir.
3. Bağlantı PC veya telefonda kesilince UI ≤2 sn içinde “bağlı değil” gösterir; tuşlar disabled.
4. Uygulama arka planda / kilitliyken (FGS açıksa) en az bildirim aksiyonları çalışır **veya** ürün kararıyla bilinçli olarak kapatılır.
5. İzin reddinde net mesaj + Ayarlar’a yönlendirme.
6. Process kill sonrası zombie “bağlı” state kalmaz.
7. Logcat’te SecurityException / NPE üretmez (normal akışta).
8. Paket adı `com.example` değildir; release imzalı APK/AAB build edilir.
9. README gerçek davranışı yansıtır (Space/Enter tutarlı).

---

## 10. Bilinçli Kapsam Dışı (şimdilik)

- iOS / desktop companion app
- Tam QWERTY sanal klavye
- Çok cihaz aynı anda
- Cloud hesap / sync
- Oyun controller (Gamepad HID) profili (ileride ayrı epic olabilir)

---

## 11. Hızlı Referans — Mevcut Kod Dokunuş Noktaları

| Konu | Dosya |
|------|--------|
| UI + scan + connect polling | `MainActivity.kt` |
| HID register / send / FGS | `BluetoothHidService.kt` |
| Layout / ekranlar | `res/layout/activity_main.xml` |
| İzin + service type | `AndroidManifest.xml` |
| SDK / deps | `app/build.gradle.kts` |
| Kullanıcı dokümantasyonu | `README.md` |

---

**Son not (insan + AI işbirliği):**  
§8 cevapları + ChatGPT review → `PRODUCTION_ROADMAP.md` içinde kilitlendi.  
Kod refactor’a **Sprint 1 onayı + package name** sonrası geçilir. Refactor öncesi bu brief’i değiştirme; roadmap esas alınır.
)
