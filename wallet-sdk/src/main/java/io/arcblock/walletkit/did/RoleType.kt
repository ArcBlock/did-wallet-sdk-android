package io.arcblock.walletkit.did

import io.reactivex.internal.fuseable.QueueFuseable.ANY

/**
 * Author       :paperhuang
 * Time         :2019/2/14
 * Edited By    :
 * Edited Time  :
 *
 * @link https://github.com/ArcBlock/asset-chain/blob/master/core/mcrypto/lib/index.js#L137
 * 
 **/

enum class RoleType(val value: Int) {
  ACCOUNT(0),
  NODE(1),
  DEVICE(2),
  APPLICATION(3),
  SMART_CONTRACT(4),
  BOT(5),
  ASSET(6),
  STAKE(7),
  VALIDATOR(8),
  GROUP(9),
  TX(10),
  TETHER(11),
  SWAP(12),
  DELEGATE(13),
  VC(14),
  BLOCKLET(15),
  STORE(16),
  TOKEN(17),
  FACTORY(18),
  ROLLUP(19),
  STORAGE(20),
  PROFILE(21),
  PASSKEY(22),
  ANY(63),
  ERROR(-1),

}

fun String.toRoleType() = when(this.lowercase()) {
  RoleType.ACCOUNT.name.lowercase() -> RoleType.ACCOUNT
  RoleType.NODE.name.lowercase() -> RoleType.NODE
  RoleType.DEVICE.name.lowercase() -> RoleType.DEVICE
  RoleType.APPLICATION.name.lowercase() -> RoleType.APPLICATION
  RoleType.SMART_CONTRACT.name.lowercase() -> RoleType.SMART_CONTRACT
  RoleType.BOT.name.lowercase() -> RoleType.BOT
  RoleType.ASSET.name.lowercase() -> RoleType.ASSET
  RoleType.STAKE.name.lowercase() -> RoleType.STAKE
  RoleType.VALIDATOR.name.lowercase() -> RoleType.VALIDATOR
  RoleType.GROUP.name.lowercase() -> RoleType.GROUP
  RoleType.TX.name.lowercase() -> RoleType.TX
  RoleType.TETHER.name.lowercase() -> RoleType.TETHER
  RoleType.SWAP.name.lowercase() -> RoleType.SWAP
  RoleType.DELEGATE.name.lowercase() -> RoleType.DELEGATE
  RoleType.VC.name.lowercase() -> RoleType.VC
  RoleType.BLOCKLET.name.lowercase() -> RoleType.BLOCKLET
  RoleType.STORE.name.lowercase(),"registry" -> RoleType.STORE
  RoleType.TOKEN.name.lowercase() -> RoleType.TOKEN
  RoleType.FACTORY.name.lowercase() -> RoleType.FACTORY
  RoleType.ROLLUP.name.lowercase() -> RoleType.ROLLUP
  RoleType.STORAGE.name.lowercase() -> RoleType.STORAGE
  RoleType.PROFILE.name.lowercase() -> RoleType.PROFILE
  RoleType.PASSKEY.name.lowercase() -> RoleType.PASSKEY
  RoleType.ANY.name.lowercase() -> RoleType.ANY

  else -> RoleType.ERROR
}