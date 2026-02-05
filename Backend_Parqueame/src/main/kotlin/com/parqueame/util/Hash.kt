package com.parqueame.util

import org.mindrot.jbcrypt.BCrypt

fun hashPassword(raw: String): String =
    BCrypt.hashpw(raw, BCrypt.gensalt(12))