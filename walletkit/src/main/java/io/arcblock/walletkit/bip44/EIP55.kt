package io.arcblock.walletkit.bip44

import io.arcblock.walletkit.did.HashType
import io.arcblock.walletkit.did.toHexString
import io.arcblock.walletkit.utils.address
import io.arcblock.walletkit.utils.did
import io.arcblock.walletkit.utils.hash
import java.util.*

/**
 *
 *     █████╗ ██████╗  ██████╗██████╗ ██╗      ██████╗  ██████╗██╗  ██╗
 *    ██╔══██╗██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝
 *    ███████║██████╔╝██║     ██████╔╝██║     ██║   ██║██║     █████╔╝
 *    ██╔══██║██╔══██╗██║     ██╔══██╗██║     ██║   ██║██║     ██╔═██╗
 *    ██║  ██║██║  ██║╚██████╗██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗
 *    ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
 * Author       : $EMAIL
 * Time         : 2019-06-27
 * Edited By    :
 * Edited Time  :
 * Description  : ERC-55 Checksum as in https://github.com/ethereum/EIPs/blob/master/EIPS/eip-55.md
 **/

fun String.eip55() = this.removePrefix("0x").toLowerCase(Locale.ROOT).let { cleanhex ->
  val ret = cleanhex.toByteArray()
    .hash(HashType.KECCAK)
    .toHexString().let {hexHash ->
      cleanhex.mapIndexed { index: Int, hexChar: Char ->
        when {
          hexChar in '0'..'9' -> hexChar
          hexHash[index] in '0'..'7' -> hexChar.toLowerCase()
          else -> hexChar.toUpperCase()
        }
      }.joinToString("")
    }
  "0x".plus(ret)
}


