# Marul Otomasyon APK

[![Build and Release APK](https://github.com/clkfatih1453-cpu/marul-otomasyon-apk/actions/workflows/build-release-apk.yml/badge.svg?branch=main)](https://github.com/clkfatih1453-cpu/marul-otomasyon-apk/actions/workflows/build-release-apk.yml)

ESP32 tabanlı hidroponik sistem için Android uygulaması. Bluetooth (BLE) ve WiFi üzerinden sistem kontrolü sağlar.

## Hazır APK İndir

- **GitHub Releases:** https://github.com/clkfatih1453-cpu/marul-otomasyon-apk/releases
- **Debug APK (direkt):** https://github.com/clkfatih1453-cpu/marul-otomasyon-apk/releases/latest/download/marul-otomasyon-debug.apk
- **Release APK (direkt):** https://github.com/clkfatih1453-cpu/marul-otomasyon-apk/releases/latest/download/marul-otomasyon-release.apk

Repoda da hazır kopyalar bulunur:

- `/apk-builds/marul-otomasyon-debug.apk`
- `/apk-builds/marul-otomasyon-release.apk`

## Telefonda Hızlı Kurulum

1. Telefonunda **Chrome** veya başka bir tarayıcı aç.
2. Şu direkt linklerden birine dokun:
   - Test için: `marul-otomasyon-debug.apk`
   - Normal kullanım için: `marul-otomasyon-release.apk`
3. İndirme tamamlanınca **Dosyalar / İndirilenler** klasörünü aç.
4. APK dosyasına dokun.
5. Gerekirse **Bilinmeyen uygulama yükleme izni ver** seçeneğini aç.
6. **Yükle** düğmesine bas.
7. Kurulum bitince uygulamayı aç.

## QR Kod ile İndirme

Telefon kullanıcıları için en kolay yöntemlerden biri direkt indirme linkini QR koda çevirmektir:

1. Yukarıdaki direkt indirme linklerinden birini kopyala.
2. Herhangi bir QR üreticiye yapıştır.
3. QR kodu telefondan tara.
4. Açılan sayfada APK dosyasını indir.
5. İndirilen APK'yı yükle.

## Özellikler

### 1. **Kurulum Modu (BLE/Bluetooth)**
- ESP32 ile Bluetooth Low Energy üzerinden bağlantı
- WiFi ağı adı (SSID) ve şifresini gönderme
- pH, EC, sıcaklık gibi hedef değerleri ayarlama
- Sistem konfigürasyonu

### 2. **Uzak Erişim Modu (İnternet)**
- WiFi'ye bağlandıktan sonra Blynk IoT üzerinden kontrol
- İnternetten herhangi bir yerden erişim
- Gerçek zamanlı veri izleme
- Anlık sensör ölçümleri

### 3. **Kontrol Özellikleri**
- pH Düşür pompası kontrol
- Gübre A ve B pompaları kontrol
- Sirkulasyon pompası kontrol
- Depo seviyesi monitörü
- Sistem durumu (normal/kilit/arıza)

### 4. **Sensör Takibi**
- pH değeri (real-time)
- EC değeri (Elektrik İletkenliği)
- Su sıcaklığı
- Depo seviyesi yüzdesi
- Akış oranları

### 5. **Konfigürasyon**
- pH hedef değerleri (min/max)
- EC minimum değeri
- Dozaj süresi ayarı
- Ölçüm periyodu ayarı
- Günlük max dozaj limiti

## Gereksinimler

- Android 8.0+ (API 26+)
- Bluetooth 4.0+
- WiFi bağlantısı
- ESP32 (Blynk kütüphanesi ile yüklenmiş)

## Kurulum

```bash
git clone https://github.com/clkfatih1453-cpu/marul-otomasyon-apk.git
cd marul-otomasyon-apk
```

Android Studio'da açın ve çalıştırın.

## Bağlantı Akışı

```
1. Uygulama Başlatılır
   ↓
2. Bluetooth Taraması
   ↓
3. ESP32 Bulunur ve Bağlanır (BLE)
   ↓
4. WiFi Bilgileri Gönderilir
   ↓
5. ESP32 WiFi'ye Bağlanır
   ↓
6. Blynk Bağlantısı Kurulur
   ↓
7. İnternet üzerinden Kontrol Sağlanır
```

## Blynk Virtual Pins

- **V0**: pH Değeri
- **V1**: EC Değeri
- **V2**: Sıcaklık
- **V3**: Sistem Durumu
- **V4**: pH Düşür (Manuel)
- **V5**: Gübre A (Manuel)
- **V6**: Sistem Reset
- **V7**: Gübre A Akış (ml)
- **V8**: Gübre B Akış (ml)
- **V9**: Asit Akış (ml)
- **V10**: Gübre B (Manuel)
- **V11**: Depo Seviyesi

## Bluetooth Protokolü (BLE)

### Karakteristikler:
- **WiFi SSID**: UUID `00000001-0000-1000-8000-00805f9b34fb`
- **WiFi Password**: UUID `00000002-0000-1000-8000-00805f9b34fb`
- **pH Max**: UUID `00000003-0000-1000-8000-00805f9b34fb`
- **pH Min**: UUID `00000004-0000-1000-8000-00805f9b34fb`
- **EC Min**: UUID `00000005-0000-1000-8000-00805f9b34fb`

## Dosya Yapısı

```
app/src/main/java/com/marul/otomasyon/
├── ui/
│   ├── MainActivity.kt
│   ├── SetupActivity.kt
│   ├── ControlActivity.kt
│   └── SettingsActivity.kt
├── manager/
│   ├── BluetoothManager.kt
│   ├── BlynkManager.kt
│   ├── SettingsManager.kt
│   └── SensorDataManager.kt
├── model/
│   ├── SystemStatus.kt
│   ├── SensorData.kt
│   └── WifiConfig.kt
└── util/
    ├── Constants.kt
    └── Utils.kt
```

## Lisans

MIT License

## İletişim

Sorunlar ve öneriler için Issue açın.
