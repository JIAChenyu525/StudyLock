package com.studylock.app.data.repository

import org.mindrot.jbcrypt.BCrypt

open class BaseRepository {
    
    protected fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    protected fun checkPassword(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }
}