package com.v19.gal.data.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "images", primaryKeys = ["id", "type"])
data class Media(
    val id: Long,
    val albumId: Long,
    @ColumnInfo(defaultValue = "")
    val albumName: String?,
    @ColumnInfo(defaultValue = "0")
    val dateModified: Long,
    @ColumnInfo(defaultValue = "")
    val displayName: String?,
    @ColumnInfo(defaultValue = "")
    val filePath: String?,
    @ColumnInfo(defaultValue = "image/*")
    val mimeType: String?,
    @ColumnInfo(defaultValue = "")
    val relativePath: String?,
    @ColumnInfo(defaultValue = "")
    val contentCount: Int,
    @ColumnInfo(defaultValue = "0")
    val type: Int
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(albumId)
        parcel.writeString(albumName)
        parcel.writeLong(dateModified)
        parcel.writeString(displayName)
        parcel.writeString(filePath)
        parcel.writeString(mimeType)
        parcel.writeString(relativePath)
        parcel.writeInt(contentCount)
        parcel.writeInt(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Media> {

        const val IMAGE = 1
        const val VIDEO = 2

        override fun createFromParcel(parcel: Parcel): Media {
            return Media(parcel)
        }

        override fun newArray(size: Int): Array<Media?> {
            return arrayOfNulls(size)
        }
    }

}
