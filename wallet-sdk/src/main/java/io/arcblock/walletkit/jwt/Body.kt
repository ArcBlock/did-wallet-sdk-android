package io.arcblock.walletkit.jwt

import com.google.gson.JsonArray

/**
 * Author       :paperhuang
 * Time         :2019/2/18
 * Edited By    :
 * Edited Time  :
 **/
data class Body(
  var iss: String,
  var iat: String,
  var nbf: String,
  var exp: String,
  var version: String,
  var sessionId: String,
  var requestedClaims: JsonArray,
  var action: String? = null,
  var challenge: String? = null,
)
