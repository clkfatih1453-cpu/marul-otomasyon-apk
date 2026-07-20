# Kurulum Adımları

## 1. Arduino IDE Kurulumu

- Arduino IDE'yi [arduino.cc](https://www.arduino.cc/en/software) adresinden indirin
- Arduino IDE'yi açın

## 2. ESP32 Destek Ekleme

1. **Preferences** menüsüne gidin
2. **Additional Boards Manager URLs** alanına şunu yapıştırın:
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
3. **OK** tıklayın
4. **Tools** → **Board** → **Boards Manager** açın
5. "ESP32" arayın ve **esp32** paketini yükleyin

## 3. Kütüphaneleri Yükleme

**Sketch** → **Include Library** → **Manage Libraries** menüsünden:

1. "Blynk" arayın ve yükleyin
2. "OneWire" arayın ve yükleyin  
3. "DallasTemperature" arayın ve yükleyin

## 4. Board Ayarları

**Tools** menüsünden:

- **Board**: "ESP32 Dev Module" seçin
- **Flash Size**: "4MB (32Mb)" seçin
- **Upload Speed**: "921600" seçin
- **COM Port**: ESP32'nin bağlı olduğu port seçin

## 5. Kodu Yükleme

1. `ble_server_marul.ino` dosyasını Arduino IDE'de açın
2. **Upload** butonuna tıklayın
3. Yükleme tamamlanana kadar bekleyin

## 6. BLE Server Kontrol

Yüklemeden sonra:

1. **Serial Monitor** açın (Ctrl+Shift+M)
2. Baud Rate'i **115200** olarak ayarlayın
3. ESP32 yeniden başlayacak ve çıktı göreceksiniz:
   ```
   [SISTEM] Marul Otomasyon Basliyor...
   [BLE] Sunucu Baslatildi - Reklam Yapiliyor...
   [SISTEM] Hazir! Android uygulamasindan Bluetooth ile baglanin.
   ```

## 7. Android Uygulaması ile Bağlantı

1. Android cihazda Marul Otomasyon uygulamasını açın
2. "Kurulum" ekranına girin
3. "Cihazları Tara" butonuna tıklayın
4. "marul_otomasyon" cihazını seçin
5. "Bağlan" tıklayın
6. WiFi bilgilerini girin
7. "WiFi Gönder" tıklayın

## 8. İlk Bağlantı Sonrası

ESP32 WiFi'ye başarıyla bağlandıktan sonra:

- Blynk IoT platformunda yapılandırma yapabilirsiniz
- İnternetten herhangi yerden sistemi kontrol edebilirsiniz
- Sensör verilerini takip edebilirsiniz

## Sorun Giderme

### Serial Monitor'de hiçbir şey görmüyorum

- Baud Rate'i 115200 olarak ayarlayın
- USB kablonuzun veri transferini desteklediğinden emin olun
- ESP32'yi sıfırlamak için RESET butonuna basın

### Android uygulaması cihazı bulamıyor

- Serial Monitor'de "BLE Sunucu Başlatıldı" mesajını görmüş olduğunuzdan emin olun
- ESP32'nin yakınında olun (BLE 10 metre içinde çalışır)
- Android cihazda Bluetooth'u açık tutun
- Uygulamayı kapatıp yeniden açın

### WiFi bağlantısı başarısız

- SSID ve şifrenin doğru olduğundan emin olun
- Ağda herhangi bir özel karakter kullanılıyorsa kontrol edin
- ESP32'yi WiFi'nin yakınına getirin

### Blynk bağlantısı başarısız

- Blynk Auth Token'inin doğru olduğundan emin olun
- Blynk uygulamasında ilgili Template'i oluşturduğunuzdan emin olun
- İnternet bağlantınız stabil olmalı
