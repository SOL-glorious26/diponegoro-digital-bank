# Kepanjangan Nama dan Kode Dokumen: Diponegoro Digital Bank

Daftar ini menjelaskan semua singkatan nama dokumen, kode dokumen, dan istilah pendek yang dipakai di proyek.
Sumber kode dokumen: **DWP-DDB-001 v1.3, Tabel 2.3**. DWP hanya mencantumkan kode dan nama volumenya;
kepanjangan huruf per huruf di bawah disusun dari nama volume tersebut (huruf tebal menunjukkan asal singkatannya).

---

## 1. Cara membaca kode dokumen

Contoh: **`SDD-DDB-001-AND`**

| Bagian | Arti |
|---|---|
| `SDD` | Jenis dokumen: *Software Detailed Design* |
| `DDB` | Nama proyek: **D**iponegoro **D**igital **B**ank |
| `001` | Nomor urut dokumen dari jenis tersebut |
| `AND` | Platform (bila ada): `AND` = Android, `WEB` = Web, `BE` = Backend & Database |

Nama berkas memakai pola `DDB_<ID volume>_<nama>_v<versi>.docx`, misalnya `DDB_D03_Functional_Design_v0.9.3.docx`.

---

## 2. Sembilan dokumen wajib (dan volumenya)

| No | ID volume | Kode dokumen | Kepanjangan kode | Nama dalam Bahasa Indonesia | PIC |
|---|---|---|---|---|---|
| 1 | **D01** | PRJ-DDB-001 | **Pr**o**j**ect Document | Dokumen Proyek | Salomo |
| 2 | **D02** | SAD-DDB-001 | **S**oftware **A**rchitecture **D**esign | Desain Arsitektur | Salomo |
| 3 | **D03** | FSD-DDB-001 | **F**unctional **S**pecification / **D**esign | Desain Fungsional | Salomo |
| 4 | **D04A** | SDD-DDB-001-AND | **S**oftware **D**etailed **D**esign – Android | Desain Rinci Android | Gracia |
| 4 | **D04W** | SDD-DDB-001-WEB | Software Detailed Design – Web | Desain Rinci Web | Rafi |
| 4 | **D04B** | SDD-DDB-001-BE | Software Detailed Design – **B**ack**e**nd & Database | Desain Rinci Backend & Basis Data | Salomo |
| 5 | **D05A** | UIM-DDB-001-AND | **U**ser **I**nterface **M**ap – Android | Peta Antarmuka Android | Cecilia (dukungan Grace) |
| 5 | **D05W** | UIM-DDB-001-WEB | User Interface Map – Web | Peta Antarmuka Web | Nabila (dukungan Permata) |
| 6 | **D06** | STD-DDB-001 | **S**ystem **T**esting **D**ocument | Pengujian Sistem | Steven |
| 7 | **D07A** | DVM-DDB-001-AND | **D**e**v**eloper **M**anual – Android | Manual Pengembang Android | Salomo (bagian modul: Gracia) |
| 7 | **D07W** | DVM-DDB-001-WEB | Developer Manual – Web | Manual Pengembang Web | Steven (dukungan Nabila) |
| 7 | **D07B** | DVM-DDB-001-BE | Developer Manual – Backend | Manual Pengembang Backend | Salomo |
| 8 | **D08** | DEM-DDB-001 | System Program **Dem**o | Demo Program Sistem | Rafi |
| 9 | **D09A** | USM-DDB-001-AND | **Us**er **M**anual – Android | Panduan Pengguna Android | Grace |
| 9 | **D09W** | USM-DDB-001-WEB | User Manual – Web | Panduan Pengguna Web | Permata |

Arti huruf di belakang ID volume: **A** = Android, **W** = Web, **B** = Backend & Database.

---

## 3. Dokumen pendukung dan acuan

| ID / Kode | Kepanjangan | Isi singkat |
|---|---|---|
| **D00** / DWP-DDB-001 | **D**ocumentation **W**ork **P**lan (Software Engineering Documentation Work Plan) | Rencana kerja dokumentasi: pembagian 9 dokumen, 125 tugas, alur review, Definition of Done |
| SRS-DDB-001 | **S**oftware **R**equirements **S**pecification | Spesifikasi kebutuhan perangkat lunak (FR, BR, NFR, use case); acuan utama semua fitur |
| MDTA-DDB-001 | **M**odule **D**ecomposition & **T**ask **A**llocation | Pembagian modul coding ke Android/Web/Backend/Database. Berkas aslinya bernama `PANDUAN_MODULE.docx` |
| Project Brief v1.0 | Project Brief & Scope Document | Gambaran proyek, ruang lingkup, risiko awal |
| TUT-DDB-001-AND | **Tut**orial – Android | Panduan Uji Aplikasi di Ponsel |
| TUT-DDB-002-WEB | Tutorial – Web | Panduan Menjalankan dan Menguji Web |
| SRS Web v1.0 | SRS buatan tim Web (tidak resmi) | Sudah **tidak dipakai**; acuan resmi tetap SRS-DDB-001 |

---

## 4. Kode di dalam dokumen

### Kebutuhan dan aturan (dari SRS)

| Kode | Kepanjangan | Contoh |
|---|---|---|
| **FR-xx** | **F**unctional **R**equirement (kebutuhan fungsional) | FR-15 Pindai QRIS |
| **BR-xx** | **B**usiness **R**ule (aturan bisnis) | BR-01 Minimum transfer Rp5.000 |
| **NFR-xx** | **N**on-**F**unctional **R**equirement (kebutuhan non-fungsional) | NFR-01 Layar utama ≤ 2 detik |
| **UC-01** | **U**se **C**ase 01 | Transfer Dana |
| **A1–A6** | **A**lur alternatif UC-01 | A5 = koneksi putus, status "Diproses" |
| Must / Should / Could | Prioritas **MoSCoW** (Must have, Should have, Could have, Won't have) | 15 Must, 11 Should, 3 Could |

### Kode layar dan halaman

| Kode | Arti | Dokumen sumber |
|---|---|---|
| **AND-01 – AND-23** | Kode layar aplikasi **And**roid | D05A |
| **UI-01 – UI-22** | Kode halaman aplikasi Web (**U**ser **I**nterface) | D05W |

### Kode pengujian (dari D06)

| Kode | Kepanjangan |
|---|---|
| **BE-xx** | Kasus uji **B**ack**e**nd/API otomatis |
| **SA-xx** | Uji **S**istem **A**ndroid |
| **WB-xx** | Uji sistem **W**e**b** |
| **LP-xx** | Uji **L**intas **P**latform (Android ↔ Web) |
| **NF-xx** | Uji **N**on-**F**ungsional |
| **DEF-xx** | **Def**ect (temuan bug) |

### Kode lain

| Kode | Kepanjangan | Dipakai di |
|---|---|---|
| **ADR-xx** | **A**rchitecture **D**ecision **R**ecord (catatan keputusan arsitektur) | D02 |
| **R1–R17** | Risiko nomor 1–17 | D01 Bab IV |
| **BE-1 – BE-4** | Slot pengembang **B**ack**e**nd (semuanya diisi Salomo) | MDTA |
| **WPx-Tyy** | **W**ork **P**ackage x – **T**ugas yy, misalnya D04A-WP2-T01 | DWP |
| **R0 / R1 / R2 / R3** | Tahap review: R0 outline, R1 pre-review pasangan, R2 review teknis, R3 konsistensi lintas dokumen | DWP |
| **PIC** | **P**erson **i**n **C**harge (penanggung jawab) | Semua |
| **DoD** | **D**efinition **o**f **D**one (kriteria selesai) | DWP, MDTA |
| **RACI** | **R**esponsible, **A**ccountable, **C**onsulted, **I**nformed | MDTA |
| **KSLOC / PM** | Ribuan baris kode (*Kilo Source Lines of Code*) / *Person-Month* (COCOMO II) | D01 Bab V |
| **COCOMO II** | **Co**nstructive **Co**st **Mo**del II (model estimasi biaya) | D01 |

---

## 5. Istilah teknis yang sering muncul

| Singkatan | Kepanjangan |
|---|---|
| OTP | *One-Time Password* (kode sekali pakai) |
| e-KYC | *Electronic Know Your Customer* (verifikasi identitas elektronik) |
| PIN | *Personal Identification Number* |
| QRIS | *Quick Response Code Indonesian Standard* |
| API | *Application Programming Interface* |
| JWT | *JSON Web Token* |
| UML | *Unified Modeling Language* |
| ERD | *Entity Relationship Diagram* |
| MVVM | *Model–View–ViewModel* |
| APK | *Android Package Kit* |
| CORS | *Cross-Origin Resource Sharing* |
| TLS | *Transport Layer Security* |
| JaCoCo | *Java Code Coverage* (alat ukur cakupan test) |
