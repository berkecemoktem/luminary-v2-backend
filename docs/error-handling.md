# Exception handling

Uygulamanın controller, servis ve Spring Security hataları tek bir RFC 9457
Problem Details sözleşmesiyle döner. Frontend HTTP durumuna ek olarak kararlı
`code` alanını kullanmalıdır; kullanıcıya gösterilecek metin gerektiğinde
`detail`, form hataları için `fields` okunabilir.

## Hata cevabı

```json
{
  "type": "https://luminary.dev/problems/invitation-expired",
  "title": "Conflict",
  "status": 409,
  "detail": "This invitation has expired.",
  "instance": "/api/v1/invitations/accept",
  "timestamp": "2026-09-17T12:00:00Z",
  "code": "invitation-expired",
  "severity": "ERROR",
  "traceId": "f0ad18bc-8326-4cf5-999d-a700d978dc8f"
}
```

Validation hatalarında response'a alan bazlı `fields` eklenir:

```json
{
  "status": 422,
  "code": "validation-failed",
  "severity": "ERROR",
  "fields": {
    "take": "must be less than or equal to 100",
    "filters[0].field": "must not be blank"
  }
}
```

Her response'taki `X-Request-Id` header'ı ile hata body'sindeki `traceId` aynı
değerdir. Destek ve log araştırmalarında bu değer kullanılmalıdır.

## İş kuralı hatası oluşturma

Projede örnekteki `GeneralException` ihtiyacını `ApiException` karşılar.
`RuntimeException` olduğu için controller veya servis imzasına `throws`
eklenmez.

```java
throw ApiException.notFound(
        "branch-not-found",
        "Branch was not found.");
```

Hazır factory metotları:

- `badRequest`: İstek sözdizimi veya doğrudan input hatası, `400`
- `unauthorized`: Kimlik doğrulama gerekli/geçersiz, `401`
- `forbidden`: Kullanıcının işlem yetkisi yok, `403`
- `notFound`: Kaynak bulunamadı, `404`
- `conflict`: Mevcut durumla çakışma, `409`
- `unprocessable`: Geçerli biçimdeki istek işlenemiyor, `422`
- `validation`: Alan bazlı `422` hatası
- `internal`: Kontrollü sunucu hatası, `500`
- `of`: Özel bir `HttpStatus` gerektiğinde

Alan bazlı iş kuralı doğrulaması:

```java
throw ApiException.validation(
        "registration-invalid",
        "Registration could not be completed.",
        Map.of("email", "This email is already registered."));
```

## İş kuralı uyarısı

İşlem tamamlanamıyor fakat durum frontend tarafından düzeltilebilir veya
kullanıcı onayı gerektiriyorsa `ApiWarningException` kullanılabilir:

```java
throw ApiWarningException.conflict(
        "active-session-exists",
        "An active session already exists.");
```

Bu cevaplarda `severity` değeri `WARNING` olur. İşlem başarıyla tamamlanıyorsa
exception atılmamalı; bilgilendirme başarılı response DTO'sunda taşınmalıdır.

## Otomatik karşılanan hatalar

| Durum | HTTP | `code` |
| --- | --- | --- |
| Bean Validation / binding | `422` | `validation-failed` |
| Okunamayan JSON body | `400` | `malformed-body` |
| Yanlış path/query parametre tipi | `400` | `invalid-parameter` |
| Eksik query parametresi | `400` | `missing-parameter` |
| `IllegalArgumentException` | `400` | `invalid-argument` |
| Hatalı kullanıcı bilgileri | `401` | `invalid-credentials` |
| Authentication gerekli | `401` | `authentication-required` |
| Yetki veya CSRF hatası | `403` | `access-denied` |
| Endpoint bulunamadı | `404` | `resource-not-found` |
| Veritabanı unique/FK çakışması | `409` | `data-conflict` |
| Desteklenmeyen HTTP metodu | `405` | `method-not-allowed` |
| Desteklenmeyen content type | `415` | `media-type-not-supported` |
| Beklenmeyen hata | `500` | `internal-error` |

Beklenen `4xx` iş hatalarının stack trace'i tekrar tekrar loglanmaz. Business
warning'ler `WARN`, beklenmeyen hatalar stack trace ile `ERROR` seviyesinde
merkezi handler tarafından loglanır. Exception constructor'larında loglama
yapılmamalıdır.

## Frontend davranışı

- UI kararları çevrilebilir `detail` metnine değil `code` değerine bağlanmalı.
- `fields` varsa ilgili form kontrollerine dağıtılmalı.
- `401` alındığında oturum yenileme/login akışı başlatılmalı.
- `403`, login tekrarı yerine yetkisiz işlem olarak gösterilmeli.
- `WARNING`, kullanıcıya uyarı görünümüyle sunulabilir.
- `500` durumunda genel hata mesajı gösterilip `traceId` destek bilgisi olarak
  saklanmalıdır.
