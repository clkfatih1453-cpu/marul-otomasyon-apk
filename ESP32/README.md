# ESP32 BLE Server - Marul Otomasyon

Bu kod, Android APK'dan gelen Bluetooth Low Energy (BLE) komutlarını alarak ESP32'yi yapılandırmak için kullanılır.

## Kurulum

1. Arduino IDE'de şu kütüphaneleri yükleyin:
   - `BLEDevice`
   - `BLEServer`
   - `BLEUtils`
   - `BLESecurity`

2. Aşağıdaki kodu `ble_server_marul.ino` olarak kaydedin ve ESP32'ye yükleyin.

## Özellikler

- **BLE Service**: Bluetooth Low Energy servisi
- **WiFi Yazma**: SSID ve şifreyi APK'dan alır
- **Ayar Yazma**: pH, EC hedef değerlerini ayarlar
- **Veri Okuma**: Sensör verilerini APK'ya gönderir

## UUIDs

| Karakteristik | UUID | Açıklama |
|---|---|---|
| SSID | `00000001-0000-1000-8000-00805f9b34fb` | WiFi Ağ Adı |
| Password | `00000002-0000-1000-8000-00805f9b34fb` | WiFi Şifresi |
| pH Max | `00000003-0000-1000-8000-00805f9b34fb` | pH Maksimum |
| pH Min | `00000004-0000-1000-8000-00805f9b34fb` | pH Minimum |
| EC Min | `00000005-0000-1000-8000-00805f9b34fb` | EC Minimum |
| Data | `00000006-0000-1000-8000-00805f9b34fb` | Sensör Verisi |

## Bağlantı

1. Android cihazdan uygulamayı açın
2. "Kurulum" ekranından "Cihazları Tara" tıklayın
3. "marul_otomasyon" cihazını seçin
4. "Bağlan" tıklayın
5. WiFi bilgilerini girin
6. "WiFi Gönder" tıklayın

ESP32 WiFi'ye bağlandıktan sonra Blynk üzerinden internetten kontrol yapılabilir.
