package com.karunesh_palekar.opencameracollab32.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "user_table")
data class Pdf(
    @PrimaryKey(autoGenerate = true) val id: Int,
   val name :String,
   val uris :List<String>
):Parcelable

class UriTypeConverter{
    @TypeConverter
    fun fromString(value:String?):List<String>{
        val listType = object :TypeToken<List<String>>(){}.type
        return Gson().fromJson(value,listType)
    }

    @TypeConverter
    fun frmArrayList(list:List<String?>):String{
        return Gson().toJson(list)
    }
}