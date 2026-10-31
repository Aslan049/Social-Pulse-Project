# Gerçek biçimli örnek dosyalar

| Dosya | İçerik |
|---|---|
| `x_aylik_ozet.xlsx` | X: tek sayfa, **aylık** hesap seviyesinde özet (11 ay) |
| `instagram_gunluk.xlsx` | Instagram: **günlük** hesap seviyesinde seri, her metrik için ayrı sayfa (365 gün) |

- İki dosya da gönderi bazlı değil **hesap seviyesinde** veri içerir.
- Dosyalar üzerinde bazı değişiklikler yapılmıştır; gerçek hesap performansını göstermez.
- Kişisel veri içermez. Adaptör ve okuyucu testlerinin kaynağıdır
  (kopyaları `backend/src/test/resources/samples/` altındadır).

Üst klasördeki `x-sample.csv` ve `instagram-sample.csv` dosyaları sentetik, gönderi bazlı yedek örneklerdir
(MVP kapsamı dışı, gelecek çalışma).
