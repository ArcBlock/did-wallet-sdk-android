# did-wallet-sdk-android
DID Wallet Andorid version sdk



### Install

```groovy
implementation("io.arcblock.did:sdk-protocol:${version}")
implementation("io.arcblock.did:wallet-sdk:${version}")
implementation("org.web3j:core:4.6.0-android")
implementation("org.bitcoinj:bitcoinj-core:0.16.1"){
  exclude group: 'com.google.protobuf'
}
implementation("com.google.guava:guava:30.1-android")
implementation 'com.google.protobuf:protobuf-kotlin-lite:3.19.4'
implementation('com.google.crypto.tink:tink-android:1.6.1') {
  exclude module: 'protobuf-lite'
}
```

### Usage

#### DID Generate

``` kotlin
// generate mnemonic words
val codes = Bip44Utils.genMnemonicCodes()
// generate Root seed
val seed = Bip44Utils.genSeedByInsertMnemonics(codes).seed
// generate keypair derived by AppID
val keypair = IdGenerator.genAppKeyPair("zEdhj45f", 0,  seed, ED25519)
// generate DID
val did = IdGenerator.sk2did(keypair.privateKey)
```

#### Sign/Verify a message

```
val sig = Signer.sign(KeyType.ETHEREUM, messageUtf8.toByteArray(), sk)
val verified =  Signer.verify(KeyType.ETHEREUM, messageUtf8.toByteArray(), pk, sig)
```

### License
```
                Copyright [2022] [ArcBlock.io]
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

