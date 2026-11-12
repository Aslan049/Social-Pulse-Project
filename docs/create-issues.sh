#!/usr/bin/env bash
# GitHub CLI (gh) ile kalan işleri milestone ve issue olarak açar.
# Kullanım: önce `gh auth login`, sonra depo kökünde: bash docs/create-issues.sh
set -e

gh api repos/:owner/:repo/milestones -f title="Hafta 2 - Veri alımı ve temel analiz" >/dev/null
gh api repos/:owner/:repo/milestones -f title="Hafta 3 - İçgörüler ve panel" >/dev/null
gh api repos/:owner/:repo/milestones -f title="Hafta 4 - Rapor, paketleme, test, tez" >/dev/null

issue() { gh issue create --title "$1" --body "$2" --milestone "$3" >/dev/null && echo "Açıldı: $1"; }

W2="Hafta 2 - Veri alımı ve temel analiz"
issue "Esnek adaptör altyapısı (Gün 8)" "Türkçe/İngilizce başlık takma adları, geniş ve uzun tablo biçimleri, CSV girdisi, bozulmuş varyant testleri." "$W2"
issue "Satır doğrulama ve hata raporu (Gün 9)" "upload_errors tablosu ve entity'si, negatif değer ve aşırı uç uyarıları, kabul/ret sayıları." "$W2"
issue "Kalıcılaştırma ve yönetim uçları (Gün 10)" "Yükleme detayı ve hata listesi endpoint'leri, silme ve sahiplik testleri." "$W2"
issue "Yükleme ekranı (Gün 11)" "Sürükle-bırak, platform seçimi, sonuç özeti, hata tablosu, yükleme geçmişi." "$W2"
issue "Analiz servisi v1 (Gün 12)" "Seri sorguları, günden aya toplama, KPI'lar, kapsama oranı." "$W2"
issue "Dönem ve platform karşılaştırma (Gün 13)" "Dönem karşılaştırma ve platform karşılaştırma endpoint'leri (ortak dört metrik, aylık)." "$W2"

W3="Hafta 3 - İçgörüler ve panel"
issue "Dashboard iskeleti ve filtreler (Gün 15)" "Sayfa düzeni, tarih ve platform filtreleri, yükleniyor/hata durumları." "$W3"
issue "KPI kartları ve trend grafiği (Gün 16)" "Çizgi grafik, dönem değişimi, kapsam eksik uyarısı." "$W3"
issue "Platform karşılaştırma ekranı (Gün 17)" "Ortak metrikler için aylık çubuk grafik ve tablo." "$W3"
issue "Haftanın gününe göre performans (Gün 18)" "Medyan tabanlı analiz, en iyi ve en zayıf dönem, büyüme özeti." "$W3"
issue "Anormal gün tespiti (Gün 19)" "Medyan/MAD tabanlı değiştirilmiş z-skoru, en az 10 nokta, MAD=0 durumu, testler." "$W3"
issue "İçgörü arayüzü ve boş durumlar (Gün 20)" "İçgörü kartları, anormal günlerin grafikte işaretlenmesi, boş durum ekranları." "$W3"
issue "Doğruluk testi (Gün 21)" "Yapay sıçrama enjeksiyonuyla kesinlik/duyarlılık, temel yöntemlerle karşılaştırma, elle hesaplanmış referans." "$W3"

W4="Hafta 4 - Rapor, paketleme, test, tez"
issue "Excel rapor (Gün 22)" "Apache POI ile özet, seri ve karşılaştırma sayfaları." "$W4"
issue "PDF rapor (Gün 23)" "OpenPDF ile özet ve tablolar, Türkçe font gömme." "$W4"
issue "Rapor arayüzü (Gün 24)" "Filtreye göre rapor indirme, hata durumları." "$W4"
issue "Docker ile paketleme (Gün 25)" "Backend ve frontend Dockerfile'ları, tam docker-compose, .env.example." "$W4"
issue "Test günü (Gün 26)" "Entegrasyon ve yetkilendirme testleri, kapsam raporu." "$W4"
issue "Kullanılabilirlik testi (Gün 27)" "5-10 kişi, üç görev, SUS anketi." "$W4"
issue "Belgeleme (Gün 28)" "README, OpenAPI, demo senaryosu, repo temizliği." "$W4"
issue "Tez ve sunum (Gün 29)" "Test ve değerlendirme, rakip tablosu, sonuç, slaytlar." "$W4"
