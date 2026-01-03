# Bluetooth HID Remote Control

Android telefonunuzu Bluetooth HID (Human Interface Device) klavye olarak kullanın. Windows PC'nizde basit bir uzaktan kumanda olarak çalışır.

## 🎮 Özellikler

- **Ok Tuşları**: Yukarı, Aşağı, Sol, Sağ
- **Enter Tuşu**: Enter/Return tuşu
- **Basit Arayüz**: D-Pad düzeni ile kolay kullanım
- **Manuel Bağlantı**: Kullanıcı kontrolünde bağlantı

## 📋 Gereksinimler

- **Android**: 9.0 (API 28) veya üzeri
- **Windows PC**: Bluetooth desteği olan herhangi bir Windows bilgisayar
- **PC Yazılımı**: Gerekmez! Windows'un yerleşik Bluetooth HID desteği kullanılır

## 🔧 Kurulum

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

3. **Projeyi Açın**
   - Android Studio'da `File` → `Open`
   - Proje klasörünü seçin
   - Gradle senkronizasyonunun tamamlanmasını bekleyin

### APK Oluşturma

1. **Release APK Oluşturma**
   - `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Build tamamlandığında `app/build/outputs/apk/release/app-release.apk` dosyası oluşur

2. **APK'yı Telefona Yükleme**
   - APK dosyasını telefonunuza kopyalayın
   - Telefonda `Ayarlar` → `Güvenlik` → `Bilinmeyen Kaynaklardan Yükleme` seçeneğini açın
   - APK dosyasına tıklayarak yükleyin

## 🔌 Windows PC ile Eşleştirme

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
2. **"Tara"** butonuna tıklayın - yakındaki Bluetooth cihazları taranacak
3. Listeden bilgisayarınızı seçin
4. **"Bağlan"** butonuna tıklayın
5. Bağlantı kurulduğunda durum "Bağlı: [Cihaz Adı]" olarak değişecek
6. Artık D-Pad butonları aktif olacak ve kullanılabilir

## 🎯 Kullanım

### Bağlantı Akışı

1. **Cihaz Seçimi**: "Tara" butonuna tıklayın ve listeden PC'nizi seçin
2. **Bağlantı**: "Bağlan" butonuna tıklayın
3. **Girdi Ekranı**: Bağlantı başarılı olduğunda D-Pad ekranı görünecek
4. **Bağlantıyı Kesme**: "Bağlantıyı Kes" butonuna tıklayarak bağlantıyı sonlandırın

### Tuşlar

- **UP**: Yukarı ok tuşu
- **DOWN**: Aşağı ok tuşu
- **LEFT**: Sol ok tuşu
- **RIGHT**: Sağ ok tuşu
- **ENTER**: Enter/Return tuşu

## 🐛 Sorun Giderme

### Bluetooth Bağlantısı Kurulmuyor

**Sorun**: "Bağlantı başarısız" hatası alıyorum.

**Çözümler**:
1. **Bluetooth'u kapatıp açın**: Hem telefonda hem PC'de
2. **Eşleştirmeyi yeniden yapın**: 
   - Windows'ta cihazı kaldırın
   - Telefonda eşleştirmeyi silin
   - Tekrar eşleştirin
3. **Uygulamayı yeniden başlatın**: Uygulamayı tamamen kapatıp tekrar açın
4. **"Tekrar Dene" butonunu kullanın**: Bağlantı ekranında "Tekrar Dene" butonuna tıklayın

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

### Cihaz Listesinde Görünmüyor

**Sorun**: "Tara" butonuna tıkladığımda PC'm görünmüyor.

**Çözümler**:
1. **PC'de Bluetooth'un açık olduğundan emin olun**
2. **PC'yi "eşleştirilebilir" yapın**: Windows Bluetooth ayarlarında "Bluetooth veya diğer cihaz ekle" modunda olun
3. **Taramayı tekrar deneyin**: "Tara" butonuna tekrar tıklayın
4. **Manuel eşleştirme yapın**: Önce Windows ve Android'de manuel olarak eşleştirin, sonra uygulamada bağlanın

### Minimum SDK Hatası

**Sorun**: "minSdk 28 is required" hatası.

**Çözüm**:
- Bu proje Android 9 (API 28) ve üzeri gerektirir
- Telefonunuzun Android sürümünü kontrol edin: `Ayarlar` → `Telefon Hakkında` → `Android Sürümü`
- Android 9'dan eski bir cihaz kullanıyorsanız, proje çalışmayacaktır

## 📱 Teknik Detaylar

- **Minimum SDK**: API 28 (Android 9.0)
- **Target SDK**: API 34 (Android 14)
- **Dil**: Kotlin
- **Bluetooth Profili**: HID Device (Human Interface Device)
- **Servis Tipi**: Foreground Service (arka planda çalışma için)
- **Mimari**: Tek aktivite, basit UI

## ⚠️ Önemli Notlar

⚠️ **PC'de Yazılım Gerektirmez**: Bu uygulama, PC'nizde herhangi bir yazılım kurulumu gerektirmez. Windows'un yerleşik Bluetooth HID desteği kullanılır.

⚠️ **İlk Bağlantı**: İlk bağlantıda Windows, cihazı bir klavye olarak tanıyabilir ve bir PIN kodu isteyebilir. Genellikle 0000 veya 1234 çalışır.

⚠️ **Güvenlik**: Bu uygulama, bağlı olduğu cihaza klavye tuşları gönderebilir. Sadece güvendiğiniz cihazlarla eşleştirin.

⚠️ **Manuel Bağlantı**: Uygulama otomatik bağlanmaz. Her seferinde kullanıcı "Bağlan" butonuna tıklamalıdır.

## 📄 Lisans

Bu proje eğitim amaçlıdır ve özgürce kullanılabilir.

## 🆘 Destek

Sorun yaşarsanız:
1. Yukarıdaki "Sorun Giderme" bölümünü kontrol edin
2. Android Studio'nun Logcat penceresinde hata mesajlarını kontrol edin
3. Bluetooth ayarlarınızı kontrol edin

---

**İyi kullanımlar! 🎮📱**
