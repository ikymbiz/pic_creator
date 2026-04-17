# GitHub Actions セットアップ手順

SilentCamera.zip をzipのまま保存し、GitHub Actionsで自動的にAPKをビルドします。

## 📁 リポジトリ構成

```
your-repo/
├── SilentCamera.zip           ← ソースzip（そのまま保存）
└── .github/
    └── workflows/
        ├── build.yml          ← 通常ビルド（push時）
        └── release.yml        ← リリースビルド（タグ時）
```

## 🚀 セットアップ手順

### 1. GitHubリポジトリを作成

GitHubで新規リポジトリを作成（Private推奨）。

### 2. ファイルをアップロード

以下のファイルをリポジトリのルートに配置してpushします：

```
SilentCamera.zip
.github/workflows/build.yml
.github/workflows/release.yml
```

コマンド例：
```bash
git clone https://github.com/あなた/リポジトリ名.git
cd リポジトリ名
cp /path/to/SilentCamera.zip ./
mkdir -p .github/workflows
cp build.yml release.yml .github/workflows/
git add .
git commit -m "Initial commit"
git push
```

## 🔧 2つのワークフローの役割

### build.yml（通常ビルド）
- **いつ動く**: `main`/`master`ブランチにpushした時、または手動実行
- **何が出る**: Actionsタブの「Artifacts」からデバッグAPKをダウンロード可能
- **保存期間**: 30日間
- **用途**: 開発中の動作確認

### release.yml（リリースビルド）
- **いつ動く**: `v1.0.0`のようなタグをpushした時、または手動実行
- **何が出る**: GitHub Releasesページに `SilentCamera-v1.0.0.apk` が公開される
- **保存期間**: 永続（リリースを削除するまで）
- **用途**: 家族や友人に配布するとき

## 📦 使い方

### デバッグAPKを取得（テスト用）

1. ファイルをpushする
2. GitHubの「Actions」タブを開く
3. 「Build Debug APK」ワークフローをクリック
4. 完了後、下部の「Artifacts」→ `SilentCamera-debug-apk` をダウンロード
5. zipを解凍するとAPKが入っている

### リリースAPKを作成（配布用）

```bash
git tag v1.0.0
git push origin v1.0.0
```

または、GitHubの「Actions」タブ →「Release APK」→「Run workflow」で手動実行。

完了すると「Releases」ページにAPKが公開されます。

## 📲 APKのインストール

1. 端末に ダウンロードしたAPKを転送
2. 設定 →「不明なアプリのインストール」を許可
3. APKをタップしてインストール

## ⚠️ 注意事項

- このビルドは**デバッグ署名**です。本格的な配布には自分のキーストアで署名してください
- `SilentCamera.zip` のファイル名は `SilentCamera` で始まれば OK（例: `SilentCamera.zip`, `SilentCamera-v2.zip`）
- GitHub Actionsは**無料枠で月2000分**使えます（1ビルド約3〜5分）
