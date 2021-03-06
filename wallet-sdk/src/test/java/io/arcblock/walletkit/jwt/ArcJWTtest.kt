package io.arcblock.walletkit.jwt

import com.google.common.io.BaseEncoding
import com.google.gson.JsonArray
import io.arcblock.walletkit.did.DidType
import io.arcblock.walletkit.did.DidType.Companion
import io.arcblock.walletkit.did.IdGenerator
import io.arcblock.walletkit.did.KeyType.ED25519
import io.arcblock.walletkit.utils.Base58Btc
import org.junit.Assert
import org.junit.Test

/**
 * Author       :paperhuang
 * Time         :2019/2/18
 * Edited By    :
 * Edited Time  :
 **/
class ArcJwtTest{

  @Test
  fun testParseJWT(){
    val authInfo = "eyJhbGciOiJFZDI1NTE5IiwidHlwIjoiSldUIn0.eyJhY3Rpb24iOiJyZXNwb25zZUF1dGgiLCJhcHBJbmZvIjp7ImRlc2NyaXB0aW9uIjoiQSBzaW1wbGUgd29ya3Nob3AgZm9yIGRldmVsb3BlcnMgdG8gcXVpY2tseSBkZXZlbG9wLCBkZXNpZ24gYW5kIGRlYnVnIHRoZSBESUQgZmxvdy4iLCJsb2dvIjoiaHR0cHM6Ly9leGFtcGxlLWFwcGxpY2F0aW9uL2xvZ28iLCJuYW1lIjoiQUJUIERJRCBXb3Jrc2hvcCJ9LCJyZXF1ZXN0ZWRDbGFpbXMiOlt7Iml0ZW1zIjpbImJpcnRoZGF5IiwiZnVsbE5hbWUiXSwibWV0YSI6eyJkZXNjcmlwdGlvbiI6IlBsZWFzZSBwcm92aWRlIHlvdXIgcHJvZmlsZSBpbmZvcm1hdGlvbi4ifSwidHlwZSI6InByb2ZpbGUifV0sInVybCI6Imh0dHA6Ly8xMC4xMTMuMTAuMTMxOjQwMDAvYXBpL2xvZ29uLyIsImV4cCI6IjE1NTA0NzI1OTQiLCJpYXQiOiIxNTUwNDcwNzk0IiwiaXNzIjoiZGlkOmFidDp6MTFLUHNNOVZ1OU45ZHRSczU2UVI0RzdIZ3RKaGVuZng2UnQiLCJuYmYiOiIxNTUwNDcwNzk0In0.Ehd0NHJ8k926X3mDtFZ3PLX2eMhWfF2HKFXrHa46m3Q2_Gh7ASfqCLOtxJCbyfsTWYCUrur5Tbt3JG3NNKbgCA"
    val jwt = authInfo.split(".")
    val header =  BaseEncoding.base64Url().decode(jwt[0])
    val body =  BaseEncoding.base64Url().decode(jwt[1])

    System.out.println("header:${String(header)}")
    System.out.println("body:${String(body)}")
  }

  @Test
  fun testSignature(){
    var //token = "eyJhbGciOiJFZDI1NTE5IiwidHlwIjoiSldUIn0.eyJhcHBJbmZvIjp7ImNoYWluSG9zdCI6Imh0dHA6Ly8xNTkuNjUuMTM4Ljc1OjgyMTAvYXBpLyIsImNoYWluSWQiOiJmb3JnZSIsImNoYWluVG9rZW4iOiJUQkEiLCJkZWNpbWFscyI6MTYsImRlc2NyaXB0aW9uIjoi5pyA5YWo5pyA5paw5Y675Lit5b-D5YyW5oqA5pyv6K--5aCC77yM6K6p5L2g5a2m55-l6K-G6aKG6K-B5Lmm77yM6L-Y5pyJVEJB5aWW5Yqx5ZOm77yBIiwiaWNvbiI6Ii9pbWFnZXMvbG9nb0AyeC5wbmciLCJuYW1lIjoiQXJjQmxvY2sg5bCP6K--5aCCIiwic3VidGl0bGUiOiLorqnkvaDlv6vpgJ_kuobop6NBcmNCbG9ja-aKgOacryJ9LCJyZXF1ZXN0ZWRDbGFpbXMiOlt7ImRpZCI6IiIsImRpZF90eXBlIjoiYXNzZXQiLCJtZXRhIjp7ImRlc2NyaXB0aW9uIjoiUGxlYXNlIHNlbGVjdCBhc3NldCBTdXBlciBDZXJ0IHRvIGNvbnRpbnVlLiJ9LCJ0YXJnZXQiOiJTdXBlciBDZXJ0IiwidHlwZSI6ImRpZCJ9XSwidXJsIjoiaHR0cDovLzE1OS42NS4xMzguNzU6NDAwMC9hcGkvdHJhbnNhY3Rpb24vNiIsIndvcmtmbG93Ijp7ImRlc2NyaXB0aW9uIjoi6auY57qn6K-B5Lmm5Y-v5Lul6aKG5Y-WMTIyQlRBIn0sImV4cCI6IjE1NTM0ODM4OTciLCJpYXQiOiIxNTUzNDgyMDk3IiwiaXNzIjoiZGlkOmFidDp6TktaWmFLUmNIcFdKcTRURzdoMlJKUEM1RXRFU3BaQldGNzYiLCJuYmYiOiIxNTUzNDgyMDk3In0.OCWOsIc9aObR1TJu0fLso8iYwxmbwxx81_Y8b148vq_ZCi3WDQis9vRVRC2xsMTkd-xtbo-FxvHCylUcEnvGBQ"
        token=  "eyJhbGciOiJFZDI1NTE5IiwidHlwIjoiSldUIn0.eyJhcHBJbmZvIjp7ImNoYWluSG9zdCI6Imh0dHA6Ly8xNTkuNjUuMTM4Ljc1OjgyMTAvYXBpLyIsImNoYWluSWQiOiJmb3JnZSIsImNoYWluVG9rZW4iOiJUQkEiLCJkZWNpbWFscyI6MTYsImRlc2NyaXB0aW9uIjoi5pyA5YWo5pyA5paw5Y675Lit5b-D5YyW5oqA5pyv6K--5aCC77yM6K6p5L2g5a2m55-l6K-G6aKG6K-B5Lmm77yM6L-Y5pyJVEJB5aWW5Yqx5ZOm77yBIiwiaWNvbiI6Ii9pbWFnZXMvbG9nb0AyeC5wbmciLCJuYW1lIjoiQXJjQmxvY2sg5bCP6K--5aCCIiwic3VidGl0bGUiOiLorqnkvaDlv6vpgJ_kuobop6NBcmNCbG9ja-aKgOacryJ9LCJyZXF1ZXN0ZWRDbGFpbXMiOlt7ImRpZCI6IiIsImRpZF90eXBlIjoiYXNzZXQiLCJtZXRhIjp7ImRlc2NyaXB0aW9uIjoiUGxlYXNlIHNlbGVjdCBhc3NldCBBcmNCbG9jayBTZW5pb3IgQ2VydCB0byBjb250aW51ZS4ifSwidGFyZ2V0IjoiQXJjQmxvY2sgU2VuaW9yIENlcnQiLCJ0eXBlIjoiZGlkIn1dLCJ1cmwiOiJodHRwOi8vMTU5LjY1LjEzOC43NTo0MDAwL2FwaS90cmFuc2FjdGlvbi81Iiwid29ya2Zsb3ciOnsiZGVzY3JpcHRpb24iOiLmga3llpzkvaDvvIzlj6_ku6Xpooblj5YxMjJCVEEifSwiZXhwIjoiMTU1MzQ4NDE2OCIsImlhdCI6IjE1NTM0ODIzNjgiLCJpc3MiOiJkaWQ6YWJ0OnpOS1paYUtSY0hwV0pxNFRHN2gyUkpQQzVFdEVTcFpCV0Y3NiIsIm5iZiI6IjE1NTM0ODIzNjgifQ.tOxNKvVFQTgFP4Juua9xV4xnCbTfnR262YMEtQDj54jZfFTzhmujSQgOgNjjBwqzU_cNMu-7XkYMxv6CnPr1Dg"
    var pk = Base58Btc.decode("z7znjqzuq8JWTdEU7gamu4L5Jm1TphWNYqEnKPRpmnXve")
    var sk =  BaseEncoding.base16().decode("F68C1D234D6DCCE7B09115B83F34ABEE9FF1FC536E3FA44E6FD2C63B5F4FD700DCADD4FE1D595DA9A53910AF1B45D26FF4D1F4AC91486244394A5BDF50456407".toUpperCase())
    sig(token,pk,sk)

    token = "eyJhbGciOiAiRWQyNTUxOSIsICJ0eXAiOiAiSldUIn0.eyJpc3MiOiAiZGlkOmFidDp6MVVUOWFuMVo0VzFnbm16QVNuZUVSMko1ZXF0eDVqZndneCIsICJpYXQiOiAxNTUzNzQ2NzIzLCAibmJmIjogMTU1Mzc0NjcyMywgImV4cCI6IDE1NTM3NDg1MjMsICJ1cmwiOiAiaHR0cDovLzEwLjExMy4xMS40MDo1MDAwL2FwaS9tb2JpbGUtYnV5LXRpY2tldC8iLCAiYWN0aW9uIjogInJlc3BvbnNlQXV0aCIsICJhcHBJbmZvIjogeyJjaGFpbkhvc3QiOiAiaHR0cDovLzEwLjExMy4xMS40MDo4MjExL2FwaSIsICJjaGFpbklkIjogImZvcmdlIiwgImNoYWluX3Rva2VuIjogIlRCQSIsICJkZWNpbWFscyI6IDE2LCAiZGVzY3JpcHRpb24iOiAiQ3JlYXRlIGFuZCBqb2luIGV2ZW50cyBvbiBFdmVudCBDaGFpbiIsICJpY29uIjogImh0dHA6Ly9kaWQtd29ya3Nob3AuYXJjYmxvY2suY286NTAwMC9zdGF0aWMvaW1hZ2VzL2V2ZW50Y2hhaW4ucG5nIiwgIm5hbWUiOiAiRXZlbnQgQ2hhaW4iLCAic3VidGl0bGUiOiAiQSBkZWNlbnRyYWxpemVkIHNvbHV0aW9uIGZvciBldmVudHMifSwgInJlcXVlc3RlZENsYWltcyI6IFt7ImRhdGEiOiAiejY2aFpLVUNkMlVRNUxxeXVteUVyS2JYd0ZOWmhLMTZlUWs5NkVCdFVCQlJkIiwgIm1ldGEiOiB7ImRlc2NyaXB0aW9uIjogIkNvbmZpcm0gdGhlIHB1cmNoYXNlIGJlbG93LiJ9LCAibWV0aG9kIjogInNoYTMiLCAib3JpZ2luIjogIno0OVBvMm9jN1RDeFI1M3dvbkhtYm5MTWpMYXJIRm9rcG5mN0VyeVpvQUhUUUNIcVR3ZlA1Sk1ucndKaW81czdiUk16R3VKM2l6b2hzOHVzUXRkYmpHeDV4Y3hzWlhKcExWRWVTMnNBcjhzcWRQbWdkN1g1WG5NRzlrWm05alViTVNoS1NWdGFnemZjOFlQbTJxR05YRU4zdnVkZkRSTnhFSHN0V0Y3ZGdBeG1oTmVHNk1tTG9zWDhwY2NTUDFKb1N1QXVtQ2JEQXBuRm5xRGlnemVUbVRoZnVmR3B5UEczTHpMM2tkUGNIZ1NGazhOa3czRlZ1QTdQZjV4dUpWczZwOExTYWVEWFlYVGlCS1VxWmFXaTNBUHhGRGF5eWE1ZTJLRjJmeXBrTFpKNkZEaG5Xa1RpYWhwdVNYTnRlZFUyNkYzUW84eHRiTExkTFZvWmpSZHJLYW9QeldVN0xSNTN0YlRnTXI4dXBnc1J4dzZtZjZMWFdTdFpSTXBMSlJZM3UxWVpYOXVDOTdabzdIZ3ZZMkdlY3ppb1g0Y0ZTckI1azVXNzc1YVA5RDFHemU2WjVHZDhEaEJlY3g5amhHcmRDclhYb3N1bVh1UjdCRXM4N1hWNEduNHNuVlc5U2pGZE1hQyIsICJ0eXBlIjogInNpZ25hdHVyZSJ9XSwgIndvcmtmbG93IjogeyJkZXNjcmlwdGlvbiI6ICJidXktdGlja2V0In19.A5Bod62ogA5IAHF33s9wWm_s2-ZyqIhkx1mM-9XvLqegqXkTqlnubbWADakqW-4Y9wsKw3iCobUU9h_QaQVbDQ"
    pk = Base58Btc.decode("zExrfT2pXtVqdAqgZwjvdMBo5RpqSqn1fa43Wp93peuSR")
    sk =  BaseEncoding.base16().decode("F68C1D234D6DCCE7B09115B83F34ABEE9FF1FC536E3FA44E6FD2C63B5F4FD700DCADD4FE1D595DA9A53910AF1B45D26FF4D1F4AC91486244394A5BDF50456407".toUpperCase())

    sig(token,pk,sk)
  }

  fun sig(token:String,pk: ByteArray, sk: ByteArray){
    val jwt = token.split(".")

    val sig = io.arcblock.walletkit.signer.Signer.sign(ED25519,token.substringBeforeLast(".").toByteArray(),sk)
    println("sig:${BaseEncoding.base64Url().encode(sig)}")
    println(" js:${jwt[2]}")
    //    val newPk = IdGenerator.sk2pk(ED25519,sk)
    //    val ppk = Base58Btc.encode(IdGenerator.sk2pk(ED25519,sk))
    //    println("pk:$ppk")
    val ok = io.arcblock.walletkit.signer.Signer.verify(ED25519,token.substringBeforeLast(".").toByteArray(),pk,BaseEncoding.base64Url().decode(jwt[2]))
    println("result:$ok")

  }

  @Test
  fun testGenRespon() {
    val sk = "18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725"
    val skByte = BaseEncoding.base16().decode(sk)
    val did = IdGenerator.sk2did(skByte, DidType.DID_TYPE_FORGE)
    val respon = ArcJWT.genFeedBackJWT(JsonArray(),skByte, did, "1.1.0")
    val json = ArcJWT.parseJWT(respon)
    Assert.assertEquals(json["iss"].asString, did)

  }
}
