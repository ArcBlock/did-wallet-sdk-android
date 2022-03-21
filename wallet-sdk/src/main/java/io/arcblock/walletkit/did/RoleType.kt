package io.arcblock.walletkit.did

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
  REGISTRY(16),
  TOKEN(17),
  FACTORY(18),
  ROLLUP(19),
  ANY(63);
}
