package io.arcblock.walletkit.bean

/**
 *
 *     █████╗ ██████╗  ██████╗██████╗ ██╗      ██████╗  ██████╗██╗  ██╗
 *    ██╔══██╗██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝
 *    ███████║██████╔╝██║     ██████╔╝██║     ██║   ██║██║     █████╔╝
 *    ██╔══██║██╔══██╗██║     ██╔══██╗██║     ██║   ██║██║     ██╔═██╗
 *    ██║  ██║██║  ██║╚██████╗██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗
 *    ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
 * Author       : shan@arcblock.io
 * Time         : 2019-06-17
 * Edited By    :
 * Edited Time  :
 * Description  :
 **/
class ChainInfo {
  /**
   * the endpoint of your Forge Network
   */
  var chainHost: String = ""

  /**
   * the ID of Forge Network
   */
  var chainId: String = ""

  /**
   * the token symbol of Forge Network
   */
  var chainToken: String = ""

  /**
   * the decimal of your token , default is 16
   */
  var decimals: Int = 16

  /**
   * the token icon of Forge Network
   */
  var icon: String = ""


  /**
   * forge version
   */
  var chainVersion: String = ""

}
