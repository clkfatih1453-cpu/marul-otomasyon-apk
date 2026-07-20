/*
 * ================================================
 *  İNCİ TARIM - Hidroponik Otomasyon Sistemi
 *  ESP32 MQTT Firmware - Manuel Konfigürasyon
 * ================================================
 *
 *  KURULUM:
 *  1. Aşağıdaki 3 satırı doldurun
 *  2. Arduino IDE'den ESP32'ye yükleyin
 *  3. Bitti!
 *
 *  Gerekli Kütüphaneler (Tools > Manage Libraries):
 *    - PubSubClient       (Nick O'Leary)
 *    - DHT sensor library (Adafruit)
 *    - OneWire            (Jim Studt)
 *    - DallasTemperature  (Miles Burton)
 *    - NTPClient          (Fabrice Weinberg)
 * ================================================
 */

// ============================================================
//  *** BURAYA YAZ - 3 SATIR ***
// ============================================================
#define WIFI_SSID   "WIFI_ADINIZ"       // WiFi ağ adı
#define WIFI_PASS   "WIFI_SIFRENIZ"     // WiFi şifresi
#define MQTT_HOST   "192.168.1.100"     // Raspberry Pi IP adresi
// ============================================================

#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <NTPClient.h>
#include <WiFiUDP.h>
#include <Preferences.h>

// ============================================================
//  Pin Tanımları  (devrenize göre değiştirin)
// ============================================================
#define PIN_DHT22        4    // DHT22 — ortam sıcaklık + nem
#define PIN_DS18B20      5    // DS18B20 — su sıcaklığı
#define PIN_PH_SENSOR   34    // pH sensörü (analog ADC1)
#define PIN_EC_SENSOR   35    // EC sensörü (analog ADC1)
#define PIN_TRIG        18    // HC-SR04 TRIG — tank seviyesi
#define PIN_ECHO        19    // HC-SR04 ECHO
#define PIN_FLOW_METER  23    // YF-S201 — eklenen su miktarı

#define PIN_PUMP_CIRC   26    // Devirdaim pompası (röle)
#define PIN_PUMP_DOSE_A 27    // Gübre A peristaltik pompa
#define PIN_PUMP_DOSE_B 14    // Gübre B peristaltik pompa
#define PIN_PUMP_ACID   12    // Asit (pH düşürücü) pompası
#define PIN_PUMP_WATER  13    // Su dolum pompası
#define PIN_LIGHT       25    // Spektrum ışık

// ============================================================
//  Sistem Sabitleri  (tankınıza göre ayarlayın)
// ============================================================
#define MQTT_PORT          1883
#define MQTT_CLIENT        "esp32_marul"
#define TANK_HEIGHT_CM       50    // Tank yüksekliği cm
#define SENSOR_OFFSET_CM      5    // Sensörün taban boşluğu cm
#define FLOW_PULSES_PER_L   450    // YF-S201 darbe/litre
#define SENSOR_INTERVAL    5000    // Sensör okuma periyodu (ms)
#define RELAY_ON            LOW    // Aktif-LOW röle: LOW=açık
#define RELAY_OFF          HIGH

// ============================================================
//  MQTT Topic'leri (uygulamayla eşleşmeli — değiştirmeyin)
// ============================================================
#define T_SENSOR_PH         "marul/sensor/ph"
#define T_SENSOR_EC         "marul/sensor/ec"
#define T_SENSOR_TEMP       "marul/sensor/temp"
#define T_SENSOR_HUMID      "marul/sensor/humidity"
#define T_SENSOR_WATER_T    "marul/sensor/water_temp"
#define T_SENSOR_LEVEL      "marul/sensor/water_level"
#define T_SENSOR_WATER_ADD  "marul/sensor/water_added"
#define T_SENSOR_FERT_A     "marul/sensor/fert_a_ml"
#define T_SENSOR_FERT_B     "marul/sensor/fert_b_ml"
#define T_SENSOR_ACID       "marul/sensor/acid_ml"
#define T_STATUS_CIRC       "marul/status/circ"       // Devirdaim pompa durumu
#define T_STATUS_LIGHT      "marul/status/light"
#define T_STATUS_FERT_A     "marul/status/fert_a"
#define T_STATUS_FERT_B     "marul/status/fert_b"
#define T_STATUS_ACID       "marul/status/ph_down"
#define T_CTRL_DOSE_A       "marul/control/dose_a"
#define T_CTRL_DOSE_B       "marul/control/dose_b"
#define T_CTRL_DOSE_ACID    "marul/control/dose_acid"
#define T_CTRL_PUMP_TMR     "marul/control/pump_timer"
#define T_CTRL_LIGHT_TMR    "marul/control/light_timer"
#define T_CTRL_CIRC         "marul/control/circ"      // Switch: devirdaim
#define T_CTRL_FERT_A       "marul/control/fert_a"    // Switch: gübre A
#define T_CTRL_FERT_B       "marul/control/fert_b"    // Switch: gübre B
#define T_CTRL_PH_DOWN      "marul/control/ph_down"   // Switch: asit
#define T_CTRL_RESET        "marul/control/reset"     // Sıfırla butonu

// ============================================================
//  Nesneler
// ============================================================
WiFiClient         wifiClient;
PubSubClient       mqtt(wifiClient);
DHT                dht(PIN_DHT22, DHT22);
OneWire            oneWire(PIN_DS18B20);
DallasTemperature  ds18b20(&oneWire);
WiFiUDP            ntpUDP;
NTPClient          timeClient(ntpUDP, "pool.ntp.org", 10800); // UTC+3
Preferences        prefs;

// ============================================================
//  Çalışma Değişkenleri
// ============================================================
// Pompa zamanlayıcısı (uygulama "08:00 - 20:00" formatında gönderir)
int pumpStartH = 0,  pumpStartM = 0,  pumpEndH = 24, pumpEndM = 0;
int lightStartH = 6, lightStartM = 0, lightEndH = 22, lightEndM = 0;

// Dozaj sayaçları (güç kesintisinde NVS'e kaydedilir)
float fertAMlTotal  = 0;
float fertBMlTotal  = 0;
float acidMlTotal   = 0;
float waterAddedLit = 0;

// Akış sensörü sayacı (interrupt ile artar)
volatile unsigned long flowPulseCount = 0;

// Durum
bool pumpRunning = false;
bool lightOn     = false;

// Zamanlama
unsigned long lastSensorPublish = 0;

// ============================================================
//  pH Kalibrasyon
//  pH4 solüsyonuyla ölçüp phAt4V değerini düzenleyin
//  pH7 solüsyonuyla ölçüp phAt7V değerini düzenleyin
// ============================================================
float adcToPH(int raw) {
    const float volt   = raw * (3.3f / 4095.0f);
    const float phAt4V = 2.23f;  // pH4 tamponu için volt ölçümü
    const float phAt7V = 1.87f;  // pH7 tamponu için volt ölçümü
    const float slope  = (7.0f - 4.0f) / (phAt7V - phAt4V);
    float ph = 7.0f + slope * (volt - phAt7V);
    return constrain(ph, 0.0f, 14.0f);
}

// ============================================================
//  EC Kalibrasyon
//  1.413 mS/cm standart çözeltisiyle V ölçüp ecAt1413mS düzenleyin
// ============================================================
float adcToEC(int raw) {
    const float volt        = raw * (3.3f / 4095.0f);
    const float ecAt1413mS  = 1.02f;  // 1.413 mS/cm çözeltisi için volt
    const float ec          = volt * (1.413f / ecAt1413mS);
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
    digitalWrite(PIN_TRIG, LOW);  delayMicroseconds(2);
    digitalWrite(PIN_TRIG, HIGH); delayMicroseconds(10);
    digitalWrite(PIN_TRIG, LOW);
    long dur     = pulseIn(PIN_ECHO, HIGH, 30000);
    float distCm = dur * 0.0343f / 2.0f;
    float waterCm = TANK_HEIGHT_CM - distCm + SENSOR_OFFSET_CM;
    return constrain((int)(waterCm / TANK_HEIGHT_CM * 100.0f), 0, 100);
}

// ============================================================
//  "HH:MM - HH:MM" formatını parse et
// ============================================================
void parseTimer(const String& s, int& sH, int& sM, int& eH, int& eM) {
    if (s.length() < 13) return;
    sH = s.substring(0,  2).toInt();
    sM = s.substring(3,  5).toInt();
    eH = s.substring(8, 10).toInt();
    eM = s.substring(11,13).toInt();
}

// ============================================================
//  Saat aralığı içinde mi?
// ============================================================
bool isInRange(int curH, int curM, int sH, int sM, int eH, int eM) {
    int cur   = curH * 60 + curM;
    int start = sH * 60 + sM;
    int end   = (eH == 0 && eM == 0) ? 24 * 60 : eH * 60 + eM;
    return cur >= start && cur < end;
}

// ============================================================
//  Peristaltik pompa dozaj
//  ML_PER_SEC = pompanızın gerçek hızına göre ayarlayın
// ============================================================
void doseML(int pin, float ml, float& counter, const char* statusTopic) {
    const float ML_PER_SEC = 1.0f; // ml/saniye — pompaya göre ayarlayın
    mqtt.publish(statusTopic, "1");
    digitalWrite(pin, RELAY_ON);
    unsigned long t = millis();
    while (millis() - t < (unsigned long)(ml / ML_PER_SEC * 1000.0f)) {
        mqtt.loop();
        yield();
    }
    digitalWrite(pin, RELAY_OFF);
    counter += ml;
    mqtt.publish(statusTopic, "0");
    // Sayacı kalıcı kaydet
    prefs.begin("marul", false);
    if      (pin == PIN_PUMP_DOSE_A) prefs.putFloat("fert_a", fertAMlTotal);
    else if (pin == PIN_PUMP_DOSE_B) prefs.putFloat("fert_b", fertBMlTotal);
    else if (pin == PIN_PUMP_ACID)   prefs.putFloat("acid",   acidMlTotal);
    prefs.end();
}

// ============================================================
//  MQTT Mesaj Callback
// ============================================================
void onMqttMessage(char* topic, byte* payload, unsigned int len) {
    String msg, t(topic);
    for (unsigned int i = 0; i < len; i++) msg += (char)payload[i];
    Serial.println("[MQTT IN] " + t + " = " + msg);

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
    // ── Manuel Switch Kontrolleri ──
    else if (t == T_CTRL_CIRC) {
        bool on = (msg == "1");
        digitalWrite(PIN_PUMP_CIRC, on ? RELAY_ON : RELAY_OFF);
        pumpRunning = on;
        mqtt.publish(T_STATUS_CIRC, on ? "1" : "0");
    }
    else if (t == T_CTRL_FERT_A) {
        bool on = (msg == "1");
        digitalWrite(PIN_PUMP_DOSE_A, on ? RELAY_ON : RELAY_OFF);
        mqtt.publish(T_STATUS_FERT_A, on ? "1" : "0");
    }
    else if (t == T_CTRL_FERT_B) {
        bool on = (msg == "1");
        digitalWrite(PIN_PUMP_DOSE_B, on ? RELAY_ON : RELAY_OFF);
        mqtt.publish(T_STATUS_FERT_B, on ? "1" : "0");
    }
    else if (t == T_CTRL_PH_DOWN) {
        bool on = (msg == "1");
        digitalWrite(PIN_PUMP_ACID, on ? RELAY_ON : RELAY_OFF);
        mqtt.publish(T_STATUS_ACID, on ? "1" : "0");
    }
    // ── Zamanlama ──
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
    // ── Sistem Sıfırla ──
    else if (t == T_CTRL_RESET) {
        if (msg == "1") {
            Serial.println("Uzaktan sifirlama!");
            mqtt.publish("marul/status/online", "0");
            delay(300);
            ESP.restart();
        }
    }
}

// ============================================================
//  MQTT Bağlan + Subscribe
// ============================================================
void connectMQTT() {
    int retries = 0;
    while (!mqtt.connected() && retries < 10) {
        Serial.printf("MQTT bağlanıyor %s (deneme %d)...\n", MQTT_HOST, retries + 1);
        if (mqtt.connect(MQTT_CLIENT)) {
            Serial.println("MQTT bağlandı!");
            mqtt.subscribe(T_CTRL_DOSE_A);
            mqtt.subscribe(T_CTRL_DOSE_B);
            mqtt.subscribe(T_CTRL_DOSE_ACID);
            mqtt.subscribe(T_CTRL_PUMP_TMR);
            mqtt.subscribe(T_CTRL_LIGHT_TMR);
            mqtt.subscribe(T_CTRL_CIRC);
            mqtt.subscribe(T_CTRL_FERT_A);
            mqtt.subscribe(T_CTRL_FERT_B);
            mqtt.subscribe(T_CTRL_PH_DOWN);
            mqtt.subscribe(T_CTRL_RESET);
            mqtt.publish("marul/status/online", "1");
            return;
        }
        Serial.printf("Başarısız (rc=%d), 3s bekle\n", mqtt.state());
        delay(3000);
        retries++;
    }
    if (!mqtt.connected()) {
        Serial.println("MQTT bağlanamadı! Sensör yayını olmadan devam.");
    }
}

// ============================================================
//  Sensörleri oku ve MQTT'ye gönder
// ============================================================
void publishSensors() {
    char buf[16];

    // Ortam sıcaklık + nem
    float temp = dht.readTemperature();
    float hum  = dht.readHumidity();
    if (!isnan(temp)) { snprintf(buf,sizeof(buf),"%.1f",temp); mqtt.publish(T_SENSOR_TEMP, buf); }
    if (!isnan(hum))  { snprintf(buf,sizeof(buf),"%.1f",hum);  mqtt.publish(T_SENSOR_HUMID, buf); }

    // Su sıcaklığı
    ds18b20.requestTemperatures();
    float wt = ds18b20.getTempCByIndex(0);
    if (wt > -50) { snprintf(buf,sizeof(buf),"%.1f",wt); mqtt.publish(T_SENSOR_WATER_T, buf); }

    // pH
    snprintf(buf,sizeof(buf),"%.2f", adcToPH(analogRead(PIN_PH_SENSOR)));
    mqtt.publish(T_SENSOR_PH, buf);

    // EC
    snprintf(buf,sizeof(buf),"%.2f", adcToEC(analogRead(PIN_EC_SENSOR)));
    mqtt.publish(T_SENSOR_EC, buf);

    // Tank seviyesi
    snprintf(buf,sizeof(buf),"%d", readTankLevel());
    mqtt.publish(T_SENSOR_LEVEL, buf);

    // Eklenen su (akış sensörü)
    noInterrupts();
    unsigned long pulses = flowPulseCount;
    flowPulseCount = 0;
    interrupts();
    waterAddedLit += (float)pulses / FLOW_PULSES_PER_L;
    snprintf(buf,sizeof(buf),"%.2f", waterAddedLit);
    mqtt.publish(T_SENSOR_WATER_ADD, buf);

    // Dozaj sayaçları
    snprintf(buf,sizeof(buf),"%.1f", fertAMlTotal); mqtt.publish(T_SENSOR_FERT_A, buf);
    snprintf(buf,sizeof(buf),"%.1f", fertBMlTotal); mqtt.publish(T_SENSOR_FERT_B, buf);
    snprintf(buf,sizeof(buf),"%.1f", acidMlTotal);  mqtt.publish(T_SENSOR_ACID, buf);
}

// ============================================================
//  Zamanlayıcı Kontrolü (NTP saatine göre)
// ============================================================
void checkTimers() {
    if (!timeClient.isTimeSet()) return;
    int h = timeClient.getHours();
    int m = timeClient.getMinutes();

    bool pumpShouldRun = isInRange(h,m, pumpStartH,pumpStartM, pumpEndH,pumpEndM);
    if (pumpShouldRun != pumpRunning) {
        pumpRunning = pumpShouldRun;
        digitalWrite(PIN_PUMP_CIRC, pumpRunning ? RELAY_ON : RELAY_OFF);
        mqtt.publish(T_STATUS_CIRC, pumpRunning ? "1" : "0");
        Serial.printf("Pompa: %s\n", pumpRunning ? "AÇIK" : "KAPALI");
    }

    bool lightShouldOn = isInRange(h,m, lightStartH,lightStartM, lightEndH,lightEndM);
    if (lightShouldOn != lightOn) {
        lightOn = lightShouldOn;
        digitalWrite(PIN_LIGHT, lightOn ? RELAY_ON : RELAY_OFF);
        mqtt.publish(T_STATUS_LIGHT, lightOn ? "1" : "0");
        Serial.printf("Işık: %s\n", lightOn ? "AÇIK" : "KAPALI");
    }
}

// ============================================================
//  Kaydedilmiş sayaç ve zamanlayıcıları yükle
// ============================================================
void loadSavedData() {
    prefs.begin("marul", true);
    fertAMlTotal  = prefs.getFloat("fert_a", 0);
    fertBMlTotal  = prefs.getFloat("fert_b", 0);
    acidMlTotal   = prefs.getFloat("acid",   0);
    waterAddedLit = prefs.getFloat("water",  0);
    String pt = prefs.getString("pump_tmr",  "00:00 - 24:00");
    String lt = prefs.getString("light_tmr", "06:00 - 22:00");
    prefs.end();
    parseTimer(pt, pumpStartH,  pumpStartM,  pumpEndH,  pumpEndM);
    parseTimer(lt, lightStartH, lightStartM, lightEndH, lightEndM);
    Serial.printf("NVS yüklendi → FertA:%.1f  FertB:%.1f  Asit:%.1f  Su:%.2fL\n",
                  fertAMlTotal, fertBMlTotal, acidMlTotal, waterAddedLit);
}

// ============================================================
//  SETUP
// ============================================================
void setup() {
    Serial.begin(115200);
    Serial.println("\n=============================");
    Serial.println("   İnci Tarım Başlatılıyor   ");
    Serial.println("=============================");

    // Röle pinleri — önce RELAY_OFF yaz (kapanı önle)
    uint8_t relays[] = {PIN_PUMP_CIRC, PIN_PUMP_DOSE_A, PIN_PUMP_DOSE_B,
                        PIN_PUMP_ACID, PIN_PUMP_WATER,  PIN_LIGHT};
    for (uint8_t p : relays) { pinMode(p, OUTPUT); digitalWrite(p, RELAY_OFF); }

    // Giriş pinleri
    pinMode(PIN_TRIG, OUTPUT);
    pinMode(PIN_ECHO, INPUT);
    pinMode(PIN_FLOW_METER, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(PIN_FLOW_METER), onFlowPulse, RISING);

    // Sensörler
    dht.begin();
    ds18b20.begin();
    analogReadResolution(12);
    analogSetAttenuation(ADC_11db);

    loadSavedData();

    // WiFi bağlantısı
    Serial.printf("WiFi: %s\n", WIFI_SSID);
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    unsigned long t0 = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - t0 < 20000) {
        delay(500);
        Serial.print(".");
    }
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("\nWiFi bağlanamadı! 10 saniye sonra yeniden denenecek...");
        delay(10000);
        ESP.restart();
    }
    Serial.printf("\nWiFi bağlandı → IP: %s\n", WiFi.localIP().toString().c_str());

    // NTP saat
    timeClient.begin();
    timeClient.update();
    Serial.printf("Saat (UTC+3): %02d:%02d\n", timeClient.getHours(), timeClient.getMinutes());

    // MQTT
    mqtt.setServer(MQTT_HOST, MQTT_PORT);
    mqtt.setCallback(onMqttMessage);
    mqtt.setKeepAlive(30);
    connectMQTT();

    Serial.println("=== Sistem Hazır ===\n");
}

// ============================================================
//  LOOP
// ============================================================
void loop() {
    // WiFi koptu mu?
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi kesildi, yeniden bağlanılıyor...");
        WiFi.reconnect();
        unsigned long t0 = millis();
        while (WiFi.status() != WL_CONNECTED && millis() - t0 < 10000) delay(500);
        if (WiFi.status() != WL_CONNECTED) { ESP.restart(); return; }
    }

    // MQTT koptu mu?
    if (!mqtt.connected()) connectMQTT();
    mqtt.loop();

    // NTP güncelle
    timeClient.update();

    // Her SENSOR_INTERVAL ms'de bir ölç ve gönder
    if (millis() - lastSensorPublish >= SENSOR_INTERVAL) {
        lastSensorPublish = millis();
        publishSensors();
        checkTimers();
    }
}
