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
[Web ブラウザ]  ──┐
                  ├──▶ [BFF :8080] ──▶ [product-service :8081]
[モバイルアプリ] ──┘                └──▶ [review-service  :8082]
```

## 技術スタック

| サービス | 言語 | フレームワーク | ポート |
|---------|------|-------------|------|
| BFF | Kotlin 1.9 | Spring Boot 3.3.5 (WebFlux) | 8080 |
| product-service | Kotlin 1.9 | Spring Boot 3.3.5 (Web) | 8081 |
| review-service | Kotlin 1.9 | Spring Boot 3.3.5 (Web) | 8082 |

**共通:**
- ビルドツール: Gradle 8.10.2 (Kotlin DSL)
- Java: 21
- コンテナ: Docker / Docker Compose

**BFF固有:**
- キャッシュ: Caffeine
- 認証: JWT (jjwt 0.12.6)
- HTTPクライアント: WebClient (Reactor)

## アーキテクチャ

### BFFの役割

| 機能 | 説明 |
|------|------|
| APIの集約 | 商品・レビューを1リクエストにまとめる |
| レスポンス変換 | Web用（全フィールド）/ モバイル用（軽量）で返すデータを変える |
| 並列呼び出し | `Mono.zip` で複数サービスを同時に呼び出して高速化 |
| 認証・認可 | JWTトークン検証・ロールベースアクセス制御 |
| エラーハンドリング | サービスダウン時のフォールバック・タイムアウト制御 |
| キャッシュ | レビューデータをBFFでキャッシュしてバックエンド負荷を軽減 |

### リクエストフロー（商品詳細）

```
クライアント
    │ GET /api/products/1
    ▼
[BFF] JWT検証 → キャッシュ確認
    │
    ├─ 並列 ─▶ [product-service] GET /products/1
    └─ 並列 ─▶ [review-service]  GET /reviews/product/1
    │
    ▼ Mono.zip で集約
クライアントへレスポンス返却
```

## リポジトリ構成

```
Kotlin/
├── bff-app/            # BFF（本リポジトリ）
├── product-service/    # 商品マイクロサービス
├── review-service/     # レビューマイクロサービス
└── docker-compose.yml  # 全サービス統合起動
```

## 起動方法

### ローカル起動（サービス個別）

各サービスのディレクトリで実行する。

```bash
# product-service（ポート8081）
cd product-service && ./gradlew bootRun

# review-service（ポート8082）
cd review-service && ./gradlew bootRun

# BFF（ポート8080）
cd bff-app && ./gradlew bootRun
```

### Docker起動（全サービス一括）

`Kotlin/` ディレクトリで実行する。

```bash
# ビルド＋起動（product-service・review-service が起動してから BFF が起動する）
docker compose up -d --build

# ログ確認
docker compose logs -f

# 特定サービスのログ
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

### 商品（BFF経由）

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
curl http://localhost:8080/actuator/health  # BFF
curl http://localhost:8081/actuator/health  # product-service
curl http://localhost:8082/actuator/health  # review-service
```

## テストユーザー

| ユーザー名 | パスワード | ロール |
|-----------|----------|-------|
| `user` | `user123` | USER |
| `admin` | `admin123` | ADMIN |

## プロジェクト構成

### BFF (bff-app/)

```
src/main/kotlin/com/example/bff/
├── BffApplication.kt
├── config/
│   ├── CacheConfig.kt        # Caffeineキャッシュ設定
│   ├── JwtAuthFilter.kt      # JWT検証フィルター
│   ├── JwtUtil.kt            # JWT生成・検証
│   ├── SecurityConfig.kt     # Spring Security設定
│   └── WebClientConfig.kt    # WebClient設定（サービスURLを注入）
├── controller/
│   ├── AuthController.kt     # ログインエンドポイント
│   ├── CacheController.kt    # キャッシュ管理エンドポイント
│   └── ProductController.kt  # 商品BFFエンドポイント（集約・変換）
├── exception/
│   ├── ErrorResponse.kt
│   ├── Exceptions.kt
│   └── GlobalExceptionHandler.kt
├── model/
│   ├── Auth.kt
│   └── Product.kt
└── service/
    ├── ProductService.kt     # product-serviceへのWebClient呼び出し
    └── ReviewService.kt      # review-serviceへのWebClient呼び出し（キャッシュつき）
```

### product-service (product-service/)

```
src/main/kotlin/com/example/product/
├── ProductServiceApplication.kt
├── controller/
│   └── ProductController.kt  # GET /products, GET /products/{id}
└── model/
    └── Product.kt
```

### review-service (review-service/)

```
src/main/kotlin/com/example/review/
├── ReviewServiceApplication.kt
├── controller/
│   └── ReviewController.kt  # GET /reviews/product/{productId}
└── model/
    └── Review.kt
```

## 環境変数

| 変数名 | デフォルト値 | 説明 |
|-------|------------|------|
| `SERVICES_PRODUCT-URL` | `http://localhost:8081` | product-serviceのURL |
| `SERVICES_REVIEW-URL` | `http://localhost:8082` | review-serviceのURL |
| `JWT_SECRET` | application.yml参照 | JWTシークレットキー |
| `JWT_EXPIRATION_MS` | `3600000` | トークン有効期限（ミリ秒） |
| `CACHE_REVIEWS_MAX_SIZE` | `100` | キャッシュ最大件数 |
| `CACHE_REVIEWS_EXPIRE_MINUTES` | `5` | キャッシュ有効期限（分） |
