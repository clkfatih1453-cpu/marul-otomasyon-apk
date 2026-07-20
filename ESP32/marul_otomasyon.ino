/*
 * ================================================
 *  İNCİ TARIM - Hidroponik Otomasyon Sistemi
 *  ESP32 MQTT Firmware v2.0
 * ================================================
 *
 * Gerekli Kütüphaneler (Arduino Library Manager):
 *   - PubSubClient          (Nick O'Leary)
 *   - DHT sensor library    (Adafruit)
 *   - OneWire               (Jim Studt)
 *   - DallasTemperature     (Miles Burton)
 *   - NTPClient             (Fabrice Weinberg)
 *
 * İlk Kurulum:
 *   1. Kodu yükle (WiFi/MQTT ayarı gerekmez)
 *   2. Telefon Bluetooth'undan "InciTarim-Config" ile eşleş
 *   3. İnci Tarım uygulamasındaki "ESP32 Yapılandır" ekranını aç
 *   4. WiFi adı, WiFi şifresi, MQTT IP gir → Gönder
 *   5. ESP32 ayarları kaydeder ve yeniden başlar
 *
 *  BOOT butonu 3 sn. basılı tutulursa config modu sıfırlanır.
 * ================================================
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <NTPClient.h>
#include <WiFiUDP.h>
#include <Preferences.h>
#include "BluetoothSerial.h"

#define MQTT_PORT     1883
#define MQTT_CLIENT   "esp32_marul"
#define BT_DEVICE_NAME "InciTarim-Config"
#define PIN_BOOT      0   // BOOT butonu (config sıfırlama için)

// ============================================================
//  Pin Tanımları
// ============================================================

// Sensörler
#define PIN_DHT22         4    // Ortam sıcaklık + nem
#define PIN_DS18B20       5    // Su sıcaklığı
#define PIN_PH_SENSOR     34   // pH  (analog, ADC1)
#define PIN_EC_SENSOR     35   // EC  (analog, ADC1)
#define PIN_TRIG          18   // HC-SR04 ultrasonic TRIG (tank seviyesi)
#define PIN_ECHO          19   // HC-SR04 ultrasonic ECHO
#define PIN_FLOW_METER    23   // Su akış sensörü (YF-S201)

// Röle çıkışları (LOW = ON aktif röle için)
#define PIN_PUMP_CIRC     26   // Devirdaim pompası
#define PIN_PUMP_DOSE_A   27   // Gübre A peristaltik pompa
#define PIN_PUMP_DOSE_B   14   // Gübre B peristaltik pompa
#define PIN_PUMP_ACID     12   // Asit (pH düşürücü) peristaltik pompa
#define PIN_PUMP_WATER    13   // Su dolum pompası
#define PIN_LIGHT         25   // Spektrum ışık

// ============================================================
//  Sabitler
// ============================================================
#define TANK_HEIGHT_CM      50   // Tank yüksekliği (cm) - kendi tankınıza göre ayarlayın
#define SENSOR_OFFSET_CM     5   // Ultrasonik sensörün tank tabanına mesafe boşluğu
#define FLOW_PULSES_PER_L  450   // YF-S201: ~450 pulse/litre (kalibrasyon gerekebilir)
#define RELAY_ON            LOW   // Aktif LOW röle modülü
#define RELAY_OFF           HIGH

// MQTT topic'leri (Android uygulamasıyla birebir eşleşmeli)
#define T_SENSOR_PH        "marul/sensor/ph"
#define T_SENSOR_EC        "marul/sensor/ec"
#define T_SENSOR_TEMP      "marul/sensor/temp"
#define T_SENSOR_HUMID     "marul/sensor/humidity"
#define T_SENSOR_WATER_T   "marul/sensor/water_temp"
#define T_SENSOR_LEVEL     "marul/sensor/water_level"
#define T_SENSOR_WATER_ADD "marul/sensor/water_added"
#define T_SENSOR_FERT_A    "marul/sensor/fert_a_ml"
#define T_SENSOR_FERT_B    "marul/sensor/fert_b_ml"
#define T_SENSOR_ACID      "marul/sensor/acid_ml"

#define T_STATUS_PUMP      "marul/status/pump"
#define T_STATUS_LIGHT     "marul/status/light"
#define T_STATUS_FERT_A    "marul/status/fert_a"
#define T_STATUS_FERT_B    "marul/status/fert_b"
#define T_STATUS_ACID      "marul/status/ph_down"

#define T_CTRL_DOSE_A      "marul/control/dose_a"
#define T_CTRL_DOSE_B      "marul/control/dose_b"
#define T_CTRL_DOSE_ACID   "marul/control/dose_acid"
#define T_CTRL_PUMP_TMR    "marul/control/pump_timer"
#define T_CTRL_LIGHT_TMR   "marul/control/light_timer"

// ============================================================
//  Nesneler
// ============================================================
BluetoothSerial    btSerial;
WiFiClient         wifiClient;
PubSubClient       mqtt(wifiClient);
DHT                dht(PIN_DHT22, DHT22);
OneWire            oneWire(PIN_DS18B20);
DallasTemperature  ds18b20(&oneWire);
WiFiUDP            ntpUDP;
NTPClient          timeClient(ntpUDP, "pool.ntp.org", 10800); // UTC+3
Preferences        prefs;

// Kaydedilen WiFi/MQTT ayarları
String savedSSID   = "";
String savedPass   = "";
String savedMqtt   = "";
bool   configMode  = false; // BT provisioning modu aktif mi

// ============================================================
//  Değişkenler
// ============================================================

// Zamanlayıcılar
int pumpStartH = 0,  pumpStartM = 0,  pumpEndH = 24, pumpEndM = 0;
int lightStartH = 6, lightStartM = 0, lightEndH = 22, lightEndM = 0;

// Sayaçlar (NVS'e kaydedilir - güç kesintisinde korunur)
float fertAMlTotal   = 0;
float fertBMlTotal   = 0;
float acidMlTotal    = 0;
float waterAddedLit  = 0;

// Akış sensörü
volatile unsigned long flowPulseCount = 0;
unsigned long lastFlowCheck = 0;

// Durum
bool pumpRunning  = false;
bool lightOn      = false;

// Döngü zamanlama
unsigned long lastSensorPublish = 0;
const unsigned long SENSOR_INTERVAL = 5000; // 5 saniyede bir ölç & yayınla

// ============================================================
//  pH Kalibrasyon (probe'unuza göre ayarlayın)
//  ADC okuma 0-4095 → pH 0-14
//  Örnek kalibrasyon: pH4 buffer = ~2480, pH7 buffer = ~1860
// ============================================================
float adcToPH(int raw) {
    // 2 nokta doğrusal kalibrasyon
    const float volt       = raw * (3.3f / 4095.0f);
    const float pH         = 3.5f * volt + 0.0f;  // <-- kalibrasyona göre düzenle
    return constrain(pH, 0.0f, 14.0f);
}

// ============================================================
//  EC Kalibrasyon
//  Çıktı: mS/cm cinsinden
// ============================================================
float adcToEC(int raw) {
    const float volt = raw * (3.3f / 4095.0f);
    const float ec   = volt * 1.5f;              // <-- kalibrasyona göre düzenle
    return constrain(ec, 0.0f, 10.0f);
}

// ============================================================
//  Akış Sensörü ISR
// ============================================================
void IRAM_ATTR onFlowPulse() {
    flowPulseCount++;
}

// ============================================================
//  Tank Seviyesi (%)
// ============================================================
int readTankLevel() {
    digitalWrite(PIN_TRIG, LOW);
    delayMicroseconds(2);
    digitalWrite(PIN_TRIG, HIGH);
    delayMicroseconds(10);
    digitalWrite(PIN_TRIG, LOW);
    long dur = pulseIn(PIN_ECHO, HIGH, 30000);
    float distCm = dur * 0.0343f / 2.0f;
    float waterCm = TANK_HEIGHT_CM - distCm + SENSOR_OFFSET_CM;
    int pct = (int)(waterCm / TANK_HEIGHT_CM * 100.0f);
    return constrain(pct, 0, 100);
}

// ============================================================
//  Timer string parse  "HH:MM - HH:MM"
// ============================================================
void parseTimer(const String& s, int& startH, int& startM, int& endH, int& endM) {
    // Format: "08:00 - 20:00"
    if (s.length() < 13) return;
    startH = s.substring(0, 2).toInt();
    startM = s.substring(3, 5).toInt();
    endH   = s.substring(8, 10).toInt();
    endM   = s.substring(11, 13).toInt();
}

// ============================================================
//  Zaman aralığı kontrolü
// ============================================================
bool isTimeInRange(int curH, int curM, int startH, int startM, int endH, int endM) {
    int cur   = curH * 60 + curM;
    int start = startH * 60 + startM;
    int end   = endH * 60 + endM;
    if (end == 0) end = 24 * 60; // 00:00 bitiş = gece yarısı
    return cur >= start && cur < end;
}

// ============================================================
//  Peristaltik pompa ile dozaj  (ml, akış hızı ~1 ml/saniye varsayım)
//  Kendi pompanızın hızına göre ML_PER_SEC değerini ayarlayın
// ============================================================
void doseML(int pin, float ml, float& totalCounter, const char* statusTopic) {
    const float ML_PER_SEC = 1.0f; // pompa hızı: ml/saniye
    unsigned long durationMs = (unsigned long)(ml / ML_PER_SEC * 1000.0f);
    mqtt.publish(statusTopic, "1");
    digitalWrite(pin, RELAY_ON);
    unsigned long start = millis();
    while (millis() - start < durationMs) {
        mqtt.loop(); // bağlantıyı canlı tut
        yield();
    }
    digitalWrite(pin, RELAY_OFF);
    totalCounter += ml;
    mqtt.publish(statusTopic, "0");
    // Toplam sayacı kaydet
    prefs.begin("marul", false);
    if (pin == PIN_PUMP_DOSE_A)  prefs.putFloat("fert_a", fertAMlTotal);
    else if (pin == PIN_PUMP_DOSE_B)  prefs.putFloat("fert_b", fertBMlTotal);
    else if (pin == PIN_PUMP_ACID)    prefs.putFloat("acid",   acidMlTotal);
    prefs.end();
}

// ============================================================
//  MQTT Mesaj Callback
// ============================================================
void onMqttMessage(char* topic, byte* payload, unsigned int length) {
    String msg;
    for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];
    String t(topic);

    Serial.printf("[MQTT] %s → %s\n", topic, msg.c_str());

    if (t == T_CTRL_DOSE_A) {
        float ml = msg.toFloat();
        if (ml > 0 && ml <= 500) doseML(PIN_PUMP_DOSE_A, ml, fertAMlTotal, T_STATUS_FERT_A);
    }
    else if (t == T_CTRL_DOSE_B) {
        float ml = msg.toFloat();
        if (ml > 0 && ml <= 500) doseML(PIN_PUMP_DOSE_B, ml, fertBMlTotal, T_STATUS_FERT_B);
    }
    else if (t == T_CTRL_DOSE_ACID) {
        float ml = msg.toFloat();
        if (ml > 0 && ml <= 200) doseML(PIN_PUMP_ACID, ml, acidMlTotal, T_STATUS_ACID);
    }
    else if (t == T_CTRL_PUMP_TMR) {
        parseTimer(msg, pumpStartH, pumpStartM, pumpEndH, pumpEndM);
        prefs.begin("marul", false);
        prefs.putString("pump_tmr", msg);
        prefs.end();
        Serial.printf("Pompa zamanlayıcı: %02d:%02d - %02d:%02d\n",
                      pumpStartH, pumpStartM, pumpEndH, pumpEndM);
    }
    else if (t == T_CTRL_LIGHT_TMR) {
        parseTimer(msg, lightStartH, lightStartM, lightEndH, lightEndM);
        prefs.begin("marul", false);
        prefs.putString("light_tmr", msg);
        prefs.end();
        Serial.printf("Işık zamanlayıcı: %02d:%02d - %02d:%02d\n",
                      lightStartH, lightStartM, lightEndH, lightEndM);
    }
}

// ============================================================
//  MQTT Bağlantı
// ============================================================
void connectMQTT() {
    while (!mqtt.connected()) {
        Serial.print("MQTT bağlanıyor...");
        bool ok = mqtt.connect(MQTT_CLIENT);
        // Şifre varsa: mqtt.connect(MQTT_CLIENT, MQTT_USER, MQTT_PASS);
        if (ok) {
            Serial.println(" bağlandı!");
            mqtt.subscribe(T_CTRL_DOSE_A);
            mqtt.subscribe(T_CTRL_DOSE_B);
            mqtt.subscribe(T_CTRL_DOSE_ACID);
            mqtt.subscribe(T_CTRL_PUMP_TMR);
            mqtt.subscribe(T_CTRL_LIGHT_TMR);
            mqtt.publish("marul/status/online", "1");
        } else {
            Serial.printf(" başarısız (rc=%d), 3s bekle\n", mqtt.state());
            delay(3000);
        }
    }
}

// ============================================================
//  Sensörleri oku ve MQTT'ye yayınla
// ============================================================
void publishSensors() {
    char buf[16];

    // Ortam sıcaklık + nem (DHT22)
    float temp = dht.readTemperature();
    float hum  = dht.readHumidity();
    if (!isnan(temp)) {
        snprintf(buf, sizeof(buf), "%.1f", temp);
        mqtt.publish(T_SENSOR_TEMP, buf);
    }
    if (!isnan(hum)) {
        snprintf(buf, sizeof(buf), "%.1f", hum);
        mqtt.publish(T_SENSOR_HUMID, buf);
    }

    // Su sıcaklığı (DS18B20)
    ds18b20.requestTemperatures();
    float wTemp = ds18b20.getTempCByIndex(0);
    if (wTemp > -50) {
        snprintf(buf, sizeof(buf), "%.1f", wTemp);
        mqtt.publish(T_SENSOR_WATER_T, buf);
    }

    // pH
    int phRaw = analogRead(PIN_PH_SENSOR);
    snprintf(buf, sizeof(buf), "%.2f", adcToPH(phRaw));
    mqtt.publish(T_SENSOR_PH, buf);

    // EC
    int ecRaw = analogRead(PIN_EC_SENSOR);
    snprintf(buf, sizeof(buf), "%.2f", adcToEC(ecRaw));
    mqtt.publish(T_SENSOR_EC, buf);

    // Tank seviyesi
    snprintf(buf, sizeof(buf), "%d", readTankLevel());
    mqtt.publish(T_SENSOR_LEVEL, buf);

    // Akış sensörü: eklenen su (L)
    noInterrupts();
    unsigned long pulses = flowPulseCount;
    flowPulseCount = 0;
    interrupts();
    float liters = (float)pulses / FLOW_PULSES_PER_L;
    waterAddedLit += liters;
    snprintf(buf, sizeof(buf), "%.2f", waterAddedLit);
    mqtt.publish(T_SENSOR_WATER_ADD, buf);

    // Dozaj sayaçları
    snprintf(buf, sizeof(buf), "%.1f", fertAMlTotal);
    mqtt.publish(T_SENSOR_FERT_A, buf);
    snprintf(buf, sizeof(buf), "%.1f", fertBMlTotal);
    mqtt.publish(T_SENSOR_FERT_B, buf);
    snprintf(buf, sizeof(buf), "%.1f", acidMlTotal);
    mqtt.publish(T_SENSOR_ACID, buf);
}

// ============================================================
//  Zamanlayıcı Kontrolü
// ============================================================
void checkTimers() {
    if (!timeClient.isTimeSet()) return;
    int h = timeClient.getHours();
    int m = timeClient.getMinutes();

    // Devirdaim Pompası
    bool pumpShouldRun = isTimeInRange(h, m, pumpStartH, pumpStartM, pumpEndH, pumpEndM);
    if (pumpShouldRun != pumpRunning) {
        pumpRunning = pumpShouldRun;
        digitalWrite(PIN_PUMP_CIRC, pumpRunning ? RELAY_ON : RELAY_OFF);
        mqtt.publish(T_STATUS_PUMP, pumpRunning ? "1" : "0");
        Serial.printf("Pompa: %s\n", pumpRunning ? "AÇIK" : "KAPALI");
    }

    // Spektrum Işık
    bool lightShouldOn = isTimeInRange(h, m, lightStartH, lightStartM, lightEndH, lightEndM);
    if (lightShouldOn != lightOn) {
        lightOn = lightShouldOn;
        digitalWrite(PIN_LIGHT, lightOn ? RELAY_ON : RELAY_OFF);
        mqtt.publish(T_STATUS_LIGHT, lightOn ? "1" : "0");
        Serial.printf("Işık: %s\n", lightOn ? "AÇIK" : "KAPALI");
    }
}

// ============================================================
//  NVS'den yükle
// ============================================================
void loadPreferences() {
    prefs.begin("marul", true);
    fertAMlTotal  = prefs.getFloat("fert_a", 0);
    fertBMlTotal  = prefs.getFloat("fert_b", 0);
    acidMlTotal   = prefs.getFloat("acid",   0);
    waterAddedLit = prefs.getFloat("water",  0);
    savedSSID  = prefs.getString("wifi_ssid", "");
    savedPass  = prefs.getString("wifi_pass", "");
    savedMqtt  = prefs.getString("mqtt_host", "");
    String pumpTmr  = prefs.getString("pump_tmr",  "00:00 - 24:00");
    String lightTmr = prefs.getString("light_tmr", "06:00 - 22:00");
    prefs.end();
    parseTimer(pumpTmr,  pumpStartH, pumpStartM, pumpEndH,  pumpEndM);
    parseTimer(lightTmr, lightStartH,lightStartM,lightEndH, lightEndM);
}

// ============================================================
//  Bluetooth Provisioning Modu
//  Protokol (satır bazlı):
//    Uygulama gönderir → ESP32 yanıtlar
//    SSID:MyNetwork    → OK:SSID
//    PASS:MyPassword   → OK:PASS
//    MQTT:192.168.1.10 → OK:MQTT
//    SAVE              → SAVED (sonra reboot)
//    STATUS            → SSID:xxx|MQTT:xxx
// ============================================================
void runBluetoothConfig() {
    btSerial.begin(BT_DEVICE_NAME);
    Serial.println("[BT] Provisioning modu aktif: " + String(BT_DEVICE_NAME));

    // LED ile göster (varsa)
    String rxBuf = "";
    unsigned long lastBlink = 0;

    btSerial.println("=== İnci Tarım ESP32 Yapılandırma ===");
    btSerial.println("Mevcut: SSID=" + (savedSSID.isEmpty() ? "(yok)" : savedSSID)
                     + " MQTT=" + (savedMqtt.isEmpty() ? "(yok)" : savedMqtt));
    btSerial.println("Komutlar: SSID:... PASS:... MQTT:... SAVE STATUS");

    while (true) {
        // BOOT butonuna 3 sn basılırsa çık (debug için)
        if (digitalRead(PIN_BOOT) == LOW) {
            delay(3000);
            if (digitalRead(PIN_BOOT) == LOW) {
                btSerial.println("BOOT basili - cikiliyor");
                break;
            }
        }

        if (!btSerial.available()) {
            delay(10);
            continue;
        }

        char c = (char)btSerial.read();
        if (c == '\r') continue;
        if (c != '\n') { rxBuf += c; continue; }

        // Tam satır geldi
        rxBuf.trim();
        Serial.println("[BT RX] " + rxBuf);

        if (rxBuf.startsWith("SSID:")) {
            savedSSID = rxBuf.substring(5);
            btSerial.println("OK:SSID=" + savedSSID);
        }
        else if (rxBuf.startsWith("PASS:")) {
            savedPass = rxBuf.substring(5);
            btSerial.println("OK:PASS=****");
        }
        else if (rxBuf.startsWith("MQTT:")) {
            savedMqtt = rxBuf.substring(5);
            btSerial.println("OK:MQTT=" + savedMqtt);
        }
        else if (rxBuf == "SAVE") {
            if (savedSSID.isEmpty() || savedPass.isEmpty() || savedMqtt.isEmpty()) {
                btSerial.println("HATA: Tum alanlari girin (SSID, PASS, MQTT)");
            } else {
                prefs.begin("marul", false);
                prefs.putString("wifi_ssid", savedSSID);
                prefs.putString("wifi_pass", savedPass);
                prefs.putString("mqtt_host", savedMqtt);
                prefs.end();
                btSerial.println("SAVED");
                btSerial.println("ESP32 yeniden baslatiliyor...");
                delay(500);
                btSerial.end();
                ESP.restart();
            }
        }
        else if (rxBuf == "STATUS") {
            btSerial.println("SSID:" + savedSSID + "|MQTT:" + savedMqtt);
        }
        else if (rxBuf == "RESET") {
            prefs.begin("marul", false);
            prefs.remove("wifi_ssid");
            prefs.remove("wifi_pass");
            prefs.remove("mqtt_host");
            prefs.end();
            btSerial.println("CONFIG SIFIRLANDI - Yeniden baslatiliyor...");
            delay(500);
            btSerial.end();
            ESP.restart();
        }
        else {
            btSerial.println("HATA: Bilinmeyen komut: " + rxBuf);
        }
        rxBuf = "";
    }
}

// ============================================================
//  SETUP
// ============================================================
void setup() {
    Serial.begin(115200);
    Serial.println("\n=== İnci Tarım Başlatılıyor ===");

    pinMode(PIN_BOOT,        INPUT_PULLUP);
    pinMode(PIN_TRIG,        OUTPUT);
    pinMode(PIN_ECHO,        INPUT);
    pinMode(PIN_PUMP_CIRC,   OUTPUT); digitalWrite(PIN_PUMP_CIRC,   RELAY_OFF);
    pinMode(PIN_PUMP_DOSE_A, OUTPUT); digitalWrite(PIN_PUMP_DOSE_A, RELAY_OFF);
    pinMode(PIN_PUMP_DOSE_B, OUTPUT); digitalWrite(PIN_PUMP_DOSE_B, RELAY_OFF);
    pinMode(PIN_PUMP_ACID,   OUTPUT); digitalWrite(PIN_PUMP_ACID,   RELAY_OFF);
    pinMode(PIN_PUMP_WATER,  OUTPUT); digitalWrite(PIN_PUMP_WATER,  RELAY_OFF);
    pinMode(PIN_LIGHT,       OUTPUT); digitalWrite(PIN_LIGHT,       RELAY_OFF);

    dht.begin();
    ds18b20.begin();
    analogReadResolution(12);
    analogSetAttenuation(ADC_11db);

    pinMode(PIN_FLOW_METER, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(PIN_FLOW_METER), onFlowPulse, RISING);

    loadPreferences();

    // BOOT butonu basılıysa config modunu zorla
    bool bootHeld = (digitalRead(PIN_BOOT) == LOW);

    // Config yok veya BOOT basılıysa BT provisioning moduna gir
    if (savedSSID.isEmpty() || savedMqtt.isEmpty() || bootHeld) {
        if (bootHeld) Serial.println("BOOT basili: Config modu zorlaniyor...");
        else          Serial.println("Config bulunamadi: BT provisioning moduna giriliyor...");
        configMode = true;
        runBluetoothConfig(); // Bu fonksiyon SAVE'de ESP.restart() yapar
        return;               // SAVE yapılmadan çıkılırsa buraya döner
    }

    // Normal mod: WiFi + MQTT
    Serial.printf("WiFi: %s\n", savedSSID.c_str());
    WiFi.begin(savedSSID.c_str(), savedPass.c_str());
    unsigned long wStart = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - wStart < 15000) {
        delay(500); Serial.print(".");
    }
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("\nWiFi baglanamiyor! BT config moduna giriliyor...");
        runBluetoothConfig();
        return;
    }
    Serial.printf("\nWiFi bağlandı: %s\n", WiFi.localIP().toString().c_str());

    timeClient.begin();
    timeClient.update();

    mqtt.setServer(savedMqtt.c_str(), MQTT_PORT);
    mqtt.setCallback(onMqttMessage);
    mqtt.setKeepAlive(30);
    connectMQTT();

    Serial.println("=== Sistem Hazır ===");
}

// ============================================================
//  LOOP
// ============================================================
void loop() {
    if (configMode) return; // BT modunda loop kullanılmaz

    // WiFi koptu mu?
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi kesildi, yeniden bağlanıyor...");
        WiFi.reconnect();
        delay(5000);
        return;
    }

    if (!mqtt.connected()) connectMQTT();
    mqtt.loop();
    timeClient.update();

    if (millis() - lastSensorPublish >= SENSOR_INTERVAL) {
        lastSensorPublish = millis();
        publishSensors();
        checkTimers();
    }
}
