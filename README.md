# BFF Application

## 概要

本アプリケーションは **Backend for Frontend (BFF)** パターンを実装したAPIサーバーです。

BFFとは、フロントエンドクライアント（WebブラウザやモバイルApp）ごとに最適化されたAPIを提供するサーバー層です。
複数のマイクロサービスへの呼び出しを集約し、クライアントが必要なデータだけを効率よく返す役割を担います。

### 解決する課題

| 課題 | BFFによる解決策 |
|------|--------------|
| WebとモバイルでAPIのレスポンスを変えたい | `X-Client-Type` ヘッダーで返すフィールドを切り替え |
| 1画面に複数サービスのデータが必要で遅い | 複数サービスを**並列呼び出し**して1レスポンスにまとめる |
| 一部のサービスが落ちるとすべて動かなくなる | サービス障害時の**フォールバック**で部分的に返す |
| バックエンドへのリクエストが多すぎる | BFF層での**キャッシュ**でバックエンド負荷を軽減 |
| 各クライアントで認証処理がバラバラ | **JWT認証**をBFFで一元管理 |

### システム構成

```
[Web ブラウザ]  ──┐                     ┌──▶ 商品サービス
                  ├──▶ [BFF :8080] ──┤
[モバイルアプリ] ──┘                     └──▶ レビューサービス
```

## 技術スタック

- **言語**: Kotlin 1.9
- **フレームワーク**: Spring Boot 3.3.5 (WebFlux)
- **ビルドツール**: Gradle 8.10.2 (Kotlin DSL)
- **Java**: 21
- **コンテナ**: Docker / Docker Compose
- **キャッシュ**: Caffeine
- **認証**: JWT (jjwt 0.12.6)

## アーキテクチャ

```
[Web ブラウザ]  ──┐
                  ├──▶ [BFF :8080] ──▶ 商品サービス (mock)
[モバイルアプリ] ──┘                 └──▶ レビューサービス (mock)
```

### BFFの役割

| 機能 | 説明 |
|------|------|
| APIの集約 | 商品・レビューを1リクエストにまとめる |
| レスポンス変換 | Web用（全フィールド）/ モバイル用（軽量）で返すデータを変える |
| 並列呼び出し | 複数サービスを同時に呼び出して高速化 |
| 認証・認可 | JWTトークン検証・ロールベースアクセス制御 |
| エラーハンドリング | サービスダウン時のフォールバック・タイムアウト制御 |
| キャッシュ | レビューデータをBFFでキャッシュしてバックエンド負荷を軽減 |

## 起動方法

### ローカル起動

```bash
./gradlew bootRun
```

### Docker起動

```bash
# ビルド＋起動
docker compose up -d --build

# ログ確認
docker compose logs -f bff

# 停止
docker compose down
```

## API一覧

### 認証

| メソッド | パス | 説明 | 認証 |
|--------|------|------|------|
| POST | `/api/auth/login` | ログイン・JWTトークン取得 | 不要 |

**リクエスト例:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}'
```

**レスポンス例:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "user",
  "role": "USER",
  "expiresIn": 3600
}
```

### 商品

| メソッド | パス | 説明 | 認証 |
|--------|------|------|------|
| GET | `/api/products` | 商品一覧 | 必要 |
| GET | `/api/products/{id}` | 商品詳細（レビュー含む） | 必要 |

**クライアント別レスポンスの切り替え:**

```bash
# Web用（全フィールド）
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer {token}"

# モバイル用（軽量）
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer {token}" \
  -H "X-Client-Type: mobile"
```

**障害シミュレーション:**

```bash
# レビューサービスのエラー（商品は返る）
curl "http://localhost:8080/api/products/1?simulate=error" \
  -H "Authorization: Bearer {token}"

# レビューサービスの遅延（3秒タイムアウト後にレビューなしで返る）
curl "http://localhost:8080/api/products/1?simulate=slow" \
  -H "Authorization: Bearer {token}"
```

### キャッシュ管理（ADMINのみ）

| メソッド | パス | 説明 |
|--------|------|------|
| GET | `/api/cache/stats` | キャッシュ統計（ヒット率など） |
| DELETE | `/api/cache` | キャッシュクリア |

```bash
curl http://localhost:8080/api/cache/stats \
  -H "Authorization: Bearer {admin_token}"
```

### ヘルスチェック

```bash
curl http://localhost:8080/actuator/health
```

## テストユーザー

| ユーザー名 | パスワード | ロール |
|-----------|----------|-------|
| `user` | `user123` | USER |
| `admin` | `admin123` | ADMIN |

## プロジェクト構成

```
src/main/kotlin/com/example/bff/
├── BffApplication.kt
├── config/
│   ├── CacheConfig.kt        # Caffeineキャッシュ設定
│   ├── JwtAuthFilter.kt      # JWT検証フィルター
│   ├── JwtUtil.kt            # JWT生成・検証
│   ├── SecurityConfig.kt     # Spring Security設定
│   └── WebClientConfig.kt    # WebClient設定
├── controller/
│   ├── AuthController.kt     # ログインエンドポイント
│   ├── CacheController.kt    # キャッシュ管理エンドポイント
│   ├── ProductController.kt  # 商品BFFエンドポイント
│   └── mock/
│       ├── MockProductController.kt  # 商品サービスのモック
│       └── MockReviewController.kt   # レビューサービスのモック
├── exception/
│   ├── ErrorResponse.kt
│   ├── Exceptions.kt
│   └── GlobalExceptionHandler.kt
├── model/
│   ├── Auth.kt
│   └── Product.kt
└── service/
    ├── ProductService.kt     # 商品サービス呼び出し
    └── ReviewService.kt      # レビューサービス呼び出し（キャッシュつき）
```

## 環境変数

| 変数名 | デフォルト値 | 説明 |
|-------|------------|------|
| `JWT_SECRET` | application.yml参照 | JWTシークレットキー |
| `JWT_EXPIRATION_MS` | `3600000` | トークン有効期限（ミリ秒） |
| `CACHE_REVIEWS_MAX_SIZE` | `100` | キャッシュ最大件数 |
| `CACHE_REVIEWS_EXPIRE_MINUTES` | `5` | キャッシュ有効期限（分） |

## 将来の拡張

`docker-compose.yml` のコメントを外すことで実際のマイクロサービスと接続できます。

```yaml
# docker-compose.yml
services:
  product-service:
    image: product-service:latest
    ports:
      - "8081:8081"

  review-service:
    image: review-service:latest
    ports:
      - "8082:8082"
```
