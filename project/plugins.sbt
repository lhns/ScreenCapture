logLevel := Level.Warn

resolvers := Seq("Artifactory" at "http://lolhens.no-ip.org/artifactory/libs-release/")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M8")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.3")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.15")
