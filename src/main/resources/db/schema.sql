CREATE TABLE IF NOT EXISTS notification_requests
(
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    recipient_id      BIGINT          NOT NULL,
    notification_type VARCHAR(64)     NOT NULL,
    channel           VARCHAR(16)     NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    idempotency_key   VARCHAR(512)    NOT NULL,
    reference_type    VARCHAR(64),
    reference_id      BIGINT,
    payload           JSON,
    scheduled_at      DATETIME,
    expire_at         DATETIME,
    retry_count       INT             NOT NULL DEFAULT 0,
    max_retry_count   INT             NOT NULL DEFAULT 3,
    next_retry_at     DATETIME,
    sent_at           DATETIME,
    failure_reason    TEXT,
    is_read           TINYINT(1)      NOT NULL DEFAULT 0,
    read_at           DATETIME,
    version           BIGINT          NOT NULL DEFAULT 0,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    INDEX idx_status_next_retry   (status, next_retry_at),
    INDEX idx_status_updated_at   (status, updated_at),
    INDEX idx_recipient_read      (recipient_id, is_read, status),
    INDEX idx_status_scheduled_at (status, scheduled_at),
    INDEX idx_status_expire_at    (status, expire_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS notification_registration_failures
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    recipient_id      BIGINT      NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    channel           VARCHAR(16) NOT NULL,
    reference_type    VARCHAR(64),
    reference_id      BIGINT,
    failure_reason    TEXT,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_recipient_id (recipient_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;