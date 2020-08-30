# sqldelight-simple-jvm-driver

[![Download](https://api.bintray.com/packages/molikuner/maven-extensions/sqldelight-simple-jvm-driver/images/download.svg) ](https://bintray.com/molikuner/maven-extensions/sqldelight-simple-jvm-driver/_latestVersion)
[![Build Status](https://cloud.drone.io/api/badges/molikuner/sqldelight-simple-jvm-driver/status.svg)](https://cloud.drone.io/molikuner/sqldelight-simple-jvm-driver)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

a simple wrapper around the driver implementation for the jvm of [SQLDelight](https://github.com/cashapp/sqldelight) to simplify the usage of migrations etc.

## setup

```gradle
dependencies {
    ...
    implementation("com.molikuner.sqldelight:simple-jvm-driver:$sqldelightVersion")
    ...
}
```

## usage

```Kotlin
val driver: SqlDriver = JvmSqliteDriver(Database.Schema, "test.db")
```
by using this driver wrapper you get automatic migrations. you don't need to save anthing about your db except
the db itself and the lib will migrate your schema if a newer version is available. at initial creation of the
db the current schema will be applied and afterwards with every new migration it will be migrated. just as simple
as the android driver.

## versioning

as this is just a simple wrapper around the official [SQLDelight](https://github.com/cashapp/sqldelight) jvm driver
this library uses the same versioning as the underlying driver implementation. this also means, that there will be one release
of this lib per release of SQLDelight.

## releasing

1. update sqldelight version
2. create tag on new commit
3. push to github and wait for drone to publish

since [drone](https://drone.io) seems to be broken somehow, i'm currently releasing the versions manually by executing the following steps:

1. open build.gradle.kts and replace the following: in the bintray section
  1. user with `molikuner`
  2. key with the api key from my user profile of bintray
  3. the gpg passphrase with the passphrase from my password manager

2. run `./gradlew bintrayUpload`
3. upload all files from `build/repository/com/molikuner/sqldelight/simple-jvm-driver/<version>/` and the `.asc` files from bintray to github tag
