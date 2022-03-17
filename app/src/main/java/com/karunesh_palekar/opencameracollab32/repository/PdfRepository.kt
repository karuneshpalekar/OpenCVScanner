package com.karunesh_palekar.opencameracollab32.repository

import androidx.lifecycle.LiveData
import com.karunesh_palekar.opencameracollab32.db.UserDao
import com.karunesh_palekar.opencameracollab32.model.Pdf

class PdfRepository(private val userDao: UserDao) {

    val readAllData :LiveData<List<Pdf>> = userDao.readAllData()

    suspend fun addData(pdf: Pdf){
        userDao.addUser(pdf)
    }

}