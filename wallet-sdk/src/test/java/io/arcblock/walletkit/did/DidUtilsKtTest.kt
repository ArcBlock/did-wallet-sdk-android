package io.arcblock.walletkit.did

import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

/**
 * █████╗ ██████╗  ██████╗██████╗ ██╗      ██████╗  ██████╗██╗  ██╗
 * ██╔══██╗██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝
 * ███████║██████╔╝██║     ██████╔╝██║     ██║   ██║██║     █████╔╝
 * ██╔══██║██╔══██╗██║     ██╔══██╗██║     ██║   ██║██║     ██╔═██╗
 * ██║  ██║██║  ██║╚██████╗██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗
 * ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
 * Author       : $EMAIL
 * Time         : 2019-07-19
 * Edited By    :
 * Edited Time  :
 * Description  :
 */
class DidUtilsKtTest {

    @Test
    fun isValidDID() {
      assertTrue("did:abt:z1muQ3xqHQK2uiACHyChikobsiY5kLqtShA".isValidDID())
      assertTrue("z1muQ3xqHQK2uiACHyChikobsiY5kLqtShA".isValidDID())
      assertTrue(!"z2muQ3xqHQK2uiACHyChikobsiY5kLqtShA".isValidDID())
      assertTrue(!"z1muQ3xqHQK2uiACHyChikobsiY5kLqtSha".isValidDID())
    }


}
