# \# 🌤️ Stacja Pogodowa (Weather Station) - IoT System

# 

# Kompletny system monitorowania jakości powietrza i warunków atmosferycznych, składający się z urządzenia opartego na \*\*ESP32\*\* oraz nowoczesnej aplikacji na \*\*Androida\*\*.

# 

# System wykorzystuje \*\*Firebase Realtime Database\*\* do natychmiastowej synchronizacji danych między czujnikami a telefonem.

# 

# !\[App Screenshot](https://via.placeholder.com/800x400?text=Zrzut+ekranu+aplikacji+w+stylu+Material+Design)

# \*(Miejsce na Twój zrzut ekranu aplikacji)\*

# 

# \## 🚀 Funkcjonalności

# 

# \### 📱 Aplikacja Android

# \* \*\*Design:\*\* Nowoczesny, minimalistyczny interfejs (Flat Material Design) wykorzystujący pastelową paletę kolorów.

# \* \*\*Monitoring na żywo:\*\* Odczyt temperatury, wilgotności, ciśnienia oraz indeksu jakości powietrza.

# \* \*\*Wizualne alerty:\*\* Karty zmieniają kolor (Zielony/Żółty/Czerwony) w zależności od stanu powietrza.

# \* \*\*Powiadomienia Push:\*\* Automatyczne ostrzeżenie na telefonie, gdy jakość powietrza spadnie do poziomu "ZŁE" (nawet gdy aplikacja jest w tle).

# 

# \### 📡 Urządzenie (ESP32)

# \* \*\*Czujniki:\*\* Obsługa BME280 (I2C) oraz MQ-135 (Analog).

# \* \*\*Algorytm:\*\* Obliczanie wskaźnika jakości powietrza (`Rs/R0`) na podstawie rezystancji czujnika gazu.

# \* \*\*OLED:\*\* Lokalny podgląd wyników na ekranie 0.96''.

# \* \*\*Konektowność:\*\* Wi-Fi + bezpieczne połączenie z chmurą Google Firebase.

# 

# ---

# 

# \## 🛠️ Wymagania sprzętowe

# 

# | Element | Opis | Połączenie (Pin ESP32) |

# | :--- | :--- | :--- |

# | \*\*ESP32\*\* | Mikrokontroler (np. ESP32 DevKit V1) | - |

# | \*\*BME280\*\* | Czujnik Temp/Wilg/Ciśnienia | SDA -> D21, SCL -> D22 |

# | \*\*SSD1306\*\* | Wyświetlacz OLED 0.96'' (I2C) | SDA -> D21, SCL -> D22 |

# | \*\*MQ-135\*\* | Czujnik jakości powietrza | AOUT -> D34, VCC -> 5V (lub VIN) |

# 

# ---

# 

# \## 💻 Instalacja i Konfiguracja

# 

# \### Krok 1: Firebase (Backend)

# 1\.  Utwórz projekt w \[Firebase Console](https://console.firebase.google.com/).

# 2\.  Utwórz bazę \*\*Realtime Database\*\* w trybie testowym.

# 3\.  W zakładce \*\*Authentication\*\*, włącz metodę logowania \*\*Anonymous\*\* (Anonimowy).

# 4\.  Pobierz plik `google-services.json` i dodaj go do projektu Androida (`app/google-services.json`).

# 

# \### Krok 2: ESP32 (Arduino IDE)

# 1\.  Zainstaluj biblioteki:

# &nbsp;   \* `Firebase Arduino Client` (Mobizt)

# &nbsp;   \* `BME280` (Tyler Glenn)

# &nbsp;   \* `Adafruit SSD1306` \& `Adafruit GFX`

# 2\.  Otwórz plik `secrets.h` i uzupełnij swoje dane:

# &nbsp;   ```cpp

# &nbsp;   #define WIFI\_SSID "Twoja\_Siec"

# &nbsp;   #define WIFI\_PASS "Twoje\_Haslo"

# &nbsp;   #define API\_KEY "AIzaSy..." // Z konsoli Firebase

# &nbsp;   #define DATABASE\_URL "twoj-projekt.firebaseio.com" // Bez https://

# &nbsp;   ```

# 3\.  Wgraj kod na płytkę.

# 

# \### Krok 3: Aplikacja Android

# 1\.  Otwórz projekt w \*\*Android Studio\*\*.

# 2\.  Upewnij się, że plik `google-services.json` znajduje się w folderze `app/`.

# 3\.  Zsynchronizuj projekt (Sync Gradle).

# 4\.  Uruchom aplikację na telefonie lub emulatorze.

# 

# ---

# 

# \## 🎨 Struktura Projektu (Android)

# 

# \* `MainActivity.kt` - Główna logika, odbieranie danych z Firebase, obsługa powiadomień.

# \* `res/layout/activity\_main.xml` - Warstwa wizualna (Flat UI).

# \* `res/values/colors.xml` - Definicja palety kolorów Material Design.

# \* `res/drawable/` - Ikony wektorowe (powietrze, termometr, wilgotność, ciśnienie).

# 

# ---

# 

# \## 🔍 Kalibracja MQ-135

# W kodzie Arduino znajduje się stała `R0`, która odpowiada za kalibrację czujnika gazu:

# ```cpp

# const float R0 = 76.0; // Wartość do kalibracji

