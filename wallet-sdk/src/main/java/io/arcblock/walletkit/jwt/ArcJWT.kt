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
import java.util.*

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

    return Signer.verify(type, contentByteArray, pk, BaseEncoding.base64Url().decode(sig))
  }

}
