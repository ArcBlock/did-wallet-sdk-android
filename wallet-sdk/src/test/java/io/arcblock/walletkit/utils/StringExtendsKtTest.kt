package io.arcblock.walletkit.utils

import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

class StringExtendsKtTest {

  @Test
  fun address() {
    Assert.assertEquals("zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb","did:abt:zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb".address())
    Assert.assertEquals("zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb","DID:Abt:zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb".address())
    Assert.assertEquals("zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb","zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb".address())
  }

  @Test
  fun did() {

    Assert.assertEquals("did:abt:zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb","zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb".did())
    Assert.assertEquals("did:abt:zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb","did:abt:zNKdPEPXWAydNbWtZTTYfs84XnxHhDG4b6kb".did())
  }
}