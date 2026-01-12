# 🌦️ Inteligentna Stacja Pogodowa (Smart Weather Station)

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Material Design 3](https://img.shields.io/badge/Material%20Design%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

Kompletny system IoT do monitorowania warunków atmosferycznych w czasie rzeczywistym. Projekt składa się z urządzenia opartego na mikrokontrolerze ESP32 oraz dedykowanej aplikacji mobilnej na system Android.

## 📱 O Aplikacji

Aplikacja kliencka służy do wizualizacji danych zbieranych przez stację pogodową. Dzięki wykorzystaniu **Google Firebase Realtime Database**, dane są synchronizowane natychmiastowo, a użytkownik ma do nich dostęp z dowolnego miejsca na świecie. Interfejs został zaprojektowany zgodnie z wytycznymi **Material Design 3**, oferując nowoczesny wygląd oraz obsługę trybu ciemnego.

## ✨ Kluczowe Funkcje

* **📊 Monitoring w Czasie Rzeczywistym:**
    * Bieżący odczyt temperatury, wilgotności oraz ciśnienia atmosferycznego.
    * Automatyczne wykrywanie statusu połączenia (Online/Offline) z detekcją awarii stacji (>3s braku danych).
* **💨 Analiza Jakości Powietrza:**
    * Interpretacja danych z czujnika analogowego (MQ-135).
    * Trzystopniowa skala jakości: **Świetne** (<1.2V), **Dobre** (<1.5V), **Złe** (>1.5V).
    * Wizualna zmiana kolorystyki kart w zależności od stanu powietrza (Zielony / Bursztynowy / Czerwony).
* **🔔 System Powiadomień:**
    * Lokalne powiadomienia "push" ostrzegające o wykryciu smogu (przekroczenie progu alarmowego).
* **📈 Wykresy i Statystyki:**
    * Autorski widok wykresu (`SimpleGraphView`) rysujący historię temperatury.
    * Śledzenie wartości Min/Max dla bieżącej sesji pomiarowej.
* **📄 Raportowanie:**
    * Generowanie raportów dobowych do pliku **PDF**.
    * Zapis raportów bezpośrednio w pamięci telefonu (folder *Pobrane*).
* **🌗 Personalizacja:**
    * Pełne wsparcie dla **Dark Mode** (Tryb Ciemny) i Light Mode.
    * Dynamiczny przełącznik motywu w pasku aplikacji.

## 🛠️ Technologie i Biblioteki

Projekt wykorzystuje natywne rozwiązania Androida oraz usługi Google:

* **Język:** Kotlin
* **Backend:** Firebase Realtime Database
* **UI:** XML, Material Components for Android (M3)
* **Funkcje systemowe:**
    * `NotificationChannel` (Powiadomienia)
    * `PdfDocument` & `Canvas` (Generowanie PDF)
    * `ConnectivityManager` & `Handler` (Logika Offline)

## 📸 Zrzuty Ekranu

| Tryb Jasny (Light) | Tryb Ciemny (Dark) | Alert Smogowy |
|:---:|:---:|:---:|
| <img src="sciezka_do_pliku/light_mode.png" width="200"/> | <img src="sciezka_do_pliku/dark_mode.png" width="200"/> | <img src="sciezka_do_pliku/alert_mode.png" width="200"/> |

*(Podmień ścieżki na swoje pliki graficzne w repozytorium)*

## 🔌 Warstwa Sprzętowa (Hardware)

System współpracuje z fizycznym urządzeniem zbudowanym w oparciu o:
* **Mikrokontroler:** ESP32 (DevKit V1)
* **Czujnik BME280:** Temperatura, Wilgotność, Ciśnienie (I2C)
* **Czujnik MQ-135:** Jakość powietrza (Analog ADC)
* **Wyświetlacz:** OLED SSD1306 0.96"

## 🚀 Jak uruchomić projekt?

1.  Sklonuj repozytorium:
    ```bash
    git clone [https://github.com/twoj-login/weather-station-app.git](https://github.com/twoj-login/weather-station-app.git)
    ```
2.  Otwórz projekt w **Android Studio**.
3.  Skonfiguruj Firebase:
    * Utwórz projekt w konsoli Firebase.
    * Pobierz plik `google-services.json` i umieść go w folderze `app/`.
4.  Zbuduj i uruchom aplikację na emulatorze lub fizycznym urządzeniu.

## 📝 Autor

**Martyna Niżyńska**
* Studentka Informatyki
* Uniwersytet Zielonogórski
* Kontakt: 112104@stud.uz.zgora.pl

---
*Projekt zrealizowany w ramach przedmiotu Systemy Wbudowane, 2025.*
