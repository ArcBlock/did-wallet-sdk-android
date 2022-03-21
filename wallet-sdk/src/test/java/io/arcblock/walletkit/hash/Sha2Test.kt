package io.arcblock.walletkit.hash

import com.google.common.io.BaseEncoding
import org.junit.Assert
import org.junit.Test
import java.security.Security

class Sha2Test {

  @Test
  fun testSha2() {
    val algorithms: Set<String> = Security.getAlgorithms("MessageDigest")
    println(algorithms)

    val inputs = arrayOf(
      "abcd",
      "1234",
      "ABC!234*",
      "E4852B7091317E3622068E62A5127D1FB0D4AE2FC50213295E10652D2F0ABFC7"
    )
    val hash224 = arrayOf(
      "A76654D8E3550E9A2D67A0EEB6C67B220E5885EDDD3FDE135806E601",
      "99FB2F48C6AF4761F904FC85F95EB56190E5D40B1F44EC3A9C1FA319",
      "20BA36781F2F26749DD6364F52554B9EC2A2B88512E6F9C27979802D",
      "D2A6CA1BEA8BC0E28D5DE3E079407B1BB16CDD7BFF65ABCAE9F186F9"
    )
    val hash256 = arrayOf(
      "88D4266FD4E6338D13B845FCF289579D209C897823B9217DA3E161936F031589",
      "03AC674216F3E15C761EE1A5E255F067953623C8B388B4459E13F978D7C846F4",
      "EBD1B0F4D006B3AAFA93D86AAD9C8A3C59A736A60D6F464F51E54EF61043467A",
      "228CD829C2C81ACDB704049C67C898329571C0A577913F0508CF5282841D6AF5"
    )
    val hash384 = arrayOf(
      "1165B3406FF0B52A3D24721F785462CA2276C9F454A116C2B2BA20171A7905EA5A026682EB659C4D5F115C363AA3C79B",
      "504F008C8FCF8B2ED5DFCDE752FC5464AB8BA064215D9C5B5FC486AF3D9AB8C81B14785180D2AD7CEE1AB792AD44798C",
      "2A86C218F7B896485EA39F5758999C932A212CFF0309ACB8E8621CAD682CA1B35CC41154AFB4A1BF050D97E2A0EC7C14",
      "A5088E41EAED5FF6E3695B661D2403103A99C72A8735FB9BEA33C9D2A0E57F88EF642A9170929389854FFE3D9F50BF10"
    )
    val hash512 = arrayOf(
      "D8022F2060AD6EFD297AB73DCC5355C9B214054B0D1776A136A669D26A7D3B14F73AA0D0EBFF19EE333368F0164B6419A96DA49E3E481753E7E96B716BDCCB6F",
      "D404559F602EAB6FD602AC7680DACBFAADD13630335E951F097AF3900E9DE176B6DB28512F2E000B9D04FBA5133E8B1C6E8DF59DB3A8AB9D60BE4B97CC9E81DB",
      "14BEA590E28F4EE0C16336887F7E3C3E6AA744F37E09F067655E6326B905ACBD7DBE48656FBC0C71BE0C7A4776B174E380E14EF336D35ED7027E5D9C6256F947",
      "94C558C988D764655D5D7467D49AF8C271F3CBAB23BAC6069AA681044D921AAC1893B49A72A208553F769AF92DC829831837AB652245843FC836C02155E9182D"
    )

    inputs.forEachIndexed { index, input ->
      if (index == inputs.size - 1) {
        Assert.assertEquals(
          hash224[index],
          BaseEncoding.base16()
            .encode(ArcSha2Hasher.sha224ForJavaTest(BaseEncoding.base16().decode(input), 1))
        )
        Assert.assertEquals(
          hash256[index],
          BaseEncoding.base16().encode(ArcSha2Hasher.sha256(BaseEncoding.base16().decode(input), 1))
        )
        Assert.assertEquals(
          hash384[index],
          BaseEncoding.base16().encode(ArcSha2Hasher.sha384(BaseEncoding.base16().decode(input), 1))
        )
        Assert.assertEquals(
          hash512[index],
          BaseEncoding.base16().encode(ArcSha2Hasher.sha512(BaseEncoding.base16().decode(input), 1))
        )
      } else {
        Assert.assertEquals(
          hash224[index],
          BaseEncoding.base16().encode(ArcSha2Hasher.sha224ForJavaTest(input.toByteArray(), 1))
        )
        Assert.assertEquals(
          hash256[index],
          BaseEncoding.base16().encode(ArcSha2Hasher.sha256(input.toByteArray(), 1))
        )
        Assert.assertEquals(
          hash384[index],
          BaseEncoding.base16().encode(ArcSha2Hasher.sha384(input.toByteArray(), 1))
        )
        Assert.assertEquals(
          hash512[index],
          BaseEncoding.base16().encode(ArcSha2Hasher.sha512(input.toByteArray(), 1))
        )
      }

    }
  }
}
