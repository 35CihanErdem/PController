# Third-Party Licenses — PC Controller

Bu dosya, PC Controller ile birlikte kullanılan açık kaynak / üçüncü taraf
bileşenlerin lisans bilgisini listeler. Uygulamanın kendi kodu
`LICENSE` ve `COPYRIGHT.md` kapsamındadır.

Son tarama: **2026-08-10** (`app/build.gradle.kts`)

---

## Özet

| Bileşen | Sürüm (proje) | Lisans |
|---------|---------------|--------|
| Kotlin stdlib (toolchain) | Android Gradle Plugin ile | Apache-2.0 |
| AndroidX Core KTX | 1.12.0 | Apache-2.0 |
| AndroidX AppCompat | 1.6.1 | Apache-2.0 |
| Google Material Components | 1.11.0 | Apache-2.0 |
| AndroidX ConstraintLayout | 2.1.4 | Apache-2.0 |
| AndroidX Activity KTX | 1.8.2 | Apache-2.0 |
| AndroidX Lifecycle Runtime / ViewModel / Service | 2.6.2 | Apache-2.0 |
| AndroidX RecyclerView | 1.3.2 | Apache-2.0 |
| AndroidX Media | 1.7.1 | Apache-2.0 |
| Android SDK / platform APIs | compileSdk 34 | [Android Software Development Kit License](https://developer.android.com/studio/terms) |

Transitive (dolaylı) bağımlılıklar da çoğunlukla **Apache-2.0** altındadır
(AndroidX / Material ekosistemi). Release öncesi `./gradlew :app:dependencies`
çıktısıyla doğrulanması önerilir.

---

## Fontlar

Projede özel `.ttf` / `.otf` font dosyası **yok**.
Sistem / Material varsayılan tipografi kullanılıyor → ek font lisansı gerekmiyor.

---

## Kullanılmayan / yok

- Firebase, Analytics, Ads SDK — yok
- Proprietary UI kit / ücretli ikon paketi — yok (vektör drawable’lar özgün)

---

## Apache License 2.0 — kısa not

Apache-2.0 bileşenler:

- Kaynak kodda / NOTICE’da belirtilen telif bildirimlerini korumayı,
- Büyük değişikliklerde uygun NOTICE eklemeyi,
- Yazılımı “AS IS” kabul etmeyi gerektirir.

Tam metin: https://www.apache.org/licenses/LICENSE-2.0

Uygulama içi “Açık kaynak lisansları” ekranı eklemek Play / kullanıcı
şeffaflığı için isteğe bağlı ama tavsiye edilir (Material / AndroidX
için tipik pratik).

---

## Android SDK

Android SDK, uygulama içinde yeniden lisanslanan bir Maven artifact gibi
dağıtılmaz; geliştirme ve derleme için Google’ın SDK koşullarına tabidir.
Cihazda çalışan API’ler cihaz üreticisi / OS lisansına bağlıdır.

---

*Bu liste bilgilendirme amaçlıdır; hukuki tavsiye değildir.*
