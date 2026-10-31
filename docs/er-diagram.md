# SocialPulse ER Diyagramı (Revizyon 2)

Gün 5'te gönderi bazlı `posts` tablosu kaldırıldı, yerine hesap seviyesinde `metric_points` geldi.
`upload_errors` tablosu V1'de oluşturuldu, Gün 9'da kullanılacak.

```mermaid
erDiagram
    USERS ||--o{ UPLOAD_BATCHES : uploads
    USERS ||--o{ METRIC_POINTS : owns
    USERS ||--o{ REPORTS : generates
    UPLOAD_BATCHES ||--o{ METRIC_POINTS : contains
    UPLOAD_BATCHES ||--o{ UPLOAD_ERRORS : has

    USERS {
        bigint id PK
        string email UK
        string password_hash
        string full_name
        timestamptz created_at
    }
    UPLOAD_BATCHES {
        bigint id PK
        bigint user_id FK
        string platform
        string original_filename
        string status
        int total_rows
        int imported_rows
        int rejected_rows
        timestamptz uploaded_at
    }
    UPLOAD_ERRORS {
        bigint id PK
        bigint batch_id FK
        int row_number
        string field
        string message
        text raw_line
    }
    METRIC_POINTS {
        bigint id PK
        bigint user_id FK
        bigint batch_id FK
        string platform
        string metric
        string granularity
        date period_start
        numeric value
        timestamptz created_at
    }
    REPORTS {
        bigint id PK
        bigint user_id FK
        string type
        text filters_json
        timestamptz created_at
    }
```

`metric_points` benzersiz kısıtı: `(user_id, platform, metric, granularity, period_start)`.
