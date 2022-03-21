package io.arcblock.walletkit.bip44

import org.junit.Test

import org.junit.Assert.*
import java.util.*

/**
 * █████╗ ██████╗  ██████╗██████╗ ██╗      ██████╗  ██████╗██╗  ██╗
 * ██╔══██╗██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝
 * ███████║██████╔╝██║     ██████╔╝██║     ██║   ██║██║     █████╔╝
 * ██╔══██║██╔══██╗██║     ██╔══██╗██║     ██║   ██║██║     ██╔═██╗
 * ██║  ██║██║  ██║╚██████╗██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗
 * ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
 * Author       : $EMAIL
 * Time         : 2019-06-27
 * Edited By    :
 * Edited Time  :
 * Description  :
 */
class EIP55KtTest {

    @Test
    fun eip55() {
      assertEquals("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
        ,"0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed".toLowerCase(Locale.ROOT).eip55())

      assertEquals("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
        ,"0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359".eip55())

      assertEquals("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
        ,"0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359".toLowerCase(Locale.ROOT).eip55())

      assertEquals("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
        ,"0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359".toLowerCase(Locale.ROOT).eip55())
    }
}
