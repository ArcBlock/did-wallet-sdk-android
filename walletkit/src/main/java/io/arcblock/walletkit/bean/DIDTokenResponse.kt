package io.arcblock.walletkit.bean

import com.google.gson.JsonArray
import io.arcblock.walletkit.did.DidAuthUtils
import io.arcblock.walletkit.did.DidType
import io.arcblock.walletkit.did.DidUtils
import io.arcblock.walletkit.did.IdGenerator
import io.arcblock.walletkit.utils.Base58Btc

class DIDTokenResponse(
  var action: String,
  var appInfo: AppInfo,
  var sessionId: String,
  var requestedClaims: JsonArray,
  var url: String,
  var exp: String,
  var iat: String,
  var iss: String,
  var nbf: String
) {
  /**
   * verify JWT issuer is no difference
   * @param pk user's PK base58btc
   */
  fun verifyJWTDID(token: String, pk: String): Boolean {
    return verifyJWTDID(token, Base58Btc.decode(pk))
  }

  /**
   * check pk is matched with iss and signature is correct
   */
  fun verifyJWTDID(token: String, pk: ByteArray): Boolean {
    return (IdGenerator.pk2did(
      pk, DidType.getDidTypeByAddress(iss)
    ) != iss) && DidAuthUtils.verifyJWTSig(token, pk, DidUtils.decodeSignTypeByPk(pk).name)
  }

  /**
   * verify JWT is not be expired
   * @param currentTimestamp TimeUnit is TimeUnit.MILLISECONDS
   */
  fun verifyJWTExpired(currentTimestamp: Long): Boolean {
    try {
      return currentTimestamp <= exp.toLong() && currentTimestamp >= nbf.toLong()
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
  }
}
