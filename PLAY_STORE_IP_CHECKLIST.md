# Play Store öncesi — IP / lisans / telif checklist

ChatGPT’nin ayrımı doğru: **telif lisansı satın almak zorunda değilsin**.
Önce sahiplik kaydı + üçüncü taraf IP temizliği + marka riski.

Durum tarihi: **2026-08-10**

---

## A. Projede eklenen belgeler

| Dosya | Durum | Not |
|-------|--------|-----|
| `COPYRIGHT.md` | ✅ Eklendi | © 2026 Cihan Erdem Diker |
| `LICENSE` | ✅ Eklendi | Proprietary / All rights reserved |
| `THIRD_PARTY_LICENSES.md` | ✅ Eklendi | AndroidX / Material / Kotlin |
| `PRIVACY_POLICY.md` | ✅ Eklendi | Play için HTTPS’te yayınlanacak |

---

## B. Telif (senin kodun)

- [x] Kod senin / ekibin — otomatik telif doğar; “lisans satın al” gerekmez
- [x] Sahiplik metni repoda (`COPYRIGHT.md`)
- [ ] İsteğe bağlı: önemli sürüm zip’ini tarih damgalı sakla / noter / WIPO depozito
- [ ] Release APK + git tag (`v1.0.0`) ile kanıt zinciri

---

## C. Üçüncü taraf / Play IP

| Madde | Durum | Aksiyon |
|-------|--------|---------|
| Kendi Kotlin kodu | ✅ | — |
| Kendi UI (XML/drawable) | ✅ | — |
| Özel font | ✅ Yok | Sistem fontu |
| Dependency lisansları | ✅ Listelendi | Release öncesi `gradlew :app:dependencies` bir kez daha |
| Analytics / ads SDK | ✅ Yok | Eklersen politikayı güncelle |
| Store screenshot’ları | ⏳ | Kendi cihazından çek; başkasının UI’sını kopyalama |
| **YouTube / Netflix / Spotify logoları** | ⚠️ Dikkat | Uygulamada logo **yok** (iyi). Store graphics’te de **kullanma** |
| Platform isimleri metin olarak | ⚠️ Dikkat | “YouTube kısayol seti” gibi **açıklayıcı** kal; resmi uygulama izlenimi verme |
| Store açıklaması | ⏳ | “Unofficial”, “üçüncü taraf markaların sahibi değiliz” cümlesi ekle |

Önerilen store disclaimer (TR):

> PC Controller bağımsız bir Bluetooth HID kumandasıdır. YouTube, Netflix,
> Spotify ve diğer markalar sahiplerine aittir; PC Controller bu şirketlerle
> bağlantılı veya onaylı değildir.

---

## D. Marka

**Eski ad `PController` çakışıyordu** (Play’de “PController Wifi Mouse & Media”,
`com.pc.controller`, https://pcontroller.net/).

**Güncel ürün adı: PC Controller** · paket: `com.cihan.pccontroller` ✅  
**Play Store başlığı (hedef):** `PC Controller – Bluetooth Remote`

- [x] Display name PC Controller (launcher)
- [x] applicationId `com.cihan.pccontroller`
- [ ] Store title / short description metadata
- [ ] Launcher ikon
- [ ] Benzer isimli “PController Wifi Mouse & Media” ile karışıklığa karşı listing’de net fark (Bluetooth HID, PC yazılımı yok, vb.)
- [ ] TÜRKPATENT / hedef pazar hızlı marka araması (tavsiye)

---

## E. Play Console zorunlulukları (IP dışı ama yayın için)

- [ ] Privacy Policy **HTTPS URL** (GitHub Pages / kendi site)
- [ ] Data safety formu (Bluetooth; veri satışı yok)
- [ ] Foreground service / bildirim beyanı
- [ ] Hedef kitle + içerik derecelendirmesi
- [ ] İmza: release keystore (debug ile Play’e çıkma)

---

## F. Monetization (tek app + Pro IAP)

- [x] Free = kumanda / platformlar
- [x] Pro = Touchpad + Klavye (UI kilidi)
- [x] `ProStore` + product id `pro_unlock`
- [ ] Play Console’da one-time product
- [ ] Play Billing Library bağla
- [ ] Restore purchases

Debug: Mouse/Klavye’ye bas → Pro dialog → **test için aç**

---

## Tek cümlelik karar

**Telif lisansı alma.**  
**Dependency’ler temiz (Apache-2.0 ağırlıklı).**  
**Marka: PC Controller** · Free/Pro tek uygulama · Play listing logo yasağı + privacy URL.
