package com.parqueame.services

// KMongo DSL imports
import com.parqueame.DatabaseFactory
import com.parqueame.models.PasswordResetCode
import com.parqueame.models.Usuario
import org.litote.kmongo.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*
import java.util.regex.Pattern

class AuthService {

    private val codesCollection = DatabaseFactory.passwordResetCollection
    private val userCollection = DatabaseFactory.usuariosCollection

    private fun normEmail(raw: String) = raw.trim().lowercase()

    suspend fun generateResetCode(email: String): String {
        return try {
            val e = normEmail(email)
            val code = (100000..999999).random().toString()
            val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000
            val expiresAtDate = Date(expiresAt)

            codesCollection.updateOne(
                PasswordResetCode::email eq e,
                combine(
                    set(
                        PasswordResetCode::code setTo code,
                        PasswordResetCode::expiresAt setTo expiresAt,
                        PasswordResetCode::expiresAtDate setTo expiresAtDate
                    ),
                    setOnInsert(PasswordResetCode::email, e)
                ),
                upsert()
            )

            println("✅ generateResetCode: código $code generado para $e (expira a $expiresAtDate)")
            code
        } catch (e: Exception) {
            println("❌ Error generando código de reset: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun validateCode(email: String, code: String): Boolean {
        return try {
            val e = normEmail(email)
            val c = code.trim()
            val now = System.currentTimeMillis()

            val record = codesCollection.findOne(
                and(
                    PasswordResetCode::email eq e,
                    PasswordResetCode::code eq c,
                    PasswordResetCode::expiresAt gt now
                )
            )

            val isValid = record != null

            if (isValid) {
                println("✅ validateCode: código válido para $e")
            } else {
                println("❌ validateCode: código inválido/expirado para $e (code=$c)")
                // Limpiar códigos expirados
                cleanupExpiredCodes(e)
            }

            isValid
        } catch (e: Exception) {
            println("❌ Error validando código: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Boolean {
        return try {
            val emailLowercase = normEmail(email)
            val codeNorm = code.trim()

            // 1. VERIFICAR QUE EL CÓDIGO EXISTE Y ES VÁLIDO
            val resetCode = codesCollection.findOne(
                and(
                    PasswordResetCode::email eq emailLowercase,
                    PasswordResetCode::code eq codeNorm
                )
            )

            if (resetCode == null) {
                println("❌ Código no encontrado para $emailLowercase")
                false
            } else if (System.currentTimeMillis() > resetCode.expiresAt) {
                // 2. VERIFICAR QUE NO HAYA EXPIRADO
                println("❌ Código expirado para $emailLowercase")
                // Eliminar código expirado
                codesCollection.deleteOne(
                    and(
                        PasswordResetCode::email eq emailLowercase,
                        PasswordResetCode::code eq codeNorm
                    )
                )
                false
            } else {
                // 3. BUSCAR EL USUARIO CON CASE-INSENSITIVE MATCHING
                val user = userCollection.findOne(
                    Usuario::correo regex "^${Pattern.quote(emailLowercase)}$".toRegex(RegexOption.IGNORE_CASE)
                )

                if (user == null) {
                    println("❌ Usuario no encontrado para $emailLowercase")
                    false
                } else {
                    // 4. HASHEAR LA NUEVA CONTRASEÑA
                    val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))

                    // 5. ACTUALIZAR LA CONTRASEÑA
                    val updateResult = userCollection.updateOne(
                        Usuario::correo regex "^${Pattern.quote(emailLowercase)}$".toRegex(RegexOption.IGNORE_CASE),
                        set(Usuario::contrasena setTo hashedPassword)
                    )

                    if (updateResult.modifiedCount == 0L) {
                        println("❌ No se pudo actualizar la contraseña para $emailLowercase")
                        false
                    } else {
                        // 6. ELIMINAR TODOS LOS CÓDIGOS PARA ESE EMAIL (IMPORTANTE PARA SEGURIDAD)
                        codesCollection.deleteMany(
                            PasswordResetCode::email eq emailLowercase
                        )

                        println("✅ Contraseña actualizada exitosamente para $emailLowercase")
                        true
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Error en resetPassword: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun userExists(email: String): Boolean {
        return try {
            val emailLowercase = normEmail(email)
            val count = userCollection.countDocuments(
                Usuario::correo regex "^${Pattern.quote(emailLowercase)}$".toRegex(RegexOption.IGNORE_CASE)
            )
            val exists = count > 0

            if (!exists) {
                println("❌ userExists: '$emailLowercase' no encontrado (db='${System.getenv("MONGODB_NAME")}')")
            } else {
                println("✅ userExists: '$emailLowercase' encontrado")
            }

            exists
        } catch (e: Exception) {
            println("❌ Error verificando existencia del usuario: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun getUserByEmail(email: String): Usuario? {
        return try {
            val emailLowercase = normEmail(email)
            val user = userCollection.findOne(
                Usuario::correo regex "^${Pattern.quote(emailLowercase)}$".toRegex(RegexOption.IGNORE_CASE)
            )

            if (user != null) {
                println("✅ Usuario encontrado para $emailLowercase")
            } else {
                println("❌ Usuario no encontrado para $emailLowercase")
            }

            user
        } catch (e: Exception) {
            println("❌ Error buscando usuario por email: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Método auxiliar para limpiar códigos expirados
    private suspend fun cleanupExpiredCodes(email: String) {
        try {
            val now = System.currentTimeMillis()
            val filter = and(
                PasswordResetCode::email eq email,
                PasswordResetCode::expiresAt lt now
            )

            val deletedCount = codesCollection.deleteMany(filter).deletedCount
            if (deletedCount > 0) {
                println("🧹 Limpieza: $deletedCount códigos expirados eliminados para $email")
            }
        } catch (e: Exception) {
            println("❌ Error limpiando códigos expirados: ${e.message}")
        }
    }
}