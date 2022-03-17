package com.karunesh_palekar.opencameracollab32.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.karunesh_palekar.opencameracollab32.db.UserDatabase
import com.karunesh_palekar.opencameracollab32.repository.PdfRepository
import com.karunesh_palekar.opencameracollab32.model.Pdf
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    var _pdf :LiveData<List<Pdf>>? =null

    var uriList = mutableListOf<String>()
    private val repository: PdfRepository

    init {
        val userDao = UserDatabase.getDatabase(application).userDao()
        repository = PdfRepository(userDao)
    }

    private val _message = MutableLiveData<List<String>>()
    val message: LiveData<List<String>>
        get() = _message

    fun sendMessage(text: String) {
        uriList.add(text)
        _message.value = uriList
    }

    fun getData(){
        _pdf = repository.readAllData
    }

    fun addData(pdf:Pdf){
        viewModelScope.launch {
            repository.addData(pdf)
        }
    }

}