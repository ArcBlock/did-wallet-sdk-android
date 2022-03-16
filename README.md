# did-wallet-sdk-android
DID Wallet Andorid version sdk



### Install

add maven repository

```groovy
  repositories{
    ...
      maven {
       url "http://android-docs.arcblock.io/release"
    }
  }
```

add dependencies

```groovy
implementation("io.arcblock.walletkit:protocol:${version}@jar")
implementation("io.arcblock.walletkit:walletkit:${version}") {
  exclude module: 'protobuf-lite'
  exclude group:'com.google.protobuf'
}
```

### Init

Before use walletkit ,you have to set up your own Application info to it.

```kotlin
val walletKit = WalletKit(AppInfo(), WalletInfo(), callback)

val client = ArcWalletClientUtils.getApiClient(context,"https://xxx.abtnetwork.io/api")
```

`AppInfo` is description of your application .

`ChainInfo` is description of your Forge Network information . all the information can be get from Forge State .
