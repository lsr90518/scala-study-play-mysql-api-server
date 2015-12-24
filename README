# Scala on PlayでAPIサーバを構築する wtih MySQL

Scala on PlayでAPIサーバーを構築するサンプルです。より本番に近い環境を再現するため、DBはMySQLを使用しています。
なお、本レポジトリは以下に示す手順を全て行った後の環境です。

## 前提条件
* Macで環境構築する手順です。
* Scala自体の環境構築は手順を忘れたので、Scalaが使える状態を前提としています

## 動作環境

| - | version |
|---|---|
| scala | 2.11.7 |
| Play Framework| 2.4.6 |

## アジェンダ



### 環境構築

#### Play Frameworkを使う環境を整える
brewでactivatorをインストールします。Play Framework 2.3よりこの手順に変更されているため、気をつけて下さい。

```
$ brew install typesafe-activator
```

#### Play Frameworkのプロジェクトを作成する
先ほどインストールしたactivatorのコマンドを叩き、プロジェクトを作成します。途中で使用するテンプレートを効かれるので、`play-scala`を選択。

```
$ activator new scala-study-play-mysql-api-server
```

#### 動作確認
プロジェクトのルートディレクトリで、`activator run`コマンドを実行してサーバを立ちあげます。ブラウザで`http://localhost:9000/`にアクセスするとページが表示されます(初回アクセス時にScalaのコンパイルが走るので、最初は若干遅いです)。

```
$ cd scala-study-play-mysql-api-server
$ activator run
```

### 簡単なAPIを作成する
#### ルーティングの設定
APIを作成するに辺り、まずはURLルーティングの設定を行います。ルーティングの設定ファイル`routes`に、以下のルーティングを設定します。

`/conf/routes`

```
GET /message  controllers.Application.message
```

#### ControllerにAPIのActionを作成
ControllerにActionを追加します。`play.api.libs.json`のインポートを忘れずに。

`app/controllers/Application.scala`

```
package controllers

import play.api._
import play.api.mvc._

// import文を追加
import play.api.libs.json._;

class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  // Actionを追加
  def message = Action {
    val json = Json.obj("message" -> "Hello!");

    Ok(Json.toJson(json));
  }
}
```

これで完了です。[Advanced REST client](https://chrome.google.com/webstore/detail/advanced-rest-client/hgmloofddffdnphfgcellkdfbfbjeloo)などのツールを使用して、`GET http://localhost:9000/message`へリクエストを送信してみましょう。
