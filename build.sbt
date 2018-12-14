/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Atos IT Solutions and Services GmbH, National University of Ireland Galway, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.Cmd
import NativePackagerHelper._
import com.typesafe.sbt.packager.SettingsHelper._

description := "BIG IoT Exchange."

val monocleVersion = "1.5.1-cats"
val paradiseVersion = "2.1.0"

val env = scala.util.Properties.envOrElse("PROJECTDEPS", "")
// usage of the flag: sbt "-DenableRetrieveManaged=true" universal:publish
val enableRetrieveManagedProp = sys.props.getOrElse("enableRetrieveManaged", "false")

lazy val commonSettings = Seq(
  organization := "org.eclipse.bridgeiot",
  version := "0.9-SNAPSHOT",

  scalaVersion := "2.12.7",
  
  retrieveManaged := enableRetrieveManagedProp.toBoolean,
  
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),

  resolvers += (publishTo in Universal).value.get,
  publishTo in Universal := Some(Resolver.mavenLocal),
  resolvers ++= Seq(
    "Local Nexus" at "https://nexus.big-iot.org/content/repositories/snapshots/",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    Resolver.sonatypeRepo("releases")
  ),
  libraryDependencies ++= Seq(
    "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
    "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,

    "org.codehaus.janino" % "janino" % "2.7.8"
  )
)

// exchange-api
val apiLocalDeps: List[ClasspathDep[ProjectReference]] = if (env == "")
  List(ProjectRef(file("../marketplace-microservice"), "marketplace-microservice"))
else Nil
val apiLibDeps = if (apiLocalDeps.isEmpty)
  List("org.eclipse.bridgeiot" %% "marketplace-microservice" % "0.1-SNAPSHOT")
else Nil

lazy val `exchange-api` = (project in file("exchange-api"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    mappings in Universal := {if (enableRetrieveManagedProp.toBoolean) directory("lib_managed") else (mappings in Universal).value},
    mappings in Universal += { //   Add version.properties file to target directory
      val versionFile = target.value / "version.properties"
      IO.write(versionFile, s"version=${version.value}")
      versionFile -> "version.properties"
    },
    makeDeploymentSettings(Universal, packageBin in Universal, "zip"), 
    commonSettings,
    publishTo := Some("Local Nexus" at "https://nexus.big-iot.org/content/repositories/snapshots/"),
    credentials += Credentials(file("./.credentials")),
    credentials += Credentials(System.getenv("NEXUS_REALM"), System.getenv("NEXUS_HOST"),
      System.getenv("NEXUS_USER"), System.getenv("NEXUS_PASSWORD")),

    libraryDependencies ++= apiLibDeps ++ Seq(
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )
  .dependsOn(apiLocalDeps: _*)

// exchange-impl
val implLocalDeps = apiLocalDeps
val implLibDeps = apiLibDeps

lazy val `exchange-impl` = (project in file("exchange-impl"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    mappings in Universal := {if (enableRetrieveManagedProp.toBoolean) directory("lib_managed") else (mappings in Universal).value},
    mappings in Universal += { //   Add version.properties file to target directory
      val versionFile = target.value / "version.properties"
      IO.write(versionFile, s"version=${version.value}")
      versionFile -> "version.properties"
    },
    makeDeploymentSettings(Universal, packageBin in Universal, "zip"),
    commonSettings,

    mainClass in Compile := Some("exchange.server.Exchange"),
    Revolver.settings,
    publish := (),

    libraryDependencies ++= implLibDeps ++ Seq(
      "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
      "org.apache.jena" % "jena-arq" % "3.1.1",
      "com.squareup.okhttp" % "okhttp" % "2.7.5",
      "com.github.jsonld-java" % "jsonld-java" % "0.9.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.7",
      "virtuoso" % "virt_jena3" % "7.2.4.2" from "https://github.com/openlink/virtuoso-opensource/raw/stable/7/binsrc/jena3/virt_jena3.jar",
      "virtuoso" % "virtjdbc4" % "7.2.4.2" from "https://github.com/openlink/virtuoso-opensource/raw/stable/7/libsrc/JDBCDriverType4/virtjdbc4.jar",
      "org.apache.commons" % "commons-collections4" % "4.0",

      "org.scalatest" %% "scalatest" % "3.0.5" % "test"
),

    dockerCommands := Seq(
      Cmd("FROM", "openjdk:8-jre-alpine"),
      Cmd("RUN", "apk update && apk add bash && rm -rf /var/cache/apk/* && adduser -S -H -u 2020 bigiot bigiot"),
      Cmd("EXPOSE", "8080"),
      Cmd("ADD", "opt /opt"),
      Cmd("WORKDIR", s"/opt/exchange"),
      Cmd("RUN", "chmod", "+x", s"/opt/exchange/bin/exchange-impl"),
      Cmd("USER", "bigiot"),
      Cmd("CMD", s"/opt/exchange/bin/exchange-impl")
    ),

    parallelExecution in Test := false,

    dockerRepository := Some("registry.gitlab.com/big-iot"),

    defaultLinuxInstallLocation in Docker := "/opt/exchange"
  )
  .dependsOn(`exchange-api`)
  .dependsOn(implLocalDeps: _*)

lazy val exchange = (project in file("."))
  .aggregate(`exchange-api`, `exchange-impl`)
  .settings(
    publish := ()
  )
