# PC Controller — Gizlilik Politikası (Privacy Policy)

**Son güncelleme:** 10 Ağustos 2026  
**Geliştirici:** Cihan Erdem Diker  
**İletişim:** systemtenis@gmail.com  
**Uygulama:** PC Controller (`com.cihan.pccontroller`)

Bu metin Google Play ve kullanıcı bilgilendirmesi için hazırlanmıştır.
Yayınlandığında kalıcı bir HTTPS URL’de (ör. GitHub Pages) barındırılmalıdır.

---

## 1. Özet

PC Controller, Android telefonunuzu Bluetooth üzerinden bilgisayara
**HID (klavye / fare / medya)** cihazı gibi bağlayan bir kumanda uygulamasıdır.

- Hesap oluşturmaz.
- Sunucuya kişisel veri göndermez.
- Reklam veya analitik SDK’sı kullanmaz (mevcut sürüm).

---

## 2. Toplanan veriler

### 2.1 Cihazda kalan veriler

Uygulama yalnızca cihazınızda şu tür bilgileri tutabilir:

- Son seçilen platform / klavye düzeni gibi **tercihler** (SharedPreferences)
- Sistem Bluetooth eşleştirmelerinden görünen **cihaz adları ve adresleri**
  (yalnızca bağlantı kurmak için; sunucuya yüklenmez)

### 2.2 Bluetooth

Uygulama Bluetooth izinlerini şu amaçlarla kullanır:

- Eşleştirilmiş bilgisayarları listelemek
- HID profili ile bağlanmak
- Klavye, fare ve medya tuş raporları göndermek

Bluetooth trafiği telefon ile seçtiğiniz bilgisayar arasındadır;
geliştiriciye iletilmez.

### 2.3 Toplanmayanlar

- Konum geçmişi satışı / reklam profilleme
- Kişisel kimlik bilgisi (ad, e-posta hesabı zorunlu değil)
- Yazılan metinlerin buluta kaydı (tuşlar HID olarak PC’ye gider;
  geliştirici sunucusuna gitmez)

---

## 3. İzinler

| İzin | Amaç |
|------|------|
| Bluetooth / Bluetooth Connect / Scan / Advertise | HID bağlantısı |
| Foreground service (connected device) | Bağlantı sürerken bildirim |
| Bildirimler | Bağlantı durumu ve medya kısayolları |

---

## 4. Üçüncü taraflar

Uygulama, YouTube / Netflix / Spotify vb. servislere **resmi entegrasyon
yapmaz**; yalnızca bilgisayarınıza klavye/fare benzeri giriş gönderir.
Bu platformların kendi gizlilik politikaları geçerlidir.

Açık kaynak kütüphaneler: `THIRD_PARTY_LICENSES.md`.

---

## 5. Çocuklar

Uygulama çocuklara yönelik tasarlanmamıştır. 13 yaş altından bilerek
veri toplanmaz.

---

## 6. Değişiklikler

Politika güncellenirse bu belgedeki tarih yenilenir. Önemli değişikliklerde
uygulama veya store açıklamasında duyuru yapılabilir.

---

## 7. İletişim

Sorular, öneriler veya veri talepleri için: **systemtenis@gmail.com**

---

*Bu belge genel bilgilendirme amaçlıdır; hukuki tavsiye değildir.*
