# minecraft-alias

Minecraft Java Edition (Spigot/Paper 1.20+) 用の軽量プラグインで、任意のコマンドにシンプルなエイリアスを設定できます。

## ✨ 主な機能

- `/alias add <exec_command> <alias_command>` でエイリアス登録
- `/alias list [page]` で 10 件ごとのページ表示
- `/alias remove <alias_command>` で削除
- エイリアス実行時に元コマンドを自動実行。追加の引数も自動的に引き渡し
- プレースホルダー `%player%`, `%displayname%` に対応
- OP または `minecraftalias.admin` 権限を持つプレイヤーのみが管理

## ⚙️ 動作環境

- Java 17+
- Spigot / Paper 1.20.1 互換サーバー

## 🧱 ビルド方法

```bash
mvn clean package
```

`target/minecraft-alias.jar` をサーバーの `plugins/` に配置し、サーバーを再起動してください。

## 📋 コマンド一覧

| コマンド | 説明 |
| --- | --- |
| `/alias add <exec_command...> <alias_command>` | `/alias_command` を実行すると `/<exec_command>` が実行されるように登録します |
| `/alias remove <alias_command>` | 登録済みエイリアスを削除します |
| `/alias list [page]` | 登録済みエイリアスをページ表示します (1 ページ 10 件) |

> 例: `/alias add warp spawn` → `/spawn` で `/warp` が実行されます。`/spawn home` のように引数を付けても `/warp home` として渡されます。
>
> 複数単語のコマンドは最後の引数をエイリアス名として解釈し、それ以外をすべて実行コマンドとして登録します。`/alias add "gamemode creative" creative` のように登録すれば、`/creative` で `/gamemode creative` が実行されます。

## 🗂️ 設定ファイル

`plugins/MinecraftAlias/config.yml`

```yaml
aliases:
	spawn: warp spawn
```

手動で編集しても構いませんが、ゲーム内コマンドから管理することを推奨します。

## 🧪 テスト

MockBukkit を用いたユニットテストを用意しています。ビルドと同じく Maven で実行できます。

```bash
mvn test
```

## 📄 ライセンス

MIT
