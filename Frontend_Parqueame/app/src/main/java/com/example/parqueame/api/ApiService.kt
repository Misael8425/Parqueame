package com.example.parqueame.api

import com.example.parqueame.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ---------- Autenticación y registro ----------
    @POST("/usuarios")
    suspend fun registerUsuario(@Body usuario: Usuario): Response<ApiResponse>

    @GET("/validar-cedula/{cedula}")
    suspend fun validarCedula(@Path("cedula") cedula: String): Response<ApiResponse>

    // ---------- Recuperación de contraseña ----------
    @POST("/auth/request-reset")
    suspend fun solicitarCodigo(@Body request: EmailRequest): Response<ApiResponse>

    @POST("/auth/verify-code")
    suspend fun verificarCodigo(@Body request: CodeVerificationRequest): Response<ApiResponse>

    @POST("/auth/reset-password")
    suspend fun cambiarContrasena(@Body request: ResetPasswordRequest): Response<ApiResponse>

    // ---------- Login ----------
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ---------- DGII y perfil ----------
    @GET("/dgii/rnc/{rnc}")
    suspend fun consultarRnc(@Path("rnc") rnc: String): Response<DgiiRncResponse>

    @GET("/usuarios/me")
    suspend fun obtenerPerfil(@Query("correo") correo: String): Response<UsuarioPerfil>

    @GET("/usuarios/me")
    suspend fun obtenerPerfilPorId(@Header("X-User-Id") userId: String): Response<UsuarioPerfil>

    @PUT("/usuarios/actualizar")
    suspend fun actualizarPerfil(@Body request: ActualizarPerfilRequest): Response<ApiResponse>

    // ---------- Stripe ----------
    @POST("/create-payment-intent")
    suspend fun createPaymentIntent(): Response<StripePaymentResponse>

    @POST("/stripe/customer-bootstrap")
    suspend fun getCustomerBootstrap(@Header("X-User-Id") userId: String): Response<StripeCustomerBootstrap>

    @POST("/stripe/create-setup-intent")
    suspend fun createSetupIntent(@Body request: CreateSetupIntentRequest): Response<SetupIntentResponse>

    // ---------- Stripe Cards (user-scoped) ----------
    @GET("/stripe/payment-methods")
    suspend fun getStripePaymentMethods(@Header("X-User-Id") userId: String): Response<List<StripeCardDto>>

    @DELETE("/stripe/payment-methods/{pmId}")
    suspend fun detachStripePaymentMethod(
        @Path("pmId") pmId: String,
        @Header("X-User-Id") userId: String
    ): Response<ApiResponse>

    // ---------- Búsquedas recientes ----------
    @GET("/recent-searches")
    suspend fun getRecentSearches(
        @Header("X-User-Id") userId: String,
        @Query("limit") limit: Int = 10
    ): Response<List<RecentSearchResponse>>

    @POST("/recent-searches")
    suspend fun createRecentSearch(
        @Header("X-User-Id") userId: String,
        @Body request: RecentSearchRequest
    ): Response<ApiResponse>

    @GET("/recent-searches/last")
    suspend fun getLastSearch(@Header("X-User-Id") userId: String): Response<RecentSearchResponse>

    // ---------- Parqueos ----------
    @POST("/parkings")
    suspend fun crearParqueo(
        @Header("X-User-Id") userId: String,
        @Header("X-Owner-Documento") ownerDocumento: String? = null,
        @Header("X-Owner-Tipo") ownerTipo: String? = null,
        @Body body: CreateParkingLotRequest
    ): Response<ParkingCreateResponse>

    @GET("/parkings")
    suspend fun listarParqueos(
        @Header("X-User-Id") userId: String? = null,
        @Query("createdBy") createdBy: String? = null,
        @Query("ownerDocumento") ownerDocumento: String? = null,
        @Query("ownerTipo") ownerTipo: String? = null
    ): Response<List<ParkingLotDto>>

    @GET("/parkings/{id}")
    suspend fun obtenerParqueoId(@Path("id") id: String): Response<ParkingLotDto>

    @GET("/parkings/approved")
    suspend fun obtenerParqueosAprobados(): Response<List<ParkingLotDto>>

    // ---------- Comentarios ----------
    @GET("/parkings/{id}/comments")
    suspend fun obtenerComentarios(@Path("id") id: String): Response<List<ParkingCommentDto>>

    @POST("/parkings/{id}/comments")
    suspend fun agregarComentario(
        @Path("id") id: String,
        @Body body: CreateCommentRequest
    ): Response<ParkingLotDto>

    // (alias, si lo usas en otra parte)
    @GET("/parkings/{id}")
    suspend fun getParkingByIdDto(@Path("id") id: String): Response<ParkingLotDto>

    @PUT("/parkings/{id}")
    suspend fun updateParking(
        @Path("id") id: String,
        @Body body: CreateParkingLotRequestDto,
        @Header("X-User-Id") xUserId: String? = null,
        @Header("X-Owner-Documento") ownerDocumento: String? = null,
        @Header("X-Owner-Tipo") ownerTipo: String? = null
    ): Response<ParkingLotDto>

    // ---------- Cambiar ESTADO del parqueo ----------
    @PUT("/parkings/{id}/status/{status}")
    suspend fun setParkingStatus(
        @Path("id") id: String,
        @Path("status") status: String,
        @Body body: UpdateParkingStatusRequest? = null
    ): Response<ParkingLotDto>

    // ---------- Wallet ----------
    @GET("/wallet/summary")
    suspend fun getWalletSummary(@Header("X-User-Id") userId: String): Response<WalletSummaryDto>

    @GET("/wallet/transactions")
    suspend fun getWalletTransactions(
        @Header("X-User-Id") userId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<List<TransactionDto>>

    @PUT("/wallet/bank-account")
    suspend fun updateBankAccount(
        @Header("X-User-Id") userId: String,
        @Body body: UpdateBankAccountRequest
    ): Response<BankAccountDto>

    // ---------- RESERVAS ----------
    @POST("/reservations")
    suspend fun createReservation(
        @Header("X-User-Id") userId: String,
        @Body body: CreateReservationRequest
    ): Response<ReservationDto>

    @GET("/reservations/{id}")
    suspend fun getReservation(@Path("id") reservationId: String): Response<ReservationDto>

    @GET("/reservations/by-user/{userId}")
    suspend fun listReservationsByUser(@Path("userId") userId: String): Response<List<ReservationDto>>

    @GET("/reservations/by-parking/{parkingId}")
    suspend fun listReservationsByParking(@Path("parkingId") parkingId: String): Response<List<ReservationDto>>

    // --- Activas ---
    @GET("/reservations/active/by-user/{userId}")
    suspend fun getActiveReservationsByUser(@Path("userId") userId: String): Response<List<ReservationDto>>

    @GET("/reservations/active/by-parking/{parkingId}")
    suspend fun getActiveReservationsByParking(@Path("parkingId") parkingId: String): Response<List<ReservationDto>>

    @PATCH("/reservations/{id}/cancel")
    suspend fun cancelReservation(@Path("id") reservationId: String): Response<ApiResponse>

    // Disponibilidad (epoch MINUTOS)
    @GET("/parkings/{id}/availability")
    suspend fun checkAvailability(
        @Path("id") parkingId: String,
        @Query("startMin") startMin: Long,
        @Query("endMin") endMin: Long
    ): Response<ParkingAvailabilityResponse>

    @GET("/usuarios/{id}/public")
    suspend fun getUsuarioPublico(@Path("id") userId: String): Response<UsuarioPerfil>

    // ---------- Ratings (nuevo: elimina hardcoded) ----------
    @GET("/parkings/{id}/rating/summary")
    suspend fun getParkingRatingSummary(@Path("id") parkingId: String): Response<RatingSummaryDto>

    // ============================================================
    // ===================== QR (tap-to-continue) =================
    // ============================================================
    // NUEVO flujo sin escaneo: tap → backend → navegar
    @POST("/qr/tap")
    suspend fun tapQr(@Body body: TapQrRequest): Response<TapQrResponse>

    // (Opcional) Si mantienes el flujo QR anterior:
    @POST("/qr/create")
    suspend fun createQr(@Body body: QrCreateRequest): Response<QrCreateResponse>

    @GET("/qr/status/{token}")
    suspend fun getQrStatus(@Path("token") token: String): Response<QrStatusResponse>


    // ⭐ Calificar parqueo
    @POST("api/parkings/{id}/ratings")
    suspend fun rateParking(
        @Path("id") parkingId: String,
        @Body request: RateParkingRequest
    ): Response<ApiResponseWithData<RateParkingResponse>>

    @GET("api/company/dashboard")
    suspend fun getDashboard(
        @Query("period") period: PeriodDto,
        @Query("branchId") branchId: String?
    ): DashboardResponse
}