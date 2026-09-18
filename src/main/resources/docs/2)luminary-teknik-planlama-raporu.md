# LUMINARY — Teknik Planlama ve Uygulama Raporu

**AI destekli sınav hazırlık ve eğitim yönetim platformu**  
**Sürüm:** 1.0 — Teknik planlama taslağı  
**Tarih:** 11 Eylül 2026  
**Ekip varsayımı:** İki geliştirici, sınırlı başlangıç bütçesi, GitHub merkezli çalışma  
**Teslimat hedefi:** Ölçülebilir bir kurum pilotu ve ardından ilk satılabilir B2B2C MVP

> Bu rapor bir uygulama planıdır; özelliklerin geliştirilmiş olduğunu, altyapının kurulmuş olduğunu veya hukuki uygunluğun sağlandığını iddia etmez. Kaynaklarda kesinleşmemiş seçimler aşağıda **öneri** veya **karar kapısı** olarak belirtilmiştir. Sayısal eşikler, maliyet sınırları ve hizmet hedefleri pilotta doğrulanacak başlangıç varsayımlarıdır.

## İçindekiler

1. [Amaç, kaynaklar ve ürünün teknik karşılığı](#amac)
2. [MVP kapsamı ve ürün sınırları](#kapsam)
3. [Mimari kararlar ve açık karar kapıları](#kararlar)
4. [Sistem mimarisi ve modül sınırları](#mimari)
5. [Teknoloji seçimi ve geliştirme yapısı](#teknolojiler)
6. [Kimlik, yetkilendirme ve çok kiracılı veri modeli](#guvenlik-modeli)
7. [Veri modeli ve veri yaşam döngüsü](#veri-modeli)
8. [API ve istemci sözleşmeleri](#api)
9. [Asenkron işler, olaylar ve tutarlılık](#olaylar)
10. [Ölçüm, weakness, öneri ve risk kuralları](#analitik)
11. [Özelliklerin ayrıntılı uçtan uca akışları](#akislar)
12. [AI ve RAG kalite mühendisliği](#ai-kalite)
13. [Güvenlik, gizlilik ve içerik hakları](#gizlilik)
14. [Dağıtım, gözlemlenebilirlik ve maliyet kontrolü](#operasyon)
15. [Test stratejisi ve pilot kabulü](#test)
16. [Teslimat sıralaması ve ekip çalışma modeli](#teslimat)
17. [Kapsamlı geliştirme backlog’u](#backlog)

---

<a id="amac"></a>
## 1. Amaç, Kaynaklar ve Ürünün Teknik Karşılığı

### 1.1. Kaynaklar

| Kaynak | Bu rapora taşıdığı kararlar |
| --- | --- |
| [Project Overview & Product Vision](project-overview.md) | YKS odağı; öğrenci ve kurum deneyiminin aynı veri döngüsünü paylaşması; Practice Lab, My Tasks, Smart Study, Weakness Map, deneme yönetimi; B2B ağırlıklı gelir modeli; MVP dışı alanlar. |
| [Initial Roadmap and Design](initial-roadmap-and-design.md) | Angular, Java/Spring Boot, PostgreSQL/pgvector, Python/FastAPI; modular monolith; tenant izolasyonu; deterministik analitik; outbox yaklaşımı; düşük operasyon maliyeti; GitHub tabanlı teslimat. |

Bu belgeler ürün vizyonunu ve başlangıç yönünü tanımlar. Bu rapor ayrıca veri sahipliğini, transaction sınırlarını, hata durumlarını, API davranışını, kalite kapılarını, operasyonu ve görev düzeyinde teslimatı netleştirir.

**Çalışma alanı notu:** Belgelerin bulunduğu `idl-access-token-svc` dizininin uygulama kodu Luminary uygulaması olarak değerlendirilmemiştir. Yeni ürün için ayrı bir Luminary deposu önerilir. Kaynakta anlatılan bitirme projesindeki mevcut özelliklerin kod kalitesi veya yeniden kullanılabilirliği bu rapor kapsamında doğrulanmamıştır; önce kısa bir envanter çalışması yapılmalıdır.

### 1.2. Asıl ürün ve teknik başarı döngüsü

Luminary yalnızca AI ile soru üreten bir uygulama değildir. Öğrencinin öğrenme verisini anlamlı çalışma aksiyonuna, kurum için de destek kararına dönüştüren bir B2B2C platformudur.

```text
Çalışma / deneme sonucu
    → doğrulanmış ham veri
    → açıklanabilir performans ölçümü
    → kişiselleştirilmiş görev
    → öğrencinin yeni çalışması
    → kurumun incelemesi ve müdahalesi
    → yeni ölçüm
```

Teknik tasarımın başarısı şu sorularla ölçülür:

- Bir cevaptan Weakness Map’e kadar veri kaybolmadan ilerliyor mu?
- Önerilen görevin gerekçesi ham veriye kadar izlenebiliyor mu?
- Kurum yalnızca yetkili olduğu öğrenci verisini görebiliyor mu?
- AI devre dışıyken kimlik, deneme, görev, ölçüm ve dashboard çalışıyor mu?
- Öğrenciye dair yetersiz veri, yanlış biçimde kesin bir yargıya dönüşüyor mu?
- İki kişilik ekip sistemi güvenle dağıtabiliyor, izleyebiliyor ve geri yükleyebiliyor mu?

### 1.3. Hedef kullanıcı ve ticari bağlam

- **Öğrenci:** YKS’ye hazırlanan 11. sınıf, 12. sınıf ve mezun öğrenciler.
- **İlk alıcı:** Düzenli deneme yapan ve rehberlik ihtiyacı bulunan özel dershaneler; ilk pilotta bir kurum ve 100–300 öğrenci.
- **Sonraki alıcı:** Özel okullar, çok şubeli kurumlar ve zincirler.
- **Ana gelir:** Kurumun öğrenci koltuğu başına dönemlik veya aylık aboneliği.
- **İkincil gelir:** Sonraki aşamada B2C Premium; daha sonra Enterprise, entegrasyon ve özel raporlama.
- **Satış vaadi:** Akademik gelişime destek ve görünürlük; kesin net artışı, başarı sıralaması veya sınav sorusu tahmini garantisi değil.

---

<a id="kapsam"></a>
## 2. MVP Kapsamı ve Ürün Sınırları

### 2.1. Teslimat seviyeleri

| Seviye | Anlamı | Çıkış koşulu |
| --- | --- | --- |
| **P0 — Güvenli dikey dilim** | Sentetik verili geliştirme; ardından sınırlı gerçek kullanımla temel döngü | Güvenli giriş, tenant izolasyonu, öğrenci, practice/cevap, ilk ölçüm, görev ve temel kurum görünürlüğü; gerçek veri öncesi gerekli gizlilik/operasyon kontrolleri. |
| **P1 — Satılabilir MVP** | Kaynakların önerdiği öğrenci + kurum çekirdeğinin tamamı | Smart Study, deneme aktarımı, trend/weakness, risk uyarısı, müdahale kaydı, kota/koltuk yönetimi ve pilot kalite kapıları. |
| **P2 — Intelligence** | Veri üzerinde daha güçlü kişiselleştirme | Coach, gelişmiş öneriler, sınav trendleri, hedef tabanlı çalışma; doğrulanmış veri ve kullanım ihtiyacına bağlı. |
| **P3 — Scale / Expansion** | Kullanıcı, kurum ve operasyon ölçeği | Çoklu şube/roller, benchmark, ulusal analiz, yeni sınavlar, mobil ve ihtiyaç odaklı altyapı ayrıştırması. |

P0 tek başına kaynaklarda tanımlanan tam MVP değildir. Bir özelliğin pilotta ertelenmesi, satış kapsamının ve pazarlama iddialarının da açıkça daraltılmasını gerektirir.

### 2.2. Özellik kapsam matrisi

| Alan | P0/P1 kapsamı | Bilinçli sınır |
| --- | --- | --- |
| Öğrenci hesabı | Güvenli davet, profil, sınıf/mezun bilgisi, temel hedef | Üniversite/bölümden otomatik net hedefi türetme yok. |
| Practice Lab | Ders/konu/seviye seçimi, çoktan seçmeli soru seti, cevap, açıklama, geçmiş | İlk sürümde açık uçlu cevap puanlama ve fotoğraftan soru çözme yok. |
| My Tasks | Manuel ve kural tabanlı görev, ilerleme, tamamlanma, gerekçe | Serbest AI ajanın kendiliğinden çalışma planı değiştirmesi yok. |
| Smart Study | Yetkili içerikte kitap/sayfa seçimi, açıklama, kaynak gösterimi | İnternetten sınırsız PDF toplama, öğrenci PDF yükleme ve genel amaçlı sohbet yok. |
| Weakness Map | Konu bazlı ölçüm, örneklem/güncellik, öneriye geçiş | Az veriden kesin yeterlilik/başarısızlık çıkarımı yok. |
| Deneme sonuçları | Öğrenci profilinde ders netleri ve karşılaştırılabilir trend | Ders toplamından konu yanlışı çıkarımı yok; konu analizi için soru-konu eşlemesi gerekir. |
| Kurum yönetimi | Kurum, sınıf/grup, öğrenci, davet, aktif üyelik | Ayrı öğretmen/rehber/şube rol hiyerarşisi yok. |
| Dashboard | Aktif öğrenci, çalışma/cevap/görev/deneme ölçümleri, destek listesi | Yüzlerce grafik, canlı veri akışı veya ulusal karşılaştırma yok. |
| Risk ve müdahale | Açıklanabilir kurallar, incele/ertele/çözüldü, takip tarihi | Psikolojik tanı, otomatik disiplin veya yüksek etkili karar yok. |
| Ticari yönetim | Tek kurum paketi, koltuk limiti, AI kotası, pilot/sözleşme durumu | MVP’de ödeme sağlayıcısı, üç paketli katalog ve otomatik fatura motoru zorunlu değil. |

### 2.3. YKS, TYT ve AYT kararı

**Öneri:** İlk pilotu TYT akışlarıyla uçtan uca doğrulamak; veri modelini `exam_program → exam_type → subject → topic → curriculum_version` biçiminde AYT’ye açık kurmak. AYT kapsamı pilot kurumla içerik/deneme formatları üzerinden ayrıca kararlaştırılmalıdır.

- Bu tercih YKS vizyonunu değiştirmez; teslimat kapsamını küçültür.
- TYT ve AYT netleri aynı ölçeğin eşdeğer ölçümleri gibi birleştirilmez.
- LGS, KPSS ve diğer sınavlar MVP dışıdır.
- AYT ilk müşterinin satın alma koşuluysa yalnız ilgili ders ve içeriklerle kapsam genişletilir; takvim ve kalite değerlendirmesi yeniden yapılır.
- Desteklenmeyen sınav/ders UI’da çalışır görünmez; ürün metni gerçek kapsamla tutarlı olur.

### 2.4. MVP dışında tutulacaklar

AI Study Coach sohbeti, ileri predictive ML, ulusal Weakness Map, kurumlar arası benchmark, üniversite tercih motoru, ebeveyn paneli, sosyal özellikler/chat, leaderboard, gamification, kapsamlı öğretmen paneli, native mobil uygulama, white-label ve çoklu bölge dağıtımı ilk satılabilir sürümün ön koşulu değildir. Bu alanlar son bölümde ayrı, koşullu backlog olarak ele alınmıştır.

---

<a id="kararlar"></a>
## 3. Mimari Kararlar ve Açık Karar Kapıları

### 3.1. Karar kayıtları — ADR özeti

| ID | Karar / öneri | Gerekçe ve bedeli |
| --- | --- | --- |
| ADR-01 | Domain modülleri olan Spring Boot modular monolith | Tek dağıtım ve transaction kolaylığı; modül sınırları otomatik testle korunmazsa sıradan monolit riski vardır. |
| ADR-02 | Python/FastAPI yalnız AI ve belge işleme için ayrı servis | Python ekosisteminden yararlanır; ağ hatası, sözleşme ve ikinci runtime yönetimi gerekir. |
| ADR-03 | PostgreSQL, ortak şema, tenant kapsamlı kayıtlar | Düşük maliyet; uygulama filtresi, bileşik anahtar ve RLS savunması zorunlu. |
| ADR-04 | Kurum tenant’ı ve kişisel tenant ayrı; kimlik global | B2C verisi ortak bir varsayılan tenant’a yığılmaz; kurum değişiminde görünürlük açık yönetilir. |
| ADR-05 | Kalıcı işler + transactional outbox + PostgreSQL worker | Kafka gerektirmeden dayanıklılık; tekrar işleme, lease, hata kuyruğu ve temizlik uygulamanın sorumluluğudur. |
| ADR-06 | Net, ölçüm, weakness, risk ve görev kararları deterministik | Açıklanabilirlik/test edilebilirlik; eşikler eğitim uzmanı ve pilotla kalibre edilir. |
| ADR-07 | REST/OpenAPI ve uzun işlerde polling | İki kişi için basit entegrasyon; WebSocket başlangıçta gereksiz. |
| ADR-08 | Backend tek iş verisi yazarı; Python’da genel DB yetkisi yok | Tenant ve transaction kuralları merkezde kalır; belge/embedding aktarımı kontrollü batch sözleşmesi gerektirir. |
| ADR-09 | Nesne depolama private; pgvector aynı PostgreSQL’de | Ayrı vektör DB yok; vektör ve transactional yük birlikte izlenir. |
| ADR-10 | MVP’de yönetilen OIDC, web için BFF oturumu önerisi | Tarayıcıda uzun ömürlü token tutmayı önler; sunucu oturumu ve CSRF kontrolü gerekir. |
| ADR-11 | Tek kurum paketi + uygulama içi entitlement/kota | Erken ödeme altyapısı maliyetini önler; tahsilat/fatura operasyonu başta manuel olabilir. |
| ADR-12 | Tek bölge, küçük container dağıtımı, managed DB tercih | Operasyon azalır; tek compute arızasında kesinti kabulü ve geri yükleme planı gerekir. |
| ADR-13 | Global katalog ile öğrenci özel verisi fiziksel tablo düzeyinde ayrı | Her tabloya körlemesine `tenant_id` eklemek yerine erişim sınıfları belirgin olur. |
| ADR-14 | İlk içerik ve soru kapsamı dar, sürümlü ve incelenebilir | İçerik hızı yerine kalite; uzman incelemesi ekip kapasitesine dahil edilir. |

### 3.2. Geliştirmeyi veya yayını bloke eden kararlar

| Kapı | Karar | Son karar zamanı | Karar çıkmazsa güvenli yol |
| --- | --- | --- | --- |
| K-01 | Mevcut bitirme projesinden hangi kod/varlık kullanılabilir? | İlk planlama | Yeniden kullanım varsaymadan küçük iskelet; mevcut kodu körlemesine taşıma yok. |
| K-02 | Pilot TYT mi, TYT + belirli AYT dersleri mi? | Taksonomi ve içerik üretimi öncesi | TYT önerisi; pilot satış kapsamı yazılı daraltılır. |
| K-03 | OIDC sağlayıcısı, barındırma bölgesi ve veri aktarım koşulları | Gerçek kullanıcı açılmadan | Lokal/sentetik veriyle devam; gerçek öğrenci verisi gönderilmez. |
| K-04 | MEB/yayıncı içeriğinin saklama, işleme, AI ve gösterim hakları | İçerik ingestion öncesi | Hakları alınmış veya ekibin özgün içeriği; izinsiz MEB yayını yok. |
| K-05 | Çocuk verisi, aydınlatma, hukuki dayanak, kurum sözleşmesi | Gerçek veri pilotundan önce | Hukuk uzmanı incelemesi; gerekiyorsa uygun veli/yasal temsilci süreci. |
| K-06 | Pilot ücretli mi, süre/koltuk/kota ve başarı kriteri ne? | Kurum onboarding öncesi | Ölçülebilir, süreli pilot sözleşmesi; belirsiz sınırsız ücretsiz kullanım yok. |
| K-07 | Kabul edilen soru kalitesi ve inceleme sorumlusu kim? | AI soruları öğrenciye açılmadan | İncelenmiş soru havuzu; doğrulanamayan AI sorusunu yayınlamama. |
| K-08 | Saklama, silme, yedek RPO/RTO ve aylık bütçe | Gerçek veri pilotundan önce | Yazılı politika ve geri yükleme denemesi olmadan production yok. |

Bu rapor hukuki görüş veya sağlayıcı fiyat teklifi değildir. Lisanslar, veri aktarımı, güncel hizmet koşulları ve teknoloji destek tarihleri uygulama başlangıcında ayrıca doğrulanır.

---

<a id="mimari"></a>
## 4. Sistem Mimarisi ve Modül Sınırları

### 4.1. Mantıksal yerleşim

```text
Öğrenci / Kurum yöneticisi
          │ HTTPS, aynı origin
          ▼
Nginx / TLS ── Angular statik uygulaması
          │ /api ve /auth
          ▼
Spring Boot — BFF + REST + Domain modülleri
          ├── PostgreSQL: iş verisi, oturum, outbox, işler, ölçümler
          ├── pgvector: yetkili içerik chunk embedding'leri
          ├── Private object storage: kaynak belgeler ve import dosyaları
          ├── OIDC sağlayıcısı: kullanıcı kimliği
          └── İç ağ → FastAPI: üretim, extraction, embedding, açıklama
                               └── İzin verilen AI sağlayıcısı

Spring Boot içindeki sınırlı worker havuzları
          ├── soru üretim işleri
          ├── içerik/import işleri
          └── outbox → ölçüm → öneri/risk projeksiyonları
```

MVP’de worker’lar aynı backend sürecinde farklı, sınırlı executor havuzlarıdır. CPU/ağ ağırlıklı AI ve PDF işleri Python tarafında çalışır. API latency’si etkilenirse aynı Java artifact’ı ayrı `worker` çalışma profiliyle dağıtılır; yeni domain servisi icat edilmez.

### 4.2. Modüller ve sahiplik

| Modül | Sahip olduğu iş ve veri | Dışarı sunduğu sözleşme |
| --- | --- | --- |
| `access` | Kimlik eşleme, üyelik/rol kontrolü, oturum, davet güvenliği, aktif bağlam | Yetkili principal/tenant, üyelik sorgusu; kimlik sağlayıcı adaptörü. |
| `institution` | Kurum, sınıf/grup, öğrenci kaydı, müdahale kaydı | Kurum/grup üyelik görünümü; öğrenci davet/onboarding akışı. |
| `student` | Tenant kapsamlı öğrenci profili, sınıf düzeyi, temel hedef | Yetkili profil ve minimum öğrenci bağlamı. |
| `curriculum` | Sınav türü, ders, konu, kazanım/sürüm, puanlama politikası | Kimlikleri sürümlü, salt okunur katalog. |
| `practice` | Oturum, soru snapshot’ı, cevap, sonuç, soru itirazı | Practice API; `AttemptRecorded`, `QuestionInvalidated` olayları. |
| `tasks` | Manuel/önerilmiş görev, ilerleme, tamamlanma, gerekçe | Task API; görev oluşturma portu; yaşam döngüsü olayları. |
| `exams` | Deneme, import taslağı, yayınlanmış sonuç ve revizyon | Sonuç okuma sözleşmesi; `ExamResultsPublished/Corrected` olayları. |
| `content` | Kaynak/lisans, belge sürümü, sayfa/chunk, embedding ve çalışma isteği | Erişim filtreli retrieval; içerik ve Smart Study API. |
| `analytics` | Günlük/konu ölçümü, snapshot, trend, risk ve freshness | Öğrenci/kurum okuma modelleri; açıklanabilir öneri girdileri. |
| `recommendation` | Sürümlü görev seçme kuralları ve öneri gerekçesi | Task önerisi; gelecekte Coach için karar bağlamı. |
| `entitlement` | Kurum paketi, koltuk, özellik hakkı, kullanım/kota rezervasyonu | Atomik kota rezervasyonu, plan ve kullanım sorgusu. |
| `platform` | Outbox/iş yürütme, audit altyapısı, storage/mail/AI adaptörleri | Teknik portlar; domain kararı içermez. |

### 4.3. Kod seviyesinde sınırlar

- Her modül `api/application/domain/infrastructure` sorumluluklarını kendi içinde ayırır; küçük modüllerde gereksiz boş katman oluşturulmaz.
- Diğer modülün JPA entity’si veya repository’si import edilmez. API DTO, application port veya sürümlü olay kullanılır.
- `practice → analytics → recommendation → tasks` döngüsü karşılıklı servis çağrılarıyla kurulmaz; olaylar ve salt okunur portlarla ayrılır.
- Dashboard, ham tabloları controller içinde birleştirmez; analytics’in tenant kapsamlı okuma modellerini kullanır.
- Auth, validation ve yetkilendirme yalnız UI’da veya controller’da bırakılmaz; application use-case sınırında uygulanır.
- AI/HTTP çağrısı sırasında DB transaction’ı açık tutulmaz. Önce iş kaydı, sonra dış çağrı, sonra kısa sonuç transaction’ı yapılır.
- Tek deployment, sınırsız ortak kod anlamına gelmez. `shared` paketi yalnız kimlik tipleri, saat, hata ve olay zarfı gibi gerçekten ortak kavramları içerir.
- ArchUnit ile yasak bağımlılık ve döngü testleri zorunludur; Spring Modulith ancak sağladığı somut fayda doğrulanırsa eklenir.

### 4.4. Kaynak gerçekler ve türetilmiş veriler

**Gerçek kaynak:** Cevaplar, yayınlanmış deneme sonuçları, görev durumları, üyelikler ve içerik sürümleri.  
**Yeniden üretilebilir görünüm:** Topic metrics, weakness snapshot, trend, risk, kurum toplamları.  
**Yardımcı üretim:** AI açıklamaları ve öneri metinleri; sayısal gerçeklerin yerine geçmez.

Ölçüm bozulursa kaynak veriden yeniden kurulabilmelidir. AI açıklaması kaybolduğunda öğrencinin cevabı veya deneme sonucu kaybolmuş sayılmamalıdır.

---

<a id="teknolojiler"></a>
## 5. Teknoloji Seçimi ve Geliştirme Yapısı

### 5.1. Önerilen stack

| Alan | Başlangıç seçimi | Uygulama notu / alternatif sınırı |
| --- | --- | --- |
| Web | Angular + TypeScript; Angular Material/CDK | Tek SPA içinde öğrenci ve kurum route alanları; başlangıçta ayrı uygulamalar yok. |
| UI state | Angular signals + servisler; HTTP akışlarında RxJS | Karmaşık global state ihtiyacı ölçülmeden NgRx zorunlu değil. |
| Backend | Java 21 LTS + desteklenen kararlı Spring Boot hattı | JDK/Boot uyumu kickoff’ta doğrulanır; Spring MVC yeterli, tüm sistemi reactive yapma yok. |
| Build | Gradle Wrapper + dependency locking | Mevcut çalışma alanının şirket içi build ayarları yeni ürüne otomatik taşınmaz. |
| Veri erişimi | Spring Data JPA; analitik/pgvector için kontrollü native SQL/JdbcClient | Dinamik filtrelerde yalnız parametreli sorgular; repository tenant sözleşmeleri açık. |
| Migration | Flyway | Her şema değişimi sürümlü; production’da otomatik `ddl-auto=update` yok. |
| Veritabanı | Desteklenen PostgreSQL sürümü + uyumlu pgvector | Extension desteği ve yedek/restore uyumu seçilen sağlayıcıda test edilir. |
| Auth | Yönetilen OIDC; AWS seçilirse Cognito ilk aday | Keycloak yalnız lokal test veya operasyon yükü kabul edilirse; özel parola sistemi yazılmaz. |
| Web oturumu | Spring Security OAuth2 Login + Spring Session JDBC | BFF modeli; Redis gerektirmeden sunucu oturumu; hassas token erişimi sınırlandırılır. |
| API | REST, OpenAPI 3, springdoc uyumlu sürüm | Frontend istemcisi mümkünse sözleşmeden üretilir. |
| AI | Python’ın desteklenen sürümü + FastAPI, Pydantic, httpx | İlk provider tek; adapter arayüzü alternatiflere açık. |
| Belge işleme | pypdf + gerekli sayfalarda pdfplumber | OCR, taranmış içerik zorunlu olmadıkça ertelenir; kütüphane lisansı incelenir. |
| CSV/XLSX | Apache Commons CSV + Apache POI | Makro/formül çalıştırılmaz; streaming, satır/dosya/zip açılım sınırları uygulanır. |
| Storage | S3 uyumlu private object storage | Geliştirmede MinIO veya hafif test alternatifi; üretimde lisans ve operasyon değerlendirilir. |
| AI işleme sırası | PostgreSQL job tabloları | Celery/Redis/Kafka ikinci bir iş kuyruğu olarak ilk günden eklenmez. |
| Backend test | JUnit 5, Mockito, Testcontainers, WireMock, ArchUnit | PostgreSQL/RLS/pgvector davranışı H2 ile taklit edilmez. |
| Frontend/E2E test | Seçilen Angular sürümünün desteklenen runner’ı + Playwright | Kullanıcı akışları ve negatif yetki senaryoları birlikte test edilir. |
| Python test/kalite | pytest, Ruff, tip kontrolü | Provider sözleşmesi, JSON doğrulama, timeout ve belge fixture’ları. |
| Operasyon | Docker/Compose, Nginx, GitHub Actions, temel Terraform | Kubernetes, ALB ve CloudFront MVP için zorunlu değil. |
| İzleme | JSON log, Micrometer/Actuator, hata/uptime bildirimi | Actuator yönetim uçları internete açılmaz; OTel/Grafana genişlemesi ihtiyaç bazlı. |

**Sürüm politikası:** Angular/Node, Spring Boot/Java, Python ve PostgreSQL/pgvector uyum matrisi ilk kurulum issue’sunda sabitlenir. Güncel sürüm numaraları bu raporda doğrulanmış gibi sunulmaz. Lock dosyaları, destek/EOL tarihleri, CVE kontrolü ve aylık güncelleme PR’ları çalışma standardıdır.

### 5.2. Yeni depo düzeni

```text
luminary/
  frontend/       Angular Student + Institution
  backend/        Spring Boot, domain modülleri, migration ve testler
  ai-service/     FastAPI, provider adapter, ingestion ve evaluation
  infra/          Compose, Terraform, deployment betikleri
  contracts/      OpenAPI ve sürümlü olay/AI şemaları
  docs/           ADR, ürün kararları, runbook, test/pilot belgeleri
  .github/        CI/CD, issue/PR şablonları, CODEOWNERS
```

Yerel başlatma dokümanı Windows PowerShell ve Linux/macOS farklarını içerir. Production Linux container’dır. Repository’ye gerçek öğrenci verisi, AI anahtarı veya telif durumu belirsiz kitap eklenmez.

---

<a id="guvenlik-modeli"></a>
## 6. Kimlik, Yetkilendirme ve Çok Kiracılı Veri Modeli

### 6.1. Kimlik ile çalışma alanını ayırma

| Kavram | Anlamı |
| --- | --- |
| `User` | Global kullanıcı kimliği; OIDC `issuer + subject` ile eşlenir. E-posta tek başına kimlik anahtarı değildir. |
| `Tenant` | `INSTITUTION` veya `PERSONAL` çalışma alanı. Kurumun veya bireysel öğrencinin veri sınırı. |
| `Membership` | Kullanıcı + tenant + rol + durum; yetkilendirmenin güvenilir kaynağı. |
| `StudentProfile` | Tenant içindeki öğrenci kaydı; davet kabul edilene kadar `user_id` boş olabilir. |
| `ActiveTenant` | Sunucu oturumunda seçili, her istekte üyeliği doğrulanan çalışma alanı. |

**MVP önerisi:** Bir öğrencinin en fazla bir aktif kurum üyeliği olabilir; ayrıca kişisel çalışma alanı bulunabilir. Veri modeli çoklu üyeliğe açık kalsa da birden fazla kurum arasında eşzamanlı veri paylaşımı MVP’de sunulmaz. Kişisel alan otomatik doldurulmaz; B2C giriş açıldığında oluşturulur.

- Kişisel tenant’ların her biri ayrıdır. Bütün B2C öğrencileri ortak `default_tenant` altında tutulmaz.
- Kurum çalışma alanında yapılan çalışma ve girilen deneme aynı öğrenci profilinde birleşir; kurum paneli burayı görür.
- Kişisel alanda yapılan çalışma kurum yöneticisine otomatik açılmaz.
- Kuruma katılma, geçmiş kişisel verinin tümünü paylaşma anlamına gelmez. MVP’de otomatik veri taşıma/birleştirme yapılmaz.
- Kurumdan ayrılma eski kayıtların `tenant_id` değerini değiştirmez. Erişim, saklama ve öğrencinin veri talebi politika/sözleşme ile yönetilir.
- Aktif kurum üyeliğinin bitmesi, global kimliğin silinmesi değildir. Yeni kişisel kayıt eski kurum arşivine kendiliğinden erişemez.

### 6.2. Web giriş akışı — önerilen BFF

1. Angular kullanıcıyı backend’in login başlangıç adresine yönlendirir.
2. Backend OIDC Authorization Code akışını başlatır; `state`, `nonce` ve desteklenen PKCE doğrulanır.
3. Callback sonrasında backend issuer, imza, süre ve ilgili audience kontrollerini sağlayıcı kütüphanesi üzerinden yapar.
4. OIDC subject yerel kullanıcıya eşlenir; devre dışı kullanıcı ve üyelik kontrol edilir.
5. Sunucuda oturum açılır; tarayıcıya yalnız `HttpOnly`, `Secure`, uygun `SameSite` politikası olan oturum cookie’si gönderilir.
6. Aktif tenant sunucuda seçilir. Mutasyonlarda CSRF token; tenant değişiminde yeniden üyelik kontrolü kullanılır.
7. Logout yerel oturumu ve mümkün olduğunda provider oturumunu sonlandırır; oturum timeout/yenileme politikası uygulanır.

OIDC token’ları `localStorage` içinde tutulmaz. Public bearer API ileride gerekirse Authorization Code + PKCE ve access-token audience doğrulaması ayrı sözleşme olarak eklenir; ID token API erişim anahtarı değildir. BFF ve SPA token yaklaşımı aynı sürümde gereksiz yere birlikte işletilmez.

### 6.3. Rol ve kaynak yetki matrisi

| Kaynak/işlem | STUDENT | INSTITUTION_ADMIN | Operasyon erişimi |
| --- | --- | --- | --- |
| Öğrenci profili | Kendi tenant profilini görür; izinli alanlarını değiştirir | Aynı tenant’taki öğrencileri yönetir | Varsayılan erişim yok. |
| Practice/cevap/görev | Yalnız kendi kaydı | Eğitimsel sonuç/özet, yetkili kapsamda; öğrenci adına cevap yazamaz | Varsayılan erişim yok. |
| Deneme sonucu | Kendi yayınlanmış sonucu | Tenant denemesi/import/yayınlama | Gerekçeli, süreli, audit’li destek prosedürü. |
| Kurum dashboard’u | Erişemez | Yalnız kendi tenant’ı | Genel öğrenci verisine sınırsız admin paneli yok. |
| Smart Study girdisi | Kendi isteği/yanıtı | Ham kişisel promptlar yerine izinli kullanım özeti | İçerik sorunu için en az veriyle inceleme. |
| Müdahale notu | Varsayılan olarak iç personel notunu görmez | Tenant kapsamında; minimum veri | Politika kapsamlı, audit’li erişim. |
| Üyelik/koltuk | Kendi durumunu görür | Tenant koltuk/davet yönetimi | Plan değişikliği için kısıtlı yönetim prosedürü. |

MVP’de ayrı öğretmen veya rehberlik rolü yoktur. Pilot kurumda bu görevi yapan yetkili kişi `INSTITUTION_ADMIN` kullanabilir; bu rolün geniş veri erişimi kurum tarafından bilinerek onaylanır.

### 6.4. DB ve uygulama izolasyonu

- Tenant, request body’de gelen değere güvenilerek atanmaz. Session + aktif üyelikten türetilir; tenant seçme isteği yalnız bir aday seçimidir.
- Her tenant tablosunda `tenant_id NOT NULL`; liste, detay, arama, export ve job sorgularında tenant filtresi bulunur.
- İlişkilerde `(tenant_id, id)` unique anahtar ve bileşik foreign key kullanılır. Bir tenant’ın cevabı başka tenant’ın sorusuna bağlanamaz.
- Öğrenci rolünde ayrıca `student_id` sahipliği doğrulanır. Aynı kurumda olmak diğer öğrencinin sorularını/görevlerini görme yetkisi vermez.
- Savunma katmanı olarak PostgreSQL RLS önerilir: runtime rolü tablo sahibi/superuser değildir, `BYPASSRLS` yoktur; gerekli tablolarda `FORCE ROW LEVEL SECURITY` değerlendirilir.
- Tenant context transaction başlangıcında **transaction-local** ayarlanır; connection pool’a kalıcı session ayarı sızdırılmaz. Context yoksa sorgu kapalı davranır.
- RLS tenant izolasyonunu güçlendirir; öğrenci sahipliği ve rol kurallarının yerine geçmez.
- Worker iş zarfındaki tenant’ı güvenilir DB kaydından alır; scoped işlem açar. Outbox keşfi için erişilen operasyon metadatası ile öğrenci payload erişimi ayrı yetkilendirilir.
- Başlangıçta tüm tenant’ları gezmesi gereken job dispatcher yalnız iş kimliği/durum gibi minimum metadata görür; iş verisi tenant context altında okunur. Migration rolü uygulama rolünden ayrıdır.
- Cache, object key, signed URL, arama ve export aynı sınırları korur. Tahmin edilmesi zor UUID tek başına yetkilendirme değildir.

---

<a id="veri-modeli"></a>
## 7. Veri Modeli ve Veri Yaşam Döngüsü

### 7.1. Veri sınıfları

1. **Global, yönetilen katalog:** Sınav türü, ders, konu, kamuya açık gösterimine hakkı bulunan içerik metadata’sı. Yazma yalnız kontrollü operasyon akışıyla yapılır.
2. **Kimlik/güvenlik verisi:** Kullanıcı, oturum ve üyelik. Genel analitik verisi değildir; ayrı erişim kuralları vardır.
3. **Tenant iş verisi:** Öğrenci, üretilen soru/cevap, görev, deneme, risk, müdahale ve kurum raporu.
4. **Operasyon verisi:** İş, outbox, audit, kullanım/kota. Payload’da kişisel veri minimum tutulur; tenant bağlamı kaybolmaz.

Her global tablo öğrenci verisinden bilinçli bir istisnadır. Soru üretimi tenant’a özeldir; anonimleştirme ve içerik incelemesi olmadan ortak soru bankasına taşınmaz.

### 7.2. Çekirdek tablolar

| Grup / tablo | Önemli alanlar | Kural |
| --- | --- | --- |
| `tenants`, `institutions` | tür, ad, durum, saat dilimi | Kurum tenant’ının tek kurum kökü; kişisel tenant’ın kurum kaydı yok. |
| `users` | id, auth_issuer, auth_subject, durum | `(auth_issuer, auth_subject)` unique; gereksiz kimlik bilgisi yok. |
| `memberships` | tenant, user, role, status, joined_at, revoked_at | Üyelik aktifliği her korunan akışta uygulanır; rol global kullanıcı alanı değildir. |
| `student_profiles` | tenant, id, nullable user, external_student_ref, grade, target_rank | Kurum öğrenci numarası tenant içinde unique; davetsiz kayıt ile aktif hesap ayrı. |
| `groups`, `group_memberships` | tenant, grup, öğrenci, geçerlilik dönemi | Geçmiş raporda sınıf değişiminin etkisi politika ile tanımlanır. |
| `invitations` | tenant, öğrenci/rol, token_hash, expires_at, used_at | Tek kullanımlı, süreli; ham davet anahtarı DB/log’da tutulmaz. |
| `exam_types`, `subjects`, `topics` | kod, ebeveyn, curriculum_version, active | Silmek yerine kullanımdan kaldırma; geçmiş soru/sonuç sürümünü korur. |
| `practice_sessions` | tenant, student, filtreler, status, generation_job_id, version | Oturum kapsamı ve istenen soru sayısı immutable istek snapshot’ı. |
| `practice_questions` | tenant, session, topic, difficulty, options, answer_key, explanation, provenance | İlk gösterimden sonra içerik snapshot’ı korunur; cevap anahtarı yalnız sunucu tarafı DTO’da. |
| `question_attempts` | tenant, student, session_question, attempt_no, selected_option, correct, answered_at, duration | Aynı ilk cevap için unique constraint; cevap sonradan sessizce değişmez. |
| `question_reports` | tenant, question, reason, review_status, resolution | Hatalı soru sonuçtan çıkarılabilir; eski denetim izi korunur. |
| `tasks` | tenant, student, source, topic, target_count, due_at, status, rule_version, evidence | Öneri anahtarı tekrar görev üretimini engeller; manuel/ölçülen tamamlama ayrıdır. |
| `exams`, `exam_subjects` | tenant, type, date, form/version, question_count, scoring_policy | TYT/AYT ve farklı puanlama politikaları açık ayrılır. |
| `exam_imports`, `exam_import_rows` | tenant, file_hash, status, mapping, validation_errors, revision | Taslak satırlar yayınlanmış sonuç görünümüne girmez. |
| `exam_results` | tenant, exam, student, subject, correct/wrong/blank veya reported_net, revision | Aktif revizyonda `(tenant, exam, student, subject)` unique. |
| `exam_question_mappings`, `exam_item_results` | tenant, exam form, soru, topic, student, outcome | Konu analizi yalnız bu ayrıntı sağlanırsa; ders toplamından üretilmez. |
| `content_sources`, `content_versions` | hak/lisans kaydı, sürüm, storage_key, checksum, status, erişim sınıfı | Kaynak değişince yeni sürüm; hakkı geri çekilen içerik retrieval dışı. |
| `content_chunks`, `content_embeddings` | content_version, page_index, printed_page, offsets, topic, model/version, vector | Global lisanslı ve tenant’a özel içerik erişim sınıfı açık; embedding modeli karıştırılmaz. |
| `study_requests` | tenant, student, içerik/sayfa, durum, citation, model/prompt version | Ham serbest metin için kısa ve gerekçeli saklama; öğrenci görünürlüğü scoped. |
| `student_daily_metrics` | tenant, student, local_date, attempted, correct, active_duration | Tenant saat diliminden üretilmiş gün anahtarı; süre ölçümü tanımı saklı. |
| `student_topic_metrics`, `weakness_snapshots` | tenant, student, topic, n, mastery, evidence_status, rule_version, source_version | Örneklem ve güncellik skordan ayrı; ham veriden yeniden üretilebilir. |
| `student_exam_trends`, `risk_assessments` | tenant, student, comparable_scope, reasons, assessed_at, evidence | Kesin gelecek tahmini değil; sürümlü kuralların çıktısı. |
| `interventions` | tenant, student, assessment, assignee, status, note, follow_up_at | Kim-ne zaman-ne yaptı audit edilir; serbest not veri minimizasyonuna tabi. |
| `subscriptions`, `seat_allocations`, `usage_ledger` | tenant, dönem, plan, haklar, rezervasyon, gerçek tüketim | Aynı kullanım iki kez düşmez; ücret ve kullanım kaydı ayrıdır. |
| `jobs`, `outbox_events`, `processed_events` | tenant, type, status, attempts, lease, event_id, schema_version | Kalıcı işleme, deduplication ve tekrar deneme omurgası. |
| `audit_logs`, `privacy_requests` | actor, tenant, action, resource, zaman, talep durumu | Hassas payload yerine referans/değişiklik özeti; ayrı saklama/erişim. |

### 7.3. İndeks, bütünlük ve sorgu kuralları

- Liste/analitik indeksleri gerçek sorgulara göre tasarlanır: örneğin `(tenant_id, student_id, answered_at)` ve `(tenant_id, status, due_at)`.
- Pagination’da stabil sıralama ve benzersiz ikinci anahtar kullanılır; büyük cevap/olay geçmişinde cursor pagination tercih edilir.
- Netler `float` değil uygun `numeric` tipidir. Şema constraint’leri negatif adetleri ve geçersiz seçenek/enum değerlerini reddeder.
- Bütün zamanlar UTC `timestamptz`; UI ve günlük aggregate hesapları tenant saat dilimiyle yapılır. Başlangıç için `Europe/Istanbul` önerilir.
- Optimistic locking görev, import ve yönetim değişikliklerinde kayıp güncellemeyi önler.
- İdempotency anahtarı `(tenant, user, operation, key)` kapsamında unique; istek özetiyle bağlanır.
- Dosya hash’i aynı içeriği belirlemeye yardımcı olur; tenant yetkilendirmesinin veya import revizyon kararının yerine geçmez.
- JSONB; provenance, kural gerekçesi ve değişken AI metadata için uygundur. Temel ilişki ve sorgulanan kimlikler belirsiz JSON alanlarına gömülmez.
- `EXPLAIN ANALYZE` ve gerçekçi pilot fixture’larıyla indeksler doğrulanır; başlangıçta partitioning zorunlu değil.

### 7.4. Düzeltme, arşiv ve silme

- Öğrenci cevabı ilk gönderimde immutable; hatalı soru invalidation kaydıyla analitikten çıkarılır.
- Deneme düzeltmesi yeni revizyon, değişiklik özeti ve yeniden hesaplama olayı üretir; geçmiş sonuç sessizce overwrite edilmez.
- Müfredat ve içerik sürümleri geçmiş anlamı korur; yeni embedding eski sonuçların kaynağını değiştirmez.
- Soft delete tek başına KVKK silme süreci değildir. Kimlik, operasyon verisi, objeler, türetilmiş ölçümler ve provider saklaması birlikte değerlendirilir.
- Kaynak veri silinince yeniden hesaplama eski kişisel veriyi geri getirmemelidir. Silme işareti/revizyon kontrolü ve restore sonrası silme taleplerini yeniden uygulama prosedürü gerekir.

---

<a id="api"></a>
## 8. API ve İstemci Sözleşmeleri

### 8.1. Ortak ilkeler

- REST tabanı `/api/v1`; OpenAPI sürümlü contract kaynağıdır. İç AI API’ları `/internal/v1` altında private ağdadır.
- Okuma DTO’ları entity’lerden ayrıdır. Practice sorusu DTO’su doğru seçenek, gizli açıklama veya puanlama iç bilgisini içermez.
- Hatalar RFC 9457 Problem Details biçimindedir: `type`, `title`, `status`, `detail`, `instance`, güvenli `code`, `traceId`, gerekirse alan hataları.
- `401` oturum yok; `403` işlem yetkisi yok; varlığını açığa çıkarmamak gereken kapsam dışı kaynakta `404`; `409` sürüm/idempotency/durum çatışması; `422` anlamsal doğrulama; `429` hız/kota sınırı; `503` geçici bağımlılık sorunu.
- Mutasyonlar CSRF korumalıdır. Liste filtreleri, sayfa boyutu, metin uzunluğu, soru sayısı ve tarih aralığı allowlist/sınırla doğrulanır.
- Uzun işlem `202 Accepted`, iş kimliği ve durum URL’si döner; sonuç hazır olana kadar sayfa yenilense de iş kaybolmaz.
- İstemci yalnız güvenli/geçici hatayı jitter’lı backoff ile tekrarlar; idempotency’siz mutasyonu otomatik tekrarlamaz.
- İstek kimliği log, outbox, AI çağrısı ve response üzerinde ilişkilendirilir; kişisel bilgiyi correlation ID içine koyma yok.

### 8.2. Temel endpoint kataloğu

| Alan | Önerilen sözleşmeler | Yetki / davranış |
| --- | --- | --- |
| Oturum | `GET /me`, `GET /me/workspaces`, `POST /me/active-workspace` | Aktif üyelikten görünüm; tenant değiştirme sunucuda doğrulanır. |
| Öğrenci | `GET/PATCH /students/me`, `GET /curriculum/subjects`, `GET /curriculum/topics` | Profil sahipliği; yalnız desteklenen/sürümlü katalog. |
| Kurum | `GET/PATCH /institution`, `GET/POST /institution/students`, `PATCH /institution/students/{id}` | Kurum yöneticisi; lifecycle ve koltuk kontrolü. |
| Davet/grup | `POST /institution/students/{id}/invitations`, `POST /invitations/accept`, `GET/POST /institution/groups` | Davet token’ı tek kullanımlı; kabul login bağlamında. |
| Practice | `POST /practice/sessions`, `GET /practice/sessions/{id}`, `GET /practice/sessions` | Üretim 202; liste kendi geçmişi; kota ve sahiplik. |
| Cevap/sonuç | `POST /practice/sessions/{id}/questions/{questionId}/attempts`, `POST /practice/sessions/{id}/complete`, `GET /practice/sessions/{id}/result` | İdempotent ilk cevap; sonuç yalnız tanımlanan görünürlük anında. |
| Soru itirazı | `POST /practice/questions/{id}/reports` | Öğrencinin eriştiği soruyla sınırlı; denetimli inceleme. |
| Görev | `GET/POST /students/me/tasks`, `PATCH /students/me/tasks/{id}`, `POST /students/me/tasks/{id}/complete` | Kaynak/durum geçişi doğrulanır; otomatik progress kullanıcı tarafından sahteleştirilemez. |
| İçerik | `GET /content/books`, `GET /content/books/{id}/pages`, `POST /study/explanations` | Kaynak erişimi ve kota; açıklama asenkron olabilir. |
| Performans | `GET /students/me/metrics`, `/weakness`, `/exam-results` | Aynı prefix; evidence/freshness metadata zorunlu. |
| Deneme | `GET/POST /institution/exams`, `POST /institution/exams/{id}/imports`, `GET /institution/imports/{id}` | Dosya veya elle giriş aynı doğrulama katmanını kullanır. |
| Import yayınlama | `POST /institution/imports/{id}/publish`, `POST /institution/exams/{id}/corrections` | Onaylı taslak; idempotent yayınlama; revizyon/audit. |
| Kurum analiz | `GET /institution/dashboard`, `/institution/analytics/subjects`, `/institution/students/{id}/performance` | Tenant + filtre kapsamlı aggregate; freshness bilgisi. |
| Müdahale | `GET /institution/attention`, `POST /institution/interventions`, `PATCH /institution/interventions/{id}` | Gerekçe, sorumlu, durum ve takip tarihi. |
| Kullanım | `GET /institution/subscription`, `GET /institution/usage`, `GET /students/me/usage` | Yönetici kurum toplamını; öğrenci kendi hakkını görür. |
| İş durumu | `GET /jobs/{id}` | İş kimliğini bilen herkes değil, tenant ve iş sahibi/yetkili admin. |

### 8.3. UI davranışı ve erişilebilirlik

- Öğrenci navigasyonu: **Home, Practice Lab, My Tasks, Smart Study, Performance**.
- Kurum navigasyonu: **Dashboard, Students, Exams, Analytics, Attention**. Plan/hesap ayarları ikincil alandır.
- Her ekran için loading, empty, insufficient-data, partial/stale, forbidden ve error durumları tasarlanır.
- Tenant adı çalışma alanı değişiminde görünürdür; arka plandaki eski tenant istekleri iptal edilir ve istemci cache’i temizlenir.
- Hatalı işlemde form verisi korunur; çift submit engellenir; uzun üretimde sahte ilerleme yüzdesi yerine gerçek durum gösterilir.
- Klavye navigasyonu, odak yönetimi, renk dışı durum göstergesi ve ekran okuyucu etiketleri temel kabul kriteridir.
- Matematik metni için güvenli renderer kullanılır; HTML/Markdown sanitize edilir; AI çıktısı güvenilir HTML sayılmaz.

---

<a id="olaylar"></a>
## 9. Asenkron İşler, Olaylar ve Tutarlılık

### 9.1. Senkron ve asenkron sınırı

| İş | İşleme biçimi | Kullanıcıya görünür tutarlılık |
| --- | --- | --- |
| Profil/görev düzenleme | Kısa DB transaction’ı | Commit sonrası doğrudan görünür. |
| Cevap gönderme | Cevap + sonuç + outbox aynı transaction | Cevap sonucu anında; konu/dashboard ölçümü gecikmeli. |
| AI soru/açıklama | Kalıcı job → dış çağrı → doğrulama → sonuç commit | `QUEUED/RUNNING/READY/FAILED`; polling. |
| PDF ingestion | Aşamalı, checkpoint’li job | Belge yalnız `PUBLISHED` olduğunda öğrenciye açılır. |
| Deneme import | Dosya/staging asenkron; onaylı taslağı yayınlama atomik | Taslak görünmez; yayınlanmış revizyon bir bütün olarak görünür. |
| Ölçüm/öneri/risk | Outbox tüketimi + zamanlanmış reconciliation | `computedAt`, `dataThrough`, `pending` ile gecikme açıklanır. |
| Bildirim | İş kaydı üzerinden gönderim | E-posta hatası öğrencinin kaydını geri almaz. |

### 9.2. Olay sözleşmesi

Her olay zarfı en az `eventId`, `eventType`, `schemaVersion`, `tenantId`, `aggregateId`, `aggregateVersion`, `occurredAt`, `correlationId` taşır. Payload yalnız tüketicinin ihtiyacı kadar kimlik ve olgu içerir; ad/e-posta ve serbest prompt taşınmaz.

| Olay | Üreten | Tüketen / sonuç |
| --- | --- | --- |
| `AttemptRecorded.v1` | Practice | Analytics güncellemesi; uygun task ilerlemesi. |
| `PracticeSessionCompleted.v1` | Practice | Aktivasyon/engagement ve oturum özeti. |
| `QuestionInvalidated.v1` | Practice | İlgili ölçümün yeniden kurulması; gerekiyorsa açıklamalı sonuç düzeltmesi. |
| `ExamResultsPublished.v1` | Exams | Ders trendi; veri uygunsa konu ölçümü; risk. |
| `ExamResultsCorrected.v1` | Exams | Etkilenen öğrencilerin aktif revizyon üzerinden yeniden hesabı. |
| `StudentMetricsUpdated.v1` | Analytics | Sürümlü öneri/risk hesabı; aynı anlamda değişim yoksa yeni görev yok. |
| `TaskCompleted.v1` | Tasks | Görev tamamlama metriği; tamamlanma türü korunur. |
| `MembershipRevoked.v1` | Access | Erişim/iş iptali değerlendirmesi; mevcut kurum listesinin güncellenmesi. |

### 9.3. Transactional outbox protokolü

1. İş kaydı ile outbox olayı aynı DB transaction’ında yazılır; birisi olmazsa ikisi de olmaz.
2. Dispatcher uygun işleri kısa transaction ile `FOR UPDATE SKIP LOCKED` benzeri kilitleme kullanarak alır; `locked_until`, worker kimliği ve lease token atanır.
3. Transaction kapatılır. Ağ/AI işlemi açık DB kilidi altında yapılmaz.
4. Tüketici tenant scope açar ve `(consumer_name, event_id)` kaydını kontrol eder.
5. Projection değişimi, yeni gerekiyorsa outbox olayı ve `processed_events` kaydı **aynı transaction** içinde commit edilir.
6. Başarı ACK’i, hâlâ geçerli lease token ile yapılır. Geç worker yeni worker’ın sonucunu overwrite edemez.
7. Worker ölürse lease süresi sonrasında iş yeniden alınır. Teslimat **at-least-once**; iş etkisi idempotent olacak şekilde tasarlanır.

**Exactly-once iddiası yoktur.** Ağ kesintisinde provider işlemiş olsa bile yanıt alınamamış olabilir. Sağlayıcı idempotency destekliyorsa kullanılır; desteklemiyorsa mükerrer AI maliyeti izlenir ve retry bütçesi sınırlanır. Uygulamada aynı soru setinin/kotanın ikinci kez yazılması engellenir.

### 9.4. Hata, retry ve yeniden oynatma

- Timeout, `429` ve geçici `5xx` için sınırlı exponential backoff + jitter; kalıcı doğrulama/izin hatası için otomatik sonsuz retry yok.
- Başlangıç önerisi: en çok 3 otomatik dış çağrı denemesi; job türüne göre toplam süre ve token bütçesi ayrı belirlenir.
- Deneme limiti aşılırsa `DEAD` veya terminal `FAILED`; güvenli hata kodu ve operasyon uyarısı. Operatör gerekçeli retry/cancel yapabilir.
- `aggregateVersion` eski/geç gelen olayın yeni durumu bozmasını önler. Analytics kritik durumlarda kaynak veriden yeniden hesaplar.
- Reconciliation görevi kaynak kayıt sayısı/sürümü ile projection durumunu karşılaştırır; sessiz olay kaybını veya yarım işleme hatasını bulur.
- Yeni scoring sürümünde yeni projection sürümü oluşturulur; tamamlandıktan sonra okuma görünümü geçirilir. Eski/yeni skorlar karışık sunulmaz.
- Kullanıcı silinmiş veya erişimi iptal edilmişse geciken job yeniden yetki/durum kontrolü yapar; silinen veriyi geri oluşturmaz.
- Outbox saklama süresi işlenme/replay ihtiyacına göre tanımlanır; işlenmemiş kayıt otomatik temizlikle silinmez.

### 9.5. Job durum modeli

`QUEUED → RUNNING → SUCCEEDED` temel hattıdır; ara sonuç gerektiren üretimde kullanıcıya `READY` görünümü verilebilir. Hata hattı `RUNNING → RETRY_WAIT → RUNNING` ve sonunda `FAILED/DEAD`; kullanıcı iptali `CANCEL_REQUESTED → CANCELLED` biçimindedir.

İptal, başlamış provider çağrısının maliyetini her zaman geri alamaz. Sistem biten sonucu yayınlamamayı ve kota politikasını uygulamayı garanti eder; dış sağlayıcı tüketimi ayrı kaydedilir.

---

<a id="analitik"></a>
## 10. Ölçüm, Weakness, Öneri ve Risk Kuralları

### 10.1. Önce ölçüm sözlüğü

| Ölçüm | Tanım | Yanlış yorumlamayı önleme |
| --- | --- | --- |
| Çözülen soru | Geçerli soruya kaydedilmiş ilk cevap | Aynı HTTP isteği ve tekrar cevap sayacı artırmaz. |
| Accuracy | Geçerli ilk cevaplardaki doğru / cevaplanan | Atlanan sorular ayrı sayılır; gizlice paydaya eklenmez/çıkarılmaz. |
| Oturum tamamlama | Öğrencinin açıkça bitirdiği practice; cevaplanan/atlanan ayrı | Tüm soruları atlayan oturum başarı veya anlamlı çalışma sayılmaz. |
| Çalışma süresi | İzinli aralıktaki aktif etkileşim/heartbeat süresi | Açık sekme süresi çalışma sayılmaz; cihaz dışı çalışma bilinemez. |
| Aktif öğrenci | Dönemde en az bir anlamlı çalışma/cevap/görev aksiyonu | Login veya açık dashboard tek başına öğrenci aktivitesi değildir. |
| Görev tamamlama | Dönemde tamamlanan / dönemde vadesi gelen uygun görev | Manuel ve otomatik doğrulanmış tamamlama ayrı raporlanır. |
| Deneme neti | Sürümlü sınav politikasına göre doğru − yanlış / ceza böleni | Net-only importta doğru/yanlış ve konu performansı bilinmez. |
| Risk | Gözlenen akademik/ürün kullanım sinyallerinin kural özeti | Öğrencinin kişiliği, psikolojisi veya geleceği hakkında tanı değil. |

### 10.2. Weakness v1 — uygulanabilir başlangıç kuralı

**Önerilen hesap:** Son 30 gündeki geçerli, ilk ve tekrar içerik etkisi sınırlandırılmış cevaplar kullanılır. Her cevabın ağırlığı `w = 2^(-ageDays / 14)`; konu göstergesi `mastery = (1 + Σ(w × correct)) / (2 + Σw)` olur. Bu yumuşatılmış çalışma göstergesidir; bilimsel olarak doğrulanmış öğrenme olasılığı iddiası değildir.

**Gösterim için önerilen yeterlilik koşulları:** En az 10 benzersiz geçerli soru, en az 2 farklı oturum, toplam ağırlık en az 5 ve son 14 gün içinde en az bir cevap. Bu şartlar pilot verisiyle kalibre edilir.

| Durum | Önerilen UI anlamı |
| --- | --- |
| Yeterlilik yok | `INSUFFICIENT_DATA` — “Bu konu için daha fazla çalışma verisi gerekiyor.” |
| Veri var ama güncellik yok | `STALE` — “Son ölçüm eski; güncel durum için kısa pratik önerilir.” |
| Yeterli veri ve mastery < 0,45 | `WEAK` — “Öncelikli çalışma alanı.” |
| 0,45 ≤ mastery < 0,75 | `DEVELOPING` — “Gelişmekte.” |
| mastery ≥ 0,75 | `STRONG` — “Mevcut çalışmalarda güçlü.” |

- Skorla birlikte `sampleSize`, cevap aralığı, `lastPracticedAt`, `evidenceStatus` ve `ruleVersion` gösterilir.
- Güncellik azalması tek başına “öğrenmesini kaybetti” şeklinde yorumlanmaz; düşük güncellik ile düşük başarı ayrıdır.
- AI’ın atadığı zorluk etiketi kalibre edilmiş psikometrik güçlük değildir. V1’de zorluk bazlı alt görünümler ayrı tutulur; doğrulanmadan ağırlık eklenmez.
- Aynı sorunun tekrar çözülmesi görev alışkanlığına katkı verebilir; yeterlilik kanıtını sınırsız büyütmez. İçerik fingerprint’i/soy bilgisi saklanır.
- Hatalı/iptal edilmiş soru pay ve paydadan çıkarılır; projection güncellenir.
- Practice konu göstergesi ile deneme ders neti tek bir opak puana çevrilmez. Ayrı kanıtlar üzerinden açıklanır.

### 10.3. Deneme trendi

- Aynı sınav türü, ders ve uyumlu soru/puanlama kapsamındaki en az 3 yayınlanmış sonuçla trend gösterilir; 1–2 sonuçta yalnız geçmiş/değişim sunulur.
- Sıralama `exam_date` üzerinden; geç yükleme zamanı üzerinden değil. Aynı sınavın revizyonu yeni deneme sayılmaz.
- Sınav zorluğu eşitlenmemişse trend “farklı deneme güçlüklerinden etkilenebilir” uyarısıyla sunulur.
- Ders neti düşüşü, belirli bir konuda hata artışı demek değildir. Konu yorumu yalnız soru-konu mapping ve öğrenci soru sonucu varsa yapılır.
- Sıralama varsa kapsamı belirtilir: kurum içi katılımcılar, belirli deneme veya başka kaynak. Türkiye geneli sıralama üretilmez.
- TYT için başlangıç net politikası tipik olarak `doğru − yanlış/4` olabilir; resmi/güncel kural doğrulanarak sürümlü konfigürasyona alınır, tüm sınavlara hard-code edilmez.

### 10.4. Görev önerisi v1

1. Yeterli/güncel kanıtı bulunan zayıf konular alınır; veri yoksa teşhis değil kısa başlangıç pratiği önerilir.
2. Konunun son çalışma zamanı, aktif görevleri, günlük öneri sınırı ve içerik bulunabilirliği kontrol edilir.
3. Aynı konu+kural için açık görev varsa yeni görev açılmaz; tamamlanan görev için önerilen 7 günlük cooldown uygulanır.
4. Başlangıçta en fazla 2 otomatik aktif görev; örneğin 10–15 soruluk, öğrencinin zaman bütçesine sığan çalışma.
5. `ruleVersion`, ilgili metric snapshot’ı ve insan tarafından okunabilir deterministik gerekçe kaydedilir.
6. Öğrenci görevi kabul edebilir/erteleyebilir; erteleme başarısızlık olarak damgalanmaz.
7. Tamamlama sonrası yeni kanıt gelmeden aynı öneri tekrar oluşturulmaz.

MVP’de “Neden önerildi?” sorusu şablon + gerçek ölçümlerle yanıtlanır. Genel sohbet tabanlı AI Coach P2’dir; kaynakların MVP dışı Coach kararı korunur.

### 10.5. Risk v1 ve inceleme listesi

Önerilen puan aralığı 0–100’dür. Her sinyal `TRUE/FALSE/UNKNOWN` ve dayanak verisi taşır.

| Sinyal | Başlangıç önerisi | Puan |
| --- | --- | --- |
| Platform içi inaktivite | Aktive olmuş öğrencide 7–13 gündür anlamlı aktivite yok | 15 |
| Uzun inaktivite | 14+ gün yok; önceki inaktivite puanına eklenmez | 30 |
| Deneme düşüşü | En az 3 karşılaştırılabilir sonuçta iki ardışık düşüş ve ders soru sayısının ≥%10’u kadar toplam net düşüşü | 25 |
| Kalıcı konu zorluğu | Yeterli verili iki ayrı ölçümde aynı konu zayıf; ölçümler arası ≥7 gün | 25 |
| Çalışma açığı | Bilinen zayıf konuda son 7 günde önerilen çalışmanın yapılmaması; atama ve kullanım gözlemi mevcut | 20 |

`0–29`: düşük gözlenen risk; `30–59`: inceleme önerisi; `60–100`: öncelikli inceleme. **Bu eşikler pedagojik olarak doğrulanmış değildir.** Aynı kanıtın birden fazla sinyale etkisi pilotta değerlendirilir; ağırlık ve false-positive oranı sürümlenir.

- Yeni/hiç aktive olmamış öğrenci “sağlıklı” sayılmaz; onboarding veya `INSUFFICIENT_DATA` listesine girer.
- Eksik deneme/veri sinyali sıfır riskmiş gibi gizlenmez. `coverage=PARTIAL` gösterilir; eksik boyutlarla “HEALTHY” etiketi verilmez.
- Outbox gecikmesi veya genel servis kesintisi inaktivite uyarısı üretmemelidir. Veri tazeliği kapısı bulunur.
- Platformda görünmeyen çalışma, öğrencinin hiç çalışmadığını kanıtlamaz; UI “Luminary’de son aktivite” der.
- Her uyarıda gerekçe, zaman penceresi, veri kapsamı ve yapılabilecek destek aksiyonu yer alır.
- Aynı öğrenci+kural için açık uyarı güncellenir; bildirim yağmuru yaratılmaz. Erteleme ve follow-up takip edilir.
- Kurum insan değerlendirmesi yapar; puan tek başına not, sınıf değişikliği veya disiplin kararına dönüşmez.

---

<a id="akislar"></a>
## 11. Özelliklerin Ayrıntılı Uçtan Uca Akışları

### 11.1. Kurum kurulumu, öğrenci daveti ve hesap aktivasyonu

**Aktör:** Operasyon sorumlusu → kurum yöneticisi → öğrenci.  
**Ön koşul:** Pilot/sözleşme, gerekli aydınlatma süreçleri, tenant ve plan tanımı tamam.

1. Operasyon, kontrollü ve audit’li prosedürle kurum tenant’ını ve ilk admin davetini oluşturur.
2. Admin OIDC üzerinden kendi hesabıyla daveti kabul eder; ortak yönetici parolası paylaşılmaz.
3. Admin sınıf/grup açar ve tekil veya toplu öğrenci kaydı girer. Öğrenci numarası kurum içinde eşsizdir; T.C. kimlik numarası gerekmez.
4. Koltuk servisi atomik rezervasyon yapar; limit doluysa öğrenci daveti net hata verir veya taslakta kalır.
5. Süreli, tek kullanımlı davet bağlantısı gönderilir. Mail gönderilemezse yeniden gönderme mümkündür; öğrenci kaydı kaybolmaz.
6. Öğrenci giriş yapar, uygun bilgilendirme/onay sürecini tamamlar ve daveti kabul eder.
7. Davetin hedefi, tenant, kullanılma/süre durumu ve mevcut hesap bağlantısı kontrol edilir. Farklı hesabın kaydı devralması önlenir.
8. Üyelik aktive olur; öğrenci profili kullanıcıya bağlanır; davet koltuğu aktif koltuğa döner; audit oluşur.
9. Öğrenci sınıf düzeyi ve isteğe bağlı temel hedefini tamamlar; ilk çalışma yönlendirmesi gösterilir.

**Hatalar/kenarlar:** Süresi geçmiş davet, çift kabul, duplicate öğrenci no, yanlış hesap, koltuk doluluğu, pasif kurum, çoklu kurum kısıtı, üyelik iptali. Süresi dolan davet rezervasyonu bırakır; aktif koltuğun boşaltılması geçmiş kaydı silmez.

**Kabul:** Başka tenant daveti veri erişimi vermez; tekrar kabul ikinci üyelik/koltuk yaratmaz; admin öğrenci parolasını görmez veya göndermez.

### 11.2. Bireysel kayıt ve kurumdan bağımsız çalışma

1. Self-service kayıt açıldıysa öğrenci OIDC ile giriş yapar; kişisel tenant + STUDENT üyeliği oluşturulur.
2. Free hakları ve AI kotası uygulanır; sınırsız ücretsiz üretim yoktur.
3. Kurum daveti kabul edilirse ayrı kurum çalışma alanı görünür; öğrenci açıkça bağlam değiştirir.
4. Kurum alanındaki veri kurumun eğitim görünümüne girer; kişisel geçmiş kendiliğinden taşınmaz.
5. Kurum üyeliği sona ererse yeni kurumsal işlemler durur; kişisel alan varsa kendi kurallarıyla devam eder.

**MVP sınırı:** B2C modelinin veri sınırı baştan kurulur; açık B2C pazarlama, self-service satış ve ödeme P2/P3’e ertelenebilir. Bir kişisel tenant test fixture’ı izolasyon tasarımını doğrulamak için P0’da bulunur.

### 11.3. AI Practice Lab — soru seti üretimi

**Girdi:** Sınav türü, ders/konu kimliği, desteklenen zorluk, soru sayısı; isteğe bağlı görev bağlantısı.

1. UI katalogdan seçim yaptırır; seçilemeyen ders ve aşırı soru sayısını engeller.
2. Backend üyelik/sahiplik, katalog sürümü, parametre ve entitlement kontrolü yapar.
3. İdempotency anahtarı ve istek özeti kontrol edilir. Aynı anahtar/farklı istek `409` döner.
4. Bir transaction’da `GENERATING` oturumu, job ve kota rezervasyonu yazılır; `202` döner.
5. Worker gerekli minimum konu/seviye/öğrenme bağlamını toplar; öğrenci adı veya e-postasını AI’a göndermez.
6. FastAPI sürümlü prompt ve JSON schema ile provider’a gider; belirlenmiş token, timeout ve deneme bütçesine uyar.
7. Format, seçenek sayısı/benzersizliği, tek geçerli cevap, konu uyumu, tekrar, güvenlik ve uygulanabilir doğruluk kontrolleri çalışır.
8. Eksik/uygunsuz soru varsa sınırlı yeniden üretim yapılır. İstenen set tamamlanmadan sessizce eksik set yayınlanmaz.
9. Backend sonucu yeniden doğrular; soru snapshot’larını, provenance ve model/prompt sürümünü kaydeder.
10. Oturum `READY` olur; kota rezervasyonu gerçek tüketime çevrilir; polling UI soruları yükler.

**Başarısızlık:** Provider yoksa kontrollü failure veya konu/seviye açısından uygun, kullanım hakkı olan önceden doğrulanmış soru havuzu kullanılır. Fallback kaynağı saklanır; gerçek zamanlı AI üretimiymiş gibi sunulmaz. Güvenli fallback yoksa boş/uydurma soru sunmak yerine yeniden deneme önerilir.

**Kabul:** Doğru seçenek çözümden önce ağ yanıtına girmez; başka öğrencinin iş durumu görülemez; çift istek ikinci set/kota kesintisi oluşturmaz; provider hatası mevcut görev/deneme/dashboard’u durdurmaz.

### 11.4. Soru çözme, sonuç ve metrik güncellemesi

1. Öğrenci kendi `READY/IN_PROGRESS` oturumundaki soruyu açar; gösterim zaman damgası alınır.
2. Seçilen seçenek ve idempotency anahtarı gönderilir. İstemciden `correct=true` kabul edilmez.
3. Backend oturum sahipliğini, soru ilişkisini, durumunu, seçeneği ve daha önce cevaplanıp cevaplanmadığını kontrol eder.
4. İlk cevap, sunucudaki anahtarla deterministik puanlanır. Cevap + sonuç + `AttemptRecorded` aynı transaction’da yazılır.
5. Aynı isteğin tekrarı aynı sonucu döner; farklı ikinci cevap mevcut sonucu değiştirmez.
6. İlk cevap sonrasında doğru seçenek ve izinli açıklama açılır. Tekrar çözme ayrı çalışma olarak takip edilir.
7. Öğrenci oturumu bitirir; doğru/yanlış/cevaplanan/atlanan/süre özeti döner.
8. Outbox tüketicisi topic ve günlük metric’i, uygun görev ilerlemesini ve kurum görünümünü günceller.
9. UI “ölçümler güncelleniyor” durumunu yönetir; cevap sonucu ile gecikmeli dashboard arasında çelişkiyi açıklayabilir.

**Hatalar:** Ağ kesintisi, çift tıklama, iki sekmeden cevap, expired membership, iptal edilmiş soru, client saat farkı. Süre verisi sınırlı ve yaklaşık kabul edilir; güvenlik/puanlama client saatine dayanmaz.

**Soru itirazı:** Öğrenci hatayı bildirir; inceleyen kişi soruyu karantinaya alabilir. İptal edilen sorunun etkisi yeniden hesaplanır, gerekirse kullanıcıya sonuç düzeltmesi açıklanır. AI açıklaması itirazı otomatik reddetmez.

### 11.5. My Tasks ve Home

1. Home kendi görevlerini, son ilerlemeyi ve yeterli kanıt varsa en fazla birkaç önceliği getirir.
2. Öğrenci manuel görev açabilir veya kuralın önerdiği görevi başlatır.
3. Görev, uygun Practice oturumuna `taskId` ile bağlanır; konu/öğrenci ve minimum hedef doğrulanır.
4. Geçerli cevap olayları idempotent biçimde ilerlemeyi artırır; tekrar içerik sayım politikası açıktır.
5. Otomatik görev hedefe ulaşınca `COMPLETED`; manuel çalışma kullanıcı beyanıyla tamamlandıysa `SELF_REPORTED` kanıt türü saklanır.
6. Görev ertelenebilir/iptal edilebilir; süresi geçen görevler ayrı görünür. Kapalı göreve yeni cevap ilerleme yazmaz.
7. Home “neden bu görev?” açıklamasını metrik snapshot’ından üretir; boş veri durumunda başlangıç çalışması gösterir.

**Durumlar:** `SUGGESTED → ACCEPTED → IN_PROGRESS → COMPLETED`; ayrıca `DISMISSED`, `CANCELLED`. Gecikme (`overdue`) temel durumdan türetilir; görev durumunu gereksiz kombinasyonlara bölmez.

**Kabul:** Aynı analytics olayı iki aynı görev yaratmaz; manuel completion accuracy’yi değiştirmez; aktif kurum değişiminde görev cache’i karışmaz.

### 11.6. İçerik kabulü ve MEB/izinli belge ingestion

**Aktör:** Öğrenci değil, yetkili içerik operatörü. **Ön koşul:** Kaynağın işleme ve gösterim hakkı kayıtlı.

1. Kaynak, kitap adı, baskı/sürüm, sınav/ders kapsamı, hak dayanağı ve son kullanım koşulları kaydedilir.
2. PDF private quarantine alanına alınır; dosya türü, boyut, hash, sayfa sayısı ve zararlı içerik kontrolleri yapılır.
3. İş başlar; Python worker’a yalnız ilgili dosya için kısa ömürlü okuma erişimi verilir.
4. Metin sayfa bazında çıkarılır. PDF sayfa indeksi ile basılı sayfa numarası ayrı saklanır.
5. Formül, tablo ve Türkçe karakter kalitesi örneklenir. Yetersiz/taranmış sayfa `NEEDS_REVIEW`; OCR otomatik doğru varsayılmaz.
6. Bölüm/sayfa sınırını koruyan chunk oluşturulur; başlangıç denemesi olarak yaklaşık 400–800 token ve %10–15 örtüşme değerlendirilir.
7. Chunk’a ders/konu, kaynak sürümü, sayfa ve offset metadata’sı eklenir.
8. Embedding model/sürüm/dimension kaydedilerek hesaplanır; backend kontrollü batch’lerle yazar.
9. Retrieval ve kaynak gösterimi örnek sorgularla değerlendirilir; yetkili inceleme sonrası içerik `PUBLISHED` olur.
10. Yeniden çalıştırma hash+sürüm+stage anahtarıyla duplicate chunk üretmez; başarısız aşamadan devam edebilir.

**Kabul:** Yayınlanmamış veya hakkı bitmiş belge retrieval’a girmez; görsel ağırlıklı sayfanın yanlış metni güvenilir kaynak diye sunulmaz; yeni sürüm eskisinin sayfa referanslarını sessizce değiştirmez.

### 11.7. Smart Study — “Bu sayfayı anlat”

1. Öğrenci izinli kitap ve sayfayı seçer; desteklenen açıklama türünü belirler.
2. Backend içerik hakkı/erişim, öğrenci sahipliği, metin uzunluğu ve kota kontrolü yapar.
3. Sayfa seçilmişse önce o sayfa ve sınırlı komşu bağlam alınır. Bütün kitapta semantik arama sayfa talebinin yerine geçmez.
4. Konu sorusunda query embedding üretilir; vector sorgusu kitap/sürüm/tenant/erişim filtreleri **arama sırasında** uygulanarak çalışır.
5. Gerekirse PostgreSQL text search ile hibrit retrieval denenir; Türkçe kalite ve filtrelenmiş sonuç sayısı ölçülür.
6. Top-k başlangıçta yaklaşık 4–6 chunk; token/context bütçesine göre sınırlandırılır. Eşik model/veri setiyle kalibre edilir.
7. Yeterli kaynak yoksa yanıt “Bu içerikte yeterli kaynak bulunamadı” olur; genel bilgiler kaynaklıymış gibi doldurulmaz.
8. AI’a kaynaklar güvenilmeyen içerik olarak aktarılır; belge içindeki talimatlar sistem talimatı sayılmaz.
9. Yanıt şeması açıklama + izinli citation kimlikleri ister. Backend citation’ların gerçekten retrieval setinde olduğunu doğrular.
10. UI kitap, baskı ve sayfayı gösterir; öğrencinin erişimi olmayan URL veya ham object key açılmaz.

**Hatalar:** Boş retrieval, ilgisiz kaynak, provider timeout, geçersiz citation, lisans iptali, XSS içeren Markdown, prompt injection. Düşük güvenli yanıt yayınlanmaz; kullanıcı alternatif sayfa/çalışma seçebilir.

**MVP sınırı:** Özet ve sayfa açıklaması önceliklidir. Flashcard/mini quiz gibi ek formatlar temel kalite sağlandıktan sonra; genel AI Coach bu endpoint’e gizlice eklenmez.

### 11.8. Deneme tanımı, CSV/XLSX import ve yayınlama

1. Admin sınav türü, tarih, ad, ders soru sayıları ve puanlama politikasıyla deneme oluşturur.
2. Sistem örnek CSV/XLSX şablonu ve alan tanımlarını sunar. CSV önce, pilot gerektiriyorsa XLSX aynı doğrulama sözleşmesiyle teslim edilir.
3. Admin dosyayı yükler; boyut/satır sınırı, gerçek dosya türü, makro/formül ve zip-bomb kontrolleri uygulanır.
4. Satırlar staging’e alınır. Türkçe karakter, virgüllü ondalık, boş hücre ve tarih formatları açık normalizasyon kuralına tabidir.
5. Öğrenci mapping’i tenant içindeki öğrenci numarasıyla yapılır; belirsiz isim eşleşmesi otomatik birleştirilmez.
6. Ders kodu, soru sayısı, negatif adet, duplicate öğrenci+ders, eksik zorunlu alan ve deneme kapsamı kontrol edilir.
7. Doğru/yanlış/boş varsa toplam soru sayısıyla tutarlılık ve net hesaplanır. Sadece net varsa geçerli net aralığı kontrol edilir, ayrıntı uydurulmaz.
8. Önizleme; geçerli/hatalı satır, öğrenci eşleşmesi, uyarı ve örnek sonuçları gösterir. Hata raporu güvenli indirilebilir.
9. MVP varsayılanı **tam dosya doğrulanmadan yayınlamama**dır. Satırların sessiz kısmi kabulü yapılmaz.
10. Admin onaylayınca active revision atomik yayınlanır; sonuçlar ve outbox kaydı birlikte commit edilir.
11. Trend/weakness/risk hesapları asenkron güncellenir; öğrenci yalnız yayınlanmış sonuçları görür.
12. Düzeltme yeni revizyon olarak önizlenir/yayınlanır; audit ve etkilenen metric rebuild işi oluşur.

**Hatalar:** Aynı dosyanın tekrarı, aynı denemeye ikinci publish, öğrenci aktarım sırasında pasifleştirilmesi, timeout, eşzamanlı admin düzeltmesi. İdempotency, revision check ve transaction ile tek geçerli görünüm korunur.

**Konu analizi eklentisi:** Deneme formu+soru no→konu mapping ve öğrenci soru outcome’ları varsa ek import şablonu kullanılır. Yoksa dashboard yalnız ders düzeyinde analiz yapar.

### 11.9. Öğrenci performans ekranı

1. Profil/sınav kapsamı seçilir; topic map, practice geçmişi, görev ve deneme trendi ortak döneme göre sorgulanır.
2. Her kart kaynak/dönem/örneklem ve son hesap zamanını taşır.
3. Weakness kartından ilgili konu pratiğine veya görevine geçilir.
4. Deneme trendinde sınav tarihi, ders kapsamı, eksik veri ve farklı güçlük uyarısı görünür.
5. Veri düzeltmesi/iptal edilmiş soru sonucu değişmişse tutarlı güncel görünüm sunulur; ayrıntı açıklanabilir.

**Kabul:** Bir net toplamından “Problemler hatan %31 arttı” gibi kanıtsız cümle üretilmez; küçük örneklemde `STRONG/WEAK` kesin etiketi verilmez.

### 11.10. Kurum dashboard’u, öğrenci profili ve weakness analizi

1. Admin dönem/sınıf/grup filtrelerini seçer; backend filtrelerin aynı tenant’a ait olduğunu doğrular.
2. Analytics hazır öğrenci/gün/konu aggregate’lerini okur; istekte tüm cevap geçmişini yeniden hesaplamaz.
3. Toplam/aktif öğrenci, cevap/oturum/görev, ders trendleri, veri eksiği ve inceleme listesi getirilir.
4. Metrik tanımı ve payda UI’da erişilebilirdir. Aktif koltuk sayısı ile haftalık aktif öğrenci karıştırılmaz.
5. Admin öğrenciyi açtığında gerekçeli zayıf alanlar, görevler ve deneme geçmişini aynı kapsamda görür.
6. Kurum konu görünümü öğrenci bazlı ölçümleri kullanır; çok soru çözen tek öğrencinin tüm sınıfı domine etmesi önlenir.
7. Önerilen varsayılan: konu görünümünde yeterli kanıtlı öğrencilerin eşit ağırlıklı ortalaması + katılan/yeterli verili öğrenci sayısı birlikte gösterilir.
8. Güncel sınıf üyeliği ile geçmiş kohort analizi ayrı filtre/politikadır; karşılaştırmanın hangi popülasyonu kullandığı saklanır.

**Kabul:** Her liste/detay/export tenant kapsamlıdır; boş sınıf sıfır başarı olarak sunulmaz; eski projection “canlı” diye etiketlenmez.

### 11.11. Risk inceleme ve müdahale döngüsü

1. Risk motoru taze veriden puan, kapsam ve gerekçe üretir; açık aynı uyarıyı günceller.
2. Admin listede öncelik, öğrenci, gerekçe ve son aktiviteyi görür.
3. İnceleme açılır; ham kanıta ve trend dönemine ulaşılır.
4. Admin “öğrenciyle görüşme”, “ek çalışma”, “takip et” gibi akademik aksiyon ve takip tarihi kaydeder.
5. İç notlarda sağlık/aile gibi gereksiz hassas veri yazılmaması UI’da hatırlatılır; notlar AI’a gönderilmez.
6. Uyarı `OPEN → ACKNOWLEDGED → IN_PROGRESS → RESOLVED` veya `SNOOZED` ilerler.
7. Yeni ölçümler takipte sunulur; admin yanlış alarm işaretleyebilir. Bu geri bildirim kural kalibrasyonuna girer.
8. Uyarıyı kapatmak risk puanını yapay olarak sıfırlamaz; değerlendirme verisi ile iş akışı durumu ayrıdır.

**Kabul:** Aynı veri için bildirim tekrarları sınırlı; müdahale kimliği/zamanı audit’li; risk modeli karar verici değil destekleyicidir.

### 11.12. Abonelik, koltuk ve AI kullanım hakkı

1. Kurum için süreli tek plan tanımlanır: koltuk limiti, etkin özellikler ve dönemlik AI bütçesi.
2. Davet sırasında koltuk rezerve edilir; aktiflik/rezervasyon sayımı atomiktir ve aynı öğrenci iki koltuk tüketmez.
3. Üretim işinden önce öğrenci ve kurum limitleri kontrol edilir; tahmini kullanım rezervasyonu yapılır.
4. Başarı/başarısızlık sonunda gerçek provider kullanımı kaydedilir; kullanıcı kotası hata politikasına göre kesinleştirilir veya iade edilir.
5. Günlük/aylık bütçe aşıldığında yeni pahalı üretim durur; mevcut sonuçlar ve temel deneme/görev işlevleri politika dahilinde devam eder.
6. Plan süresi bitince yeni koltuk/üretim sınırlandırılır; verinin anında silinmesi veya belirsiz rehin tutulması yerine sözleşmeli erişim/export/saklama süreci uygulanır.
7. Pilot fatura/tahsilat başlangıçta manuel olabilir. Plan değişimi audit’li operasyon prosedürüyle yapılır; DB’ye rastgele elle SQL yazılmaz.

**Kabul:** “Unlimited” pazarlaması teknik/fiyat sınırı olmadan kullanılmaz; eşzamanlı istek kota aşamaz; provider başarısızlığı görünmeyen sınırsız maliyet yaratmaz.

---

<a id="ai-kalite"></a>
## 12. AI ve RAG Kalite Mühendisliği

### 12.1. AI servisinin sözleşmesi

İç servis operasyonları: soru üretimi, belge çıkarımı/chunk önerisi, embedding üretimi ve kaynaklı açıklama. Her istek operation/job kimliği, schema/prompt sürümü, minimum içerik bağlamı ve bütçe taşır. Yanıt typed JSON, kullanım bilgisi, model sürümü ve güvenli hata kodu içerir.

- Tarayıcı provider API’sine erişmez; anahtarlar backend/Python secret alanında kalır.
- FastAPI genel internetten çağrı alamaz; iç ağ + döndürülebilir servis kimliği kullanılır. mTLS daha sonra gerekebilir.
- Python arbitrary DB query, kurum öğrenci listesini alma veya sınırsız object okuma yetkisine sahip değildir.
- Büyük extraction/embedding yanıtları sınırlı batch veya tek işe özel private artifact üzerinden backend’e aktarılır; genel yazma yetkisi verilmez.
- Servis role/tenant kararını LLM’e bırakmaz. LLM tool execution, SQL çalıştırma, web gezinme ve öğrenci kaydı mutasyonu MVP’de yoktur.

### 12.2. Soru kalite katmanları

1. **Yapısal:** Schema, required alanlar, seçenek sayısı, tek doğru anahtar, boyut sınırı.
2. **İç tutarlılık:** Anahtar-açıklama uyumu, seçenek tekrarları, boş/gizli cevap, imkânsız veri.
3. **Alan doğruluğu:** Uygun soru tipinde deterministik hesap/constraint kontrolü; genel sorularda eğitim uzmanı örneklemi.
4. **Müfredat:** Konu/seviye ve hedef dil uyumu; desteklenmeyen alanı reddetme.
5. **Güvenlik ve içerik:** Yaşa uygun eğitim bağlamı, kişisel veri ve telifli metnin kontrolsüz tekrarı riskleri.
6. **Operasyon:** Report/karantina/geri çekme; prompt veya model değişiminde regression değerlendirmesi.

JSON’un geçerli olması cevabın doğru olduğunu göstermez. İkinci bir LLM kontrolü de tek başına kanıt değildir. Pilot başında küçük doğrulanmış kapsam ve insan incelemesi, içerik hacminden daha önemlidir.

### 12.3. Değerlendirme veri seti

Önerilen başlangıç: desteklenen ders/konulara dengeli dağılmış en az 100 soru ve 50 kaynaklı açıklama örneği; kesin büyüklük kapsam ve uzman kapasitesiyle ayarlanır.

| Test seti | Ölçülen boyutlar |
| --- | --- |
| Soru üretimi | Doğru cevap, çözülebilirlik, tek anlamlılık, konu uyumu, seçenek kalitesi, açıklama doğruluğu. |
| Retrieval | Doğru sayfanın/chunk’ın bulunması, Recall@k, filtre sonrası sonuç, eksik kaynakta abstention. |
| Kaynaklı açıklama | Citation geçerliliği, kaynağın yanıtı gerçekten desteklemesi, formül/türkçe kalitesi. |
| Kötücül girdiler | Prompt injection, belge içi talimat, XSS, kişisel veri isteme, tenant dışı içerik arama. |
| Dayanıklılık | Timeout, `429`, yarım JSON, boş yanıt, provider kesintisi, tekrar çağrı ve bütçe aşımı. |

Örnek pilot kapıları: yayınlanan çıktılarda yapısal kontrol geçişi %100; uzman örnekleminde en az %95 içerik doğruluğu; güvenlik/tenant sızıntısı örneklerinde sıfır kabul; citation referans bütünlüğü %100. Bunlar garanti değil, sürüm kabul hedefidir; değerlendirici, örneklem ve hata türleri raporlanır. Kritik yanlışlıkta ilgili alan/model yayını durdurulur.

### 12.4. Model/embedding değişikliği

- Provider adapter; `modelId`, prompt, schema ve evaluation sürümünü birlikte kaydeder.
- Yeni model önce sabit veri setinde maliyet/latency/kalite karşılaştırmasına girer; doğrudan bütün öğrencilere geçirilmez.
- Embedding modeli veya dimension değişirse yeni indeks/sürüm oluşturulur; karışık uzayda similarity yapılmaz.
- Kaynak gösteren açıklama cache anahtarı içerik+retrieval+prompt+model sürümünü kapsar; öğrenciye özel bağlam tenantlar arası cache’lenmez.
- Provider’ın saklama/eğitim kullanımı ve bölge koşulları incelenir; “anonim ID kullandık” kişisel veri değerlendirmesini tek başına bitirmez.

---

<a id="gizlilik"></a>
## 13. Güvenlik, Gizlilik ve İçerik Hakları

### 13.1. Tehdit modeli ve zorunlu kontroller

| Tehdit | Minimum kontrol | Doğrulama |
| --- | --- | --- |
| Tenantlar arası IDOR/BOLA | Üyelik + tenant + sahiplik; RLS; bileşik FK | A/B tenant negatif API/DB/iş/export testleri. |
| Hesap/davet ele geçirme | OIDC, admin MFA önerisi, tek kullanımlı hash’li davet, rate limit | Expiry/replay/yanlış hesap testleri. |
| XSS/CSRF | Güvenli cookie, CSRF, CSP, sanitize, güvenli matematik renderer | E2E kötü HTML/Markdown ve çapraz origin testleri. |
| SQL injection | Parametreli sorgu ve sıralama allowlist | Repository testleri; SAST/DAST. |
| AI abuse/maliyet saldırısı | Kullanıcı+tenant kota, eşzamanlılık, token/dosya sınırı | Paralel üretim ve tüketim mutabakatı testi. |
| Dosya saldırısı/SSRF | MIME+imza doğrulama, boyut/zip limit, tarama, URL allowlist | Bozuk PDF/XLSX, metadata URL ve ağ sınırı testleri. |
| Veri sızıntısı/log | PII redaction; private storage; kısa signed URL; secret tarama | Log/trace/export örneklerinin incelenmesi. |
| Yetki iptali gecikmesi | Her istekte aktif üyelik; job bitişinde kontrol | İptal sonrası açık sekme/iş/URL testleri. |
| Veri kaybı | DB/object yedeği, restore, silme talebi tekrar uygulama | İzole ortama gerçek geri yükleme provası. |

### 13.2. KVKK ve çocuk verisi

- Kurum ve Luminary’nin veri sorumlusu/veri işleyen rolleri iş modeline göre uzmanla belirlenir; varsayılan tek rol ilan edilmez.
- Hangi amaçla hangi veri işlendiği, dayanak, saklama, erişim ve yurt dışı aktarımı envantere alınır.
- Hedef kitlenin bir bölümü reşit olmayabilir. Yaşa uygun aydınlatma ve gerekliyse veli/yasal temsilci akışı hukuk değerlendirmesiyle tasarlanır; her işlem için genel bir “onay kutusu” yeterli sayılmaz.
- Veli/yasal temsilci gerekliliğini karşılamak, kapsamlı ebeveyn dashboard’unu MVP’ye almak anlamına gelmez.
- GDPR, yalnız coğrafi/işleme kapsamı doğuruyorsa ayrıca değerlendirilir; KVKK ile aynı kurallar varsayılmaz.
- Ad, e-posta, öğrenci numarası, performans ve kullanım kayıtları minimum kapsamda tutulur. T.C. kimlik numarası, tam doğum tarihi ve sağlık bilgisi ürün için zorunlu değilse toplanmaz.
- Rehberlik notları hassas kişisel alanlara kayabilir; yapılandırılmış akademik aksiyon ve kısa not tercih edilir.
- Öğrenci verisi model eğitimi/veri satışı için varsayılan kullanılmaz. Ulusal analiz gerçek anonimleştirme ve yeniden tanımlama risk değerlendirmesi gerektirir.

### 13.3. Önerilen saklama başlangıçları — onay gerektirir

| Veri | Başlangıç önerisi | Dikkat |
| --- | --- | --- |
| Import kaynak dosyası / satır hataları | Başarılı aktarım sonrası 30 gün | Kurum itiraz/düzeltme ihtiyacı ve sözleşme ile ayarlanır. |
| Ham AI prompt/yanıt tanı kaydı | Varsayılan kapalı; gerekirse maskeli, en çok 7 gün | Üründe gösterilen soru/açıklama kaydı ayrı yaşam döngüsündedir. |
| Uygulama logları | 30 gün | Kimlik/token/prompt içermemeli; güvenlik olayında kontrollü uzatma. |
| Audit | Öneri 12 ay | Hukuki gereklilik, erişim ve minimizasyonla onaylanır. |
| İşlenmiş outbox/dedup kaydı | Replay penceresiyle uyumlu, örneğin 30–90 gün | Dedup kaydını çok erken silmek duplicate etki doğurabilir. |
| Akademik ham veri | Sözleşme/eğitim dönemi ve gerekçeli saklama politikası | Otomatik sınırsız tutma veya ilişki biter bitmez kör silme yok. |
| Yedek | Öneri 30 gün döngü + sağlayıcı PITR seçeneği | Silinen veri restore ile geri gelirse silme talepleri yeniden uygulanır. |

Süreler hukuken zorunlu süre iddiası değildir. K-05/K-08 kapılarında veri türü başına kesinleştirilir; otomatik lifecycle işleri ve testleri backlog’a dahildir.

### 13.4. MEB ve sınav içerikleri

İnternette erişilebilir olmak ticari saklama, yeniden dağıtma, embedding, AI işleme veya türev içerik üretme izni anlamına gelmez. MEB kitapları, yayıncı denemeleri ve geçmiş ÖSYM soruları için hak kapsamı ayrı incelenir.

Kaynak kaydı kullanım koşulu, izin belgesi/referansı, baskı, kapsam ve geri çekme sürecini içerir. Hak net değilse içerik yayınlanmaz; özgün veya açıkça lisanslı alternatif kullanılır. Geçmiş sınav sorularının ilerideki trend analizi de bu kapıdan geçer.

---

<a id="operasyon"></a>
## 14. Dağıtım, Gözlemlenebilirlik ve Maliyet Kontrolü

### 14.1. Minimum üretim yerleşimi

**Öneri:** Tek bölge; küçük bir VM veya sade managed container runtime; Angular/Nginx, backend ve iç ağdaki AI container’ı; ayrı managed PostgreSQL; private object storage; yönetilen OIDC.

- Bir VM’de compute toplamak pilot için kabul edilebilir kesinti riski taşır; yüksek erişilebilirlik olarak pazarlanmaz.
- DB’yi aynı host’a koymak bütçe nedeniyle seçilirse off-host şifreli yedek, disk izleme ve restore prosedürü zorunludur; ayrı hata alanı tercih edilir.
- Lokal Compose ortamı production ile aynı güvenlik varsayımına sahip değildir. Lokal admin parolası production’a taşınmaz.
- Production DB/Python portları public değildir; yalnız reverse proxy gereken portları açar.
- Stage sentetik veri kullanır ve production secrets/DB’sinden ayrıdır. Aynı hesaptaki farklı database bile ayrı rol/ağ politikasıyla sınırlandırılır.

### 14.2. CI/CD ve migration

1. Feature branch → küçük PR → en az diğer geliştiricinin review’u.
2. Değişen bileşenler için format/lint/tip kontrolü, unit/contract test, backend build ve selected integration çalışır.
3. Tenant negatif testleri ve kritik E2E merge kapısıdır; maliyetli gerçek provider evaluation zamanlanmış/manuel onaylı koşar.
4. Secret/CVE/image taraması ve lisans kontrolü; SBOM ve sürümlü image üretimi.
5. Merge sonrası immutable image, commit SHA ile etiketlenir; `latest` tek kaynak olmaz.
6. Stage deploy, migration ve smoke test; production için GitHub Environment onayı.
7. Cloud erişiminde mümkünse GitHub OIDC/workload identity; uzun ömürlü bulut anahtarından kaçınılır.
8. Production migration ayrı kısıtlı rol ve kontrollü adımla yürütülür. Expand-contract değişim, eski/yeni uygulama uyumu ve migration süresi test edilir.
9. Health/readiness sonrası trafik; smoke test başarısızsa önceki uyumlu image’a dönülür.
10. DB migration otomatik “down” varsayılmaz. Destructive değişimde yedek/restore veya düzeltici migration planı gereklidir.

### 14.3. Sinyaller ve başlangıç hizmet hedefleri

| Alan | Önerilen pilot hedefi | Ölçüm notu |
| --- | --- | --- |
| Normal API | p95 < 500 ms | AI/dosya işi ve internet gecikmesi hariç; tanımlı yük profilinde. |
| Kurum dashboard | p95 < 1 s | 300 öğrenci ve örnek tarihsel veriyle; payload/sorgu sınırı dahil. |
| Cevaptan ölçüme gecikme | p95 < 60 s; 5 dk üzeri alarm | Başarılı commit’ten projection sürümüne kadar. |
| Practice üretimi | p95 ≤ 90 s hedef; yaklaşık 120 s iş deadline’ı | Provider/kapsama bağlı doğrulanır; garanti değil; kullanıcıya gerçek durum. |
| Erişilebilirlik | Pilot için aylık %99,5 hedef | Onaylanmış bakım/kesinti politikasıyla; satış SLA’sı sayılmaz. |
| Backup | RPO ≤ 24 saat, RTO ≤ 4 saat başlangıç hedefi | Tatbikatla doğrulanır; managed PITR ile iyileştirilebilir. |

İzlenecek metrikler: request/error rate, p95 latency, DB connection/disk/slow query, job sayısı ve en yaşlı iş, dead job, outbox lag, projection freshness, AI başarı/ret/timeout, token ve öğrenci/kurum maliyeti, kota reddi, import hatası, backup başarısı.

Metrik etiketlerine student ID/tenant ID gibi yüksek cardinality kişisel kimlikler doldurulmaz. Ayrıntılı olay incelemesi erişim kontrollü log/audit üzerinden yapılır.

### 14.4. Bütçe modeli

`Aylık maliyet = compute + DB/yedek + storage/transfer + OIDC/mail + gözlemleme + AI token/embedding/OCR maliyeti`.

`AI maliyeti = giriş token × giriş birim fiyatı + çıkış token × çıkış birim fiyatı + embedding/OCR + tekrar deneme maliyeti`.

- Değişken fiyatlar raporda güncelmiş gibi sabitlenmez; sağlayıcı/bölge seçimi sonrası teklif tablosu hazırlanır.
- 100/300/1.000 öğrenci için düşük/orta/yoğun kullanım senaryosu çıkarılır; eşzamanlı kullanıcı sayısı ayrıca varsayılır.
- Başlangıçta öğrenci/gün, kurum/ay ve tüm platform için limit; %50/%80/%100 bütçe uyarıları ve acil üretim kapatma anahtarı önerilir.
- RAG içeriğini bir kez embedding’e çevirme, uygun cache, kısa bağlam ve düşük maliyetli model kalite korunarak kullanılır.
- Başarısız provider çağrısının maliyeti de ölçülür. Soru başına maliyet yalnız başarılı çıktıların token’ı değildir.
- Kurum fiyatı yalnız AI giderini değil destek, altyapı, içerik incelemesi, ödeme/vergi ve ekip zamanını da karşılamalıdır.

### 14.5. Operasyon runbook’ları

En az şu senaryolar için tek sayfalık uygulanabilir prosedür: provider kesintisi, kota/maliyet sıçraması, outbox tıkanması, bozuk import, yanlış soru geri çekme, tenant veri şüphesi, kullanıcı silme talebi, DB doluluğu, secret rotation, release rollback, DB/object restore ve kurum onboarding/offboarding.

Her prosedürde sinyal, sorumlu, güvenli ilk aksiyon, doğrulama ve iletişim adımı bulunur. İş verisini onarmak için denetimsiz production SQL kullanımına dayalı operasyon tasarlanmaz.

### 14.6. Büyüme tetikleyicileri

| Ölçülen sorun | Önce denenecek | Gerekirse büyütme |
| --- | --- | --- |
| DB latency | İndeks, sorgu/projection, connection pool | Read replica veya ölçülmüş cache ihtiyacında Redis. |
| AI/API kaynak yarışı | Executor sınırı ve ayrı worker profili | Ayrı AI/worker compute. |
| Outbox gecikmesi | İdempotent paralel worker ve backpressure | Bağımsız consumer/replay ihtiyacında managed queue veya Kafka. |
| Analitik DB’yi zorluyor | Aggregate ve batch optimizasyonu | Ayrı analytics store/warehouse. |
| pgvector darboğazı | Filtre/index/recall tuning, embedding optimizasyonu | Ölçülmüş sınırda ayrı vector store. |
| Release/servis ölçeği | Basit managed runtime ve otomasyon | Ekip/iş yükü haklı çıkarırsa Kubernetes/EKS + GitOps. |
| Dağıtık hata takibi | Correlation ID ve temel metrik | OTel tracing ve merkezi gözlemleme stack’i. |

Kubernetes veya Kafka büyümenin zorunlu son noktası değildir; sorun daha basit managed hizmetle çözülebiliyorsa o tercih edilir.

---

<a id="test"></a>
## 15. Test Stratejisi ve Pilot Kabulü

### 15.1. Test matrisi

| Katman | Zorunlu senaryolar |
| --- | --- |
| Domain unit | Net hesaplama, düşük örneklem, güncellik, eşik sınırı, risk UNKNOWN, görev cooldown, kota rezervasyonu. |
| DB/Testcontainers | Flyway sıfırdan/upgrade, bileşik FK, RLS runtime rolü, pool tenant reset, unique/idempotency, pgvector filtre. |
| API güvenlik | A tenant → B tenant; aynı tenant’ta öğrenci A → öğrenci B; role downgrade, iptal üyelik, job/export/signed URL. |
| Outbox/iş | Commit öncesi/sonrası crash, ACK kaybı, duplicate, sıra dışı olay, lease expiry, retry sınırı, reconciliation. |
| Import | Türkçe encoding, ondalık, duplicate, eksik öğrenci, net-only, yanlış soru toplamı, revizyon, tam dosya rollback. |
| AI contract | Bozuk JSON, yanlış seçenek, boş retrieval, sahte citation, timeout, `429`, provider down, bütçe ve injection. |
| Frontend | Boş/eski/yetersiz veri, form validation, tenant değişimi, route guard, klavye/ekran okuyucu. |
| E2E | Davet → login → practice → cevap → weakness → görev → kurum; import → öğrenci trendi → risk → müdahale. |
| Yük | 300 öğrencili veri; başlangıç denemesi 100 eşzamanlı oturum, 10 normal API isteği/sn, 15 dk; ayrı sınırlı AI burst. |
| Operasyon | Stage deploy, migration, smoke, rollback, izole backup restore, silme taleplerinin restore sonrasında uygulanması. |

Yük profili pilot kullanımına göre ayarlanır; 300 kayıtlı öğrenci 300 eşzamanlı AI çağrısı demek değildir. Normal PR testleri gerçek LLM harcaması yapmaz; golden evaluation kontrollü çalışır.

### 15.2. Definition of Done

Bir görev yalnız “kod merge oldu” diye bitmez:

- Kabul kriteri karşılanmış; ilgili unit/integration/negatif yetki testi yazılmıştır.
- API/event/migration değişimi sürümlü ve geriye uyumluluk etkisi belgelenmiştir.
- Loading/error/empty/insufficient-data davranışı olan UI tamamdır.
- Tenant, rol, ownership, audit, idempotency ve rate limit etkisi incelenmiştir.
- Log/metric eklendiyse kişisel veri ve cardinality kontrol edilmiştir.
- Doküman/runbook ve feature flag gerekli ise günceldir.
- Diğer geliştirici review etmiş; CI geçmiş; staging smoke başarılıdır.
- AI özelliğinde evaluation; veri değişiminde migration/restore etkisi değerlendirilmiştir.

### 15.3. Teknik yayına çıkış kapıları

1. Bilinen kritik/yüksek riskli yetki veya veri sızıntısı açığı yok; zorunlu izolasyon testleri geçiyor.
2. Tekrar işleme cevap/net/görev/kota sonuçlarını çift saymıyor.
3. AI ve RAG kalite kapıları tanımlanmış örneklemde karşılanıyor; hakları belirsiz içerik yok.
4. Import düzeltmesi ve metrik rebuild gerçekçi fixture ile doğrulanmış.
5. Backup restore hedefleri tatbikatta ölçülmüş; image rollback ve migration prosedürü hazır.
6. Provider kesintisinde temel özellikler çalışıyor; üretim için timeout/kota/kill switch var.
7. Aydınlatma, sözleşme, gerekli temsilci süreci, veri envanteri ve silme/export yolu tamam.
8. Kritik kullanıcı yolunda engelleyici hata yok; performans/freshness hedefleri ölçülmüş.

### 15.4. Pilot ürün ölçümü

Önerilen pilot süresi onboarding sonrası **4–6 hafta**; kurumla kararlaştırılır. Aşağıdaki hedefler piyasa benchmark’ı değil, tartışılacak başlangıç hipotezidir.

| KPI | Kesin tanım önerisi | Başlangıç hipotezi |
| --- | --- | --- |
| Öğrenci aktivasyonu | Daveti kabul edenlerin 7 gün içinde ilk anlamlı practice’i bitirmesi | ≥%60. Davet teslim/kabul oranı ayrıca izlenir. |
| Haftalık aktiflik | Aktive olmuş pilot kohortunda anlamlı aksiyon yapan öğrenci | 3. haftadan itibaren ≥%40. |
| Görev kullanımı | Vadesi gelen kabul edilmiş görevlerde tamamlama | ≥%40; manuel/otomatik ayrı. |
| Deneme kapsaması | Kurumun pilotta yaptığı desteklenen denemelerden yayınlanan sonuç | ≥%90; hata/düzeltme oranı ayrıca. |
| Kurum kullanımı | En az bir yetkili admin’in haftalık inceleme akışı | Haftada ≥2 anlamlı oturum. |
| Uyarı aksiyonu | Yeterli kanıtlı uyarılardan 7 gün içinde incelenenler | ≥%60; yanlış alarm geri bildirimi ayrıca. |
| Kullanılabilirlik/değer | Öğrenci ve kurum görüşmesi; görevin yararlı bulunması | Haftalık kısa görüşme ve sorun listesi. |
| Ticari doğrulama | Pilot sonunda fiyatı bilerek devam etmek isteyen kurum | En az bir ücretli devam kararı hedefi; otomatik varsayılmaz. |

WAU/MAU ve cohort retention, toplam hesap sayısından ayrı izlenir. Pilot örneklemi küçük olduğu için net artışını doğrudan Luminary’nin nedensel etkisi ilan etmek doğru değildir; başlangıç seviyesi, katılım, farklı deneme güçlüğü ve görüşmeler birlikte değerlendirilir.

**Pilot kararları:** Güvenlik/kalite sorunu varsa durdur; kullanım düşükse onboarding/akış iyileştir; değer var ama ödeme yoksa fiyat/segmenti test et; kalite+kullanım+ticari ilgi varsa kontrollü genişlet.

---

<a id="teslimat"></a>
## 16. Teslimat Sıralaması ve Ekip Çalışma Modeli

### 16.1. İş bölümü

- **A — Backend/Platform:** Java, DB, auth, tenant, jobs, analytics, AI entegrasyonu, CI/CD ve operasyonun teknik sahibi.
- **B — Frontend/Product:** Angular, UX, erişilebilirlik, istemci testleri, onboarding, pilot görüşmeleri; AI evaluation/içerik akışında ortak katkı.
- **A+B:** Kapsam, maliyet, hukuk/uzman koordinasyonu, AI kalite kararı ve release go/no-go. Her teknik issue’nun yine tek sorumlusu olmalıdır.
- Bir geliştirici yalnız API, diğeri aylarca yalnız ekran üretmez; contract-first dikey dilimler staging’de birlikte gösterilir.
- Eğitimsel doğruluk ve hukuki kararlar iki geliştiricinin varsayımlarıyla kapatılmaz; gerektiğinde dış uzman zaman/maliyeti planlanır.

### 16.2. Önerilen 12 teslimat dilimi

Kaynak dokümandaki 12 sprint, **aşağıdaki sıralama için başlangıç referansıdır; süre taahhüdü değildir**. İki haftalık sprint seçilirse 12 sprint yaklaşık 24 takvim haftasıdır; yarı zamanlı çalışma, mevcut kod kalitesi, içerik hakkı ve dış uzman bekleme süreleri bunu değiştirir.

| Dilim | Ana çıktı | Kanıt / bağımlılık |
| --- | --- | --- |
| 1 | Kaynak envanteri, açık kararlar, repo/CI/lokal iskelet | Temiz bilgisayarda ayağa kalkma; K-01/K-02 ve provider/hukuk araştırması başlar. |
| 2 | Tenant, üyelik, güvenli giriş, UI shell, tek öğrenci daveti | İki tenant ve iki öğrenciyle negatif test; gerçek veri yok. |
| 3 | Müfredat, profil, job/outbox, kota ve stub AI | Dayanıklı iş testi; AI olmadan ilk uçtan uca contract. |
| 4 | Practice üretim/çözüm/sonuç ve kalite kontrolü | Tek konuda doğrulanmış practice; eşzamanlı/tekrar istek testi. |
| 5 | Weakness v1, My Tasks, Home ve temel kurum görünümü | Cevap → öneri → görev → kurum dikey döngüsü; P0. |
| 6 | Kurum öğrenci/grup yönetimi ve deneme CSV import | Preview, validasyon, publish; pilot dosya formatı doğrulaması. |
| 7 | Deneme düzeltme/trend, öğrenci performansı, XLSX ihtiyacı | Veri yeterliliği ve revision rebuild; temel P1. |
| 8 | Hakları onaylı içerik ingestion ve retrieval | K-04; sayfa/formül kalite değerlendirmesi. |
| 9 | Smart Study ve kaynaklı açıklama | Kaynak doğruluğu, boş retrieval, injection ve maliyet testi. |
| 10 | Kurum analitik, risk, müdahale ve kullanım görünümü | Açıklanabilir uyarıdan kayıtlı aksiyona döngü. |
| 11 | Yük/izolasyon/erişilebilirlik, privacy, restore ve release provası | Kontroller baştan sürer; burada toplu go/no-go doğrulaması yapılır. |
| 12 | Kontrollü onboarding ve pilot başlangıcı | 100–300 öğrenciye kademeli açılım; takip eden 4–6 hafta değerlendirme. |

Operasyon/güvenlik işleri 11. dilime bırakılmaz; ilk dilimden itibaren ilgili feature ile yapılır. İçerik hakkı ve provider seçimi beklerken diğer güvenli dikey dilimler geliştirilebilir.

### 16.3. Öncelik ve kapasite yönetimi

- Bir sprintte az sayıda uçtan uca teslimat; kişi başına en fazla bir ana geliştirme işi + küçük destek işi.
- Öneri olarak kapasitenin %20–30’u review, test, hata ve operasyon için bırakılır; gerçek hız ilk 2–3 sprintten sonra ölçülür.
- Büyük işler issue’lara ayrılır; aşağıdaki `L` görevler sprint’e alınmadan daha küçük alt görevlere bölünür.
- Feature flag’ler yarım özelliği saklar; güvenlik kapısını atlamak için kullanılmaz.
- İlk demosu olmayan mimari altyapı haftaları yerine öğrenci → kurum döngüsü önce gösterilir.

---

<a id="backlog"></a>
## 17. Kapsamlı Geliştirme Backlog’u

### 17.1. Backlog kullanım kuralları

Bu bölüm GitHub Epic/Issue’larına aktarılacak görev envanteridir. **Bütün maddeler planlanan iştir; tamamlanmış iş işareti değildir.** P2/P3 maddeleri vizyon kapsamını korur; ihtiyaç doğrulanmadan sprint’e alınmaz.

- **Kimlik:** `E00-01` biçimi; epic ve görev bağlantılarında kullanılır.
- **Öncelik:** P0 güvenli çekirdek; P1 tam/satılabilir MVP; P2 Intelligence; P3 Scale/Expansion. P0+P1 birlikte MVP kapsamıdır.
- **Sorumlu:** A backend/platform, B frontend/product, O ortak ürün/uzman kararı. O işlerinde sprint planlamasında tek DRI atanır.
- **Boyut:** S ≈ en çok 1 kişi-gün; M ≈ 1–3 kişi-gün; L ≈ 3–5 kişi-gün ve parçalanması gerekir. Bunlar kaba planlama bantlarıdır, teslim sözü veya toplam takvim hesabı değildir.
- Her tablodaki kabul çıktısı, Bölüm 15.2’deki ortak Definition of Done’a **ek** koşuldur. Test, yetki, hata durumu ve dokümantasyon ayrıca yazılmamış olsa bile her feature’a dahildir.
- Epic bağımlılıkları temel sıralamayı belirtir; bir epic’in bütün işleri bitmeden her sonraki işin beklemesi gerekmez. Örneğin UI sözleşme/mock ile paralel ilerler, production yayını gerçek bağımlılığın bitmesini bekler.
- Bir issue şablonu: problem, kapsam dışı, endpoint/olay/tablo etkisi, yetki, normal+hata akışı, kabul örnekleri, test planı, migration, gözlemleme, bağımlılık, boyut ve sorumlu içerir.

### E00 — Ürün kararları, kapsam ve yeniden kullanım envanteri

**Bağımlılık:** Kaynak belgeler ve ekip/pilot adayı görüşmesi. **Çıkış:** Onaylı kapsam, açık karar sahipleri ve doğrulanabilir ürün varsayımları.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E00-01 | Mevcut Luminary kodu, veri, ekran ve içerik envanterini çıkar | P0 | O / M | Her varlık için erişim, hak durumu, çalıştırılabilirlik ve koru/iyileştir/yeniden yaz kararı kayıtlı. |
| E00-02 | Pilot kullanıcı yolculuklarını ve kapsamını kesinleştir | P0 | B / M | Öğrenci ve admin’in ilk hafta yapacağı işler; TYT/AYT kararı ve kapsam dışı liste onaylı. |
| E00-03 | K-01–K-08 karar günlüğünü aç, DRI/tarih/blokaj ata | P0 | O / S | Her kapının sahibi ve yayın engeli görünür; belirsizlikler karar gibi sunulmuyor. |
| E00-04 | Mimari ADR ve modül sahipliklerini kaydet | P0 | A / M | BFF, tenant, AI sınırı, outbox ve storage kararı alternatif/bedelle belgeli. |
| E00-05 | Eğitimsel veri ve metrik sözlüğünü kurumla doğrula | P0 | O / M | Accuracy, aktivite, net, weakness, risk ve veri yeterliliği örnekleri mutabık. |
| E00-06 | İlk provider/region ve 100/300/1.000 öğrenci maliyet senaryosunu çıkar | P0 | A / M | Güncel fiyat kaynağı/tarihi, varsayımlar ve aylık üst limit yazılı. |
| E00-07 | Pilot sözleşme/fiyat/süre/koltuk ve başarı hipotezini belirle | P1 | O / M | Ücretsiz/ücretli koşul, çıkış/yenileme ve KPI paydaları net. |
| E00-08 | GitHub epic/issue planı ve ilk üç dikey dilimi oluştur | P0 | B / M | İşler bağımlı, küçük ve demonstrasyon çıktılı; P2/P3 mevcut sprint’e sızmıyor. |

### E01 — Repository, yerel ortam ve mühendislik temeli

**Bağımlılık:** E00-01, E00-04. **Çıkış:** Yeni geliştirici sentetik ortamı tek belgeli akışla çalıştırabilir; PR kalite kapıları aktif.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E01-01 | Ayrı Luminary monorepo ve dizin iskeletini kur | P0 | A / S | Frontend/backend/AI/infra/contracts/docs ayrımı; mevcut token servisiyle bağımlılık yok. |
| E01-02 | JDK/Boot/Gradle, Angular/Node, Python ve DB sürümlerini kilitle | P0 | A / M | Uyumluluk/EOL matrisi, wrapper ve lock dosyaları mevcut; clean build tekrarlanabilir. |
| E01-03 | Spring Boot modül iskeleti, hata modeli ve health endpoint’i kur | P0 | A / M | Tek uygulama kalkıyor; yönetim endpoint’i public değil; örnek modül testi geçiyor. |
| E01-04 | Angular strict proje ve temel test/format kurulumunu yap | P0 | B / M | Production build ve component testi CI’da geçiyor; environment’a secret gömülmüyor. |
| E01-05 | FastAPI proje iskeleti ve Python kalite araçlarını kur | P0 | A / M | Sağlık kontrolü, typed contract, pytest/Ruff ve dependency lock çalışıyor. |
| E01-06 | Compose ile PostgreSQL/pgvector, storage ve auth test ortamı oluştur | P0 | A / M | Windows PowerShell ve Linux yönergeleriyle temiz kurulum; kalıcı/test volume ayrımı açık. |
| E01-07 | Örnek environment ve secret yönetimini düzenle | P0 | A / S | `.env.example` yalnız placeholder; eksik secret başlangıçta kontrollü hata; gitignore/test var. |
| E01-08 | PR CI matrisini ve path filtrelerini kur | P0 | A / M | İlgili build/unit/contract koşuyor; güvenlik testleri yanlış path filtresiyle atlanmıyor. |
| E01-09 | Branch protection, CODEOWNERS, PR/issue şablonlarını tanımla | P0 | B / S | Review ve zorunlu CI atlanamıyor; acil istisna prosedürü belgeli. |
| E01-10 | Modül bağımlılığı ve OpenAPI drift kontrollerini ekle | P0 | A / M | Yasak repository/entity importu ve contract uyumsuzluğu PR’ı durduruyor. |

### E02 — Şema, migration ve veri erişim güvenliği

**Bağımlılık:** E01-03/E01-06, ADR-03/04/13. **Çıkış:** Gerçek PostgreSQL’de izole, sürümlü veri temeli.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E02-01 | Tenant/global/kimlik tablo sınıflamasını ve ER diyagramını çıkar | P0 | A / M | Tüm ilişkilere sahip modül, erişim sınırı ve silme davranışı atanmış. |
| E02-02 | Flyway baseline ve runtime/migration DB rollerini kur | P0 | A / M | Boş DB’den kurulum geçiyor; runtime DDL/superuser yetkisi alamıyor. |
| E02-03 | Tenant, user, membership ve student şemalarını oluştur | P0 | A / M | Global kimlik, tenant rolü, unclaimed öğrenci ve kurum/personal senaryosu destekli. |
| E02-04 | Bileşik FK/unique/constraint standardını uygula | P0 | A / M | Başka tenant kaydına ilişki DB seviyesinde reddediliyor. |
| E02-05 | Transaction-local tenant context ve fail-closed erişimi kur | P0 | A / L | Context yok/yanlışsa veri dönmüyor; pooled connection sonraki tenant’a sızmıyor. |
| E02-06 | RLS policy ve gerçek runtime rolüyle negatif testler yaz | P0 | A / L | A/B tenant, owner bypass ve FORCE RLS kararı doğrulanmış; RLS ownership’in yerine geçmiyor. |
| E02-07 | UTC/saat dilimi, numeric net ve optimistic locking standardı ekle | P0 | A / M | Gün sınırı, net hassasiyeti ve iki eşzamanlı güncelleme testleri geçiyor. |
| E02-08 | Sentetik veri factory ve pilot ölçek seed setini üret | P0 | A / M | İki kurum, personal tenant, davetsiz/pasif öğrenci ve 300 öğrencili geçmiş üretilebiliyor. |
| E02-09 | Migration upgrade, query plan ve indeks test yaklaşımını kur | P0 | A / M | Önceki şemadan upgrade ve tenant’lı liste/aggregate sorgularının planı kayıtlı. |

### E03 — Giriş, oturum, üyelik ve yetkilendirme

**Bağımlılık:** E02, K-03; UI E04 ile paralel. **Çıkış:** Her korunan kaynakta rol + tenant + sahiplik kontrolü.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E03-01 | OIDC sağlayıcı spike’ı ve test tenant’ı oluştur | P0 | A / M | Login/logout, redirect, issuer/audience ve admin MFA yeteneği doğrulanmış. |
| E03-02 | BFF Authorization Code/PKCE callback doğrulamasını uygula | P0 | A / L | State/nonce, süre, imza, yanlış issuer ve callback replay testleri geçiyor. |
| E03-03 | `issuer + subject` kullanıcı eşleme ve lifecycle’ını kur | P0 | A / M | Aynı e-posta farklı issuer’da yanlış birleşmiyor; pasif kullanıcı giriş yapamıyor. |
| E03-04 | Sunucu oturumu, cookie, CSRF ve timeout/logout kurallarını uygula | P0 | A / L | Token localStorage’da yok; CSRF’siz mutasyon reddediliyor; logout session’ı kapatıyor. |
| E03-05 | Aktif workspace seçimi ve her istekte üyelik doğrulamasını kur | P0 | A / M | Body/header tenant manipülasyonu yetki sağlamıyor; revoked üyelik hemen reddediliyor. |
| E03-06 | Rol ve öğrenci sahipliği policy katmanını oluştur | P0 | A / L | Student aynı tenant’taki diğer öğrenciyi göremiyor; admin tenant dışına çıkamıyor. |
| E03-07 | `/me` ve workspace API/OpenAPI sözleşmelerini yayınla | P0 | A / S | Kimlik/rol görünümü minimum veri içeriyor; erişimsiz workspace listelenmiyor. |
| E03-08 | Davet hash, expiry, hedef bağlama ve tek kullanım mekanizmasını yaz | P0 | A / M | Çift kabul, süresi geçmiş token ve yanlış hesap kayıt devralamıyor. |
| E03-09 | Güvenli kurum ilk-admin provisioning prosedürü hazırla | P0 | A / M | Yetkili/audit’li komut veya operasyon aracı; genel süperadmin veri gezintisi yok. |
| E03-10 | Personal tenant ve bir aktif kurum kısıtını uygula | P0 | A / M | Kişisel çalışma farklı tenant; ikinci kurumun kabul davranışı açık ve testli. |
| E03-11 | Login, oturum bitişi ve workspace değişim ekranlarını yap | P0 | B / M | Eski tenant cache/istekleri temizleniyor; yanlış bağlamda işlem görünmüyor. |
| E03-12 | Auth/davet brute-force ve güvenli hata politikası ekle | P0 | A / M | Kullanıcı varlığı gereksiz ifşa edilmiyor; rate limit ve audit PII içermiyor. |
| E03-13 | Yetki iptalinin job, export ve signed URL etkisini test et | P0 | A / M | Gecikmiş iş erişimi yeniden kontrol ediyor; signed URL TTL ve kalan risk belgeli. |
| E03-14 | Hesap kurtarma ve destek yetkisi prosedürünü hazırla | P1 | O / M | Provider üzerinden kurtarma; parola paylaşımı yok; destek erişimi süreli/gerekçeli. |

### E04 — Tasarım sistemi, navigation ve ortak UI

**Bağımlılık:** E00-02, E01-04; E03 contract’ları. **Çıkış:** Öğrenci ve admin deneyimleri ortak, erişilebilir kabukta çalışır.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E04-01 | Öğrenci/admin wireframe ve bilgi mimarisini hazırla | P0 | B / M | Ana yolculuklar, mobile web ve kurum navigation’ı onaylı. |
| E04-02 | Tema, tipografi, spacing ve durum bileşenlerini oluştur | P0 | B / M | Weak/strong/risk yalnız renk ile anlatılmıyor; ortak UI kataloğu var. |
| E04-03 | Rol temelli layout, route ve lazy loading’i uygula | P0 | B / M | Erişimsiz sayfa gizli/engelli; guard’ın backend yetkisi olmadığı belgeli. |
| E04-04 | Typed API client ve ortak Problem Details işleyicisini ekle | P0 | B / M | Alan hatası, 401/403/409/429/503 ve trace ID kullanılabilir gösteriliyor. |
| E04-05 | Loading/empty/error/stale/insufficient-data bileşenlerini yap | P0 | B / M | Her veri ekranı için tekrar kullanılabilir durumlar; veri yokken sahte grafik yok. |
| E04-06 | Job polling, backoff, iptal ve yeniden yükleme desteği ekle | P0 | B / M | Sayfa yenileme job’u kaybetmiyor; tenant/route değişince polling duruyor. |
| E04-07 | Güvenli Markdown/matematik gösterimini kur | P0 | B / M | Formül fixture’ları düzgün; script/unsafe link sanitize ediliyor. |
| E04-08 | Türkçe tarih/sayı/saat dilimi ve form doğrulamasını standartlaştır | P0 | B / M | Netin virgüllü gösterimi ile API numeric alanı karışmıyor; UTC dönüşümü testli. |
| E04-09 | Klavye, odak, ekran okuyucu ve responsive testlerini ekle | P1 | B / M | Temel WCAG 2.2 AA hedefleri için otomatik+manuel kontrol; bilinen engeller kayıtlı. |
| E04-10 | İzinli ürün olayı ölçüm katmanını ve feature flag görünümünü kur | P0 | B / M | Semantik olay tanımları tutarlı; prompt/PII analytics payload’ına girmiyor. |

### E05 — Müfredat kataloğu, öğrenci profili ve başlangıç deneyimi

**Bağımlılık:** E02/E03/E04, K-02. **Çıkış:** Desteklenen sınav/konulara bağlı öğrenci çalışma bağlamı.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E05-01 | TYT/AYT kapsamlı sürümlü katalog modelini kur | P0 | A / M | Exam type/ders/konu kimlikleri kararlı; sürüm arşivi geçmişi koruyor. |
| E05-02 | İlk pilot ders/konu seed’lerini eğitim uzmanıyla doğrula | P0 | O / M | Yalnız desteklenen kapsam aktif; ad/başlık eşlemeleri tekrar etmiyor. |
| E05-03 | Katalog read API, filtre ve cache politikasını uygula | P0 | A / M | Pasif konu yeni practice’e seçilemiyor; geçmiş sonuçta okunabiliyor. |
| E05-04 | Profil GET/PATCH, sınıf/mezun ve temel hedef validasyonunu yap | P0 | A / M | Yalnız izinli alan güncelleniyor; tenant/user kimliği değiştirilemiyor. |
| E05-05 | Öğrenci onboarding ve profil ekranını geliştir | P0 | B / M | Hedef isteğe bağlı; kullanıcı minimum alanla ilk çalışmaya geçebiliyor. |
| E05-06 | İlk çalışma başlangıç kartı ve boş veri yönlendirmesi yap | P0 | B / M | Veri yokken zayıflık iddiası değil konu seçimi/kısa başlangıç pratiği sunuluyor. |
| E05-07 | Sınıf ve hedef değişiminin geçmiş analize etkisini tanımla | P1 | A / M | Eski cevap/deneme scope’u yeni hedefle sessizce yeniden etiketlenmiyor. |
| E05-08 | Profil tamamlanması ve activation olaylarını tanımla | P0 | B / S | Profil kaydı ile ilk anlamlı practice ayrı KPI; duplicate frontend event’i sayacı şişirmiyor. |
| E05-09 | Mevcut öğrenci verisi varsa güvenli migration planı çıkar | P1 | A / M | Kaynak yoksa uygulanmaz kaydı; varsa mapping, izin, dry-run ve geri dönüş var. |
| E05-10 | Öğrenci profil negatif/edge-case testlerini tamamla | P0 | A / M | Yanlış rol, pasif üyelik, geçersiz hedef, davetsiz profil ve başka öğrenci reddediliyor. |

### E06 — Kurum, sınıf/grup ve öğrenci yönetimi

**Bağımlılık:** E03, E05; koltuk işlemlerinde E08-01/02. **Çıkış:** Kurum kendi öğrenci listesini güvenli yönetebilir.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E06-01 | Kurum ayarları, status ve saat dilimi API’sini kur | P0 | A / M | Admin yalnız kendi kurumunu değiştiriyor; saat dilimi değişim etkisi uyarılı. |
| E06-02 | Tekil öğrenci oluşturma/listeleme/güncelleme use-case’ini yaz | P0 | A / M | Student ref tenant’ta unique; davetsiz kaydın görünürlüğü ve validation net. |
| E06-03 | Sınıf/grup ve öğrencinin grup üyeliği işlemlerini yap | P1 | A / M | Tarihli üyelik, aynı tenant kontrolü ve pasif grup davranışı testli. |
| E06-04 | Davet gönderme/yenileme/iptal ile koltuk lifecycle’ını bağla | P0 | A / M | Mail arızası kayıt kaybettirmiyor; expired davet rezervasyonu bırakıyor. |
| E06-05 | Kurum öğrenci listesi, arama/filtre/pagination ekranını yap | P0 | B / M | Durum, sınıf, davet ve aktivasyon görünür; boş/eksik veri ayrışıyor. |
| E06-06 | Öğrenci formu, davet aksiyonları ve grup yönetimi UI’ını yap | P1 | B / M | Çift submit yok; hata formu koruyor; koltuk limiti açıklanıyor. |
| E06-07 | Toplu öğrenci CSV taslak/önizleme/validate akışını yaz | P1 | A / L | Belirsiz isim eşlemesi yok; duplicate/ref/encoding hataları satır bazlı. |
| E06-08 | Toplu öğrenci yükleme ve hata raporu UI’ını yap | P1 | B / M | Yayınlamadan önce sayılar ve eşleşmeler inceleniyor; tekrar yükleme güvenli. |
| E06-09 | Pasifleştirme/kurumdan ayrılma ve erişim iptalini uygula | P0 | A / M | Geçmiş tenant değişmiyor; yeni iş/oturum engelleniyor; audit var. |
| E06-10 | Kapsamlı offboarding ve personal workspace geçiş yönergesini hazırla | P1 | O / M | Geçmiş erişim/export/saklama net; otomatik kişisel veri kopyalama yok. |
| E06-11 | Öğrenci yönetimi audit ve güvenli export’unu ekle | P1 | A / M | Sadece yetkili alanlar; CSV formula injection önlenmiş; export job scoped. |
| E06-12 | Kurum yönetimi E2E ve eşzamanlı koltuk testlerini yaz | P1 | B / M | İki admin aynı öğrenciyi/koltuğu çift açamıyor; tenant dışı grup reddediliyor. |

### E07 — Kalıcı işler, outbox ve dayanıklı işleme

**Bağımlılık:** E02/E03, E01-10. **Çıkış:** Crash/duplicate/retry altında kayıpsız ve idempotent iş etkisi.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E07-01 | Sürümlü olay zarfı ve job türü sözleşmelerini oluştur | P0 | A / M | Tenant, event/aggregate version ve correlation alanları zorunlu; PII minimum. |
| E07-02 | Outbox/job/processed-event şeması ve transaction writer’ı yaz | P0 | A / M | Domain commit rollback olduğunda olay/iş de rollback oluyor. |
| E07-03 | Dispatcher, lease, heartbeat ve fencing uygulamasını yaz | P0 | A / L | İki worker aynı iş sonucunu overwrite edemiyor; eski lease ACK’i reddediliyor. |
| E07-04 | Kullanım türüne göre sınırlı executor ve backpressure ekle | P0 | A / M | AI/PDF yükü cevap API thread’lerini tüketmiyor; kuyruk limiti kontrollü. |
| E07-05 | Retry/backoff/jitter, deadline ve terminal hata politikası kur | P0 | A / M | Kalıcı hata tekrar etmiyor; maksimum deneme/süre sonrası operasyon görünürlüğü var. |
| E07-06 | Idempotent consumer transaction ve processed-event kaydını uygula | P0 | A / L | Projection etkisi ve dedup kaydı birlikte; ACK kaybı çift sayım üretmiyor. |
| E07-07 | Job status/cancel/retry yetkili API’sini ekle | P0 | A / M | Öğrenci başka işin durumunu göremiyor; operasyon retry gerekçeli/audit’li. |
| E07-08 | Reconciliation, projection rebuild ve sürüm geçişini kur | P0 | A / L | Kaynak/projection farkı tespit edilip tekrarlanabilir onarılıyor; eski olay yeni state’i bozmuyor. |
| E07-09 | Kuyruk/freshness/dead-job metrikleri ve retention işleri ekle | P0 | A / M | En eski iş yaşı ve failure görülebiliyor; işlenmemiş olay silinmiyor. |
| E07-10 | Fault-injection dayanıklılık test paketini yaz | P0 | A / L | Commit/ACK arası crash, lease expiry, duplicate, reorder ve revoked tenant senaryoları geçiyor. |

### E08 — Paket, koltuk, kota ve maliyet muhasebesi

**Bağımlılık:** E02/E03; pahalı job akışlarında E07. **Çıkış:** Kontrolsüz AI harcaması veya koltuk aşımı yok.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E08-01 | Tek kurum planı, dönem, feature entitlement şemasını oluştur | P0 | A / M | Trial/active/expired durumları ve yetki matrisi sürümlü. |
| E08-02 | Atomik koltuk rezervasyonu/aktivasyon/serbest bırakmayı uygula | P0 | A / M | Paralel davet koltuk limitini aşamıyor; aynı öğrenci iki kez sayılmıyor. |
| E08-03 | Öğrenci+tenant+platform kullanım limiti politikasını tanımla | P0 | O / S | Gün/dönem/saat dilimi ve başarısız iş iade kuralları açık. |
| E08-04 | Kota rezervasyon ve gerçek tüketim ledger’ını yaz | P0 | A / L | Idempotent settle/release; crash sonrası rezervasyon onarımı testli. |
| E08-05 | Provider token/fiyat sürümü ve job maliyet kaydını ekle | P0 | A / M | Retry/başarısızlık maliyeti dahil; fiyat değişimi geçmişi bozmuyor. |
| E08-06 | Rate limit, eşzamanlılık ve acil AI kill switch’i kur | P0 | A / M | Limitte 429/güvenli yanıt; mevcut görev ve deneme okumaları sürüyor. |
| E08-07 | Kurum kullanım/koltuk ve öğrenci kota API+UI’ını yap | P1 | B / M | Kullanıcı yalnız kendi hakkını, admin kurum toplamını görüyor. |
| E08-08 | Abonelik bitişi/yenileme/grace dönemi davranışını uygula | P1 | A / M | Yeni pahalı iş kapalı; export/saklama hakkı politika ile tutarlı. |
| E08-09 | Audit’li plan yönetimi ve manuel tahsilat işletim akışını kur | P1 | A / M | Plan değişimi güvenli araçla; SQL müdahalesine bağımlı değil. |
| E08-10 | Harcama uyarısı, kota mutabakatı ve concurrency testlerini tamamla | P0 | A / M | %50/%80/%100 veya onaylı eşikler alarm üretiyor; ledger toplamları doğrulanıyor. |

### E09 — AI platformu, sözleşmeler ve kalite değerlendirmesi

**Bağımlılık:** E01-05, E07/E08; K-03/K-07. **Çıkış:** Sınırlı yetkili, test edilebilir ve maliyeti görünür AI entegrasyonu.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E09-01 | Soru/açıklama/embedding için typed iç API contract’larını çıkar | P0 | A / M | Request/response schema, hata, model ve kullanım metadata’sı sürümlü. |
| E09-02 | Provider adapter ve offline deterministic stub uygula | P0 | A / M | UI/E2E gerçek LLM olmadan çalışıyor; sağlayıcı kodu domain’e sızmıyor. |
| E09-03 | FastAPI iç ağ/service authentication ve secret rotation kur | P0 | A / M | Public çağrı reddediliyor; DB/list-all-students yetkisi yok. |
| E09-04 | Prompt/schema registry ve provenance kaydını oluştur | P0 | A / M | Her yayınlanan çıktı hangi prompt/model/schema ile üretildiğiyle izleniyor. |
| E09-05 | Token/context limiti, timeout ve sınırlı retry uygula | P0 | A / M | Toplam iş bütçesi aşılmıyor; provider 429/timeout güvenli hataya dönüyor. |
| E09-06 | Çıktı schema, seçenek ve temel tutarlılık doğrulayıcılarını yaz | P0 | A / L | Bozuk/eksik/çelişkili çıktı yayınlanmıyor; kontrollü yeniden üretim testi var. |
| E09-07 | Konu uyumu, tekrar ve doğruluk kontrol stratejisini kur | P0 | A / L | Uygun tiplerde deterministik doğrulama; diğerlerinde inceleme/karantina yolu var. |
| E09-08 | Golden set, uzman inceleme rubriği ve hata kategorilerini oluştur | P0 | B / L | Desteklenen konular örneklenmiş; değerlendiren kişi ve karar kaydı görünür. |
| E09-09 | Prompt injection, kişisel veri ve kötü biçimli içerik testlerini yaz | P0 | A / M | Kaynaktaki talimat yetki/endpoint/DB işlemi yaptıramıyor; çıktı sanitize sözleşmesi testli. |
| E09-10 | Model karşılaştırma ve sürüm kabul raporu otomasyonunu kur | P1 | A / M | Kalite/latency/maliyet önceki sürümle kıyaslanıyor; başarısız aday açılmıyor. |
| E09-11 | Provider kesintisi, fallback ve içerik karantina politikasını uygula | P0 | A / M | Kaynağı doğru etiketli fallback veya açık failure; uydurma başarı yok. |
| E09-12 | PII’siz kullanım logu, değerlendirme raporu ve insan geri bildirimini bağla | P1 | B / M | Soru itirazı ilgili model/konu hata oranına bağlanabiliyor; öğrenci kimliği rapora sızmıyor. |

### E10 — AI Practice Lab, cevaplama ve sonuç

**Bağımlılık:** E03/E04/E05/E07/E08/E09. **Çıkış:** Öğrenci güvenli ve doğrulanmış soruyu çözer; sonucu kalıcı ölçüm kaynağı olur.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E10-01 | Session/question/attempt durum ve veri modelini oluştur | P0 | A / M | Cevap anahtarı sunucuya özel; snapshot/provenance ve unique ilk cevap kuralı var. |
| E10-02 | Session create validation/idempotency/kota transaction’ını yaz | P0 | A / M | Aynı istek tek job; aynı anahtar farklı body 409; tenant body’den atanamıyor. |
| E10-03 | Üretim worker’ını AI sözleşmesine bağla | P0 | A / L | Tüm set doğrulanınca READY; yarım sette publish yok; failure kota politikası uygulanıyor. |
| E10-04 | Practice seçim formu ve generation durum UI’ını yap | P0 | B / M | Ders/konu/seviye/sayı sınırları; bekleme/yeniden deneme ve kota görünümü var. |
| E10-05 | Güvenli soru okuma DTO ve yetkili session API’sini yaz | P0 | A / M | Çözüm öncesi ağ payload’ında cevap/ipuçlu metadata yok; başka öğrenci 404. |
| E10-06 | Soru çözme ekranı ve aktif etkileşim ölçümünü geliştir | P0 | B / L | Klavye, matematik, bağlantı kesintisi ve boş/atlanan soru akışı testli. |
| E10-07 | İlk cevap submit ve sunucu puanlamasını uygula | P0 | A / L | Client correct flag kabul edilmiyor; cevap+outbox atomik; iki sekmede çift cevap yok. |
| E10-08 | Cevap sonrası feedback/çözüm gösterimini yap | P0 | B / M | Açıklama yalnız yetkili aşamada; invalid soru ve tekrar çalışma bilgisi görünür. |
| E10-09 | Session complete ve sonuç özeti API’sini yaz | P0 | A / M | Doğru/yanlış/cevapsız/süre tanımları tutarlı; yeniden complete idempotent. |
| E10-10 | Sonuç ekranı ve topic/task’a yönlendirmeyi yap | P0 | B / M | Anlık sonuç ile gecikmeli metric ayrımı; veri yokken sahte öneri yok. |
| E10-11 | Practice geçmişi, devam et ve filtreleme akışını oluştur | P0 | B / M | Sayfa yenileme/oturum bitişi sonrası güvenli devam; başkasının geçmişi yok. |
| E10-12 | Tekrar soru/fingerprint ve mastery kanıtı sayım politikasını uygula | P0 | A / M | Aynı içerik tekrarları topic yeterliliğini sınırsız büyütmüyor. |
| E10-13 | Soru bildirimi API/UI ve inceleme kuyruğunu kur | P1 | B / M | Öğrenci yalnız eriştiği soruyu raporluyor; içerik operatörü güvenli inceleyebiliyor. |
| E10-14 | Karantina/iptal ve metric düzeltme olayını uygula | P1 | A / M | Yanlış soru yeni kullanıma kapanıyor; eski etkisi yeniden hesaplanıyor/audit’li. |
| E10-15 | Practice uçtan uca hata ve dayanıklılık testlerini tamamla | P0 | A / L | Provider down, timeout, invalid JSON, çift submit, revoked üyelik ve iş replay testli. |

### E11 — Ham ölçüm, Weakness Map ve veri tazeliği

**Bağımlılık:** E07, E10-07; deneme bileşeni E15 ile genişler. **Çıkış:** İzlenebilir, sürümlü ve düşük veriye dürüst performans görünümü.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E11-01 | Ölçüm sözlüğünü hesaplama contract’ına dönüştür | P0 | A / M | İlk cevap, aktif süre, oturum, tekrar ve invalid soru sayım örnekleri test fixture’ı. |
| E11-02 | Günlük/topic metric ve snapshot şemasını oluştur | P0 | A / M | Tenant, kaynak sürüm, örneklem, rule version ve computedAt alanları var. |
| E11-03 | Attempt/complete consumer’ını idempotent uygula | P0 | A / L | Duplicate/reorder olayı günlük ve topic sayısını şişirmiyor. |
| E11-04 | Geç gelen olay, saat dilimi ve gün penceresi hesabını yaz | P0 | A / M | Event zamanı doğru güne işleniyor; gece sınırı ve düzeltme testi var. |
| E11-05 | Weakness v1 skoru ve kanıt yeterliliğini uygula | P0 | A / M | 10 soru/2 session/güncellik/ağırlık kapıları ve eşik sınırları unit testli. |
| E11-06 | Güçlük/tekrar/iptal veri sınırlarını modele ekle | P0 | A / M | AI zorluk etiketi kalibre ölçüm gibi ağırlıklandırılmıyor; geçersiz sorular dışarıda. |
| E11-07 | Öğrenci metrics/weakness API’sini geliştir | P0 | A / M | Her skor sampleSize/evidenceStatus/dataThrough/ruleVersion ile döner. |
| E11-08 | Weakness Map, konu detayı ve öneri geçiş UI’ını yap | P0 | B / L | Zayıf/gelişen/güçlü/eski/yetersiz durumları ve gerekçeli pratik linki var. |
| E11-09 | Zamanlanmış recency güncelleme ve incremental rebuild yaz | P0 | A / M | Yeni cevap gelmese de stale durumu güncelleniyor; kaynak veriden aynı sonuç üretilebiliyor. |
| E11-10 | Scoring sürüm değişimi ve shadow karşılaştırmasını kur | P1 | A / M | Eski/yeni ölçümler karışmadan geçiş; beklenmedik dağılım değişimi görülebiliyor. |
| E11-11 | Data quality/reconciliation ve freshness alarmını bağla | P0 | A / M | Kaynak/aggregate farkı ve >5 dk lag örneği alarm/test üretiyor. |
| E11-12 | Uzman/pilot kalibrasyonu ve eşik karar kaydını yap | P1 | O / M | Düşük örneklem ve yanlış sınıflama örnekleri incelenmiş; eşikler varsayım olarak belgeli. |

### E12 — My Tasks, kural tabanlı öneri ve Home

**Bağımlılık:** E05, E10/E11; task model/UI daha erken mock ile yapılabilir. **Çıkış:** Ölçüm, uygulanabilir ve tekrarlamayan çalışma görevine dönüşür.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E12-01 | Görev şeması, durum geçişi ve evidence türlerini tasarla | P0 | A / M | Manuel/önerilmiş ve self-reported/ölçülen completion ayrışıyor. |
| E12-02 | Manuel görev CRUD ve sahiplik doğrulamasını yaz | P0 | A / M | Kullanıcı otomatik progress/rule alanlarını değiştiremiyor; optimistic locking var. |
| E12-03 | Weakness→öneri kural motorunu uygula | P0 | A / M | Günlük aktif limit, içerik bulunabilirliği ve yeterli kanıt kontrol ediliyor. |
| E12-04 | Deduplication/cooldown ve gerekçe snapshot’ını ekle | P0 | A / M | Aynı metric olayı tekrar görev yaratmıyor; neden önerildiği geçmiş kanıtla açıklanıyor. |
| E12-05 | Task-linked practice ve idempotent ilerlemeyi bağla | P0 | A / M | Başka konu/öğrenci cevabı hedefi artırmıyor; kapanmış görev tekrar ilerlemiyor. |
| E12-06 | Kabul/ertele/iptal/tamamlama use-case’lerini uygula | P0 | A / M | Geçersiz geçiş 409; erteleme zayıflık puanına doğrudan ceza değil. |
| E12-07 | My Tasks liste/oluştur/detay ve filtre UI’ını yap | P0 | B / L | Overdue ve active ayrımı; keyboard ve hata/boş durumları var. |
| E12-08 | Home öncelik, progress ve neden kartlarını oluştur | P0 | B / M | En fazla birkaç anlamlı aksiyon; MVP’de gereksiz Coach sohbeti yok. |
| E12-09 | Görev/KPI olayları ve admin özet contract’ını ekle | P0 | A / M | Payda dönemi açık; manuel completion accuracy sayacına girmiyor. |
| E12-10 | Öneri→practice→completion E2E ve sınır testlerini yaz | P0 | B / M | Duplicate, cooldown, veri yok, limit dolu, yarıda kalan ve iptal görev testli. |

### E13 — İçerik hakları, PDF ingestion ve vektör indeksi

**Bağımlılık:** K-04, E05/E07/E09 ve E19 içerik politikası. **Çıkış:** Yalnız izinli, sürümlü, sayfaya geri izlenebilir içerik yayında.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E13-01 | İlk kitap/içerik kaynaklarının hak ve kapsam envanterini hazırla | P1 | O / M | Saklama, embedding, AI işleme ve gösterim hakkı ayrı doğrulanmış; belirsiz kaynak bloklu. |
| E13-02 | Kaynak/lisans/sürüm/sayfa/chunk/embedding şemasını kur | P1 | A / M | PDF index ve basılı sayfa ayrı; model/dimension ve erişim sınıfı kayıtlı. |
| E13-03 | Private upload, quarantine ve dosya tarama akışını uygula | P1 | A / M | Yetkili operatör; MIME/imza/boyut/sayfa sınırı; public object oluşmuyor. |
| E13-04 | Extraction worker ve sınırlı dosya erişimini kur | P1 | A / L | Python yalnız ilgili işe ait dosyayı okuyabiliyor; hata kısa transaction/checkpoint ile yönetiliyor. |
| E13-05 | Türkçe/formül/tablo/sayfa kalite fixture’larını hazırla | P1 | B / M | Bozuk veya taranmış sayfa NEEDS_REVIEW; yanlış extraction sessiz yayınlanmıyor. |
| E13-06 | Sınırları koruyan chunking ve metadata zenginleştirmesini yaz | P1 | A / M | Chunk kaynağa geri izleniyor; bölüm/sayfa kesimi ve overlap örneklerle doğrulanmış. |
| E13-07 | Embedding batch, maliyet ve sürüm kayıtlarını uygula | P1 | A / M | Aynı model uzayı; idempotent batch; boyut/token limiti ve gerçek maliyet görünür. |
| E13-08 | pgvector filtreli sorgu ve indeks/recall spike’ını yap | P1 | A / L | Filtre sonradan veri sızdıran biçimde uygulanmıyor; küçük/filtreli kümede recall ölçülmüş. |
| E13-09 | Ingestion checkpoint/retry/dedup ve progress görünümü ekle | P1 | A / M | Aynı hash+sürüm tekrarında duplicate chunk yok; başarısız aşamadan devam ediyor. |
| E13-10 | İçerik inceleme/publish/geri çekme operasyonunu kur | P1 | B / M | Basit güvenli araç yeterli; onaysız kitap listelenmiyor; hak iptali retrieval’ı durduruyor. |
| E13-11 | İçerik/embedding sürüm yükseltme ve yeniden indekslemeyi uygula | P1 | A / M | Eski citation bozulmuyor; farklı dimension karışmıyor; cache invalidation testli. |
| E13-12 | Kitap/sayfa katalog API’si ve izin kontrollerini tamamla | P1 | A / M | Öğrencinin erişmediği sürüm/object adresi sızmıyor; signed URL expiry testli. |
| E13-13 | Haklı içerikle retrieval kabul testi ve yayın tutanağı yap | P1 | O / M | Golden sorgular, kaynak izlenebilirliği ve formül kalitesi kapısı geçilmiş. |

### E14 — Smart Study ve kaynaklı açıklama

**Bağımlılık:** E13, E04/E08/E09. **Çıkış:** Öğrenci seçtiği içerikten dayanaklı, güvenli ve kaynak gösteren açıklama alır.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E14-01 | Sayfa açıklama istek/job/yanıt contract ve saklama modelini kur | P1 | A / M | Tenant/student/content scope; kota, prompt/citation sürümü ve retention net. |
| E14-02 | Sayfa öncelikli retrieval ve sınırlı komşu bağlamı uygula | P1 | A / M | “Bu sayfa” isteği kitap geneli ilgisiz sonuca dönüşmüyor. |
| E14-03 | Konu sorgusunda vector/opsiyonel text retrieval ve threshold ekle | P1 | A / L | Yetki filtreleri sorgu içinde; top-k/context bütçesi ve boş retrieval abstention testli. |
| E14-04 | Kaynaklı açıklama prompt/typed response akışını geliştir | P1 | A / M | Provider’a yalnız gerekli chunk; öğrenci kimliği veya kurum notları gönderilmiyor. |
| E14-05 | Citation allowlist, kaynak desteği kontrolü ve output sanitize ekle | P1 | A / M | Uydurma kaynak referansı reddediliyor; doğrulanamayan ifade güvenilir diye yayınlanmıyor. |
| E14-06 | Kitap/sayfa seçici ve güvenli okuyucu arayüzünü yap | P1 | B / L | Baskı/sayfa farkı anlaşılır; yetkisiz veya geri çekilmiş kitap açılmıyor. |
| E14-07 | Açıklama/özet sonucu, citation linkleri ve geri bildirim UI’ını yap | P1 | B / M | Kaynak açılabiliyor; yanıt yok/timeout/limit durumları ve erişilebilirlik tamam. |
| E14-08 | İçerik sürümlü cache ve kullanıcıya özel bağlam ayrımını uygula | P1 | A / M | Tenantlar arası özel prompt/yanıt cache sızıntısı yok; lisans iptalinde cache temizleniyor. |
| E14-09 | Retrieval/grounding/latency/maliyet kalite raporunu üret | P1 | B / M | En az önerilen örneklem veya onaylı alternatif; uzman sonuçları ve kapı kararı belgeli. |
| E14-10 | Smart Study injection/boş kaynak/timeout E2E testlerini tamamla | P1 | A / M | Belge içi talimat, kötü link, sahte citation ve provider kesintisi güvenli davranıyor. |

### E15 — Deneme yönetimi, CSV/XLSX import ve düzeltme

**Bağımlılık:** E05/E06/E07/E11; format kararı pilot kurumdan. **Çıkış:** Deneme sonucu doğrulanarak yayınlanır ve açıklanabilir trende dönüşür.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E15-01 | Deneme, ders kapsamı, puanlama ve sonuç revizyon modelini kur | P1 | A / M | TYT/AYT, soru sayısı, test formu ve aktif revision açık; numeric net var. |
| E15-02 | Deneme create/list/detail API ve yetki kontrolünü yaz | P1 | A / M | Admin yalnız tenant denemesini yönetiyor; geçersiz tarih/ders/kapsam reddediliyor. |
| E15-03 | CSV şablonu, alan sözlüğü ve örnek pilot dosyayı hazırla | P1 | B / M | Öğrenci ref, ders kodu, net-only/ayrıntı farkı, encoding/ondalık kuralları açıklanmış. |
| E15-04 | Güvenli dosya upload ve import job/staging akışını kur | P1 | A / M | Boyut/satır sınırı, hash, private storage ve tekrar yükleme kontrolü var. |
| E15-05 | CSV parser ve normalize katmanını yaz | P1 | A / M | Türkçe UTF-8/BOM, delimiter, virgüllü sayı ve boş alan fixture’ları geçiyor. |
| E15-06 | Pilot gerekiyorsa XLSX streaming parser’ını ekle | P1 | A / M | Formül/makro çalışmıyor; zip/sheet/satır sınırı; CSV ile aynı validation sonucu. |
| E15-07 | Tenant içi student mapping ve duplicate çözümünü uygula | P1 | A / M | Öğrenci no eşleşmesi deterministik; belirsiz ad otomatik birleşmiyor. |
| E15-08 | Sonuç validasyonu ve sürümlü net hesaplamasını yaz | P1 | A / M | Adet toplamı/net aralığı doğru; net-only veriden soru outcome uydurulmuyor. |
| E15-09 | Preview/alan hatası/hata raporu API’sini oluştur | P1 | A / M | Satır/alan/güvenli kod var; hata export’u formula injection ve tenant açısından güvenli. |
| E15-10 | Deneme oluştur/listele/import önizleme UI’ını yap | P1 | B / L | Admin eşleşme ve hata sayısını onaydan önce görüyor; kısmi sessiz kabul yok. |
| E15-11 | Atomik publish ve outbox entegrasyonunu uygula | P1 | A / L | Tek active revision; double publish idempotent; işlem ortasında çökme yarım sonuç bırakmıyor. |
| E15-12 | Düzeltme/revizyon karşılaştırma/audit akışını kur | P1 | A / L | Önceki sonuç izleniyor; eşzamanlı düzeltme version check; affected-student rebuild oluşuyor. |
| E15-13 | Öğrenciye yayınlanmış sonuç ve ders trendi API’sini yap | P1 | A / M | Taslak görünmüyor; tarih/tür/soru kapsamına göre karşılaştırılabilirlik uygulanıyor. |
| E15-14 | Deneme ayrıntısı, publish onayı ve düzeltme UI’ını yap | P1 | B / M | Etkilenecek öğrenci/satır sayısı açık; yanlış revision overwrite uyarılı. |
| E15-15 | Topic-level analiz için soru-konu mapping ve item import’u ekle | P1 | A / L | Yalnız pilotta ayrıntı varsa etkin; mapping yoksa konu analizi kapalı, ders analizi çalışır. |
| E15-16 | İsteğe bağlı elle tekil sonuç girişini aynı validation’a bağla | P1 | B / M | Import dışı giriş de revision/audit üretir; hesaplama ikinci kez UI’da farklı yazılmıyor. |
| E15-17 | Import/revision/rollback test matrisi ve pilot provası yap | P1 | A / L | Bozuk XLSX, duplicate, geç yükleme, revocation, net-only ve A/B tenant fixture’ları geçiyor. |

### E16 — Öğrenci performansı ve çalışma geçmişi

**Bağımlılık:** E10/E11/E12/E15. **Çıkış:** Öğrenci kendi gelişimini kanıt, kapsam ve aksiyonla birlikte görür.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E16-01 | Performans ekranı veri contract ve dönem filtrelerini tanımla | P1 | B / M | Practice, task ve exam dönem/kaynak ayrımı; freshness ve örneklem alanları zorunlu. |
| E16-02 | Sınırlı/paginated performans read API’sini tamamla | P1 | A / M | N+1 ve sınırsız tarih aralığı yok; başka öğrenci kaynakları birleşmiyor. |
| E16-03 | Genel ilerleme kartları ve çalışma geçmişi UI’ını yap | P1 | B / M | Aktif süre ölçümünün yaklaşık olduğu ve platform kapsamı açık. |
| E16-04 | Weakness detayını practice ve göreve bağla | P1 | B / M | Konu, veri yeterliliği, güncellik ve gerekçe gösteriliyor; tek tıkla uygun çalışma. |
| E16-05 | Deneme netleri ve karşılaştırılabilir trend grafiklerini yap | P1 | B / M | TYT/AYT karışmıyor; 1–2 sonuçta trend iddiası yok; güçlük uyarısı görünür. |
| E16-06 | Veri düzeltmesi/invalid soru/freshness durumlarını göster | P1 | B / M | Eski verinin canlı gibi gösterilmesi yok; revision değişimi tutarlı. |
| E16-07 | Çalışma ve görev KPI contract’larını ekranla doğrula | P1 | A / M | Aynı metrik Home/Performance/kurum ekranında farklı paydayla isimsiz sunulmuyor. |
| E16-08 | Performans erişilebilirlik ve düşük veri E2E testlerini yaz | P1 | B / M | Renksiz/klavye kullanım, boş ders, stale konu, değişmiş hedef ve yetki testli. |

### E17 — Kurum dashboard’u ve toplu weakness analizi

**Bağımlılık:** E06/E11/E15/E16; temel özet E11 sonrası P0. **Çıkış:** Kurum güvenilir özetlerden destek ihtiyacına ulaşır.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E17-01 | Kurum metrik sözlüğü, dönem ve öğrenci paydalarını kesinleştir | P0 | O / M | Kayıtlı/koltuk/aktive/haftalık aktif ayrımı; sınıf değişimi ve tarihsel kohort kuralı açık. |
| E17-02 | Temel tenant öğrenci/aktivite aggregate API’sini yaz | P0 | A / M | İlk öğrenci çalışma döngüsü admin tarafından güvenli izlenebiliyor. |
| E17-03 | Kurum günlük/konu/deneme read modellerini oluştur | P1 | A / L | Ham cevapları her request’te hesaplamıyor; kaynak/freshness bilgisi var. |
| E17-04 | Ders/konu grup analizi ve yeterli veri coverage hesabını yaz | P1 | A / M | Öğrenci eşit ağırlık/politika net; çok çalışan tek kişi sınıfı domine etmiyor. |
| E17-05 | Dönem/grup/öğrenci durum filtresi ve indeksleri uygula | P1 | A / M | Tenant dışı grup ID’si reddediliyor; büyük dönem/sayfa sınırı var. |
| E17-06 | Admin dashboard kartlarını ve temel durumlarını yap | P0 | B / M | Veri yok/hesaplanıyor/başarısız ayrımı; ilk anlamlı kurum görünümü çalışır. |
| E17-07 | Ders/konu weakness ve performans trend ekranlarını yap | P1 | B / L | Örneklem/payda ve karşılaştırma kapsamı görünür; ulusal ortalama iddiası yok. |
| E17-08 | Admin öğrenci performans profilini geliştir | P1 | B / M | Kendi kurumundaki görev/deneme/aktivite özetleri; özel Smart Study prompt’ları yok. |
| E17-09 | Yetkili aggregate/öğrenci raporu export’u ekle | P1 | A / M | Erişim/audit/signed URL ve formula injection kontrolü; gereksiz kolonlar dışarıda. |
| E17-10 | Dashboard query plan, latency ve cache ihtiyacını ölç | P1 | A / M | 300 öğrencili fixture’da hedef ölçülmüş; Redis eklemeden önce darboğaz raporu var. |
| E17-11 | Kurum veri tutarlılığı ve negatif yetki testlerini yaz | P1 | A / M | UI/API/export aynı tenant/payda; pasif/grup değiştiren öğrenci senaryoları testli. |

### E18 — Risk değerlendirmesi ve müdahale iş akışı

**Bağımlılık:** E11/E12/E15/E17, E00-05. **Çıkış:** Açıklanabilir inceleme önerisi kayıtlı insan aksiyonuna dönüşür.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E18-01 | Sinyal/kapsam/eşik modelini uzmanla değerlendir | P1 | O / M | TRUE/FALSE/UNKNOWN, cold-start ve kısmi veri anlamları mutabık; tanı iddiası yok. |
| E18-02 | Risk assessment ve ayrı müdahale state şemasını oluştur | P1 | A / M | Kural/snapshot/dönem ve OPEN/RESOLVED iş akışı puandan ayrılmış. |
| E18-03 | İnaktivite/deneme düşüşü/kalıcı weakness/practice gap kurallarını yaz | P1 | A / L | Ağırlıklar ve tarih sınırları sürümlü/unit testli; aynı sinyal iki kez eklenmiyor. |
| E18-04 | Veri tazeliği, eksik kanıt ve genel kesinti kapısını uygula | P1 | A / M | Eksik veri HEALTHY olmuyor; outbox/servis kesintisi yanlış inaktivite alarmı üretmiyor. |
| E18-05 | Uyarı deduplication, cooldown ve yeniden değerlendirmeyi yaz | P1 | A / M | Aynı öğrenci+kural için açık uyarı güncelleniyor; spam yok. |
| E18-06 | İnceleme listesi ve kanıt detayı API’sini oluştur | P1 | A / M | Gerekçe, kapsam, son hesap ve ilgili veri bağlantısı tenant scoped. |
| E18-07 | Önceliklendirilmiş dikkat listesi UI’ını yap | P1 | B / M | Renk dışında açıklama; veri yetersiz öğrenciler ayrı; eylem önerisi net. |
| E18-08 | Müdahale oluştur/ata/ertele/çöz/takip API’sini uygula | P1 | A / M | Değişiklik audit’li; uyarı kapatmak puanı yapay sıfırlamıyor. |
| E18-09 | Müdahale formu, takip takvimi/listesi ve kısa not UI’ını yap | P1 | B / M | Akademik aksiyon odaklı; hassas gereksiz bilgi girmeme uyarısı ve yetki var. |
| E18-10 | Yanlış alarm geri bildirimi ve kural kalibrasyon raporu ekle | P1 | B / M | Kurum inceleme sonucu metric’e bağlanıyor; otomatik kontrolsüz ağırlık güncellemesi yok. |
| E18-11 | Uyarı sonrası aksiyon KPI ve süreli hatırlatma mekanizmasını kur | P1 | A / M | Aynı uyarı duplicate sayılmıyor; MVP bildirim tercihen uygulama içi. |
| E18-12 | Risk/müdahale E2E, cold-start ve privacy testlerini tamamla | P1 | A / L | Öğrenci başka not göremiyor; kısmi veri, iptal deneme ve stale metric senaryoları geçiyor. |

### E19 — Güvenlik, KVKK, içerik ve veri talepleri

**Bağımlılık:** E00 kararları; feature’larla paralel yürür. **Çıkış:** Gerçek veri öncesi temel yükümlülükler, yayın öncesi tamamlanmış güvenlik kapıları.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E19-01 | Veri envanteri, veri akış diyagramı ve tehdit modelini çıkar | P0 | A / M | Öğrenci→backend→AI/storage/provider aktarımı ve güven sınırları kayıtlı. |
| E19-02 | Hukuk uzmanıyla KVKK rol/dayanak/kurum sözleşmesini belirle | P0 | O / L | Veri sorumlusu/işleyen rolleri, amaç, erişim ve yükümlülükler yazılı; gerçek veri kapısı. |
| E19-03 | Yaşa uygun bilgilendirme ve gerekli temsilci sürecini tasarla | P0 | O / M | Hukuki değerlendirmeye dayalı süreç; ebeveyn paneli zorunlu kapsam haline gelmiyor. |
| E19-04 | Aydınlatma/onay sürüm ve kanıt kayıt akışını uygula | P0 | A / M | İlgili metin sürümü/zaman/dayanak kayıtlı; geri çekme ve değişen koşul davranışı tanımlı. |
| E19-05 | Provider/bölge/yurt dışı aktarım ve saklama koşullarını incele | P0 | O / M | AI/OIDC/storage/mail için gerekli sözleşme ve güvenli veri minimizasyonu onaylı. |
| E19-06 | İçerik izin/publish/geri çekme politikasını kesinleştir | P1 | O / M | MEB/ÖSYM/yayıncı hakları ayrı; izinsiz içerik ingestion/yayın kapısı kapalı. |
| E19-07 | Saklama/lifecycle ve silme kapsamını veri türü bazında yapılandır | P0 | A / M | Log/import/prompt/academic/audit/backup süreleri onaylı; otomatik temizlik tasarımı var. |
| E19-08 | Veri erişim/export talebi için doğrulama ve teslim akışını kur | P1 | A / L | Talep sahibinin kimliği doğrulanıyor; sadece kendi/yetkili verisi güvenli teslim ediliyor. |
| E19-09 | Silme/anonimleştirme talep orkestrasyonunu uygula | P1 | A / L | DB/objeler/türev metrik/job/cache kapsamlı; silinen veri replay ile geri gelmiyor. |
| E19-10 | Restore sonrasında silme taleplerini yeniden uygulama prosedürü yap | P1 | A / M | İzole restore fixture’ında silinmiş kullanıcı tekrar aktif veriye dönüşmüyor. |
| E19-11 | Güvenlik başlıkları/CSP/CSRF/XSS/CORS ve upload hardening uygula | P0 | A / L | Kötü HTML/dosya/URL senaryosu bloklu; wildcard credential CORS yok. |
| E19-12 | Audit şeması, erişimi, redaction ve tanılama log standardını uygula | P0 | A / M | Token, parola, öğrenci prompt’u log’da yok; güvenlik olayı yeterli metadata ile izleniyor. |
| E19-13 | Secret/dependency/image taraması ve düzeltme politikası kur | P0 | A / M | Kritik açık için release kapısı; istisna süreli/gerekçeli; secret rotation provası var. |
| E19-14 | Hesap/tenant güvenlik testi ve dış inceleme ihtiyacını değerlendir | P1 | O / M | Kritik/yüksek açıklar kapalı; çözülemeyen risk go/no-go kararında açıkça kayıtlı. |
| E19-15 | Veri olayı bildirimi, destek erişimi ve kurum offboarding runbook’unu yaz | P1 | O / M | Sorumlu/iletişim/hukuki değerlendirme/kanıt koruma ve en az yetki prosedürü test edilmiş. |

### E20 — Production, izleme, yedek ve işletim

**Bağımlılık:** E01/E02; gerçek veri için E19-02/05. **Çıkış:** Küçük ekip sistemi ölçebilir, dağıtabilir, durdurabilir ve geri yükleyebilir.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E20-01 | Compute/DB/storage/OIDC sağlayıcı ve ağ topolojisini seç | P0 | A / M | Bütçe, pgvector, bölge, private bağlantı ve tek-hata-noktası kabulü belgeli. |
| E20-02 | Minimum IaC ve dev/stage/prod config ayrımını oluştur | P0 | A / L | Ortamlar yeniden kurulabilir; state/secret erişimi sınırlı; prod veri stage’e kopyalanmıyor. |
| E20-03 | Non-root Docker image, health/readiness ve kaynak sınırlarını ekle | P0 | A / M | Backend/AI ayrı limit; sağlıksız instance’a trafik yok; imajda secret yok. |
| E20-04 | DNS/TLS, reverse proxy ve private servis/DB bağlantılarını kur | P0 | A / M | Yalnız gerekli portlar public; sertifika yenileme ve admin port sınırı testli. |
| E20-05 | GitHub image registry ve OIDC tabanlı deployment erişimini kur | P0 | A / M | Immutable SHA image; minimum cloud izinleri; production environment onaylı. |
| E20-06 | Stage deploy/migration/smoke ve production release pipeline’ını yaz | P0 | A / L | Migration failure deploy’u durduruyor; önceki uyumlu image’a dönüş testli. |
| E20-07 | Structured log/correlation ve temel metric panosunu kur | P0 | A / M | Request→job→AI ilişkilendiriliyor; PII ve yüksek cardinality etiket yok. |
| E20-08 | Error/latency/DB/job lag/provider/cost alarmlarını yapılandır | P0 | A / M | Alarm test bildirimi geliyor; eşiğin sahibi ve runbook bağlantısı var. |
| E20-09 | DB ve object şifreli off-host yedek/lifecycle politikasını uygula | P0 | A / M | Otomatik backup başarısı izleniyor; key erişimi ve version retention kayıtlı. |
| E20-10 | İzole DB+object restore tatbikatı yap ve RPO/RTO ölç | P0 | A / L | Uygulama restored veriyle çalışıyor; hedef karşılanmıyorsa kapı açılmıyor. |
| E20-11 | Provider down/outbox dead/import düzeltme runbook’larını hazırla | P1 | A / M | Diğer geliştirici belgeyle güvenli müdahale yapabiliyor. |
| E20-12 | Aylık maliyet dashboard’u ve bütçe stop/uyarılarını bağla | P0 | A / M | Provider ve altyapı maliyeti birlikte; başarısız iş/egress gideri görünür. |
| E20-13 | Capacity/query/worker yük testi ve ilk ayarları yap | P1 | A / M | API p95/freshness hedefleri ölçülmüş; kaynak ve concurrency limitleri gerekçeli. |
| E20-14 | On-call, bakım, incident, rollback ve secret rotation provası yap | P1 | O / M | İki kişilik sahiplik/iletişim açık; production için tek kişinin hafızasına bağımlılık yok. |

### E21 — Entegrasyon, yayın kapıları ve pilot yürütme

**Bağımlılık:** İlgili P0/P1 epikleri; test altyapısı E01’den başlar. **Çıkış:** Gerçek kullanıcıyla güvenli pilot ve kanıtlı devam/durdurma kararı.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E21-01 | Gereksinim→test→backlog izlenebilirlik matrisini oluştur | P0 | B / M | Her MVP özelliğinin pozitif/negatif/operasyon kabul örneği bağlı. |
| E21-02 | Deterministik API/AI/storage/mail test double’larını tamamla | P0 | A / M | CI testleri gerçek öğrenci verisi, mail veya LLM ücreti üretmiyor. |
| E21-03 | Davet→practice→metric→task→kurum E2E’yi merge kapısı yap | P0 | B / L | İki tenant ile taze ortamda geçiyor; test flaky ise gizlice bypass edilmiyor. |
| E21-04 | Import→trend→risk→müdahale E2E’yi tamamla | P1 | B / L | Düzeltme/rebuild ve açık uyarı lifecycle’ı dahil. |
| E21-05 | Smart Study→citation ve içerik geri çekme E2E’sini tamamla | P1 | B / M | Yetkisiz/silinmiş içerik ve boş retrieval güvenli; kaynak linki doğru. |
| E21-06 | Uygulama geneli negatif yetki/concurrency/fault paketi çalıştır | P1 | A / L | Tenant/ownership/job/export, duplicate cevap ve kota/publish yarışları geçiyor. |
| E21-07 | Erişilebilirlik, mobile web ve pilot tarayıcı uyumluluğunu doğrula | P1 | B / M | Kritik yol klavye ve hedef telefon/tarayıcılarla tamamlanabiliyor. |
| E21-08 | AI golden değerlendirme ve içerik yayın go/no-go’sunu yap | P1 | O / M | Kapsam/örneklem/model sürümüyle kalite hedefleri raporlanmış; kritik hata açık değil. |
| E21-09 | Güvenlik/privacy/backup/performance release checklist’ini imzala | P1 | O / M | Bölüm 15.3 kapılarında kanıt bağlantısı var; eksik zorunlu kontrolle pilot açılmıyor. |
| E21-10 | Kurum onboarding, yardım metni ve demo verisini hazırla | P1 | B / M | Admin öğrenci/davet/import/inceleme akışını yardımsız veya kısa rehberle tamamlıyor. |
| E21-11 | Önce küçük kohortla aç, sonra 100–300 öğrenciye kademele | P1 | O / L | Her adımda hata/kota/freshness gözleniyor; feature rollback/stop yolu hazır. |
| E21-12 | KPI dashboard ve haftalık öğrenci/admin görüşmelerini yürüt | P1 | B / L | Paydalar tutarlı; kullanım sorunu ve ürün değeri ayrı raporlanıyor. |
| E21-13 | Pilot hata triage, support ve yanlış alarm/AI geri bildirimini yönet | P1 | O / L | Öncelik/sahip/SLA beklentisi açık; kritik kalite sorunu hızlı karantinaya alınıyor. |
| E21-14 | 4–6 hafta sonunda teknik/ürün/ticari pilot raporu çıkar | P1 | O / M | Devam/daralt/durdur kararı; ücretli devam niyeti ve sonraki backlog veriyle gerekçeli. |

### E22 — MVP sonrası: AI Coach ve gelişmiş kişiselleştirme

**Tetikleyici:** P1 kullanımının sürdürülebilir olması; doğru ölçüm, yeterli geçmiş ve bütçe. **Bağımlılık:** E09/E11/E12/E15/E19/E21. Aşağıdaki işler onaylanmadan geliştirme taahhüdü değildir.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E22-01 | Coach kullanım senaryosu/kapsam ve maliyet discovery’si yap | P2 | O / M | Genel sohbet değil, veri temelli “bugün ne çalışayım?” gibi sınırlı işler seçilmiş. |
| E22-02 | Sürümlü student-context assembler ve açıklama provenance’ı kur | P2 | A / L | Weakness/task/exam/goal minimum context; tenant/ownership ve tarih kapsamı korunuyor. |
| E22-03 | Recommendation v2 için aday sıralama ve kısıt motoru geliştir | P2 | A / L | Öğrenci zaman bütçesi, önkoşul, tekrar ve açık görev sınırları testli. |
| E22-04 | Coach açıklama API’si, oturum ve güvenli cevap contract’ını tasarla | P2 | A / L | LLM görevi tek başına yaratmıyor; karar backend’de; düşük veri dürüst açıklanıyor. |
| E22-05 | Coach UI, kaynak/gerekçe ve kullanıcı geri bildirimini yap | P2 | B / L | Önerinin hangi veriye dayandığı görünür; riskli/uygunsuz iddia raporlanabiliyor. |
| E22-06 | Kişiselleştirilmiş haftalık plan ve öğrenci onay akışı geliştir | P2 | A / L | Plan önerisi onaylanmadan mevcut görevleri değiştirmiyor; yeniden planlama idempotent. |
| E22-07 | Zaman planı/erteleme/yeniden planlama UI’ını geliştir | P2 | B / L | Aşırı görev yükü, tatil ve öğrencinin tercihleri yönetilebilir; baskıcı bildirim yok. |
| E22-08 | Kişiselleştirilmiş bildirim tercih/sessiz saat/dedup modelini kur | P2 | A / L | İzin ve unsubscribe; uygulama/mail kanal limiti; hassas veri bildirim başlığında yok. |
| E22-09 | Üniversite/bölüm hedefi için veri kaynağı ve metodoloji araştır | P2 | O / L | Resmi/güvenilir kaynak, yıl/sürüm/hak kaydı; net→sıralama belirsizliği açık. |
| E22-10 | Hedef bazlı gelişim senaryosu ve veri yeterliliği API’sini geliştir | P2 | A / L | Kesin kabul/sıralama garantisi yok; hedef açığı gerekçe ve varsayımla sunuluyor. |
| E22-11 | Hedef seçimi ve senaryo karşılaştırma UI’ını yap | P2 | B / L | Eski yıl verisi/güçlük/eksik AYT uyarıları görünür; resmi tercih hizmeti gibi sunulmuyor. |
| E22-12 | Coach/plan kalite, maliyet ve etki değerlendirmesini yap | P2 | O / L | Baseline’a karşı fayda ölçülmüş; düşük fayda/yüksek maliyette özellik durdurulabiliyor. |

### E23 — MVP sonrası: sınav trendleri, benchmark ve veri zekâsı

**Tetikleyici:** Hakları net veri, yeterli kurum/kohort ve güvenilir longitudinal ölçüm. **Bağımlılık:** E05/E11/E15/E19/E21. Pseudonymous kimlik anonimlik değildir.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E23-01 | Geçmiş YKS soruları/metadatası için hak ve veri kalite incelemesi yap | P2 | O / L | Kaynak/yıl/sürüm/izin doğrulanmadan toplama veya yeniden yayın yok. |
| E23-02 | Tarihsel soru→konu taxonomy mapping ve uzman review akışını kur | P2 | A / L | Müfredat değişimi, çok konulu soru ve belirsiz mapping sürümlü. |
| E23-03 | Konu sıklığı/ağırlığı analiz API ve görünümlerini yap | P2 | B / L | Payda/yıl aralığı ve belirsizlik görünür; “bu yıl çıkacak” kesin tahmini yok. |
| E23-04 | Tarihsel sıklığı öneri motoruna kontrollü girdi olarak ekle | P2 | A / L | Öğrencinin ihtiyacı ve mevcut müfredat öncelikli; tek sinyal bütün planı belirlemiyor. |
| E23-05 | Kurum benchmark için izin/kohort/anonimleştirme tasarımını yap | P3 | O / L | Minimum örneklem, kurum sayısı, küçük hücre/dominance bastırma ve yeniden tanımlama incelemesi onaylı. |
| E23-06 | İzole aggregate pipeline ve benchmark read modelini geliştir | P3 | A / L | Ham tenant öğrenci verisi diğer kuruma açılmıyor; çıkarım/differencing testleri var. |
| E23-07 | Benchmark UI, karşılaştırılabilirlik ve örneklem uyarılarını yap | P3 | B / L | İzinli kohort dışında karşılaştırma yok; yetersiz hücreler gösterilmiyor. |
| E23-08 | Ulusal Weakness Map kapsamı ve temsiliyet değerlendirmesi yap | P3 | O / L | Kullanıcı örneklemi Türkiye geneliymiş gibi sunulmuyor; yayın için yeterlilik kapısı var. |
| E23-09 | Gelişmiş trend/segment/cohort analitiklerini tasarla ve doğrula | P3 | A / L | Ölçüm tanımı, bias ve veri kalitesi izleniyor; warehouse ihtiyacı önce ölçülüyor. |
| E23-10 | Predictive ML fizibilite, label, baseline ve adalet değerlendirmesi yap | P3 | O / L | Rule-based sisteme karşı doğrulanmış fayda yoksa model üretime alınmıyor; insan denetimi korunuyor. |

### E24 — MVP sonrası: kurum ölçeği, gelir ve yeni deneyimler

**Tetikleyici:** Pilot sonrası müşteri ihtiyacı ve ücretli kullanım. **Bağımlılık:** E03/E06/E08/E19/E21. Her `L` iş keşif sonrasında UI/API/veri/test alt issue’larına bölünür.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E24-01 | Self-service B2C kayıt ve Free/Premium haklarını aç | P2 | A / L | Personal tenant, abuse önleme, kota ve kuruma katılma sınırları doğrulanmış. |
| E24-02 | Ödeme sağlayıcısı/webhook/abonelik yenileme entegrasyonunu yap | P2 | A / L | İmzalı webhook, replay/idempotency, failed payment, iptal/iade ve ledger mutabakatı testli. |
| E24-03 | Plan/ödeme/iptal/yenileme müşteri UI’ını geliştir | P2 | B / L | Fiyat/kota/süre şeffaf; dark pattern yok; fatura/vergi süreçleri uzmanla bağlı. |
| E24-04 | Çoklu şube veri sınırı ve branch admin görünürlüğünü tasarla | P3 | A / L | Tenant/branch ayrımı açık; şube değişimi ve merkez toplamı ayrı yetki testli. |
| E24-05 | Teacher/Guidance Counselor/Branch Manager policy modelini kur | P3 | A / L | Ders/sınıf/öğrenci ataması ve least privilege; rol mirası sızıntı üretmiyor. |
| E24-06 | Öğretmen/rehberlik için atanan öğrenci ve müdahale panelini yap | P3 | B / L | Yalnız atanmış kapsam; öğretmen öğrenci yerine cevap yazamıyor; aksiyon audit’li. |
| E24-07 | Veli bağlantısı, izin, görünürlük ve ilişki sonlandırma modelini tasarla | P3 | O / L | Hukuki değerlendirme, öğrenci mahremiyeti ve yanlış kişiye rapor önleme kapısı var. |
| E24-08 | Ebeveyn özet raporu ve tercih/teslim UI’ını geliştir | P3 | B / L | Anlamlı dönem özeti; sürekli gözetim/ham prompt paylaşımı yok; iptal ve yetki testli. |
| E24-09 | Kurumsal SIS/API entegrasyonu ve scoped API erişimini tasarla | P3 | A / L | Tenant-bound scope, rate limit, idempotent import, credential rotation ve audit var. |
| E24-10 | Enterprise özel rapor, SLA ve white-label gereksinimini doğrula | P3 | O / L | Müşteriye özgü branch/fork yerine kontrollü config; support/maliyet karşılığı net. |
| E24-11 | Onaylanmış branding/özel rapor konfigürasyonunu uygula | P3 | B / L | Tenant bazlı logo/domain/template yetkili; script/HTML injection ve veri sızıntısı yok. |
| E24-12 | AYT genişlemesini ve sonra LGS/KPSS kapsamını ayrı planla | P2 | O / L | AYT pilotta değilse ilk genişleme; diğer sınavlar P3 kararı; içerik/puanlama/evaluation ayrı doğrulanıyor. |
| E24-13 | Mobil ihtiyaç discovery’si ve PWA/native kararını çıkar | P3 | B / L | Web kullanım verisiyle gerekçe; offline veri/token/senkronizasyon ve bütçe değerlendirilmiş. |
| E24-14 | Onaylanırsa mobil deneyimin kritik dikey dilimini geliştir | P3 | B / L | Login→task→practice→sonuç; cihaz depolama, erişilebilirlik ve veri tutarlılığı testli. |
| E24-15 | Gamification/sosyal/chat taleplerini ihtiyaç ve güvenlik açısından değerlendir | P3 | O / M | Öğrenmeye ölçülebilir katkı ve çocuk güvenliği olmadan geliştirme yok; MVP dışı kalır. |

### E25 — İhtiyaç odaklı altyapı büyütme backlog’u

**Tetikleyici:** Bölüm 14.6’daki ölçülmüş darboğaz. **Bağımlılık:** E20/E21 performans ve maliyet verileri. Teknoloji kullanmış olmak tek başına gerekçe değildir.

| ID | İş / teslimat | Öncelik | Sahip / boyut | Ek kabul çıktısı |
| --- | --- | --- | --- | --- |
| E25-01 | Darboğaz/alternatif/toplam sahip olma maliyeti ADR’si çıkar | P3 | A / M | Önce/sonra ölçüm ve daha basit alternatif karşılaştırılmış; upgrade go/no-go açık. |
| E25-02 | Worker’ı ayrı runtime profiline ve bağımsız ölçeğe taşı | P3 | A / L | Aynı job/lease sözleşmesi; çift iş etkisi yok; API latency iyileşmesi ölçülmüş. |
| E25-03 | Ölçülmüş cache ihtiyacında Redis ve invalidation uygula | P3 | A / L | Tenant-aware anahtar, TTL, stampede, stale izin ve cache down testleri geçiyor. |
| E25-04 | DB read replica/partition ihtiyacını değerlendir ve gerekirse uygula | P3 | A / L | Replica lag/read-your-write davranışı açık; yazma/ölçüm tutarlılığı bozulmuyor. |
| E25-05 | Queue veya Kafka/MSK geçişini outbox contract’ıyla yap | P3 | A / L | Consumer dedup, replay, DLQ, schema uyumu ve cutover/rollback ölçülmüş. |
| E25-06 | Analytics store/warehouse ve güvenli CDC/ETL kur | P3 | A / L | Tenant/izin/silme politikası türev depoya taşınıyor; kaynak mutabakatı ve lag izleniyor. |
| E25-07 | pgvector tuning sonrası ayrı vector store fizibilitesini yap | P3 | A / L | Recall/latency/maliyet ve erişim filtresi eşdeğeri kanıtlı; migration geri dönüş planlı. |
| E25-08 | Gerçek ihtiyaca göre managed runtime veya EKS değerlendirmesini yap | P3 | A / L | Operasyon/autoscale/HA faydası iki kişilik bakım maliyetini karşılıyor; otomatik zorunluluk yok. |
| E25-09 | Seçilirse GitOps/ArgoCD ve rollout politikalarını kur | P3 | A / L | Env drift, rollout, rollback, secret ve cluster RBAC testli; tek deployment kaynağı net. |
| E25-10 | OTel tracing ve merkezi metrik/log stack’ini genişlet | P3 | A / L | Correlation sürekliliği, sampling, PII temizliği ve maliyet sınırları testli. |
| E25-11 | Yalnız gerekçeli domain’i bağımsız servise ayır | P3 | A / L | Ownership/veri sınırı, contract, failure ve distributed transaction bedeli ADR ile doğrulanmış. |
| E25-12 | SLA gerektirirse HA/multi-region felaket kurtarmayı tasarla | P3 | A / L | Veri yerleşimi, RPO/RTO, maliyet, failover ve split-brain senaryosu tatbikatla doğrulanmış. |

### 17.2. Özellik → backlog izlenebilirliği

| Ürün veya teknik ihtiyaç | Ana görev grupları | Tamamlanma kanıtı |
| --- | --- | --- |
| Kimlik, tenant, B2C/B2B2C sınırı | E02, E03, E19; self-service için E24 | Personal + iki kurum + öğrenci sahipliği negatif testi. |
| Student Profile / Onboarding | E05, E06 | Davet→profil→ilk çalışma akışı ve activation ölçümü. |
| AI Practice Lab | E07–E10 | Doğrulanmış soru→ilk cevap→kalıcı sonuç; kesinti/duplicate testleri. |
| Weakness Map | E11, E16 | Örneklem/güncellik/sürümle izlenebilir topic göstergesi. |
| My Tasks / Home | E12 | Kanıtlı öneri→uygun practice→idempotent completion. |
| MEB / Smart Study | E13, E14, E19-06 | İçerik hakkı→ingestion→sayfa retrieval→doğru citation ve kalite kapısı. |
| Deneme yönetimi/akıllı analiz | E15, E16 | Doğrulanmış publish/revision; yeterli veriyle ders/konu analizi. |
| Kurum öğrenci yönetimi | E06, E08 | Davet/koltuk/grup/lifecycle; tenant dışı erişim reddi. |
| Institution Dashboard / Weakness Analysis | E17 | Doğru paydalı aggregate, filtreli performans ve taze veri göstergesi. |
| Risk / Student Monitoring / Guidance Workflow | E18 | Gerekçeli uyarı→insan incelemesi→müdahale→takip. |
| B2B gelir, kota ve ticari pilot | E00-07, E08, E21 | Kullanım maliyeti ve koltuk kontrolü; ücretli devam kararı. |
| Güvenlik/operasyon/yayın | E01, E02, E19–E21 | İzolasyon, CI, restore, alarm, yük ve yayın checklist kanıtları. |
| Coach / plan / hedefleme | E22 | Kişisel structured context ile açıklanan deterministik öneri; ölçülmüş fayda. |
| Geçmiş sınav / benchmark / national map | E23 | Hak, temsil, anonimlik ve veri yeterliliği kapıları. |
| Roller/şube/veli/gelir genişleme/mobil | E24 | Onaylı ihtiyaç, yetki modeli, kalite ve ticari gerekçe. |
| Kafka/Redis/EKS/warehouse/OTel/microservices | E25 | Ölçülmüş darboğaz ve doğrulanmış maliyet-fayda. |

### 17.3. Kritik bağımlılık hatları

- **Öğrenci çekirdeği:** E00 → E01 → E02/E03 → E05 → E07/E08/E09 → E10 → E11 → E12.
- **Kurum aksiyon döngüsü:** E03/E05/E08 → E06 → E15 → E17 → E18.
- **Kaynaklı öğrenme:** K-04/E19-06 → E13 → E14; E07/E08/E09 temel altyapısı ortak.
- **Gerçek veri kapısı:** E19-02/03/05 + gerekli E03 kontrolleri + E20-09/10 tamamlanmadan sentetik ortamdan gerçek öğrenciye geçilmez.
- **Tam MVP/pilot yayını:** Uygulanabilir P0/P1 kapsamı + E19/E20 kalite kapıları → E21-09 → E21-11.
- **Yeni yatırım kapısı:** E21-14 sonucu → ihtiyaç kanıtına göre E22/E23/E24/E25; otomatik olarak bütün P2/P3’e başlama yok.

### 17.4. Backlog’un sprint’e alınmadan önce tamamlanacak kontrolü

- [ ] Pilot sınav/ders, kurum paketi ve içerik kapsamı kararlarla uyumlu mu?
- [ ] Her görevde tek DRI, açık kabul örneği ve bağımlılık var mı?
- [ ] L işler API, UI, veri, test ve operasyon alt issue’larına ayrıldı mı?
- [ ] Gerçek öğrenci verisi, içerik hakkı veya dış sağlayıcı gerektiren işin kapısı açık mı?
- [ ] Yetki, idempotency, quota, error/empty/insufficient-data ve audit etkisi ele alındı mı?
- [ ] Özellik için kullanıcıya gösterilecek ilk uçtan uca demo tanımlı mı?
- [ ] Ekip kapasitesinde review, test, içerik incelemesi ve operasyon payı var mı?
- [ ] P2/P3 işi gerçekten ölçülmüş ihtiyaçtan mı geliyor; MVP teslimini gereksiz geciktiriyor mu?







