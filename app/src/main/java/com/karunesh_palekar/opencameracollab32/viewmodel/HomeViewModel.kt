package com.karunesh_palekar.opencameracollab32.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.karunesh_palekar.opencameracollab32.db.UserDatabase
import com.karunesh_palekar.opencameracollab32.repository.PdfRepository
import com.karunesh_palekar.opencameracollab32.model.Pdf

class HomeViewModel(application:Application):AndroidViewModel(application) {


    var _pdf : LiveData<List<Pdf>>? =null

    private val repository: PdfRepository

    init {

        val userDao = UserDatabase.getDatabase(application).userDao()
        repository = PdfRepository(userDao)
    }

    fun getData(){
        _pdf = repository.readAllData
    }

}