# İnci Otomasyon - Hızlı Başlangıç Kılavuzu

## 📱 Android APK Kurulumu

### Adım 1: Android Studio'yu Aç
```bash
git clone https://github.com/clkfatih1453-cpu/marul-otomasyon-apk.git
cd marul-otomasyon-apk
```

### Adım 2: Gradle'i Senkronize Et
- Android Studio'da "Sync Now" butonuna tıkla
- Gradle sinkronizasyonunun tamamlanmasını bekle

### Adım 3: Emülatör veya Gerçek Cihaz Hazırla
- USB ile cihazı bağla (Geliştirici Modu açık olmalı)
- Ya da Android Emülatörü çalıştır

### Adım 4: Uygulama Çalıştır
- "Run" butonuna tıkla (Shift + F10)
- Uygulama cihazda çalışacak

---

## 🔧 ESP32 Kurulumu

### Gerekli Malzemeler
- ESP32 Geliştirme Kartı
- USB Kablo (Micro-USB veya USB-C)
- Arduino IDE

### Adım 1: Arduino IDE Kurulumu
1. [arduino.cc](https://www.arduino.cc/en/software) adresinden Arduino IDE indir
2. Arduino IDE'yi yükle

### Adım 2: ESP32 Destek Ekleme
1. Arduino IDE'yi aç
2. **File** → **Preferences** açılır
3. **Additional Boards Manager URLs** alanına şunu yapıştır:
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
4. **OK** tıkla
5. **Tools** → **Board** → **Boards Manager** açılır
6. "ESP32" ara ve **esp32 by Espressif Systems** yükle

### Adım 3: Kütüphaneleri Yükle
**Sketch** → **Include Library** → **Manage Libraries** açılır. Şunları yükle:
- **Blynk** (v1.0.0 veya üstü)
- **OneWire** (v2.3.7 veya üstü)
- **DallasTemperature** (v3.9.0 veya üstü)

### Adım 4: Kodu Yükle
1. `ESP32/ble_server_marul.ino` dosyasını açılır
2. **Tools** menüsünden:
   - **Board**: "ESP32 Dev Module" seç
   - **Flash Size**: "4MB (32Mb)" seç
   - **Upload Speed**: "921600" seç
   - **Port**: ESP32'nin bağlı olduğu portu seç
3. **Upload** butonuna tıkla
4. Yükleme tamamlanana kadar bekle

### Adım 5: Seri Monitörü Aç
1. **Tools** → **Serial Monitor** (Ctrl+Shift+M)
2. Baud Rate'i **115200** olarak ayarla
3. ESP32 yeniden başlarsa:
   ```
   [SISTEM] Marul Otomasyon Basliyor...
   [BLE] Sunucu Baslatildi - Reklam Yapiliyor...
   [SISTEM] Hazir! Android uygulamasindan Bluetooth ile baglanin.
   ```

---

## 📲 İlk Bağlantı

### Android Uygulamasından:

1. **"Kurulum"** ekranına git
2. **"Cihazları Tara"** butonuna tıkla
3. Listede **"marul_otomasyon"** cihazını bul ve seç
4. **"Bağlan"** butonuna tıkla
5. WiFi bilgilerini gir:
   - **SSID**: WiFi ağının adı
   - **Şifre**: WiFi şifresi
   - **pH Max**: 6.2
   - **pH Min**: 5.5
   - **EC Min**: 1.0
6. **"WiFi Gönder"** butonuna tıkla
7. ESP32 WiFi'ye bağlanacak ve Blynk bağlantısı kurulacak

---

## 🌐 İnternet Üzerinden Kontrol

WiFi bağlantısı kurulduktan sonra:

### Blynk Uygulaması ile:
1. Blynk uygulamasını aç
2. Template: "marul otomasyon" seç
3. Auth Token: `8Cj2uteylTLTPjiwbkFyCn_0assoU6Nm`
4. Sanal pinleri yapılandır

### İnci Otomasyon APK ile:
1. **"Kontrol Paneli"** ekranına git
2. Sensör verilerini takip et
3. Pompaları kontrol et
4. Ayarları düzenle

---

## 📊 Blynk Virtual Pins

| Pin | Açıklama | Tür |
|-----|----------|-----|
| V0 | pH Değeri | Display |
| V1 | EC Değeri | Display |
| V2 | Sıcaklık | Display |
| V3 | Sistem Durumu | Label |
| V4 | pH Düşür | Button |
| V5 | Gübre A | Button |
| V6 | Sistem Reset | Button |
| V7 | Gübre A Akış | Display |
| V8 | Gübre B Akış | Display |
| V9 | Asit Akış | Display |
| V10 | Gübre B | Button |
| V11 | Depo Seviyesi | Display |

---

## 🔌 Donanım Bağlantı Diyagramı

### Sensörler:
- **pH Sensörü**: GPIO 34 (ADC1_CH6)
- **EC Sensörü**: GPIO 35 (ADC1_CH7)
- **Sıcaklık Sensörü**: GPIO 4 (OneWire)
- **Ultrasonik (Depo)**: GPIO 12 (Trig), GPIO 14 (Echo)

### Röle Kontrolleri:
- **pH Düşür**: GPIO 25
- **Gübre A**: GPIO 26
- **Gübre B**: GPIO 32
- **Sirkulasyon**: GPIO 27

### Akış Sensörleri:
- **pH Akış**: GPIO 18
- **Gübre A Akış**: GPIO 19
- **Gübre B Akış**: GPIO 21

---

## 🐛 Sorun Giderme

### Serial Monitor'de hiçbir şey görmüyorum
- ✓ Baud Rate'i 115200 olarak ayarla
- ✓ USB kablonun veri transferini destekle
- ✓ RESET butonuna bas

### Android cihaz ESP32'yi bulmuyor
- ✓ Serial Monitor'de "Reklam Yapiliyor" mesajını kontrol et
- ✓ Bluetooth'u aç
- ✓ ESP32'nin yakınında ol
- ✓ Uygulamayı yeniden başlat

### WiFi bağlantısı başarısız
- ✓ SSID ve şifrenin doğru olup olmadığını kontrol et
- ✓ WiFi ağının 2.4GHz frekansında olup olmadığını kontrol et
- ✓ ESP32 ile router arasındaki mesafeyi azalt

### Blynk bağlantısı başarısız
- ✓ Auth Token'ın doğru olup olmadığını kontrol et
- ✓ İnternet bağlantısının stabil olup olmadığını kontrol et
- ✓ Blynk uygulamasında Template'i oluşturduğundan emin ol

---

## 📚 Kaynaklar

- [Blynk Dokümantasyonu](https://docs.blynk.io/)
- [ESP32 Kütüphanesi](https://github.com/espressif/arduino-esp32)
- [Arduino IDE](https://www.arduino.cc/en/software)
- [OneWire Kütüphanesi](https://www.pjrc.com/teensy/td_libs_OneWire.html)

---

## 💡 İpuçları

1. **Deneme Modunda Başla**: İlk olarak Bluetooth ile bağlantıyı test et
2. **Seri Monitörü Kullan**: Sorun giderme için her zaman seri monitörü kontrol et
3. **Kütüphaneleri Güncelle**: En son kütüphane sürümlerini kullan
4. **Kablo Bağlantılarını Kontrol Et**: Bağlantıların sıkı ve doğru olduğundan emin ol

---

## 📞 Destek

Sorular veya sorunlar için:
- GitHub Issues'de bir rapor oluştur
- Serial Monitor çıktısını paylaş
- Sisteminizin konfigürasyonunu açıkla

**İyi Kullanımlar! 🚀**
