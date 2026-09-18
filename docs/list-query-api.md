# Pagination, filtreleme ve sıralama API'si

Bu sözleşme tablo ve liste ekranlarının aynı istek/cevap yapısını kullanması
içindir. Listeleme endpoint'leri `POST` kabul eder ve body olarak ortak
`SearchRequest` modelini alır.

İlk uygulama:

```http
POST /api/v1/me/workspaces
Content-Type: application/json
```

## İstek yapısı

```json
{
  "skip": 0,
  "take": 20,
  "filters": [],
  "sorts": []
}
```

| Alan | Açıklama | Varsayılan | Sınır |
| --- | --- | --- | --- |
| `skip` | Baştan atlanacak kayıt sayısı | `0` | En az `0` |
| `take` | Döndürülecek maksimum kayıt sayısı | `20` | `1-100` |
| `filters` | Birlikte `AND` ile uygulanan filtreler | `[]` | En fazla `10` |
| `sorts` | Öncelik sırasına göre sıralamalar | `[]` | En fazla `3` |

Alanların tamamı opsiyoneldir. Varsayılanlarla sorgu atmak için boş body
gönderilebilir:

```json
{}
```

### Filtre biçimleri

Tek değer alan operatörler `value` kullanır:

```json
{
  "field": "name",
  "operator": "CONTAINS",
  "value": "luminary"
}
```

Birden fazla değer alan `IN` ve `BETWEEN` operatörleri `values` kullanır:

```json
{
  "field": "type",
  "operator": "IN",
  "values": ["INSTITUTION", "PERSONAL"]
}
```

```json
{
  "field": "joinedAt",
  "operator": "BETWEEN",
  "values": [
    "2026-01-01T00:00:00Z",
    "2026-12-31T23:59:59Z"
  ]
}
```

`BETWEEN` tam olarak iki değer bekler. `IS_NULL` ve `IS_NOT_NULL` için
`value` veya `values` gönderilmez.

### Operatörler

| Operatör | Kullanım |
| --- | --- |
| `EQUALS`, `NOT_EQUALS` | Metin, enum, ID, sayı, boolean veya tarih eşitliği |
| `CONTAINS` | Metnin değeri herhangi bir yerinde içermesi |
| `STARTS_WITH` | Metnin verilen değerle başlaması |
| `GREATER_THAN`, `GREATER_THAN_OR_EQUAL` | Sayı veya tarih alt sınırı |
| `LESS_THAN`, `LESS_THAN_OR_EQUAL` | Sayı veya tarih üst sınırı |
| `IN` | Alanın verilen değerlerden biri olması |
| `BETWEEN` | Sayı veya tarihin iki değer arasında olması |
| `IS_NULL`, `IS_NOT_NULL` | Null kontrolü |

Her endpoint bütün operatörleri desteklemek zorunda değildir. Kullanılabilir
alanlar ve operatörler endpoint bazında allow-list ile belirlenir.

`CONTAINS` ve `STARTS_WITH` büyük/küçük harf duyarsızdır. `%` ve `_` gibi SQL
LIKE karakterleri normal metin olarak ele alınır. Tarihler ISO-8601 formatında
gönderilmelidir. Enum değerleri API'de tanımlandığı biçimde, örneğin
`INSTITUTION`, kullanılmalıdır.

### Sıralama biçimi

```json
{
  "sorts": [
    {"field": "type", "direction": "ASC"},
    {"field": "name", "direction": "DESC"}
  ]
}
```

Sıralamalar dizideki sırayla uygulanır. `direction` yalnızca `ASC` veya `DESC`
olabilir. İstemci sıralama göndermediğinde endpoint'in varsayılan sıralaması
kullanılır. Backend sonuçların sayfalar arasında yer değiştirmemesi için son
sıralama olarak benzersiz bir alanı otomatik ekler.

## Cevap yapısı

```json
{
  "items": [
    {
      "tenantId": "luminary-demo",
      "tenantName": "Luminary Demo",
      "tenantType": "INSTITUTION",
      "role": "STUDENT",
      "status": "ACTIVE"
    }
  ],
  "skip": 0,
  "take": 20,
  "totalItems": 37,
  "hasNext": true
}
```

- `items`: İstenen sayfadaki kayıtlar.
- `skip` ve `take`: Uygulanan pagination değerleri.
- `totalItems`: Filtrelere uyan toplam kayıt sayısı.
- `hasNext`: Bir sonraki sayfada kayıt olup olmadığı.

Sonraki sayfa için frontend `skip + take` değerini kullanabilir. Önceki sayfa
için `max(0, skip - take)` kullanılmalıdır.

## Workspace sorgusu

`POST /api/v1/me/workspaces` aşağıdaki sorgu alanlarını açar:

| Sorgu alanı | Filtre | Sıralama |
| --- | --- | --- |
| `tenantId` | `EQUALS`, `IN` | Hayır |
| `name` | `EQUALS`, `CONTAINS`, `STARTS_WITH` | Evet |
| `type` | `EQUALS`, `NOT_EQUALS`, `IN` | Evet |
| `role` | `EQUALS`, `NOT_EQUALS`, `IN` | Evet |
| `joinedAt` | Eşitlik, karşılaştırma ve `BETWEEN` | Evet |

Sorgu alan adları response alan adlarından farklı olabilir. Örneğin filtrede
`name` kullanılırken response içinde karşılığı `tenantName` olarak döner.

Arama, filtreleme ve sıralamayı birlikte kullanan örnek:

```json
{
  "skip": 0,
  "take": 25,
  "filters": [
    {
      "field": "name",
      "operator": "CONTAINS",
      "value": "ankara"
    },
    {
      "field": "type",
      "operator": "IN",
      "values": ["INSTITUTION"]
    }
  ],
  "sorts": [
    {
      "field": "name",
      "direction": "ASC"
    }
  ]
}
```

## Frontend kullanım notları

- Arama metni değiştiğinde `skip` tekrar `0` yapılmalıdır.
- Filtre, sıralama veya sayfa boyutu değiştiğinde de `skip` sıfırlanmalıdır.
- Yazdıkça arama için yaklaşık `300 ms` debounce kullanılması önerilir.
- Yeni arama başladığında önceki bekleyen HTTP isteği iptal edilmelidir.
- Boş arama metni için `CONTAINS` filtresi göndermek yerine ilgili filtre
  listeden kaldırılmalıdır.
- Sayfalama kontrolleri `totalItems` ve `hasNext` üzerinden kurulabilir.
- Filtre ve sıralama alanları serbest kullanıcı girdisi değil, endpoint'in
  dokümante edilmiş alanlarından seçilmelidir.

## Hatalar

Geçersiz istekler projenin RFC 9457 Problem Details yapısıyla döner. Örnek:

```json
{
  "type": "https://luminary.dev/problems/unsupported-filter-field",
  "title": "Bad Request",
  "status": 400,
  "detail": "Filtering by field 'unknown' is not supported.",
  "instance": "/api/v1/me/workspaces",
  "timestamp": "2026-09-17T12:00:00Z",
  "code": "unsupported-filter-field",
  "severity": "ERROR",
  "traceId": "..."
}
```

Başlıca hata kodları:

- `unsupported-filter-field`
- `unsupported-filter-operator`
- `unsupported-sort-field`
- `invalid-filter-value`
- `validation-failed`
- `malformed-body`

## Yeni bir backend listelemesine ekleme

Yeni bir listeleme için repository `JpaSpecificationExecutor` kullanmalı,
endpoint'e özel güvenli alanlar `QuerySchema` ile tanımlanmalı ve istemci
filtreleri `JpaQueryBuilder` ile çevrilmelidir. Kullanıcı, tenant ve yetki
kısıtları istemciden alınmamalı; servis tarafından ayrı bir `Specification`
olarak eklenip istemci filtreleriyle `AND` yapılmalıdır.
