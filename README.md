# 📱 IP Subnet Calculator (IPv4) — Android App

แอพ Android สำหรับ **คำนวณซับเน็ต IPv4** และ **ตารางอ้างอิง CIDR** ออกแบบให้ใช้งานง่าย รองรับ Material 3, Dark Mode อัตโนมัติ, ทำงานออฟไลน์ทั้งหมด

เขียนด้วย **Kotlin + Jetpack Compose** เป็นโปรเจกต์ Gradle ที่ build เป็น APK ได้ทันที

---

## ✨ ฟีเจอร์

### 🧮 หน้าคำนวณ
- รองรับรูปแบบ input หลายแบบ:
  - `192.168.1.10/24` (IP + CIDR)
  - `192.168.10.0/255.255.255.192` (IP + dotted mask)
  - `172.16.50.20 255.255.240.0` (IP คั่นวรรค mask)
  - `192.168.1.50` (IP เฉยๆ → ถือว่าเป็น /32)
- ผลลัพธ์ครบ: **Network, Broadcast, First/Last Host, Netmask, Wildcard, Total IPs, Usable Hosts, CIDR, Binary Mask, IP Class (A/B/C/D/E), Private/Public, Loopback**
- แตะที่ค่าเพื่อ **คัดลอกไปยังคลิปบอร์ด** พร้อม Toast ยืนยัน
- ปุ่มตัวอย่างด่วน (Quick Examples) ให้ทดลองได้ทันที
- รองรับ RFC 3021 สำหรับ /31 (point-to-point) และ /32 (single host)

### 📊 หน้าตารางซับเน็ต
- ตาราง CIDR ทั้งหมด **/0 ถึง /32** พร้อม Netmask, Usable Hosts, Total IPs, Class
- ปุ่มกรอง 2 โหมด:
  - **ทั้งหมด (/0–/32)** — ดูครบทุกบรรทัด
  - **ที่ใช้บ่อย (/8–/30)** — เน้นที่ใช้จริงในงาน LAN/VLSM
- ช่องค้นหา (กรอก `24`, `255.255.255`, `B` ฯลฯ)
- **แตะแถวเพื่อส่ง CIDR ไปคำนวณ** ในหน้าคำนวณทันที

---

## 🏗️ โครงสร้างโปรเจกต์

```
IPSubnetCalculator/
├── settings.gradle.kts
├── build.gradle.kts                     # root
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml               # version catalog
│   └── wrapper/gradle-wrapper.properties
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts                 # module config
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/                     # strings, colors, themes, launcher icons
        │   └── java/com/example/ipsubnetcalc/
        │       ├── MainActivity.kt      # entry + bottom navigation
        │       ├── core/
        │       │   ├── SubnetCalculator.kt   # ← calculation engine (pure Kotlin)
        │       │   └── SubnetTable.kt        # ← CIDR reference table
        │       └── ui/
        │           ├── CalculatorScreen.kt
        │           ├── SubnetTableScreen.kt
        │           ├── components/ (ResultCard, CopyableRow)
        │           └── theme/ (Color, Theme, Type)
        └── test/java/com/example/ipsubnetcalc/
            └── SubnetCalculatorTest.kt  # unit tests สำหรับ engine
```

---

## 🔨 วิธี build

### สิ่งที่ต้องมี
- **JDK 17** (แนะนำ Temurin / OpenJDK 17)
- **Android SDK** (compileSdk 34) — ติดตั้งผ่าน Android Studio หรือ `sdkmanager`
- ตั้งค่า environment variable `ANDROID_HOME` ให้ชี้ไปยัง SDK (เช่น `C:\Users\<user>\AppData\Local\Android\Sdk`)

### ทางเลือก A — Android Studio (ง่ายสุด ⭐ แนะนำ)
1. เปิด Android Studio → **File → Open** → เลือกโฟลเดอร์ `IPSubnetCalculator`
2. Android Studio จะสร้าง Gradle Wrapper และ sync ให้อัตโนมัติ
3. รอจนเสร็จ (อาจใช้เวลานานครั้งแรกเพื่อดาวน์โหลด dependencies)
4. กด **Run ▶** เพื่อทดสอบบน emulator/อุปกรณ์
5. หรือ **Build → Build Bundle(s)/APK(s) → Build APK(s)** เพื่อสร้าง APK

### ทางเลือก B — Command line (ต้องติดตั้ง Gradle ก่อน)
โปรเจกต์นี้ยังไม่มี `gradle-wrapper.jar` ติดมา หากใช้ command line ต้องสร้าง wrapper ก่อน:

```bash
cd IPSubnetCalculator
gradle wrapper --gradle-version 8.7      # รันครั้งเดียว เพื่อสร้าง gradlew + wrapper jar
./gradlew assembleDebug                   # บน macOS/Linux/Git Bash
gradlew.bat assembleDebug                 # บน Windows CMD/PowerShell
```

ไฟล์ APK จะอยู่ที่:
```
app/build/outputs/apk/debug/app-debug.apk
```

### รัน unit tests
```bash
./gradlew test
```

---

## 📲 ติดตั้ง APK ลงเครื่อง

1. บนมือถือ: **Settings → Security → เปิด "Unknown sources"** (หรือยืนยันตอนติดตั้ง)
2. คัดลอก `app-debug.apk` ไปยังมือถือ
3. เปิดไฟล์ APK ใน File Manager → กดติดตั้ง

หรือใช้ adb:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 ตัวอย่างการใช้งาน

| Input | Network | Broadcast | Usable Hosts |
|---|---|---|---|
| `192.168.1.10/24` | 192.168.1.0 | 192.168.1.255 | 254 |
| `10.0.0.5/8` | 10.0.0.0 | 10.255.255.255 | 16,777,214 |
| `172.16.50.20/20` | 172.16.48.0 | 172.16.63.255 | 4,094 |
| `203.0.113.7/28` | 203.0.113.0 | 203.0.113.15 | 14 |
| `192.168.1.4/30` | 192.168.1.4 | 192.168.1.7 | 2 |

---

## ⚙️ ข้อกำหนดทางเทคนิค

| รายการ | ค่า |
|---|---|
| minSdk | **26** (Android 8.0) — ครอบคลุม ~95% ของอุปกรณ์ |
| targetSdk / compileSdk | 34 (Android 14) |
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Build | Gradle 8.7 + AGP 8.5 |
| Java | 17 |

---

## 📝 หมายเหตุ

- แอพทำงาน **ออฟไลน์ทั้งหมด** ไม่ต้องใช้อินเทอร์เน็ต ไม่มีโฆษณา ไม่เก็บข้อมูล
- Logic การคำนวณแยกอยู่ใน `core/SubnetCalculator.kt` เป็น pure Kotlin เพื่อให้ทดสอบได้ง่าย (ดู `SubnetCalculatorTest.kt`)
- โปรเจกต์ยังไม่มี `gradle-wrapper.jar` (binary ที่ต้อง generate ด้วย Gradle) — **แนะนำให้เปิดผ่าน Android Studio** ซึ่งจะจัดการให้อัตโนมัติ หรือรัน `gradle wrapper --gradle-version 8.7` ครั้งเดียวจากเครื่องที่ติดตั้ง Gradle
