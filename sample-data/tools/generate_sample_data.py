#!/usr/bin/env python3
"""
SocialPulse - SENTETIK örnek veri üretici.

Uretilen dosyalar gercek bir hesaba ait DEGILDIR; test ve demo amaclidir.
Sutun adlari, platformlarin disa aktarim formatlarina benzetilmistir fakat
platformlar formati degistirebilir. Adaptorler basliklari ayri bir esleme
yapilandirmasindan okumali (bkz. plan, mimari karar 5).

Kullanim:  python generate_sample_data.py [cikti_klasoru]
Ayni seed ayni dosyalari uretir (tekrarlanabilir).
"""
import csv, math, random, sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

SEED = 42
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
START = datetime(2026, 4, 27, tzinfo=timezone.utc)   # 26 hafta
END = datetime(2026, 10, 25, tzinfo=timezone.utc)
rnd = random.Random(SEED)

X_TEXTS = [
    "Spring Boot'ta JWT ile kimlik doğrulama: adım adım rehber",
    "Bugün SAP SD'de sipariş-teslimat-fatura akışını çalıştım",
    "Yeni projeye başladım, ilk commit atıldı #yazılım",
    "PostgreSQL index'leri ne zaman işe yarar? Kısa bir thread",
    "Docker Compose ile tek komutta geliştirme ortamı",
    "Stajda öğrendiğim 5 şey: 1) okumak 2) sormak 3) not almak",
    "Kod review'da en çok yapılan hata: büyük PR'lar",
    "React mı, mobil mi? Bitirme projesi için seçim yaparken dikkat edilecekler",
    "Java 21 ile gelen özellikler, benim favorim: virtual threads",
    "Test yazmak zaman kaybı değil, yatırım #test",
    "Bu hafta okuduklarım: temiz mimari, SQL performansı, bir de roman",
    "Mülakat sorusu: REST ile GraphQL arasındaki fark nedir?",
    "Git'te rebase ve merge: ne zaman hangisi?",
    "Hata ayıklarken en çok işe yarayan şey: sorunu yüksek sesle anlatmak",
    "CSV dosyalarında Türkçe karakter sorunu: UTF-8 BOM meselesi",
    "Bir haftada bitirdiğim mini proje, ekran görüntüleri yanıtta",
    "Veri analizi: ortalama yalan söyler, dağılıma bak",
    "Haftalık özet: 3 commit, 1 bug fix, 2 yeni öğrenilen şey",
    "\"Önce çalışsın, sonra güzel olsun\" - ama testi atlama",
    "İş ilanlarında aranan beceriler: SQL, Git, İngilizce, iletişim",
]
IG_TEXTS = [
    "Bugünün çalışma masası ☕ #yazılım #bilgisayarmühendisliği",
    "Bitirme projesi yolculuğu: 1. hafta özeti",
    "Staj günlüğü: bugün neler öğrendim?",
    "Kod yazarken dinlediğim 5 playlist 🎧",
    "Mezuniyete bir yıl kala yol haritam",
    "Spring Boot nedir? 60 saniyede anlatıyorum",
    "Ofis köşem, yeni kurulum",
    "Mülakat hazırlığı için 3 ipucu",
    "SQL öğrenmeye nereden başlamalı? Kaydet, sonra lazım olur",
    "Hafta sonu bisiklet, kafa dinleme 🚴",
    "Yeni bir şey denedim: dashboard tasarımı",
    "Takip ettiğim 5 teknoloji hesabı",
    "Linkedin profilini nasıl güçlendirirsin?",
    "Bir günüm: sabah 8'den akşam 6'ya",
    "Hatalardan öğrendiklerim; 3. madde herkesi güldürecek",
]

def weighted_hour(kind):
    # X: gunduz ve aksam yogun, Instagram: aksam yogun
    hours = list(range(24))
    if kind == "x":
        w = [1,1,0,0,0,1,2,4,6,7,6,5,6,5,4,4,5,6,8,9,8,6,4,2]
    else:
        w = [0,0,0,0,0,0,1,2,3,4,4,5,6,5,4,4,5,7,9,10,9,7,4,1]
    return rnd.choices(hours, weights=w)[0]

def gen_times(n, kind):
    total_days = (END - START).days
    # son haftalara dogru artan yogunluk (buyume trendi)
    days = sorted(
        min(total_days - 1, int(total_days * (rnd.random() ** 0.8))) for _ in range(n)
    )
    out = []
    for d in days:
        dt = START + timedelta(days=d, hours=weighted_hour(kind), minutes=rnd.randint(0, 59))
        out.append(dt)
    return sorted(out)

def growth(dt):
    # zamanla artan takipci -> artan gosterim (yaklasik %60 buyume)
    return 1.0 + 0.6 * ((dt - START).days / (END - START).days)

def viral_indices(n, count):
    return set(rnd.sample(range(n), count))

# ---------------------------------------------------------------- X
def make_x(n=120):
    times = gen_times(n, "x")
    viral = viral_indices(n, 3)
    rows = []
    base_id = 1_900_000_000_000_000_000
    for i, dt in enumerate(times):
        imp = int(rnd.lognormvariate(6.3, 0.55) * growth(dt))        # ~ 550 medyan
        if i in viral:
            imp *= rnd.randint(8, 14)
        imp = max(imp, 40)
        likes = int(imp * rnd.uniform(0.012, 0.035))
        rts = int(likes * rnd.uniform(0.08, 0.25))
        replies = int(likes * rnd.uniform(0.04, 0.2))
        prof = int(imp * rnd.uniform(0.002, 0.008))
        urlc = int(imp * rnd.uniform(0.0, 0.006))
        hashc = int(imp * rnd.uniform(0.0, 0.002))
        expands = int(imp * rnd.uniform(0.004, 0.015))
        engagements = likes + rts + replies + prof + urlc + hashc + expands
        tid = str(base_id + i * 7_919 + rnd.randint(0, 5000))
        text = rnd.choice(X_TEXTS)
        rows.append({
            "Tweet id": tid,
            "Tweet permalink": f"https://x.com/demo_hesap/status/{tid}",
            "Tweet text": text,
            "time": dt.strftime("%Y-%m-%d %H:%M +0000"),
            "impressions": imp,
            "engagements": engagements,
            "engagement rate": f"{engagements / imp:.6f}",
            "retweets": rts,
            "replies": replies,
            "likes": likes,
            "user profile clicks": prof,
            "url clicks": urlc,
            "hashtag clicks": hashc,
            "detail expands": expands,
        })
    path = OUT / "x-sample.csv"
    with open(path, "w", newline="", encoding="utf-8") as f:       # BOM yok
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()), quoting=csv.QUOTE_ALL)
        w.writeheader()
        w.writerows(rows)
    return path, len(rows)

# ---------------------------------------------------------------- Instagram
def make_ig(n=60):
    times = gen_times(n, "ig")
    viral = viral_indices(n, 2)
    types = ["IG image", "IG reel", "IG carousel"]
    type_w = [4, 3, 3]
    rows = []
    base_id = 17_900_000_000_000_000
    for i, dt in enumerate(times):
        ptype = rnd.choices(types, weights=type_w)[0]
        mult = {"IG image": 1.0, "IG reel": 2.2, "IG carousel": 1.3}[ptype]
        views = int(rnd.lognormvariate(6.6, 0.5) * mult * growth(dt))
        if i in viral:
            views *= rnd.randint(7, 12)
        views = max(views, 60)
        reach = int(views * rnd.uniform(0.55, 0.85))
        likes = int(reach * rnd.uniform(0.03, 0.09))
        comments = int(likes * rnd.uniform(0.03, 0.12))
        shares = int(likes * rnd.uniform(0.05, 0.3))
        saves = int(likes * rnd.uniform(0.05, 0.4))
        follows = int(likes * rnd.uniform(0.0, 0.05))
        pid = str(base_id + i * 10_007 + rnd.randint(0, 9000))
        rows.append({
            "Post ID": pid,
            "Account username": "demo_hesap",
            "Description": rnd.choice(IG_TEXTS),
            "Publish time": dt.strftime("%m/%d/%Y %H:%M"),          # AA/GG/YYYY
            "Permalink": f"https://www.instagram.com/p/{pid[-11:]}/",
            "Post type": ptype,
            "Views": views,
            "Reach": reach,
            "Likes": likes,
            "Comments": comments,
            "Shares": shares,
            "Saves": saves,
            "Follows": follows,
        })
    # birkac gonderide reach bos olsun (NULL isleme testi)
    for r in rnd.sample(rows, 3):
        r["Reach"] = ""
    path = OUT / "instagram-sample.csv"
    with open(path, "w", newline="", encoding="utf-8-sig") as f:    # BOM VAR
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()), quoting=csv.QUOTE_MINIMAL)
        w.writeheader()
        w.writerows(rows)
    return path, len(rows)

if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for p, n in (make_x(), make_ig()):
        print(f"{p}  ({n} satır)")
