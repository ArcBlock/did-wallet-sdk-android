package io.arcblock.walletkit.jwt

import com.google.common.io.BaseEncoding
import com.google.gson.JsonObject
import com.upokecenter.cbor.CBORObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

class PasskeyJWTVerifierTest {

  private fun base64UrlEncode(data: ByteArray): String {
    return BaseEncoding.base64Url().encode(data).replace("=", "")
  }

  private fun generateP256KeyPair(): Pair<ECPrivateKey, ECPublicKey> {
    val kpg = KeyPairGenerator.getInstance("EC")
    kpg.initialize(ECGenParameterSpec("secp256r1"))
    val kp = kpg.generateKeyPair()
    return kp.private as ECPrivateKey to kp.public as ECPublicKey
  }

  private fun encodeCoseKey(pub: ECPublicKey): ByteArray {
    val w = pub.w
    val xBytes = bigIntToFixedBytes(w.affineX, 32)
    val yBytes = bigIntToFixedBytes(w.affineY, 32)

    val cborMap = CBORObject.NewMap()
    cborMap[CBORObject.FromObject(1)] = CBORObject.FromObject(2)       // kty: EC2
    cborMap[CBORObject.FromObject(3)] = CBORObject.FromObject(-7)      // alg: ES256
    cborMap[CBORObject.FromObject(-1)] = CBORObject.FromObject(1)      // crv: P-256
    cborMap[CBORObject.FromObject(-2)] = CBORObject.FromObject(xBytes) // x
    cborMap[CBORObject.FromObject(-3)] = CBORObject.FromObject(yBytes) // y
    return cborMap.EncodeToBytes()
  }

  private fun encodeUncompressedPoint(pub: ECPublicKey): ByteArray {
    val w = pub.w
    val xBytes = bigIntToFixedBytes(w.affineX, 32)
    val yBytes = bigIntToFixedBytes(w.affineY, 32)
    return byteArrayOf(0x04) + xBytes + yBytes
  }

  private fun bigIntToFixedBytes(value: java.math.BigInteger, length: Int): ByteArray {
    val bytes = value.toByteArray()
    return when {
      bytes.size == length -> bytes
      bytes.size > length -> bytes.copyOfRange(bytes.size - length, bytes.size)
      else -> ByteArray(length - bytes.size) + bytes
    }
  }

  /**
   * Build a synthetic passkey JWT with a valid WebAuthn assertion.
   */
  private fun buildPasskeyJWT(privateKey: ECPrivateKey): String {
    val header = base64UrlEncode("""{"alg":"Passkey","typ":"JWT"}""".toByteArray())
    val payload = base64UrlEncode(
      """{"iss":"did:abt:test","iat":"1711800000","exp":"9999999999","version":"1.1.0"}""".toByteArray()
    )
    val signingInput = "$header.$payload"

    // authenticatorData: 32-byte rpIdHash + 1 flag byte + 4-byte signCount
    val rpIdHash = MessageDigest.getInstance("SHA-256").digest("localhost".toByteArray())
    val flags = byteArrayOf(0x01)
    val signCount = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    val authenticatorData = rpIdHash + flags + signCount

    // clientDataJSON with challenge = SHA-256(signingInput) base64url-encoded
    val challengeHash = MessageDigest.getInstance("SHA-256").digest(signingInput.toByteArray())
    val challengeB64 = base64UrlEncode(challengeHash)
    val clientDataJSON =
      """{"type":"webauthn.get","challenge":"$challengeB64","origin":"https://localhost"}""".toByteArray()

    // signedData = authenticatorData || SHA-256(clientDataJSON)
    val clientDataHash = MessageDigest.getInstance("SHA-256").digest(clientDataJSON)
    val signedData = authenticatorData + clientDataHash

    // Sign with ECDSA P-256
    val sig = Signature.getInstance("SHA256withECDSA")
    sig.initSign(privateKey)
    sig.update(signedData)
    val signatureBytes = sig.sign()

    // Build assertion JSON
    val assertion = JsonObject()
    assertion.addProperty("authenticatorData", base64UrlEncode(authenticatorData))
    assertion.addProperty("clientDataJSON", base64UrlEncode(clientDataJSON))
    assertion.addProperty("signature", base64UrlEncode(signatureBytes))

    val assertionB64 = base64UrlEncode(assertion.toString().toByteArray())
    return "$signingInput.$assertionB64"
  }

  @Test
  fun testVerifyJWT_validSignature_coseKey() {
    val (privateKey, publicKey) = generateP256KeyPair()
    val cosePk = encodeCoseKey(publicKey)
    val jwt = buildPasskeyJWT(privateKey)

    assertTrue("Valid passkey JWT with COSE key should verify",
      PasskeyJWTVerifier.verifyJWT(jwt, cosePk, "1.1.0"))
  }

  @Test
  fun testVerifyJWT_validSignature_uncompressedPoint() {
    val (privateKey, publicKey) = generateP256KeyPair()
    val uncompressedPk = encodeUncompressedPoint(publicKey)
    val jwt = buildPasskeyJWT(privateKey)

    assertTrue("Valid passkey JWT with uncompressed point key should verify",
      PasskeyJWTVerifier.verifyJWT(jwt, uncompressedPk, "1.1.0"))
  }

  @Test
  fun testVerifyJWT_wrongKey_fails() {
    val (privateKey, _) = generateP256KeyPair()
    val (_, wrongPublicKey) = generateP256KeyPair()
    val cosePk = encodeCoseKey(wrongPublicKey)
    val jwt = buildPasskeyJWT(privateKey)

    assertFalse("JWT signed with different key should fail",
      PasskeyJWTVerifier.verifyJWT(jwt, cosePk, "1.1.0"))
  }

  @Test
  fun testVerifyJWT_malformedToken_fails() {
    val (_, publicKey) = generateP256KeyPair()
    val cosePk = encodeCoseKey(publicKey)

    assertFalse("Token with only 2 parts should fail",
      PasskeyJWTVerifier.verifyJWT("a.b", cosePk, "1.1.0"))
    assertFalse("Empty token should fail",
      PasskeyJWTVerifier.verifyJWT("", cosePk, "1.1.0"))
  }

  @Test
  fun testVerifyJWT_invalidAssertionJson_fails() {
    val (_, publicKey) = generateP256KeyPair()
    val cosePk = encodeCoseKey(publicKey)

    val header = base64UrlEncode("""{"alg":"Passkey"}""".toByteArray())
    val payload = base64UrlEncode("""{"iss":"test"}""".toByteArray())
    val badAssertion = base64UrlEncode("not-json".toByteArray())

    assertFalse("Non-JSON assertion segment should fail",
      PasskeyJWTVerifier.verifyJWT("$header.$payload.$badAssertion", cosePk, "1.1.0"))
  }

  @Test
  fun testVerifyJWT_missingAssertionFields_fails() {
    val (_, publicKey) = generateP256KeyPair()
    val cosePk = encodeCoseKey(publicKey)

    val header = base64UrlEncode("""{"alg":"Passkey"}""".toByteArray())
    val payload = base64UrlEncode("""{"iss":"test"}""".toByteArray())
    val incompleteAssertion = base64UrlEncode(
      """{"authenticatorData":"AAAA","clientDataJSON":"AAAA"}""".toByteArray()
    )

    assertFalse("Assertion missing signature field should fail",
      PasskeyJWTVerifier.verifyJWT("$header.$payload.$incompleteAssertion", cosePk, "1.1.0"))
  }

  @Test
  fun testParsePublicKey_cose() {
    val (_, publicKey) = generateP256KeyPair()
    val cose = encodeCoseKey(publicKey)
    val parsed = PasskeyJWTVerifier.parsePublicKey(cose)
    assertNotNull(parsed)
    assertTrue("Parsed COSE key x should match", parsed.w.affineX == publicKey.w.affineX)
    assertTrue("Parsed COSE key y should match", parsed.w.affineY == publicKey.w.affineY)
  }

  @Test
  fun testParsePublicKey_uncompressed() {
    val (_, publicKey) = generateP256KeyPair()
    val uncompressed = encodeUncompressedPoint(publicKey)
    val parsed = PasskeyJWTVerifier.parsePublicKey(uncompressed)
    assertNotNull(parsed)
    assertTrue("Parsed uncompressed key x should match", parsed.w.affineX == publicKey.w.affineX)
    assertTrue("Parsed uncompressed key y should match", parsed.w.affineY == publicKey.w.affineY)
  }

  @Test
  fun testBase64UrlDecode_noPadding() {
    val original = "Hello, World!"
    val encoded = base64UrlEncode(original.toByteArray())
    val decoded = PasskeyJWTVerifier.base64UrlDecode(encoded)
    assertTrue("Round-trip base64url should match", original == String(decoded))
  }

  @Test
  fun testVerifyJWT_throughArcJWT_passkeyKeyType() {
    // Verify that ArcJWT.verifyJWT routes PASSKEY KeyType to PasskeyJWTVerifier
    val (privateKey, publicKey) = generateP256KeyPair()
    val cosePk = encodeCoseKey(publicKey)
    val jwt = buildPasskeyJWT(privateKey)

    assertTrue("ArcJWT.verifyJWT with PASSKEY type should delegate to PasskeyJWTVerifier",
      ArcJWT.verifyJWT(jwt, cosePk, io.arcblock.walletkit.did.KeyType.PASSKEY, "1.1.0"))
  }

  @Test
  fun testVerifyJWT_throughArcJWT_headerAlgDetection() {
    // Verify that ArcJWT.verifyJWT detects passkey via header alg even with ED25519 KeyType
    // This matches server-side @arcblock/jwt behavior: header.alg === 'Passkey'
    val (privateKey, publicKey) = generateP256KeyPair()
    val cosePk = encodeCoseKey(publicKey)
    val jwt = buildPasskeyJWT(privateKey)

    // JWT header says alg:"Passkey", so it should be routed to PasskeyJWTVerifier
    // even though we pass ED25519 as the KeyType
    assertTrue("ArcJWT.verifyJWT should detect passkey via header alg field",
      ArcJWT.verifyJWT(jwt, cosePk, io.arcblock.walletkit.did.KeyType.ED25519, "1.1.0"))
  }
}
