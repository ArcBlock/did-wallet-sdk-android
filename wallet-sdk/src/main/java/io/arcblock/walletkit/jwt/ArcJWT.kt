package io.arcblock.walletkit.jwt

import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.arcblock.walletkit.did.DidUtils
import io.arcblock.walletkit.did.HashType
import io.arcblock.walletkit.did.KeyType
import io.arcblock.walletkit.hash.Hasher
import io.arcblock.walletkit.signer.Signer
import io.arcblock.walletkit.utils.encodeB64Url
import net.swiftzer.semver.SemVer
import java.util.Date

/**
 * Author       :paperhuang
 * Time         :2019/2/14
 * Edited By    :
 * Edited Time  :
 **/
object ArcJWT {

  private const val JWT_VERSION_REQUIRE_HASH_BEFORE_SIGN = "1.1.0"
  const val DEFAULT_JWT_VERSION = "1.0"

  fun genFeedBackJWT(
    jsonArray: JsonArray,
    sk: ByteArray,
    did: String,
    version: String,
    sessionId: String = "",
    exp: Long = 300L,
    action: String? = null,
    challenge: String? = null,
  ): String {
    val signType = DidUtils.decodeDidSignType(did)
    val jsonHeader = Header(signType.name, "JWT")
    val jsonBody = Body(
      iss = did,
      iat = (Date().time / 1000).toString(),
      nbf = (Date().time / 1000 - 10).toString(),
      exp = (Date(Date().time + exp * 1000L).time / 1000).toString(),
      version = version,
      sessionId = sessionId,
      requestedClaims = jsonArray,
      action = action,
      challenge = challenge,
    )
    val gs = Gson()
    val signContent =
      BaseEncoding.base64Url().encode(gs.toJson(jsonHeader).toByteArray()).replace("=", "")
        .plus(".")
        .plus(BaseEncoding.base64Url().encode(gs.toJson(jsonBody).toByteArray()).replace("=", ""))

    val versionPassIn = try {
      SemVer.parse(version)
    } catch (e: Exception) {
      SemVer.parse(DEFAULT_JWT_VERSION)
    }
    val signContentByteArray =
      if (versionPassIn >= SemVer.parse(JWT_VERSION_REQUIRE_HASH_BEFORE_SIGN)) {
        Hasher.hash(HashType.SHA3, signContent.toByteArray(), 1)
      } else {
        signContent.toByteArray()
      }

    val signature = Signer.sign(signType, signContentByteArray, sk)
    return signContent.plus(".").plus(signature.encodeB64Url()).replace("=", "")
  }

  fun parseJWT(token: String): JsonObject {
    val jwt = token.split(".")
    val body = String(BaseEncoding.base64Url().decode(jwt[1]))
    return JsonParser().parse(body).asJsonObject
  }

  fun verifyJWT(token: String, pk: ByteArray, type: KeyType, version: String): Boolean {
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] verifyJWT: token=$token")
    // Determine verification method from JWT header.alg (matching @arcblock/jwt behavior)
    val headerAlg = getHeaderAlg(token)
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] verifyJWT: headerAlg=$headerAlg, type=$type, version=$version, pkLen=${pk.size}")

    // Route to the correct verifier based on header.alg (primary) or KeyType (fallback)
    val alg = headerAlg.lowercase()
    val resolvedType = ALG_TO_KEY_TYPE[alg] ?: type

    if (resolvedType == KeyType.PASSKEY || alg == "passkey") {
      android.util.Log.d("ArcJWT", "[LOCAL-SDK] PASSKEY detected (alg=$alg), delegating to PasskeyJWTVerifier")
      val result = PasskeyJWTVerifier.verifyJWT(token, pk, version)
      android.util.Log.d("ArcJWT", "[LOCAL-SDK] PasskeyJWTVerifier result=$result")
      return result
    }

    val sig = token.substringAfterLast(".")
    val content = token.substringBeforeLast(".")

    val versionPassIn = try {
      SemVer.parse(version)
    } catch (e: Exception) {
      SemVer.parse(DEFAULT_JWT_VERSION)
    }

    val contentByteArray =
      if (versionPassIn >= SemVer.parse(JWT_VERSION_REQUIRE_HASH_BEFORE_SIGN)) {
        Hasher.hash(HashType.SHA3, content.toByteArray(), 1)
      } else {
        content.toByteArray()
      }

    val padded = when (sig.length % 4) {
      2 -> "$sig=="
      3 -> "$sig="
      else -> sig
    }
    val sigBytes = BaseEncoding.base64Url().decode(padded)
    val pkHex = pk.joinToString("") { "%02x".format(it) }
    val contentHashHex = contentByteArray.joinToString("") { "%02x".format(it) }
    val sigHex = sigBytes.joinToString("") { "%02x".format(it) }
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] verify: resolvedType=$resolvedType, alg=$alg, version=$version")
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] pk($pkHex)")
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] contentHash($contentHashHex)")
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] sig($sigHex) sigLen=${sigBytes.size}")
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] content(first100)=${content.take(100)}")
    val result = Signer.verify(resolvedType, contentByteArray, pk, sigBytes)
    android.util.Log.d("ArcJWT", "[LOCAL-SDK] Signer.verify result=$result for resolvedType=$resolvedType")
    return result
  }

  /**
   * Map JWT header alg values to KeyType, matching @arcblock/jwt's signer lookup.
   */
  private val ALG_TO_KEY_TYPE = mapOf(
    "ed25519" to KeyType.ED25519,
    "es256k" to KeyType.SECP256K1,
    "secp256k1" to KeyType.SECP256K1,
    "ethereum" to KeyType.ETHEREUM,
    "passkey" to KeyType.PASSKEY,
  )

  private fun getHeaderAlg(token: String): String {
    return try {
      val headerB64 = token.substringBefore(".")
      val padded = when (headerB64.length % 4) {
        2 -> "$headerB64=="
        3 -> "$headerB64="
        else -> headerB64
      }
      val headerJson = String(BaseEncoding.base64Url().decode(padded))
      val header = JsonParser().parse(headerJson).asJsonObject
      header.get("alg")?.asString ?: ""
    } catch (e: Exception) {
      ""
    }
  }

}
