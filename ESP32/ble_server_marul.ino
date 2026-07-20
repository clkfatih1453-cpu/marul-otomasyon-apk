#define BLYNK_TEMPLATE_ID   "TMPL6e0eGKAVX"
#define BLYNK_TEMPLATE_NAME "marul otomasyon"
#define BLYNK_AUTH_TOKEN    "8Cj2uteylTLTPjiwbkFyCn_0assoU6Nm"

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Preferences.h>
#include <WiFi.h>
#include <WiFiClient.h>
#include <BlynkSimpleEsp32.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <esp_task_wdt.h>

// ==========================================
// BLE TANIMLARI
// ==========================================
#define SERVICE_UUID        "0000180A-0000-1000-8000-00805f9b34fb"
#define CHAR_SSID_UUID      "00000001-0000-1000-8000-00805f9b34fb"
#define CHAR_PASSWORD_UUID  "00000002-0000-1000-8000-00805f9b34fb"
#define CHAR_PH_MAX_UUID    "00000003-0000-1000-8000-00805f9b34fb"
#define CHAR_PH_MIN_UUID    "00000004-0000-1000-8000-00805f9b34fb"
#define CHAR_EC_MIN_UUID    "00000005-0000-1000-8000-00805f9b34fb"
#define CHAR_DATA_UUID      "00000006-0000-1000-8000-00805f9b34fb"

// ==========================================
// PIN VE OTOMASYON TANIMLAMALARI
// ==========================================
#define SENSOR_PH_PIN      34
#define SENSOR_EC_PIN      35
#define SENSOR_TEMP_PIN    4

#define DEPO_TRIG_PIN      12
#define DEPO_ECHO_PIN      14

#define ROLES_PH_DOWN      25
#define ROLES_GUBRE_A      26
#define ROLES_GUBRE_B      32
#define ROLES_SIRKULASYON  27

#define FLOW_PH_PIN        18
#define FLOW_EC_A_PIN      19
#define FLOW_EC_B_PIN      21

// Hedef Karar Limitleri
float HEDEF_PH_MAX  = 6.2;
float HEDEF_PH_MIN  = 5.5;
float HEDEF_EC_MIN  = 1.0;

// Zamanlama Tanımları
const unsigned long DOZAJ_SURESI     = 5000;
const unsigned long KARISMA_SURESI   = 600000;
const unsigned long OLCUM_PERIYODU   = 120000;
const float FLOW_CALIBRATION_FACTOR = 98.0;

// Depo Kalibrasyonu
const int DEPO_BOS_MESAFE = 120;
const int DEPO_DOLU_MESAFE = 10;

// BLE Sunucu Değişkenleri
BLEServer* pServer = nullptr;
BLECharacteristic* pCharSSID = nullptr;
BLECharacteristic* pCharPassword = nullptr;
BLECharacteristic* pCharPhMax = nullptr;
BLECharacteristic* pCharPhMin = nullptr;
BLECharacteristic* pCharEcMin = nullptr;
BLECharacteristic* pCharData = nullptr;

bool deviceConnected = false;
bool oldDeviceConnected = false;

// Sistem Durum Değişkenleri
float guncelPH = 7.0;
float guncelEC = 0.0;
float suSicakligi = 25.0;
int depoYuzdesi = 100;
bool sistemKilitli = false;
bool sensorArizasiVar = false;
bool acilisIsinmaTamamlandi = false;

// Preferences (Flash Hafıza)
Preferences preferences;

// OneWire & Dallas Temperature
OneWire oneWire(SENSOR_TEMP_PIN);
DallasTemperature sensors(&oneWire);

// WiFi ve Blynk Bağlantı Durumu
bool wifiConnected = false;
bool blynkConnected = false;

unsigned long lastSensorReadTime = 0;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.println("[BLE] Android Cihaz Bagland...");
    };

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println("[BLE] Android Cihaz Baglantiyi Kesti");
    }
};

class MyCharacteristicCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        String uuid = pCharacteristic->getUUID().toString().c_str();
        String value = pCharacteristic->getValue().c_str();

        if (uuid == CHAR_SSID_UUID) {
            preferences.putString("ssid", value);
            Serial.println("[BLE] SSID: " + value);
        }
        else if (uuid == CHAR_PASSWORD_UUID) {
            preferences.putString("password", value);
            Serial.println("[BLE] Password: " + String(value.length()) + " karakter");
        }
        else if (uuid == CHAR_PH_MAX_UUID) {
            HEDEF_PH_MAX = value.toFloat();
            preferences.putFloat("ph_max", HEDEF_PH_MAX);
            Serial.println("[BLE] pH Max: " + String(HEDEF_PH_MAX));
        }
        else if (uuid == CHAR_PH_MIN_UUID) {
            HEDEF_PH_MIN = value.toFloat();
            preferences.putFloat("ph_min", HEDEF_PH_MIN);
            Serial.println("[BLE] pH Min: " + String(HEDEF_PH_MIN));
        }
        else if (uuid == CHAR_EC_MIN_UUID) {
            HEDEF_EC_MIN = value.toFloat();
            preferences.putFloat("ec_min", HEDEF_EC_MIN);
            Serial.println("[BLE] EC Min: " + String(HEDEF_EC_MIN));
        }

        // WiFi bağlantısını başlat
        if (preferences.getString("ssid", "").length() > 0) {
            connectToWiFi();
        }
    }
};

void setupBLE() {
    BLEDevice::init("marul_otomasyon");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService *pService = pServer->createService(SERVICE_UUID);

    // SSID Karakteristiği
    pCharSSID = pService->createCharacteristic(
        CHAR_SSID_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    pCharSSID->setCallbacks(new MyCharacteristicCallbacks());
    pCharSSID->setValue("WiFi Adi");

    // Password Karakteristiği
    pCharPassword = pService->createCharacteristic(
        CHAR_PASSWORD_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    pCharPassword->setCallbacks(new MyCharacteristicCallbacks());
    pCharPassword->setValue("Sifre");

    // pH Max Karakteristiği
    pCharPhMax = pService->createCharacteristic(
        CHAR_PH_MAX_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    pCharPhMax->setCallbacks(new MyCharacteristicCallbacks());
    pCharPhMax->setValue("6.2");

    // pH Min Karakteristiği
    pCharPhMin = pService->createCharacteristic(
        CHAR_PH_MIN_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    pCharPhMin->setCallbacks(new MyCharacteristicCallbacks());
    pCharPhMin->setValue("5.5");

    // EC Min Karakteristiği
    pCharEcMin = pService->createCharacteristic(
        CHAR_EC_MIN_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    pCharEcMin->setCallbacks(new MyCharacteristicCallbacks());
    pCharEcMin->setValue("1.0");

    // Data Karakteristiği (Sensör Verisi Gönderimi)
    pCharData = pService->createCharacteristic(
        CHAR_DATA_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
    );
    pCharData->addDescriptor(new BLE2902());

    pService->start();

    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(false);
    pAdvertising->setMinPreferred(0x0);
    BLEDevice::startAdvertising();

    Serial.println("[BLE] Sunucu Baslatildi - Reklam Yapiliyor...");
}

void connectToWiFi() {
    String ssid = preferences.getString("ssid", "");
    String password = preferences.getString("password", "");

    if (ssid.length() == 0) {
        Serial.println("[WiFi] SSID bulunamadi");
        return;
    }

    Serial.println("[WiFi] Baglaniyor: " + ssid);
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid.c_str(), password.c_str());

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        Serial.print(".");
        attempts++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        wifiConnected = true;
        Serial.println("\n[WiFi] Baglantilar Olusturuldu!");
        Serial.println("IP: " + WiFi.localIP().toString());
        connectToBlynk();
    } else {
        wifiConnected = false;
        Serial.println("\n[WiFi] Baglanti Basarisiz");
    }
}

void connectToBlynk() {
    Serial.println("[Blynk] Baglaniyor...");
    Blynk.config(BLYNK_AUTH_TOKEN);
    Blynk.connect();
}

float analogOrtalamaAl(int pin) {
    long toplam = 0;
    for (int i = 0; i < 20; i++) {
        toplam += analogRead(pin);
        delayMicroseconds(100);
    }
    return (float)toplam / 20.0;
}

void readSensors() {
    sensors.requestTemperatures();
    suSicakligi = sensors.getTempCByIndex(0);

    float phVoltaj = analogOrtalamaAl(SENSOR_PH_PIN) * 3.3 / 4095.0;
    float ecVoltaj = analogOrtalamaAl(SENSOR_EC_PIN) * 3.3 / 4095.0;

    guncelPH = 7.0 + ((2.50 - phVoltaj) * 3.5);
    if (suSicakligi == -127.00) suSicakligi = 25.0;
    float fSicaklik = 1.0 + 0.019 * (suSicakligi - 25.0);
    guncelEC = (ecVoltaj * 1.65) / fSicaklik;

    // Sensör Verisi Gönder
    if (deviceConnected && pCharData) {
        String sensorData = String(guncelPH) + "," + String(guncelEC) + "," + String(suSicakligi) + "," + String(depoYuzdesi);
        pCharData->setValue(sensorData.c_str());
        pCharData->notify();
    }

    // Blynk'e gönder
    if (Blynk.connected()) {
        Blynk.virtualWrite(0, guncelPH);
        Blynk.virtualWrite(1, guncelEC);
        Blynk.virtualWrite(2, suSicakligi);
        Blynk.virtualWrite(11, depoYuzdesi);
    }
}

void setup() {
    Serial.begin(115200);
    delay(1000);

    Serial.println("\n\n[SISTEM] Marul Otomasyon Basliyor...");

    // Preferences Başlat
    preferences.begin("marul_oto", false);

    // Pinleri Ayarla
    pinMode(ROLES_PH_DOWN, OUTPUT);
    pinMode(ROLES_GUBRE_A, OUTPUT);
    pinMode(ROLES_GUBRE_B, OUTPUT);
    pinMode(ROLES_SIRKULASYON, OUTPUT);

    digitalWrite(ROLES_PH_DOWN, LOW);
    digitalWrite(ROLES_GUBRE_A, LOW);
    digitalWrite(ROLES_GUBRE_B, LOW);
    digitalWrite(ROLES_SIRKULASYON, HIGH);

    // Sensörleri Başlat
    sensors.begin();

    // BLE Sunucuyu Başlat
    setupBLE();

    Serial.println("[SISTEM] Hazir! Android uygulamasindan Bluetooth ile baglanin.");
}

void loop() {
    // Blynk Bağlantısı Kontrol Et
    if (wifiConnected && !Blynk.connected()) {
        if (millis() % 10000 == 0) {
            Blynk.connect();
        }
    }

    if (Blynk.connected()) {
        Blynk.run();
    }

    // Sensörleri Oku
    if (millis() - lastSensorReadTime >= OLCUM_PERIYODU) {
        lastSensorReadTime = millis();
        readSensors();
    }

    // BLE Durum
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
    }
    if (!deviceConnected && oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
    }
}
