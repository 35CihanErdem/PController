# PController — Production Roadmap (Play Store)

> Kaynak: `ARCHITECTURE.md` + ChatGPT review + mevcut kod incelemesi.  
> Durum: **Kararlar kilitlendi (aşağıda).** Kod refactor’a henüz girilmedi.  
> Son güncelleme: 2026-08-08

---

## 0. Çalışma sırası (kilit)

```
1) Play Store gereksinimleri netleştir
2) Bluetooth HID çekirdeğini sağlamlaştır
3) Keşfi bonded-only yap
4) Ürün UI + bildirim kontrolleri
5) Cihaz matrisi test
6) Store paketleme (AAB, listing, Data Safety)
```

ChatGPT’nin önerdiği sıra doğru. Refactor, bu dosyadaki **§1 kararlar** onaylandıktan sonra başlar.

---

## 1. Kilitlenen kararlar (§8 cevapları)

| # | Soru | Karar | Gerekçe |
|---|------|--------|---------|
| 1 | Servis iletişimi | **Bound Service + `StateFlow<ConnectionState>`** | FGS zaten şart; state tek kaynak; polling yok |
| 2 | Mimari derinlik | **Minimum doğru yapı** (manager + service + ViewModel). Clean Architecture abartı | Tek özellikli app; aşırı katman maliyeti yüksek |
| 3 | UI teknoloji | **XML + ViewBinding + ViewModel** (Compose’a şimdi geçme) | Mevcut layout var; risk/kapsam düşük; Compose v2 olabilir |
| 4 | Cihaz keşfi | **Bonded-only** + “Sistem Bluetooth ayarlarından eşleştir” CTA | BLE, Windows classic HID host için yanlış araç |
| 5 | Brightness | **v1’de kaldırıldı** — Vol ± + Play/Pause (Consumer Control) | Kullanıcı onayı 2026-08-08 |
| 6 | Descriptor değişimi UX | İlk bağlanışta / HID fail’de: **“PC’den cihazı kaldırıp yeniden eşleştir”** diyaloğu | Windows HID cache gerçeği |
| 7 | FGS tipi | **Önce `connectedDevice` dene**; olmazsa `specialUse` + Play beyanı hazırla | Play inceleme dostu tercih |
| 8 | Tuş seti (geçici Sprint 2) | D-Pad, Space, Esc, **Tam Ekran=F**, Vol±, Play/Pause | Platform mapping Sprint 3 |
| 9 | Bildirim kontrolleri | **Evet — zorunlu farklılaştırıcı**: ◀ ▶ ▲ ▼ (Play/Pause Sprint 3) | Kilit ekranı kullanımı |
| 10 | Marka | **Display: PController** · Package: **`com.cihan.pcontroller`** | Onaylandı 2026-08-08 |
| 11 | Test | Unit: report builder + state machine. HID API mock. Manuel: OEM matrisi | Instrumental tam HID pahalı |
| 12 | minSdk | **28 kalsın** (HID API tabanı). target/compile **35’e yükselt** (Play hedef) | Kapsamı daraltma |
| 13 | Dil | Uygulama **Türkçe öncelik**, store listing TR (+ sonra EN) | Mevcut kullanıcı dili |

### Onay durumu

- [x] Package: `com.cihan.pcontroller`
- [x] Brightness: çıkar
- [x] Sprint 1: BAŞLATILDI / tamamlanma durumu aşağıda

### Sprint 1 ilerleme

- [x] `ConnectionState`
- [x] `HidReports` (+ 16-bit consumer descriptor)
- [x] `HidDeviceManager`
- [x] `BluetoothHidService` — static/companion state kaldırıldı, Binder + StateFlow
- [x] registerApp → registered → connect sırası
- [x] disconnect / virtual unplug event
- [x] polling kaldırıldı
- [x] brightness hack kaldırıldı
- [x] Volume + Play/Pause Consumer Control
- [x] Package rename
- [ ] Cihazda manuel doğrulama (kullanıcı)

> Sprint 1’de layout redesign yok; yalnızca Vol/Play geçici etiket + service wiring.

### Sprint 1 → Sprint 2 geçiş kapısı (smoke test)

Kod kurcalanmaz. Gerçek telefon + Windows’ta geçmeden Sprint 2’ye girilmez.

| # | Senaryo | Beklenen | Sonuç |
|---|---------|----------|-------|
| 1 | Windows’ta eski eşleşmeyi kaldır, yeniden eşleştir | Temiz HID profili | ⬜ |
| 2 | Uygulama aç → PC bağla | `Connected` | ⬜ |
| 3 | ↑ ↓ ← → Not Defteri / tarayıcı | Oklar çalışır | ⬜ |
| 4 | Space | Çalışır | ⬜ |
| 5 | Esc | **Sprint 1’de UI butonu yok** (`KeyCode.ESC` hazır; Sprint 3) — atla veya Logcat/geçici | ⬜ N/A |
| 6 | Vol − / Vol + | Windows sesi değişir | ⬜ |
| 7 | Play/Pause | Medya oynatıcıda toggle | ⬜ |
| 8 | PC Bluetooth kapat | UI ~2 sn içinde bağlı değil | ⬜ |
| 9 | BT aç → yeniden bağlan | Tekrar `Connected` | ⬜ |
| 10 | App arka plan | Bildirim aksiyonları çalışır | ⬜ |
| 11 | Kilit ekranı | Bildirim aksiyonları çalışır | ⬜ |
| 12 | App kapat/aç | Zombie `Connected` yok | ⬜ |

**C disk:** `activity_main.xml` boşalması disk dolmasıydı. Sprint 2 build öncesi C’de rahat boş alan bırak; kod bug’ı gibi debug etme.

---

## 2. Dosya bazlı review

### `AndroidManifest.xml`

| | Madde |
|---|--------|
| **Kesin değiştir** | `applicationId` ile uyumlu package/namespace; gereksiz izinleri budamak (`BLUETOOTH_ADVERTISE` muhtemelen gerekmez bonded-only’de; location BLE kalkınca çoğu senaryoda düşer — Android 11− için scan yoksa location da gidebilir) |
| **Kesin değiştir** | FGS type: `connectedDevice` araştırması / specialUse beyan metni |
| **Kesin değiştir** | `POST_NOTIFICATIONS` runtime isteği (kod tarafı) |
| **Dokunma (şimdilik)** | `exported="false"` service — doğru |
| **Play öncesi çöz** | Privacy Policy URL; Data Safety formu için hangi izinler kaldı net liste |

### `app/build.gradle.kts`

| | Madde |
|---|--------|
| **Kesin değiştir** | `namespace` / `applicationId` → `com.example` kalkacak |
| **Kesin değiştir** | `compileSdk`/`targetSdk` → **35** (Play Store güncel beklenti) |
| **Kesin değiştir** | `isMinifyEnabled = true` (release) + ProGuard kuralları |
| **Kesin değiştir** | Ölü `androidx.media` dependency kaldır (MediaStyle yoksa) |
| **Kesin değiştir** | `lifecycle-viewmodel-ktx` ekle |
| **Dokunma** | minSdk 28 |
| **Play öncesi çöz** | Signing config (upload key), `versionCode`/`versionName` politikası, AAB build |

### `BluetoothHidService.kt` — en kritik dosya

| | Madde |
|---|--------|
| **Kesin değiştir** | `companion object` global state **tamamen kaldır** |
| **Kesin değiştir** | `ConnectionState` + `StateFlow` / binder |
| **Kesin değiştir** | `onAppStatusChanged(registered=true)` → ancak o zaman `connect` kuyruğu |
| **Kesin değiştir** | Disconnect / virtual unplug → state event |
| **Kesin değiştir** | HID descriptor + `createKeyboardReport` / consumer report → ayrı `HidReports` sınıfı |
| **Kesin değiştir** | Brightness dual-send (Consumer+F6/F7) kaldır |
| **Kesin değiştir** | Notification channel’ı her `onCreate`’te silme (B6) |
| **Kesin değiştir** | Notification MediaStyle veya düzenli action set (▲▼◀▶) |
| **Dokunma (çekirdek fikir)** | `BluetoothHidDevice` + `registerApp` + `sendReport` yaklaşımı — doğru API |
| **Dokunma** | Foreground service ihtiyacı (arka plan/kilit için) |
| **Play öncesi çöz** | FGS type nihai seçim + Play Console declaration |

### `MainActivity.kt`

| | Madde |
|---|--------|
| **Kesin değiştir** | BLE scan + `ScanCallback` kaldır |
| **Kesin değiştir** | Polling loop (`delay(500) x 30`) kaldır → state collect |
| **Kesin değiştir** | Bonded list + “Eşleştirmek için Ayarlar’ı aç” |
| **Kesin değiştir** | İzin helper’a taşı; Settings deep-link |
| **Kesin değiştir** | Logic → `RemoteViewModel` |
| **Kesin değiştir** | SPACE/Enter isim tutarlılığı; Esc + media tuşları |
| **Dokunma (şimdilik)** | Tek Activity modeli — yeterli |
| **Dokunma** | ViewBinding’e geçiş XML kalır |
| **Play öncesi çöz** | İlk açılış onboarding (eşleştirme anlatımı) |

### `activity_main.xml`

| | Madde |
|---|--------|
| **Kesin değiştir** | Tek “kumanda” ekranı: üstte durum chip, ortada D-Pad, altta Space/Esc/Vol/Play |
| **Kesin değiştir** | Connection ara ekranını sadeleştir (inline progress yeterli) |
| **Kesin değiştir** | Kullanılmayan / tutarsız string-id’ler |
| **Dokunma** | ConstraintLayout + Material temel — yeniden yazmaya gerek yok, reshape yeterli |
| **Play öncesi çöz** | Screenshot’a uygun koyu/açık tema, adaptive icon |

### `README.md`

| | Madde |
|---|--------|
| **Kesin değiştir** | Gerçek akışa göre yeniden yaz (bonded-only, tuş seti, yeniden eşleştir) |
| **Play öncesi çöz** | Privacy Policy linki, destek e-postası |

---

## 3. Hedef UX (v1 ekran)

```
┌─────────────────────────────┐
│  PController                │
│  ● Bağlı · DESKTOP-PC   [✕] │  ← durum + kes
├─────────────────────────────┤
│           [ ▲ ]             │
│      [ ◀ ] [ ● ] [ ▶ ]      │  ● = Play/Pause veya ortada boş
│           [ ▼ ]             │
│                             │
│   [ Space ]     [ Esc ]     │
│   [ Vol − ]     [ Vol + ]   │
│                             │
│  (bağlı değilse)            │
│  Eşleşmiş cihazlar listesi  │
│  [ Bluetooth ayarlarını aç ]│
└─────────────────────────────┘
```

Bildirim (bağlıyken ongoing):

- Başlık: PController · `PC adı`
- Actions: ◀  ▲  ▼  ▶  (ve mümkünse Play/Pause)

---

## 4. Hedef kod iskeleti (refactor sonrası)

```
com.<package>.pcontroller/
├── PControllerApp.kt
├── ui/
│   ├── MainActivity.kt
│   └── RemoteViewModel.kt
├── bluetooth/
│   ├── ConnectionState.kt          # Idle | Starting | Registered | Connecting | Connected(device) | Failed(reason)
│   ├── HidDeviceManager.kt         # register/connect/disconnect/send
│   ├── HidReports.kt               # descriptor + report bytes
│   └── BondedDeviceRepository.kt   # sadece bondedDevices
├── service/
│   └── BluetoothHidService.kt      # FGS + binder; state manager’a delege
└── util/
    └── PermissionHelper.kt
```

`ConnectionState` tek kaynak. UI ve Notification aynı state’i dinler.

---

## 5. Sprint planı

### Sprint 0 — Karar & Play checklist (kod yok / az kod)
- [ ] Package name onayla
- [ ] Privacy Policy taslağı (Bluetooth ne için, veri toplanıyor mu → hayır)
- [ ] Data Safety: “no data collected” doğrula
- [ ] FGS type kararı research (`connectedDevice` vs `specialUse`)
- [ ] Store listing metin taslağı (TR)

### Sprint 1 — HID çekirdek (kırıcı refactor)
- [ ] `HidReports` + `ConnectionState` + `HidDeviceManager`
- [ ] Service: static kaldır, binder + StateFlow
- [ ] registerApp ready → connect sırası
- [ ] disconnect event
- [ ] Polling sil
- [ ] Brightness dual-hack sil; Vol/Play consumer keys ekle

### Sprint 2 — Keşif + Activity/ViewModel + BT UX
- [x] BLE kaldır, bonded-only
- [x] Ayarlar’a yönlendirme (eşleştir CTA)
- [x] ViewModel (`RemoteViewModel`) + state collect
- [x] İzin akışı + POST_NOTIFICATIONS + **Bluetooth’u Aç** (sistem intent, sessiz açma yok)
- [x] Yeniden eşleştir diyaloğu
- [x] Esc + Tam Ekran (F) + Play/Pause ayrıldı (geçici Genel PC seti)
- [ ] Cihazda smoke test (BT / izin / remote / lifecycle) — **Sprint 3 kapısı**

### Sprint 2 smoke test (geçiş kapısı)

| Alan | Senaryo | Sonuç |
|------|---------|-------|
| BT | Kapalı → Aç → sistem onayı → geri dönüş | ⬜ |
| BT | Açık → doğrudan devam | ⬜ |
| İzin | Red → Tekrar Dene | ⬜ |
| İzin | Kalıcı red → Ayarlara Git | ⬜ |
| Cihaz | Eşleşmiş PC listede | ⬜ |
| Cihaz | Yeni eşleştir → yenile → gelir | ⬜ |
| Remote | Oklar / Space / Esc / F / Play-Pause / Vol | ⬜ |
| Life | Arka plan + kilit + bildirim okları | ⬜ |
| Life | PC BT kapat → state düşer; aç → reconnect | ⬜ |
| Life | App öldür/aç → zombie Connected yok | ⬜ |

### Sprint 3 — Platform profilleri + ürün UI
- [ ] `RemoteAction` (anlamlı aksiyonlar: Fullscreen, SeekBack, NextSlide…)
- [ ] `PlatformProfile` → action → HID mapping (**HidDeviceManager’a gömülmez**)
- [ ] Mod seçici: YouTube, Netflix, Spotify, Sunum, Genel PC
- [ ] Profile göre kumanda grid
- [ ] YouTube: Fullscreen=F, Seek±, nav…
- [ ] Notification actions polish
- [ ] App icon + splash

> Sprint 2’ye yeni özellik sokulmaz. Smoke test temiz → Sprint 3.

### Sprint 4 — Sertleştirme
- [ ] R8/minify + smoke test
- [ ] targetSdk 35
- [ ] Log seviyesi (release’de verbose HID dump yok)
- [ ] Manuel test matrisi: Samsung / Pixel / Xiaomi × Win10 / Win11

### Sprint 5 — Store
- [ ] Release AAB + upload key
- [ ] Screenshots
- [ ] Listing + Data Safety + FGS declaration
- [ ] Internal testing track → closed → production

---

## 6. Play Store öncesi “blocker” listesi

Bunlar olmadan production’a basılmaz:

1. `com.example.*` package **yok**
2. Privacy Policy URL canlı
3. Data Safety formu doldurulmuş
4. FGS tipi + Play beyanı uyumlu
5. targetSdk Play’in kabul ettiği seviyede (35)
6. Release minify açık, imzalı **AAB**
7. Bağlantı kopunca UI ≤2 sn güncellenir (kabul kriteri)
8. En az 2 farklı Android OEM + 1 Windows 11’de manuel “mutlu yol” geçti

---

## 7. Bilinçli dokunulmayacaklar (v1)

- Compose rewrite
- Tam QWERTY klavye
- Gamepad HID
- Çoklu PC aynı anda
- Brightness (v1)
- Analytics SDK (isteğe bağlı sonra; Play Vitals yeter)

---

## 8. Sonraki adım

Sen şunu yaz:

1. Package name (ör. `com.cihan.pcontroller`)
2. Brightness: **çıkar** / **gizli menü**
3. “Sprint 1’e başla” dersen → kod refactor başlar

Bu üçü gelince `ARCHITECTURE.md` §8 “açık sorular” kapanmış sayılır ve Sprint 1 uygulanır.
)
