package io.arcblock.walletkit.utils

import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import io.arcblock.walletkit.did.HashType
import io.arcblock.walletkit.did.KeyType
import io.arcblock.walletkit.hash.Hasher
import io.arcblock.walletkit.signer.Signer
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat

/**
 *
 *     █████╗ ██████╗  ██████╗██████╗ ██╗      ██████╗  ██████╗██╗  ██╗
 *    ██╔══██╗██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝
 *    ███████║██████╔╝██║     ██████╔╝██║     ██║   ██║██║     █████╔╝
 *    ██╔══██║██╔══██╗██║     ██╔══██╗██║     ██║   ██║██║     ██╔═██╗
 *    ██║  ██║██║  ██║╚██████╗██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗
 *    ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
 * Author       : ${EMAIL}
 * Time         : 2019-06-14
 * Edited By    :
 * Edited Time  :
 * Description  :
 **/

fun String.decodeB58() = Base58Btc.decode(this)
fun String.decodeB64() = BaseEncoding.base64().decode(this)
fun String.decodeB64Url() = BaseEncoding.base64Url().decode(this)
fun String.decodeB16() = BaseEncoding.base16().decode(this)

fun String.address() = if (this.lowercase().startsWith("did:abt:", true)) {
  this.substring(8)
} else this

fun String.did() = if (this.startsWith("did:abt:", ignoreCase = true)) "did:abt:".plus(this.substring(8)) else "did:abt:".plus(this)
fun ByteArray.toByteString() = ByteString.copyFrom(this)

fun ByteArray.encodeB58() = Base58Btc.encode(this)
fun ByteArray.encodeB64() = BaseEncoding.base64().encode(this)
fun ByteArray.encodeB64Url() = BaseEncoding.base64Url().encode(this)
fun ByteArray.encodeB16() = BaseEncoding.base16().encode(this)
fun ByteArray.hash(type: HashType) = Hasher.hash(type, this)
fun ByteArray.sign(sk: ByteArray) = Signer.sign(KeyType.ED25519, this, sk)
fun ByteArray.sign(
  sk: ByteArray,
  type: KeyType
) = Signer.sign(type, this, sk)




