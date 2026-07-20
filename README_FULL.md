# İnci Otomasyon - Tam Çözüm

**Hidroponik Sistem Otomasyon Uygulaması** 🌱

ESP32 tabanlı hidroponik sistemini Bluetooth ve İnternet üzerinden kontrol etmek için sıfırdan geliştirilmiş, modern bir Android uygulaması.

---

## 🎯 Özellikler

### 📱 Android APK
- ✅ **Bluetooth (BLE)** üzerinden cihaz kurulumu
- ✅ **WiFi ayarları** APK'dan dinamik olarak ayarlanabilir
- ✅ **pH, EC, Sıcaklık, Depo Seviyesi** gerçek zamanlı monitoring
- ✅ **Pompa kontrolleri** manuel ve otomatik mod
- ✅ **Sistem durumu** gösterimi (Normal/Kilit/Arıza)
- ✅ **SharedPreferences** ile ayarlar depolama
- ✅ **Material Design 3** arayüzü
- ✅ **Türkçe** tam destek

### 🔧 ESP32 Firmware
- ✅ **BLE Server** - Bluetooth Low Energy sunucu
- ✅ **WiFi bağlantıs��** - Otomatik WiFi kurulumu
- ✅ **Blynk entegrasyonu** - İnternet üzerinden kontrol
- ✅ **Sensör okuma** - pH, EC, Sıcaklık, Depo
- ✅ **Pompa kontrolü** - 4 bağımsız pompa
- ✅ **Flash hafıza** - Ayarlar kalıcı depolama
- ✅ **Watchdog timer** - Sistem güvenliği

### 🌐 Blynk Entegrasyonu
- ✅ **İnternetten erişim** - WiFi bağlantısı kurulduktan sonra
- ✅ **11 Virtual Pin** - Eksiksiz sistem kontrolü
- ✅ **Gerçek zamanlı veriler** - Live monitoring
- ✅ **Uyarı ve loglar** - Sistem olayları kaydı

---

## 📋 Sistem Mimarisi

```
┌─────────────────────────────────────┐
│     Android İnci Otomasyon APK      │
│  (Bluetooth + İnternet Bağlantısı)  │
└──────────┬──────────────────────────┘
           │
      ┌────┴─────────────────────────┐
      │                              │
  ┌───▼────┐ BLE (Kurulum)      ┌───▼────┐
  │ ESP32  │◄─────────────────► │ Router │
  │        │                    │ WiFi   │
  └────┬───┘                    └────────┘
       │                           │
   Sensörler                   Blynk IoT
   Pompalar                    (İnternet)
```

---

## 🚀 Kurulum Adımları

### 1️⃣ Android Kurulumu
```bash
# Repository'yi klonla
git clone https://github.com/clkfatih1453-cpu/marul-otomasyon-apk.git
cd marul-otomasyon-apk

# Android Studio'da aç
# android-studio ./
```

**Gereksinimler:**
- Android Studio 2022.1+
- Android SDK 26+ (Android 8.0+)
- Gradle 7.0+

### 2️⃣ ESP32 Kurulumu

**Hardware:**
- ESP32 Dev Board
- USB Kablo
- Arduino IDE

**Software:**
1. Arduino IDE'ye ESP32 desteği ekle
2. Gerekli kütüphaneleri yükle (Blynk, OneWire, DallasTemperature)
3. `ESP32/ble_server_marul.ino` kodu yükle
4. Seri monitörü (115200 baud) ile kontrol et

### 3️⃣ İlk Bağlantı

**BLE Kurulum (İlk Kez):**
1. APK'da "Kurulum" tıkla
2. "Cihazlar Tara" ile ESP32'yi bul
3. WiFi bilgilerini gir
4. "WiFi Gönder" tıkla

**WiFi Bağlantısı (Otomatik):**
- ESP32 WiFi'ye bağlanır
- Blynk bağlantısı kurulur

**İnternet Kontrolü:**
- Blynk uygulamasını aç
- Template: "marul otomasyon" seç
- Gerçek zamanlı kontrol başla

---

## 📦 Dosya Yapısı

```
marul-otomasyon-apk/
├── app/
│   └── src/main/
│       ├── java/com/marul/otomasyon/
│       │   ├── ui/
│       │   │   ├── MainActivity.kt
│       │   │   ├── SetupActivity.kt
│       │   │   ├── ControlActivity.kt
│       │   │   └── SettingsActivity.kt
│       │   ├── manager/
│       │   │   ├── BluetoothManager.kt
│       │   │   ├── BlynkManager.kt
│       │   │   ├── SettingsManager.kt
│       │   │   └── SensorDataManager.kt
│       │   ├── model/
│       │   │   └── SystemStatus.kt
│       │   └── util/
│       │       └── Constants.kt
│       └── res/
│           ├── layout/ (XML layouts)
│           ├── drawable/ (Icons)
│           ├── values/ (Resources)
│           └── mipmap/ (App icon)
├── ESP32/
│   ├── ble_server_marul.ino
│   ├── README.md
│   └── KURULUM.md
├── build.gradle
├── settings.gradle
├── README.md
└── QUICKSTART.md
```

---

## 🔌 Blynk Virtual Pins

| Pin | Açıklama | Tip | Aralık |
|-----|----------|-----|--------|
| V0 | pH Değeri | Display | 0-14 |
| V1 | EC Değeri | Display | 0-4 mS/cm |
| V2 | Sıcaklık | Display | 0-50°C |
| V3 | Sistem Durumu | Label | Text |
| V4 | pH Düşür | Button | ON/OFF |
| V5 | Gübre A | Button | ON/OFF |
| V6 | Sistem Reset | Button | ON/OFF |
| V7 | Gübre A Akış | Display | ml |
| V8 | Gübre B Akış | Display | ml |
| V9 | Asit Akış | Display | ml |
| V10 | Gübre B | Button | ON/OFF |
| V11 | Depo Seviyesi | Display | 0-100% |

---

## 🔐 Güvenlik Özellikleri

- ✅ **Akış Sensörü Monitörü** - Pompa akış kaybı algılaması
- ✅ **Depo Seviyesi Koruması** - <%10 otomatik kilitleme
- ✅ **Sensör Arıza Algılama** - Sensor kesme kontrolü
- ✅ **Günlük Dozaj Limiti** - Maksimum 12 dozaj/gün
- ✅ **Watchdog Timer** - Sistem donması koruması
- ✅ **Sistem Kilidi** - Uyarılarda otomatik kilitleme

---

## 📊 Veri Akışı

### Kurulum Modunda:
```
Android APK (BLE) ◄──► ESP32 (BLE Server)
    ↓ (WiFi Config)
Flash Memory (Preferences)
    ↓ (Restart)
  WiFi → Router
```

### Normal Çalışma Modunda:
```
Android APK (Blynk) ◄──────► İnternet ◄──► Blynk Cloud
                                ▲
                                │
                            ESP32 (WiFi)
```

---

## 🛠️ Kustomizasyon

### pH Hedef Değerlerini Değiştir
`Constants.kt` dosyasında:
```kotlin
const val DEFAULT_PH_MAX = 6.2f
const val DEFAULT_PH_MIN = 5.5f
```

### EC Minimum Değerini Değiştir
```kotlin
const val DEFAULT_EC_MIN = 1.0f
```

### Blynk Token'ını Güncelle
```kotlin
const val BLYNK_AUTH_TOKEN = "YOUR_TOKEN_HERE"
```

---

## 🐛 Sorun Giderme

### Android APK Sorunları

**"Bluetooth Mevcut Değil" hatası**
- Cihazda Bluetooth açık mı?
- Izinler verildi mi?

**"Cihaz Bulunamadı" hatası**
- ESP32 açık mı?
- Serial monitor'de "Reklam Yapiliyor" mesajı var mı?
- Bluetooth açılı mı?

### ESP32 Sorunları

**Serial monitor boş**
- Baud rate 115200 mi?
- Doğru port seçildi mi?
- RESET tuşuna bas

**WiFi bağlanmıyor**
- SSID ve şifre doğru mu?
- WiFi 2.4GHz mi? (5GHz desteklenmez)

**Blynk bağlanmıyor**
- Token doğru mu?
- İnternet bağlantısı stabil mi?

---

## 📚 Kaynaklar

- [Blynk Dokumentasyon](https://docs.blynk.io/)
- [ESP32 Arduino Kütüphanesi](https://github.com/espressif/arduino-esp32)
- [Android Developers](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/)

---

## 📄 Lisans

MIT License - Özgürce kullanabilirsiniz

---

## 👥 Katkı

Bugs ve feature istekleri için GitHub Issues'i kullanın.

---

## 📞 İletişim

Sorular veya öneriler için:
- GitHub Issues
- Email: clkfatih1453@gmail.com

---

**Geliştirilen: 2026** 🚀

**İyi Kullanımlar! 🌱🎉**
