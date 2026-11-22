# Digital Banking — Mobile Module Assignment

## 1. Overview

Pengembangan Mobile Application untuk Digital Banking dibagi ke dalam
8 business module berdasarkan Bagian 3.2 — Kebutuhan Fungsional.

Karena terdapat 8 business module dan 4 anggota pada tim Mobile,
setiap anggota ditetapkan sebagai PIC (Person in Charge) untuk 2 modul.

Pembagian modul disusun dengan mempertimbangkan:
- kompleksitas modul;
- estimasi workload;
- kemampuan dan pengalaman masing-masing anggota; dan
- keseimbangan beban kerja antaranggota.

PIC bertanggung jawab terhadap pengembangan dan pemahaman utama
terhadap modul yang ditugaskan, namun pengembangan tetap dilakukan
secara kolaboratif. Pembagian ini tidak berarti setiap anggota bekerja
secara terpisah.

---

## 2. Business Modules

Business module yang akan dikembangkan mengacu pada Bagian 3.2
Kebutuhan Fungsional:

1. Authentication
2. Account
3. Transfer
4. Payment (QRIS)
5. Top-up
6. Virtual Card
7. Transaction History & Dashboard
8. Profile & Notification

---

## 3. Mobile Module Assignment

| PIC | Modul 1 | Modul 2 |
|---|---|---|
| **Salomo** | Authentication | Virtual Card |
| **Cecil** | Transfer | Transaction History & Dashboard |
| **Cia** | Payment (QRIS) | Profile & Notification |
| **Grace** | Account | Top-up |

### Salomo
- Authentication
- Virtual Card

### Cecil
- Transfer
- Transaction History & Dashboard

### Cia
- Payment (QRIS)
- Profile & Notification

### Grace
- Account
- Top-up

---

## 4. PIC Responsibilities

Setiap PIC bertanggung jawab terutama terhadap:

- memahami requirement dari modul yang ditugaskan;
- melakukan implementasi fitur pada Mobile Application;
- menjaga kesesuaian implementasi dengan UML dan requirement;
- melakukan testing terhadap modul;
- melakukan koordinasi dengan PIC modul lain apabila terdapat dependency;
- membantu integrasi modul dengan API dan komponen sistem lainnya.

PIC merupakan **penanggung jawab utama**, bukan satu-satunya developer
yang dapat mengubah atau mengerjakan modul tersebut.


---

## 5. Collaborative Development

Walaupun setiap anggota memiliki modul utama masing-masing, pengembangan
tetap dilakukan secara bersama-sama.

Area yang membutuhkan koordinasi lintas modul antara lain:

- Application Architecture
- API Contract
- REST API Integration
- Authentication & Authorization
- Database / Data Model
- Navigation
- Shared UI Components
- Error Handling
- Testing
- Integration antar-module

Jika suatu modul memiliki dependency terhadap modul lain, PIC terkait
akan berkoordinasi untuk menentukan interface dan integration flow yang
digunakan.

---

## 6. Development Principle

Pembagian ini menggunakan pendekatan **PIC-based development**.

Artinya:

> Setiap anggota memiliki tanggung jawab utama terhadap dua modul,
> tetapi seluruh anggota tetap bekerja sebagai satu tim dalam
> pengembangan, integrasi, dan penyelesaian project secara keseluruhan.

Pembagian modul dapat dievaluasi dan disesuaikan apabila ditemukan
perubahan requirement, ketidakseimbangan workload, atau kebutuhan
integrasi selama proses pengembangan.


## 7. Technology Stack

### Mobile
- Kotlin
- Android Studio
- Jetpack Compose
- MVVM
- Repository Pattern

### Planned Backend
- Java
- Spring Boot
- REST API
- PostgreSQL

### Data & Communication
- JSON
- REST API

### Security
- JWT
- HTTPS

### Deployment
- Docker
- Cloud/VPS