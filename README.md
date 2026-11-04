# cert4android

cert4android is a library for Android to manage custom certificates which has
been developed for [DAVx⁵](https://www.davx5.com). Feel free to use
it in your own open-source app.

_This software is not affiliated to, nor has it been authorized, sponsored or otherwise approved
by Google LLC. Android is a trademark of Google LLC._

Discussion: https://forums.bitfire.at/category/18/libraries


# Features

* uses a service to manage custom certificates
* supports multiple threads and multiple processes (for instance, if you have an UI
  and a separate `:sync` process which should share the certificate information)


# How to use

1. Clone cert4android as a submodule.
1. Add the submodule to `settings.gradle.kts` (`include(":cert4android")`) and add
   `implementation(project(":cert4android"))` to `app/build.gradle.kts`.
1. Create an instance of `CustomCertManager` (`Context` is required to connect to the
   `CustomCertService`, which manages the custom certificates).
1. Use this instance as `X509TrustManager` in your calls (for instance, when setting up your HTTP client).
   Don't forget to get and use the `hostnameVerifier()`, too.
1. Close the instance when it's not required anymore (will disconnect from the
   `CustomCertService`, thus allowing it to be destroyed).

Example of initialzing an okhttp client:

    val keyManager = ...
    CustomCertManager(...).use { trustManager ->
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
            if (keyManager != null) arrayOf(keyManager) else null,
            arrayOf(trustManager),
            null
        )
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslContext.socketFactory, trustManager)
               .hostnameVerifier(hostnameVerifier)
        val httpClient = builder.build()
        // use httpClient
    }


You can overwrite resources when you want, just have a look at the `res/strings`
directory. Especially `certificate_notification_connection_security` and
`trust_certificate_unknown_certificate_found` should contain your app name.


## Development

This fork is maintained at `github.com/nikossvnk/cert4android`. Use JDK 25,
the included Gradle 9.8.2 wrapper, and Android SDK Platform 37.0. The module uses
AGP 9.5.0, built-in Kotlin 2.4.21, Java 17 bytecode, and minSdk 23.
Certificate dialogs use view binding; the AIDL service and trust-manager API remain.

```sh
./gradlew build compileDebugAndroidTestKotlin
./gradlew connectedCheck
```

Instrumented trust-manager tests require a device/emulator and network access.
CI builds, lints, runs JVM tests where present, and compiles instrumentation;
it does not execute device tests. Dependabot and scheduled CI run weekly.

## Contact

```
bitfire web engineering – Stockmann, Hirner GesnbR
Florastraße 27
2540 Bad Vöslau, AUSTRIA
```

Email: [play@bitfire.at](mailto:play@bitfire.at) (do not use this)


# License 

Copyright (C) bitfire web engineering (Ricki Hirner, Bernhard Stockmann).

This program comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome
to redistribute it under the conditions of the [GNU GPL v3](LICENSE).

