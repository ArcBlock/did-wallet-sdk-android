package io.arcblock.walletkit.jwt

import com.google.common.io.BaseEncoding
import com.google.gson.JsonParser
import com.upokecenter.cbor.CBORObject
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

/**
 * Verifies passkey (WebAuthn/FIDO2) JWTs that use P-256 ECDSA.
 *
 * Passkey JWTs pack a WebAuthn assertion { authenticatorData, clientDataJSON, signature }
 * as base64url-encoded JSON in the JWT third segment instead of a hex signature.
 */
object PasskeyJWTVerifier {

  /**
   * Verify a passkey JWT.
   *
   * @param token The full JWT string (header.payload.assertion)
   * @param pk The public key bytes (COSE CBOR-encoded or uncompressed EC point)
   * @param version The JWT version string (e.g. "1.1.0"), reserved for future use
   * @return true if signature verification succeeds
   */
  fun verifyJWT(token: String, pk: ByteArray, version: String): Boolean {
    return try {
      val parts = token.split(".")
      if (parts.size != 3) return false

      // 1. Decode the third segment: base64url -> JSON -> { authenticatorData, clientDataJSON, signature }
      val assertionJson = String(base64UrlDecode(parts[2]), Charsets.UTF_8)
      val assertion = JsonParser().parse(assertionJson).asJsonObject

      val authenticatorDataB64 = assertion.get("authenticatorData")?.asString ?: return false
      val clientDataJSONB64 = assertion.get("clientDataJSON")?.asString ?: return false
      val signatureB64 = assertion.get("signature")?.asString ?: return false

      // 2. Decode authenticatorData from base64url -> raw bytes
      val authenticatorData = base64UrlDecode(authenticatorDataB64)

      // 3. Decode clientDataJSON from base64url -> raw bytes -> SHA-256 hash
      val clientDataJSON = base64UrlDecode(clientDataJSONB64)
      val clientDataHash = MessageDigest.getInstance("SHA-256").digest(clientDataJSON)

      // 4. signedData = authenticatorData || SHA-256(clientDataJSON)
      val signedData = authenticatorData + clientDataHash

      // 5. Decode signature from base64url -> DER-encoded ECDSA bytes
      val signatureBytes = base64UrlDecode(signatureB64)

      // 6. Parse COSE public key -> ECPublicKey
      val publicKey = parsePublicKey(pk)

      // 7. Verify with SHA256withECDSA
      val sig = Signature.getInstance("SHA256withECDSA")
      sig.initVerify(publicKey)
      sig.update(signedData)
      sig.verify(signatureBytes)
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Parse a public key from either COSE CBOR format or uncompressed point format.
   */
  internal fun parsePublicKey(pk: ByteArray): ECPublicKey {
    return try {
      parseCoseKey(pk)
    } catch (e: Exception) {
      // Fallback: uncompressed point (0x04 || x[32] || y[32]) or raw x||y (64 bytes)
      parseUncompressedPoint(pk)
    }
  }

  /**
   * Parse a COSE-encoded P-256 public key.
   * CBOR map: { 1(kty):2(EC2), -1(crv):1(P-256), -2(x):bytes, -3(y):bytes }
   */
  private fun parseCoseKey(pk: ByteArray): ECPublicKey {
    val cborMap = CBORObject.DecodeFromBytes(pk)
    val xBytes = cborMap[CBORObject.FromObject(-2)].GetByteString()
    val yBytes = cborMap[CBORObject.FromObject(-3)].GetByteString()
    return buildECPublicKey(xBytes, yBytes)
  }

  /**
   * Parse an uncompressed EC point: 0x04 || x[32] || y[32], or raw x||y (64 bytes).
   */
  private fun parseUncompressedPoint(pk: ByteArray): ECPublicKey {
    val offset = if (pk.size == 65 && pk[0] == 0x04.toByte()) 1 else 0
    val coordLen = (pk.size - offset) / 2
    val xBytes = pk.copyOfRange(offset, offset + coordLen)
    val yBytes = pk.copyOfRange(offset + coordLen, offset + coordLen * 2)
    return buildECPublicKey(xBytes, yBytes)
  }

  private fun buildECPublicKey(xBytes: ByteArray, yBytes: ByteArray): ECPublicKey {
    val x = BigInteger(1, xBytes)
    val y = BigInteger(1, yBytes)
    val point = ECPoint(x, y)
    val params = AlgorithmParameters.getInstance("EC")
    params.init(ECGenParameterSpec("secp256r1"))
    val ecSpec = params.getParameterSpec(ECParameterSpec::class.java)
    val keySpec = ECPublicKeySpec(point, ecSpec)
    return KeyFactory.getInstance("EC").generatePublic(keySpec) as ECPublicKey
  }

  internal fun base64UrlDecode(input: String): ByteArray {
    val padded = when (input.length % 4) {
      2 -> "$input=="
      3 -> "$input="
      else -> input
    }
    return BaseEncoding.base64Url().decode(padded)
  }
}
