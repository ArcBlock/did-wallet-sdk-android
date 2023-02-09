package io.arcblock.walletkit.did

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class RoleTypeTest{
  @Test
  fun testRoleTypeParse() {
    Assert.assertEquals(0, "account".toRoleType().value)
    Assert.assertEquals(1, "node".toRoleType().value)
    Assert.assertEquals(2, "device".toRoleType().value)
    Assert.assertEquals(3, "application".toRoleType().value)
    Assert.assertEquals(4, "smart_contract".toRoleType().value)
    Assert.assertEquals(5, "bot".toRoleType().value)
    Assert.assertEquals(6, "asset".toRoleType().value)
    Assert.assertEquals(7, "stake".toRoleType().value)
    Assert.assertEquals(8, "validator".toRoleType().value)
    Assert.assertEquals(9, "group".toRoleType().value)
    Assert.assertEquals(10, "tx".toRoleType().value)
    Assert.assertEquals(11, "tether".toRoleType().value)
    Assert.assertEquals(12, "swap".toRoleType().value)
    Assert.assertEquals(13, "delegate".toRoleType().value)
    Assert.assertEquals(14, "vc".toRoleType().value)
    Assert.assertEquals(15, "blocklet".toRoleType().value)
    Assert.assertEquals(16, "store".toRoleType().value)
    Assert.assertEquals(16, "registry".toRoleType().value)
    Assert.assertEquals(17, "token".toRoleType().value)
    Assert.assertEquals(18, "factory".toRoleType().value)
    Assert.assertEquals(19, "rollup".toRoleType().value)
    Assert.assertEquals(20, "storage".toRoleType().value)
    Assert.assertEquals(63, "any".toRoleType().value)
    Assert.assertEquals(-1, "unknown".toRoleType().value)

  }

}