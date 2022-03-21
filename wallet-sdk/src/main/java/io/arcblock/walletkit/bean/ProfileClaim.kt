package io.arcblock.walletkit.bean

import io.arcblock.walletkit.bean.ClaimType
import io.arcblock.walletkit.bean.IClaim
import io.arcblock.walletkit.bean.MetaInfo

class ProfileClaim(override val meta: MetaInfo, var items: List<String>) : IClaim {
  val type = ClaimType.PROFILE.toString()
}
