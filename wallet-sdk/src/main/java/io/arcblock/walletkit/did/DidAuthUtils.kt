package io.arcblock.walletkit.did

import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.arcblock.walletkit.bean.Header
import io.arcblock.walletkit.bean.AppInfo
import io.arcblock.walletkit.bean.DIDTokenResponse
import io.arcblock.walletkit.bean.IClaim
import io.arcblock.walletkit.bean.WalletInfo
import io.arcblock.walletkit.signer.Signer
import io.arcblock.walletkit.utils.did
import io.arcblock.walletkit.utils.encodeB64Url
import java.util.*

object DidAuthUtils {
  private val gs = Gson()

  /**
   * create a JWT token to response client query and to require some information from client
   * @param authClaims: Claims you require
   * @param appInfo: current Application information provide to client
   * @param currentTimestamp: current time .
   * @param wallet: application key info .
   */
  fun createDidAuthToken(authClaims: Array<IClaim>, appInfo: AppInfo, chainHost: String, currentTimestamp: Long, wallet: WalletInfo, sessionId: String = ""): String {
    val exp = (currentTimestamp/1000 + 60*60*24*365 ).toString()
//    val body = DIDTokenBody(action = "responseAuth", appInfo = appInfo, requestedClaims = authClaims, url = "https://2d373f91-4ea8-4e68-b6c3-d08bbe222dca.mock.pstmn.io/api/auth/", exp = exp,
//      iat = (currentTimestamp/1000).toString(), iss = wallet.address.did(), nbf =  (currentTimestamp/1000).toString(), sessionId = sessionId
//    )
    val body = JsonObject()
    body.addProperty("action" ,"responseAuth")
    body.add("appInfo", gs.toJsonTree(appInfo))
    body.add("chainInfo",JsonObject().let {
      it.addProperty("host",chainHost)
      it
    })
    body.addProperty("exp",exp)
    body.addProperty("iat",(currentTimestamp/1000).toString())
    body.addProperty("nbf",(currentTimestamp/1000-10).toString())
    body.addProperty("iss",wallet.address.did())
    body.addProperty("sessionId",sessionId)
    body.add("requestedClaims", gs.toJsonTree(authClaims))

    val jsonHeader = Header(wallet.getSignType().toString().toUpperCase(Locale.ROOT), "JWT")
    val content = BaseEncoding.base64Url().encode(gs.toJson(jsonHeader).toByteArray()).replace("=", "")
      .plus(".")
      .plus(BaseEncoding.base64Url().encode(gs.toJson(body).toByteArray()).replace("=", ""))
    val signature = Signer.sign(KeyType.ED25519, content.toByteArray(), wallet.sk!!)
    return content.plus(".").plus(signature.encodeB64Url())
      .replace("=", "")
  }

  /* A function to parse JWT token. */
  fun parseJWT(token: String): DIDTokenResponse {
    val jwt = token.split(".")
    val body = String(BaseEncoding.base64Url().decode(jwt[1]))
    return gs.fromJson(body, DIDTokenResponse::class.java)
  }

  /**
   * It verifies the signature of a JWT token
   *
   * @param token The JWT token to verify.
   * @param pk The public key of the user.
   * @param type The type of signature.
   * @return signature is valid.
   */
  fun verifyJWTSig(token: String, pk: ByteArray, type: String): Boolean {
    val sig = token.substringAfterLast(".")
    val content = token.substringBeforeLast(".")

    return if (KeyType.ED25519.toString().toLowerCase(Locale.ROOT) == type.toLowerCase(Locale.ROOT)) {
      Signer.verify(KeyType.ED25519, content.toByteArray(), pk, BaseEncoding.base64Url().decode(sig))
    } else {
      Signer.verify(KeyType.SECP256K1, content.toByteArray(), pk, BaseEncoding.base64Url().decode(sig))
    }
  }
}
