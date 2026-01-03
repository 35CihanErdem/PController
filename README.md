# PController - Bluetooth HID Klavye

Android telefonunuzu Bluetooth HID (Human Interface Device) klavye olarak kullanın. PC'nizde film izlerken uzaktan kumanda olarak kullanabilirsiniz. YouTube, Netflix, HBO Max ve diğer tüm medya platformlarında çalışır!

## 🎮 Özellikler

## Özellikler

- **Play/Pause**: Boşluk tuşu
- **İleri**: Sağ ok tuşu
- **Geri**: Sol ok tuşu
- **Ses Aç**: Yukarı ok tuşu
- **Ses Kapat**: Aşağı ok tuşu
- **Tam Ekran**: F tuşu

## Gereksinimler

### Android Studio Kurulumu

1. **Android Studio İndirin**
   - [Android Studio resmi sitesinden](https://developer.android.com/studio) indirin
   - Kurulum sırasında "Standard" kurulum seçeneğini seçin

2. **SDK Bileşenlerini Yükleyin**
   - Android Studio'yu açın
   - `File` → `Settings` (Windows) veya `Android Studio` → `Preferences` (Mac)
   - `Appearance & Behavior` → `System Settings` → `Android SDK`
   - Şu bileşenlerin yüklü olduğundan emin olun:
     - ✅ Android SDK Platform 34
     - ✅ Android SDK Build-Tools
     - ✅ Android SDK Platform-Tools
     - ✅ Android Emulator
     - ✅ Intel x86 Emulator Accelerator (HAXM installer) - eğer Intel işlemci kullanıyorsanız

3. **Gradle Senkronizasyonu**
   - Projeyi açtıktan sonra Android Studio otomatik olarak Gradle dosyalarını indirecektir
   - İlk açılışta biraz zaman alabilir (5-10 dakika)

## Projeyi Açma

1. Android Studio'yu açın
2. `File` → `Open`
3. `geri-sar` klasörünü seçin
4. "Trust Project" butonuna tıklayın
5. Gradle senkronizasyonunun tamamlanmasını bekleyin

## Fiziksel Cihaza Yükleme

### 1. Geliştirici Seçeneklerini Aktifleştirme

1. Telefonunuzda `Ayarlar` → `Telefon Hakkında` (veya `Cihaz Hakkında`)
2. `Yapı Numarası` (Build Number) seçeneğini **7 kez** üst üste tıklayın
3. "Geliştirici oldunuz!" mesajını göreceksiniz

### 2. USB Hata Ayıklamayı Aktifleştirme

1. `Ayarlar` → `Geliştirici Seçenekleri`
2. `USB Hata Ayıklama` seçeneğini **AÇIK** yapın
3. Onay penceresinde "Tamam" deyin

### 3. Telefonu Bilgisayara Bağlama

1. USB kablosu ile telefonu bilgisayara bağlayın
2. Telefonda "USB hata ayıklamaya izin ver" bildirimine **İzin Ver** deyin
3. Android Studio'da telefonunuzun göründüğünü kontrol edin (üstteki cihaz seçici)

### 4. Uygulamayı Çalıştırma

1. Android Studio'da yeşil ▶️ (Run) butonuna tıklayın
2. Veya `Run` → `Run 'app'` menüsünü kullanın
3. Cihaz seçim penceresinde telefonunuzu seçin
4. Uygulama telefonunuza yüklenecek ve otomatik olarak açılacak

## Windows PC ile Eşleştirme

### 1. Windows'ta Bluetooth Ayarları

1. Windows'ta `Ayarlar` → `Cihazlar` → `Bluetooth ve diğer cihazlar`
2. Bluetooth'un **AÇIK** olduğundan emin olun
3. "Bluetooth veya diğer cihaz ekle" butonuna tıklayın

### 2. Android Telefonda Eşleştirme

1. Telefonunuzda `Ayarlar` → `Bağlantılar` → `Bluetooth`
2. Bluetooth'u **AÇIK** yapın
3. "Yeni cihaz eşleştir" veya "Yeni cihaz ara" seçeneğini seçin
4. Bilgisayarınızın adını listede bulun ve eşleştirin
5. Windows'ta eşleştirme isteğini onaylayın
6. Her iki tarafta da PIN kodu girmeniz gerekebilir (genellikle 0000 veya 1234)

### 3. Uygulamada Bağlanma

1. Uygulamayı açın
2. "Bağlan" butonuna tıklayın
3. Açılan listeden bilgisayarınızı seçin
4. Bağlantı kurulduğunda durum "Bağlı: [Cihaz Adı]" olarak değişecek
5. Artık butonlar aktif olacak ve kullanılabilir

## Test Etme

### VLC Media Player ile Test

1. VLC'de bir video açın
2. Uygulamadaki butonları test edin:
   - **Play/Pause**: Videoyu duraklatır/devam ettirir
   - **İleri/Geri**: 10 saniye ileri/geri atlar
   - **Ses Aç/Kapat**: Ses seviyesini ayarlar
   - **Tam Ekran**: Tam ekran moduna geçer

### YouTube (Tarayıcı) ile Test

1. YouTube'da bir video açın
2. Butonları test edin:
   - **Play/Pause**: Video oynatmayı kontrol eder
   - **İleri/Geri**: Video zaman çizelgesinde ileri/geri gider
   - **Ses Aç/Kapat**: Tarayıcı ses seviyesini ayarlar
   - **Tam Ekran**: YouTube tam ekran moduna geçer

## Sorun Giderme

### Bluetooth Bağlantısı Kurulmuyor

**Sorun**: Uygulamada "Bağlantı başarısız" hatası alıyorum.

**Çözümler**:
1. **Bluetooth'u kapatıp açın**: Hem telefonda hem PC'de
2. **Eşleştirmeyi yeniden yapın**: 
   - Windows'ta cihazı kaldırın
   - Telefonda eşleştirmeyi silin
   - Tekrar eşleştirin
3. **Uygulamayı yeniden başlatın**: Uygulamayı tamamen kapatıp tekrar açın
4. **Servisi yeniden başlatın**: Uygulamada "Bağlantıyı Kes" sonra tekrar "Bağlan"

### İzinler Sorunu

**Sorun**: "Bluetooth izni gerekli" mesajı görüyorum.

**Çözüm**:
1. `Ayarlar` → `Uygulamalar` → `Bluetooth Keyboard`
2. `İzinler` bölümüne gidin
3. Tüm Bluetooth izinlerini **İzin Ver** yapın
4. Android 13+ için bildirim iznini de verin

### Tuşlar Çalışmıyor

**Sorun**: Bağlantı var ama tuşlar çalışmıyor.

**Çözümler**:
1. **Bağlantıyı kontrol edin**: Durum metninde "Bağlı" yazıyor mu?
2. **Uygulamayı yeniden başlatın**: Uygulamayı kapatıp açın
3. **PC'yi yeniden başlatın**: Bazen Windows Bluetooth stack'i yeniden başlatmak gerekir
4. **Başka bir uygulamada test edin**: Not Defteri'nde tuşların çalışıp çalışmadığını kontrol edin

### Android Studio Proje Açılmıyor

**Sorun**: Gradle senkronizasyon hatası alıyorum.

**Çözümler**:
1. **İnternet bağlantınızı kontrol edin**: Gradle dosyalarını indirmek için internet gerekli
2. **Gradle'ı manuel senkronize edin**: `File` → `Sync Project with Gradle Files`
3. **Gradle cache'i temizleyin**: 
   - `File` → `Invalidate Caches / Restart`
   - "Invalidate and Restart" seçeneğini seçin

### Minimum SDK Hatası

**Sorun**: "minSdk 28 is required" hatası.

**Çözüm**:
- Bu proje Android 9 (API 28) ve üzeri gerektirir
- Telefonunuzun Android sürümünü kontrol edin: `Ayarlar` → `Telefon Hakkında` → `Android Sürümü`
- Android 9'dan eski bir cihaz kullanıyorsanız, proje çalışmayacaktır

## Teknik Detaylar

- **Minimum SDK**: API 28 (Android 9.0)
- **Target SDK**: API 34 (Android 14)
- **Dil**: Kotlin
- **Bluetooth Profili**: HID Device (Human Interface Device)
- **Servis Tipi**: Foreground Service (arka planda çalışma için)

## Önemli Notlar

⚠️ **PC'de Yazılım Gerektirmez**: Bu uygulama, PC'nizde herhangi bir yazılım kurulumu gerektirmez. Windows'un yerleşik Bluetooth HID desteği kullanılır.

⚠️ **İlk Bağlantı**: İlk bağlantıda Windows, cihazı bir klavye olarak tanıyabilir ve bir PIN kodu isteyebilir. Genellikle 0000 veya 1234 çalışır.

⚠️ **Güvenlik**: Bu uygulama, bağlı olduğu cihaza klavye tuşları gönderebilir. Sadece güvendiğiniz cihazlarla eşleştirin.

## Lisans

Bu proje eğitim amaçlıdır ve özgürce kullanılabilir.

## Destek

Sorun yaşarsanız:
1. Yukarıdaki "Sorun Giderme" bölümünü kontrol edin
2. Android Studio'nun Logcat penceresinde hata mesajlarını kontrol edin
3. Bluetooth ayarlarınızı kontrol edin

---

**İyi kullanımlar! 🎬📱**

