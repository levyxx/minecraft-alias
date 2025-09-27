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
| `/alias add <exec_command...> by <alias_command...>` | `/alias_command` を実行すると `/<exec_command>` が実行されるように登録します |
| `/alias remove <alias_command...>` | 登録済みエイリアスを削除します |
| `/alias list [page]` | 登録済みエイリアスをページ表示します (1 ページ 10 件) |

> 例: `/alias add warp spawn` → `/spawn` で `/warp` が実行されます。`/spawn home` のように引数を付けても `/warp home` として渡されます。
>
> 複数単語のコマンドは最後の引数をエイリアス名として解釈し、それ以外をすべて実行コマンドとして登録します。`/alias add "gamemode creative" creative` のように登録すれば、`/creative` で `/gamemode creative` が実行されます。
>
> エイリアス名そのものにも空白を含められます。`/alias add gamemode creative by gm 1` と登録すると、`/gm 1` で `/gamemode creative` を実行できます。さらに `/<alias>` に続けて入力した追加引数は、そのまま元コマンドへ連結されます。

削除時も同じ区切りで指定できます。`/alias remove gm 1` や `/alias remove "gm 1"` のように入力してください。

## 🗂️ 設定ファイル

`plugins/MinecraftAlias/config.yml`

```yaml
aliases:
	spawn: warp spawn
```

手動で編集しても構いませんが、ゲーム内コマンドから管理することを推奨します。

## 🚀 開発

### サーバーの準備

https://papermc.io/downloads から paper を選択し、Paper x.x.xをダウンロードする。
ダウンロードした.jarファイルを以下のコマンドで実行する。

```bash
$ java -Xmx2G -jar paper-1.21.8-60.jar nogui
```

初回実行時、同ディレクトリに eula.txtが作成されるので、内容を以下に変更して保存する。

```bash
eula=true
```

再度サーバーを起動する。Done! と表示されればOK。

```bash
$ java -Xmx2G -jar paper-1.21.8-60.jar nogui
```

### .jarファイルのビルド

以下のコマンドでjava-17を選択する。

```bash
$ sudo update-alternatives --config java
```

以下のコマンドで.jarファイルをビルドする。

```bash
$ mvn clean package
```

### pluginの導入

サーバーのpluginsフォルダに既に.jarファイルがあれば削除する。

```bash
$ rm -rf ./minecraft-alias-1.0.0-SNAPSHOT.jar
```

ビルドした.jarファイルを配置する。(↓コマンドの例)

```bash
$ mv ~/minecraft/minecraft-alias/target/minecraft-alias-1.0.0-SNAPSHOT.jar .
```

### サーバーの起動

.jarファイルを最初に実行したときのjavaのバージョンに変更する(例 java-21)

```bash
$ sudo update-alternatives --config java
```

以下のコマンドでサーバーを起動する。

```bash
$ java -Xmx2G -jar paper-1.21.8-60.jar nogui
```

### サーバーに入る

Ubuntu上で以下のコマンドを実行する。

```bash
ip addr show eth0
```

inet の値を確認する。
サーバーアドレスを <inet の値>:25565 とすればサーバーに接続できる。