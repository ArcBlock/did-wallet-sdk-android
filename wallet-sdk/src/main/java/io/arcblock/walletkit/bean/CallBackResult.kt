package io.arcblock.walletkit.bean

import android.os.Parcel
import android.os.Parcelable

/**
 *
 *     █████╗ ██████╗  ██████╗██████╗ ██╗      ██████╗  ██████╗██╗  ██╗
 *    ██╔══██╗██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝
 *    ███████║██████╔╝██║     ██████╔╝██║     ██║   ██║██║     █████╔╝
 *    ██╔══██║██╔══██╗██║     ██╔══██╗██║     ██║   ██║██║     ██╔═██╗
 *    ██║  ██║██║  ██║╚██████╗██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗
 *    ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
 * Author       : ${EMAIL}
 * Time         : 2019-06-14
 * Edited By    :
 * Edited Time  :
 * Description  : Wallet App call back data
 **/
const val  RESULT_SUCCESS = 0
const val ERROR_WALLET = 100

const val ERROR_FORGE = 200

const val DATATYPE_HASH = 1
const val DATATYPE_CLAIM = 2

class CallBackResult(
  val resultCode: Int,
  val dataJson: String?,
  val dataType: Int,
  val errorInfo: String?
) : Parcelable {


  constructor(parcel: Parcel) : this(
    parcel.readInt(),
    parcel.readString(),
    parcel.readInt(),
    parcel.readString()
  ) {
  }

  fun verifyResult(): Boolean{
    return true
  }



  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(resultCode)
    parcel.writeString(dataJson)
    parcel.writeInt(dataType)
    parcel.writeString(errorInfo)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<CallBackResult> {
    override fun createFromParcel(parcel: Parcel): CallBackResult {
      return CallBackResult(parcel)
    }

    override fun newArray(size: Int): Array<CallBackResult?> {
      return arrayOfNulls(size)
    }
  }

}

