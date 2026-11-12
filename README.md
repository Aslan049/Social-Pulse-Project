# SocialPulse

Sosyal medya platformlarından **dışa aktarılmış** CSV ve Excel dosyalarını ortak bir metrik modeline çeviren, trend ve karşılaştırma analizleri yapan ve rapor üreten web paneli. Bitirme projesi olarak geliştirilmektedir.

> Platform hesabına yetki istemez: kullanıcı kendi hesabından dışa aktardığı dosyayı yükler, dosyanın kendisi saklanmaz, yalnızca ayrıştırılmış metrikler tutulur.

## Neden?

Her platform istatistiklerini kendi panelinde, farklı biçimlerde sunar (dosya türü, dil, sütun adları, günlük/aylık çözünürlük). Platformları karşılaştırmak çoğunlukla elle Excel işidir ve eksik günlerin 0 yazılması gibi hatalara açıktır. SocialPulse bu dosyaları tek bir metrik sözlüğünde birleştirir ve eksik veriyi 0 saymaz.

## Özellikler

| Özellik | Durum |
|---|---|
| Kayıt, giriş (JWT) | Tamamlandı |
| CSV ve XLSX yükleme, dosya doğrulama | Tamamlandı |
| Instagram günlük seri adaptörü | Tamamlandı |
| X aylık özet adaptörü | Tamamlandı |
| Satır doğrulama ve hata raporu | Sırada |
| Trend, dönem ve platform karşılaştırması | Planlandı |
| Haftanın gününe göre performans, anormal gün tespiti | Planlandı |
| Excel ve PDF rapor | Planlandı |
| Docker ile tek komutla çalıştırma | Planlandı |

## Desteklenen dosya biçimleri

- **Instagram:** Excel (.xlsx), her metrik için ayrı sayfa (`Tarih`, `Değer`), günlük. Özet (`İstatistikler`) ve birleşik (`Tüm Veriler`) sayfalar okunmaz.
- **X:** Excel (.xlsx) veya CSV, tek geniş tablo (satır = ay, sütun = metrik), aylık.
- Örnek dosyalar `sample-data/real-format/` altındadır. Dosyalar değiştirilmiş veridir, gerçek hesap performansını göstermez.

Ortak metrikler: görüntüleme, etkileşim, yeni takip, profil ziyareti. Platformlar arası karşılaştırma yalnızca bu dört metrikle ve aylık düzeyde yapılır.

## Mimari

```
Yükleme → dosya doğrulama → okuma (CSV/XLSX → ham tablo) → platform adaptörü
        → ortak metrik modeli (metric_points) → analiz → REST API → React arayüzü → rapor
```

- **Backend:** Java, Spring Boot, Spring Security + JWT, Spring Data JPA, Flyway, PostgreSQL, Apache POI, Commons CSV
- **Frontend:** React, TypeScript, Vite, Axios, React Router
- **Altyapı:** Docker Compose (veritabanı)
- Veri modeli: [`docs/er-diagram.md`](docs/er-diagram.md)

Temel tasarım kararları:

1. Okuma katmanı (`TabularReader`) adaptörlerden ayrıdır; adaptörler dosya türünü bilmez.
2. Her platform ayrı bir adaptördür; yeni platform eklemek yeni bir sınıf yazmaktır.
3. Ölçümler dar (uzun) tabloda tutulur; platformun vermediği metrik için satır olmaz, böylece "0" ile "veri yok" karışmaz.
4. Aynı dosya tekrar yüklenirse değerler güncellenir, çift kayıt oluşmaz.
5. Bozuk satır yüklemeyi düşürmez; nedeniyle raporlanır.

## Kurulum

Gereksinimler: Java 17+, Node.js, Docker.

```bash
# 1. Veritabanı
docker compose up -d

# 2. Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# 3. Frontend (http://localhost:5173)
cd frontend
cp .env.example .env
npm install
npm run dev
```

Ayarlar:

| Ayar | Nerede | Varsayılan |
|---|---|---|
| `JWT_SECRET` | ortam değişkeni (en az 32 karakter) | yalnızca geliştirme için bir varsayılan var |
| `app.cors.allowed-origins` | `application.properties` | `http://localhost:5173` |
| `VITE_API_URL` | `frontend/.env` | `http://localhost:8080` |

## API (özet)

| Uç nokta | Açıklama |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Kayıt ve giriş |
| `GET /api/users/me` | Giriş yapmış kullanıcı |
| `POST /api/uploads` | Dosya yükle (`platform` + `file`, multipart) |
| `GET /api/uploads` | Yüklemelerim |
| `DELETE /api/uploads/{id}` | Yüklemeyi sil |

Korumalı uçlar `Authorization: Bearer <token>` başlığı ister.

## Testler

```bash
cd backend
./mvnw test
```

Testler okuyucuları, adaptörleri ve servisleri kapsar; gerçek biçimli örnek dosyalarla da çalışır.

## Lisans

Bkz. [LICENSE](LICENSE).
