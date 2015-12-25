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
| play-slick | 1.1.1 |
| mysql-connector-java | 5.1.38 |

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

まずは簡単なAPIを作成し、動くことを確認します。ここはRuby on Railsなどの他のサーバーサイドMVCフレームワークを触ったことがある人なら、難しくないと思います。

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

### MySQLの設定をする
次にMySQLへの接続設定を行います。今回は簡略化のため、DBの初期化・マイグレーションは組み込んでいません。そのため、[Sequel Pro](http://www.sequelpro.com/)などを使用して、DB・テーブルの作成、初期レコードのInsertを行ってください。本プロジェクトでは`test`というDBに、`messages`というテーブルを作成しています。

```
> CREATE DATABSE test
> USE test
> CREATE TABLE `messages` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `text` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

#### モジュールの追加
ビルド定義ファイルに以下のモジュールを追加します。今回は`slick`というScala用O/R Mapperを使用します。slickにはPlay Frameworkから簡単に使える`play-slick`というモジュールが用意されているため、そちらを使用します。`play-slick`のバージョンは[scala及びplayのバージョンによって厳密に](https://github.com/playframework/play-slick)定められています。

同様に、MySQLへ接続するための`mysql-connector-java`を使用します。もう一つのモジュール、`commons-dbcp`はコネクションプールを管理するためのモジュールですが、今回は特に意識しなくて大丈夫です。

`build.sbt`

```
libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "mysql" % "mysql-connector-java" % "5.1.38",
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  "commons-dbcp" % "commons-dbcp" % "1.4"
)
```

#### MySQLの設定
まずは簡単に、localhostで立ち上げたMySQLへの接続を確立します。以下の設定を`conf/application.conf`に追加してください。`slick.dbs.default.db.url`の値は、"jdbc:mysql://<host_name>:<port>/<db_name>?user=<user_name>&password=<password>"のフォーマットです。

`conf/application.conf`

```
slick.dbs.default.driver="slick.driver.MySQLDriver$"
slick.dbs.default.db.driver="com.mysql.jdbc.Driver"
slick.dbs.default.db.url="jdbc:mysql://localhost:3306/test?user=root&password=password"
```

ここまで出来たら一度サーバを起動しましょう。モジュールのインストールが走るはずです。

```
$ activator run
```

### MySQLからデータを取得する
次は実際にMySQLから取得したデータを、APIレスポンスとして返却する実装をします。O/R Mapperのslick固有の処理があって難しいですが、一通りのコードを眺めてみましょう。

#### モデル定義
`app/models/database/`というディレクトリを切って、モデルを定義していきます。今回はサンプルとして、PrimaryKeyのidとtextというカラムをもった`messages`テーブルを使用しています。

ポイント
* `case class`でMessageクラスを定義する
* コンパニオンオブジェクトを作成
* コンパニオンオブジェクトは`implicit val`を持つ。これを定義することで、JSONのシリアライズ/デシリアライズが可能となる。

詳しいコードの解説は後述します。

`app/models/database/Message.scala`

```
package models.database

import play.api.libs.json._

case class Message (id: Option[Long], text: String)

object Message {
  implicit val messageWrites = Json.writes[Message]
  implicit val messageReads = Json.reads[Message]
}
```

#### DAO(Data Access Object)の定義
次に実際にDBへ接続する部分の実装です。Slickのクセが大きい所なのでまずはテンプレとして覚えていても大丈夫だと思います。SlickではDBの操作を`TableQuery`オブジェクトで隠蔽しており、それにレコードを追加したり削除することでDB操作を実現します。

`app/models/MessageDAO.scala`

```
package models

import javax.inject.Singleton
import javax.inject.Inject

import scala.concurrent.Future

import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import models.database.Message

import play.api.libs.json._

@Singleton
class MessageDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig.driver.api._

  // テーブル定義
  private class MessageTable(tag: Tag) extends Table[Message](tag, "messages") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def text= column[String]("text")

    def * = (id.?, text) <> ((Message.apply _).tupled, Message.unapply)
  }

  private val messages = TableQuery[MessageTable]

  def all(): Future[List[Message]] = dbConfig.db.run(messages.result).map(_.toList)
}
```

#### APIの実装
controllerで先ほど作成したDAOのAPIを叩きます。DAOのAPIがFutureを返すような設計になっているため、`scala.concurrent`のimport文が必要です。`dao.all()`で返されるMessageオブジェクトは、JSONシリアライズする`implicit val`を持っているため、そのまま`toJson`に渡してあげるだけでJSON形式に変換されます。

`app/controllers/Application.scala`

```
package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._;

// ここからimport文追加
import javax.inject.Inject

import models.database.Message
import models.MessageDAO

// Futureを扱うにはこの2つのimportが必要
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (dao: MessageDAO) extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  // APIの処理を書き直し
  def message = Action.async {
    dao.all().map(messages => Ok(Json.toJson(Json.toJson(messages))))
  }
}
```

ここまで出来たらDBに適当なレコードを追加して(直接MySQLを触る)、再度`GET http://localhost:9000/message`にリクエストを投げましょう。DBから取得されたレコードがレスポンスとして返されるはずです。
