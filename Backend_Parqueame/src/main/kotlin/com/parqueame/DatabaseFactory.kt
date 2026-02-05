package com.parqueame

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient as SyncMongoClient
import com.mongodb.client.MongoDatabase as SyncMongoDatabase
import com.mongodb.client.MongoCollection as SyncMongoCollection
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.parqueame.models.*
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.eq
import java.util.concurrent.TimeUnit
import org.litote.kmongo.KMongo as KMongoSync

object DatabaseFactory {

    // --- Clientes / DBs ---
    private lateinit var client: CoroutineClient
    private lateinit var syncClient: SyncMongoClient
    private lateinit var mainDb: CoroutineDatabase
    private lateinit var dgiiDb: CoroutineDatabase

    // --- ENV ---
    private val uri = System.getenv("MONGODB_URI")
    private val dbName = System.getenv("MONGODB_NAME")
    private val dgiiDbName = System.getenv("DGII_DB_NAME")

    // --- Colecciones (Coroutine) ---
    val usuariosCollection by lazy { mainDb.getCollection<Usuario>() }
    val passwordResetCollection by lazy { mainDb.getCollection<PasswordResetCode>() }
    val contribuyentesCollection by lazy { dgiiDb.getCollection<Contribuyente>("Contribuyentes") }
    val recentSearchesCollection by lazy { mainDb.getCollection<RecentSearch>("RecentSearches") }
    val parkingsCollection by lazy { mainDb.getCollection<ParkingLot>("Parkings") }
    val ratingsCollection by lazy { mainDb.getCollection<ParkingRating>("parking_ratings") }

    // Wallet
    val transactionsCollection by lazy { mainDb.getCollection<TransactionDoc>("Transactions") }
    val bankAccountsCollection by lazy { mainDb.getCollection<BankAccountDoc>("BankAccounts") }
    val withdrawalsCollection by lazy { mainDb.getCollection<WithdrawalDoc>("Withdrawals") }

    // Stripe
    val stripeCustomersCollection by lazy { mainDb.getCollection<StripeCustomerDoc>("StripeCustomers") }

    // Reservas
    val reservationsCollection by lazy { mainDb.getCollection<Reservation>("reservations") }

    // QR Sessions
    val qrSessionsCollection by lazy { mainDb.getCollection<QrSession>("qr_sessions") }

    // --- Exposición / Sync (misma BD) ---
    val db: CoroutineDatabase get() = mainDb
    val syncDb: SyncMongoDatabase by lazy { syncClient.getDatabase(dbName) }

    val parkingsCollectionSync: SyncMongoCollection<ParkingLot> by lazy {
        syncDb.getCollection("Parkings", ParkingLot::class.java)
    }
    val reservationsCollectionSync: SyncMongoCollection<Reservation> by lazy {
        syncDb.getCollection("reservations", Reservation::class.java)
    }

    // --- Inicialización ---
    suspend fun init() {
        if (uri.isNullOrEmpty() || dbName.isNullOrEmpty() || dgiiDbName.isNullOrEmpty()) {
            println("❌ Error: faltan variables MONGODB_URI / MONGODB_NAME / DGII_DB_NAME")
            return
        }

        println("🔗 Conectando a MongoDB...")

        // Cliente Coroutine (KMongo Reactive Streams)
        val rsSettings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri))
            .applyToClusterSettings { it.serverSelectionTimeout(6, TimeUnit.SECONDS) }
            .applyToSocketSettings {
                it.connectTimeout(6, TimeUnit.SECONDS)
                it.readTimeout(10, TimeUnit.SECONDS)
            }
            .applyToConnectionPoolSettings { it.maxWaitTime(6, TimeUnit.SECONDS) }
            .build()

        client = KMongo.createClient(rsSettings).coroutine
        mainDb = client.getDatabase(dbName)
        dgiiDb = client.getDatabase(dgiiDbName)

        // Cliente Sync (KMongo Kotlin codecs ready)
        syncClient = KMongoSync.createClient(ConnectionString(uri))

        try {
            mainDb.runCommand<Document>(Document("ping", 1))
            println("🟢 Ping Mongo OK")
        } catch (e: Exception) {
            println("🔴 Mongo ping failed: ${e.message}")
            e.printStackTrace()
        }

        println("✅ Conectado a bases: $dbName (principal) y $dgiiDbName (DGII)")
        createIndexes()
    }

    // --- Creación de índices ---
    private suspend fun createIndexes() {
        try {
            println("📌 Creando índices...")

            // Usuarios
            usuariosCollection.createIndex(
                Indexes.ascending("correo"),
                IndexOptions().unique(true).background(true)
            )
            usuariosCollection.createIndex(
                Indexes.ascending("documento"),
                IndexOptions().unique(true).background(true)
            )

            // Búsquedas recientes
            recentSearchesCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("userId"),
                    Indexes.descending("createdAt")
                ),
                IndexOptions().background(true)
            )

            // Parkings
            parkingsCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("status"),
                    Indexes.descending("createdAt")
                ),
                IndexOptions().background(true)
            )
            parkingsCollection.createIndex(
                Indexes.ascending("address"),
                IndexOptions().background(true)
            )

            // Ratings
            ratingsCollection.createIndex(
                Indexes.ascending("reservationId"),
                IndexOptions().unique(true).background(true)
            )
            ratingsCollection.createIndex(
                Indexes.ascending("parkingId"),
                IndexOptions().background(true)
            )
            ratingsCollection.createIndex(
                Indexes.ascending("userId"),
                IndexOptions().background(true)
            )
            ratingsCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("parkingId"),
                    Indexes.descending("createdAt")
                ),
                IndexOptions().background(true)
            )

            // Password reset TTL
            passwordResetCollection.createIndex(
                Indexes.ascending("expiresAtDate"),
                IndexOptions().expireAfter(0, TimeUnit.SECONDS).background(true)
            )

            // Transacciones
            transactionsCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("userId"),
                    Indexes.descending("createdAt")
                ),
                IndexOptions().background(true)
            )
            transactionsCollection.createIndex(
                Indexes.ascending("stripePaymentIntentId"),
                IndexOptions().background(true)
            )
            transactionsCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("status"),
                    Indexes.descending("createdAt")
                ),
                IndexOptions().background(true)
            )

            // Cuentas bancarias
            bankAccountsCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("userId"),
                    Indexes.descending("createdAt")
                ),
                IndexOptions().background(true)
            )
            bankAccountsCollection.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("userId"),
                    Indexes.ascending("isDefault")
                ),
                IndexOptions().background(true)
            )

            // Stripe customers
            stripeCustomersCollection.createIndex(
                Indexes.ascending("userId"),
                IndexOptions().unique(true).background(true)
            )
            stripeCustomersCollection.createIndex(
                Indexes.ascending("customerId"),
                IndexOptions().unique(true).background(true)
            )

            // QR Sessions
            qrSessionsCollection.createIndex(
                Indexes.ascending("sessionId"),
                IndexOptions().unique(true).background(true)
            )
            qrSessionsCollection.createIndex(
                Indexes.ascending("userId"),
                IndexOptions().background(true)
            )
            qrSessionsCollection.createIndex(
                Indexes.ascending("expiresAt"),
                IndexOptions().expireAfter(0, TimeUnit.SECONDS).background(true)
            )

            println("✅ Índices creados correctamente")

        } catch (e: Exception) {
            println("❌ Error creando índices: ${e.message}")
            e.printStackTrace()
        }
    }

    // --- Utilidades ---
    suspend fun isCorreoExistente(correo: String): Boolean {
        return usuariosCollection.findOne(Usuario::correo eq correo) != null
    }

    suspend fun isDocumentoExistente(documento: String): Boolean {
        return usuariosCollection.findOne(Usuario::documento eq documento) != null
    }

    fun getSyncClient(): SyncMongoClient = syncClient
    fun getDgiiDbName(): String = dgiiDbName
}
