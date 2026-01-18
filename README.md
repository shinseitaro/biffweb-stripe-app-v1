# Biff + Stripe 決済システム サンプルアプリ

[Biff](https://biffweb.com/) フレームワークを使用した Stripe 決済統合のサンプルアプリケーションです。

## 解説書

このリポジトリの詳細な解説は以下の Zenn Book で公開しています：

**[Biff + Stripe 決済システム構築ガイド](https://zenn.dev/shinseitaro/books/biffweb-stripe-payment-system)**

## 機能

- Stripe Checkout による決済処理
- ゲスト決済 / ログインユーザー決済
- Stripe Customer の自動作成・紐付け
- Webhook によるイベント処理
- XTDB によるデータ永続化

## プロジェクト構成

```
src/com/myapp/
├── billing/
│   ├── stripe.clj    # Stripe API クライアント
│   ├── donate.clj    # 寄付ページ・決済フロー
│   └── webhooks.clj  # Webhook エンドポイント
├── home.clj          # ホームページ
├── email.clj         # メール認証
├── middleware.clj    # ミドルウェア
├── schema.clj        # Malli スキーマ定義
├── settings.clj      # 設定ページ
└── ui.clj            # UI コンポーネント
```

## セットアップ

### 1. 依存関係のインストール

```bash
clj -M:dev dev
```

### 2. 環境変数の設定

`config.env` を作成し、必要な環境変数を設定します：

```bash
cp resources/config.template.env config.env
```

以下の値を設定してください：

```env
# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# その他（必要に応じて）
COOKIE_SECRET=...
JWT_SECRET=...
```

シークレットの生成：

```bash
clj -M:dev generate-secrets
```

### 3. 開発サーバーの起動

```bash
clj -M:dev dev
```

http://localhost:8080 でアクセスできます。

## 開発

### REPL 駆動開発

nREPL サーバーがポート 7888 で起動します。エディタから接続して開発できます。

```bash
# エイリアスを設定すると便利です
alias biff='clj -M:dev'
```

### テストデータの投入

REPL から以下を実行：

```clojure
(require '[repl])
(repl/add-fixtures)
```

## ライセンス

MIT
