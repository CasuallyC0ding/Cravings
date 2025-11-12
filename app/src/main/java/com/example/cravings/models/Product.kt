package com.example.cravings.models

import android.os.Parcel
import android.os.Parcelable

data class Product(
    var name: String? = null,
    var description: String? = null,
    var price: Double? = null,
    var imageUrl: String? = null,
    var stock: Int? = null,
    var productId: Int? = null,
    var selectedQuantity: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeValue(price)
        parcel.writeString(imageUrl)
        parcel.writeValue(stock)
        parcel.writeValue(productId)
        parcel.writeInt(selectedQuantity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product = Product(parcel)
        override fun newArray(size: Int): Array<Product?> = arrayOfNulls(size)
    }
}
