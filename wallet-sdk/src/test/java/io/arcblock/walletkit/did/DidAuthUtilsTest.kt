package io.arcblock.walletkit.did

import io.arcblock.walletkit.bean.AppInfo
import io.arcblock.walletkit.bean.MetaInfo
import io.arcblock.walletkit.bean.ProfileClaim
import io.arcblock.walletkit.bean.WalletInfo
import io.arcblock.walletkit.utils.decodeB64
import io.arcblock.walletkit.utils.encodeB58
import org.junit.Test

/**
 * █████╗ ██████╗  ██████╗██████╗ ██╗      ██████╗  ██████╗██╗  ██╗
 * ██╔══██╗██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝
 * ███████║██████╔╝██║     ██████╔╝██║     ██║   ██║██║     █████╔╝
 * ██╔══██║██╔══██╗██║     ██╔══██╗██║     ██║   ██║██║     ██╔═██╗
 * ██║  ██║██║  ██║╚██████╗██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗
 * ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
 * Author       : $EMAIL
 * Time         : 2019-07-11
 * Edited By    :
 * Edited Time  :
 * Description  :
 */
class DidAuthUtilsTest {


  @Test
  fun fullRequest() {
    //val chainHost = " https://2d373f91-4ea8-4e68-b6c3-d08bbe222dca.mock.pstmn.io/api/"
    val appInfo = AppInfo().let {
      it.description = "Swap Test"
      it.icon = "http://10.113.10.166:8807/images/logo@2x.png"
      it.name = "Anbillum Company"
      it.decimals = 16
      it.publisher = "did:abt:z1c8G4fSTHE5gJ4FKcMthQY1t5RDbXzURV2"
      it
    }
    //    val sw = SwapClaim(AgreementMeta("You are making a swap", ""), arrayOf("zjdfPMErUV1Qu2jpYpLNa2zcrDGzqpbzQ8cQ"),
    //      "https://zinc.abtnetwork.io/api", , "https://test.abtnetwork.io/api", "")
    val sk =
      "dxX9ASBY+BTNayyTnIRC4A8QadAFi3F+GuE3+It8ltcM0h56H8xeJqTHn2w03uEfKJ801fjmeaygaIt0rHBaNg==".decodeB64()
    val pk = "DNIeeh/MXiakx59sNN7hHyifNNX45nmsoGiLdKxwWjY=".decodeB64()
    println("did:${IdGenerator.pk2did(pk)}")
    println("pk:${pk.encodeB58()}")

    val claim = ProfileClaim(
      MetaInfo("Mock data", ""),
      arrayListOf("name", "avatar", "signature", "birthday", "phone", "email")
    )
    val content =
      DidAuthUtils.createDidAuthToken(arrayOf(claim), appInfo, "", System.currentTimeMillis(),
        WalletInfo("zNKnt1zevjUXPVRuoUSPguPVf7iDKK4Fh9mg", pk)
          .let {
            it.sk = sk
            it
          }
      )
//    var content = "{\"alg\":\"ED25519\",\"typ\":\"JWT\"}".toByteArray().encodeB64Url().replace("=", "").plus(".").plus(
//      ("{ \"appInfo\": { \"description\": \"paper test\", \"icon\": \"http://10.113.10.166:8807/images/logo@2x.png\", \"name\": \"无良奸商\", \"subtitle\": null }, \"chainInfo\": { \"chainHost\": \"$chainHost\", \"chainId\": \"forge\", \"chainToken\": \"ABT\", \"chainVersion\": \"0.32.2\", \"decimals\": 18}, \"requestedClaims\": [ { \"type\": \"swap\", \"meta\": { \"description\": \"You are making a swap trade.\" }, \"offerAssets\": [\"zjdfPMErUV1Qu2jpYpLNa2zcrDGzqpbzQ8cQ\"], \"offerChain\":\"https://test.abtnetwork.io/api\", \"demandToken\": 3, \"demandChain\":\"https://zinc.abtnetwork.io/api\", \"swap_addr\":\"\" } ], " +
//          "\"url\": \"http://211.159.155.66:8000/\", \"workflow\": { \"description\": \"swap\" }," +
//          "\"exp\": \"1572812982\", \"iat\": \"1562811182\", \"iss\": \"did:abt:z1c8G4fSTHE5gJ4FKcMthQY1t5RDbXzURV2\", \"nbf\": \"1562811182\" }"
//          )          .toByteArray().encodeB64Url().replace("=",""))
//    var sig = Signer.sign(ED25519, content.toByteArray(), sk!!).encodeB64Url().replace("=","")
    println("jwt:$content")


  }

}
