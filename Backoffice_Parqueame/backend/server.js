require("dotenv").config();
const express = require("express");
const cors = require("cors");
const bodyParser = require("body-parser");
const mongoose = require("mongoose");
const bcrypt = require("bcrypt");

const app = express();
const PORT = process.env.PORT || 5000;

// --- CORS Configuration
const allowedOrigins = [
  "http://localhost:5173",
  "http://localhost:3000",
  "http://localhost:3001",
  "https://frontend-production-ab00.up.railway.app",
];

app.use(
  cors({
    origin: (origin, cb) => {
      if (!origin || allowedOrigins.includes(origin)) return cb(null, true);
      return cb(new Error("CORS no permitido"), false);
    },
    credentials: true,
  })
);

app.use(bodyParser.json());

// ---- MongoDB Connections (dos DBs en el mismo cluster)
const mongoUri = process.env.MONGODB_URI;
const DB_BACKOFFICE = process.env.MONGODB_DB_BACKOFFICE || "Backoffice";
const DB_PARQUEAME  = process.env.MONGODB_DB_PARQUEAME  || "Parqueame";

if (!mongoUri) {
  console.error("❌ Missing environment variable MONGODB_URI");
  process.exit(1);
}

let connBackoffice, connParqueame;

async function connectDbs() {
  mongoose.set("strictQuery", true);

  // Conexión a Backoffice (usuarios)
  connBackoffice = await mongoose.createConnection(mongoUri, { dbName: DB_BACKOFFICE })
    .asPromise()
    .then((c) => {
      console.log(`✅ Connected to MongoDB (DB: ${DB_BACKOFFICE})`);
      return c;
    })
    .catch((err) => {
      console.error(`❌ Error connecting to DB ${DB_BACKOFFICE}:`, err.message);
      process.exit(1);
    });

  // Conexión a Parqueame (parkings)
  connParqueame = await mongoose.createConnection(mongoUri, { dbName: DB_PARQUEAME })
    .asPromise()
    .then((c) => {
      console.log(`✅ Connected to MongoDB (DB: ${DB_PARQUEAME})`);
      return c;
    })
    .catch((err) => {
      console.error(`❌ Error connecting to DB ${DB_PARQUEAME}:`, err.message);
      process.exit(1);
    });
}

// ---- Schemas & Models

// User Schema (DB: Backoffice, collection: usuarios)
const userSchema = new mongoose.Schema(
  {
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    passwordHash: { type: String }, // moderno
    password: { type: String },     // legacy opcional
    name: { type: String, default: "" },
    role: { type: String, default: "user" },
    active: { type: Boolean, default: true }
  },
  { timestamps: true, collection: "usuarios" }
);

// Subdocumento de comentario
const commentSchema = new mongoose.Schema(
  {
    type: { type: String, enum: ["rejection", "note", "system"], required: true },
    text: { type: String, required: true },
    authorId: { type: String, default: null },
    authorEmail: { type: String, default: null },
    createdAt: { type: mongoose.Schema.Types.Mixed, default: () => Date.now() },
  },
  { _id: true }
);

// Parking Schema (DB: Parqueame, collection: Parkings)
const parkingSchema = new mongoose.Schema(
  {
    // Campos "nuevos"
    localName: String,
    address: String,
    capacity: Number,
    priceHour: Number,
    daysOfWeek: [String],
    schedules: [Object],
    characteristics: [String],
    photos: [String],
    infraDocUrl: String,
    location: [Number], // [lng, lat]
    status: { type: String, enum: ['pending', 'approved', 'rejected'], default: 'pending' },
    createdBy: String,
    createdByDocumento: String,
    createdByTipoDocumento: String,
    createdAt: { type: mongoose.Schema.Types.Mixed },
    updatedAt: { type: mongoose.Schema.Types.Mixed },

    // Opcionales
    type: { type: String, default: null },            // ej. RESIDENCIAL/COMERCIAL...
    solicitudTipo: { type: String, default: null },   // Apertura/Cierre/Editar
    pendingChanges: { type: mongoose.Schema.Types.Mixed, default: null },

    // Comentarios
    comments: { type: [commentSchema], default: [] },

    // Campos "legacy"/ES
    nombreLocal: String,
    direccion: String,
    capacidad: Number,
    precioPorHora: Number,
    diasSemana: [String],
    horarios: [Object],
    documentoInfraestructuraUrl: String,
    ubicacion: [Number],
    estado: String,
    fechaCreacion: mongoose.Schema.Types.Mixed,
    fechaActualizacion: mongoose.Schema.Types.Mixed,
  },
  { collection: "Parkings", strict: false }
);

// Estas variables se inicializan después de conectar
let User;
let Parking;

// ---- Utils
function toMillis(value) {
  if (!value && value !== 0) return null;
  if (typeof value === "number") return value;
  const d = new Date(value);
  const t = d.getTime();
  return Number.isNaN(t) ? null : t;
}

function formatDateForDisplay(anyDateOrMillis) {
  const ms = toMillis(anyDateOrMillis);
  if (ms == null) return "";
  const d = new Date(ms);
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const yy = String(d.getFullYear()).slice(2);
  return `${dd}/${mm}/${yy}`;
}

function mapStatusToSpanish(status) {
  const statusMap = { pending: "Pendiente", approved: "Aprobado", rejected: "Rechazado" };
  return statusMap[status] || status;
}

function mapSpanishToStatus(estadoES) {
  const reverseMap = { Pendiente: "pending", Aprobado: "approved", Rechazado: "rejected" };
  if (!estadoES) return estadoES;
  return reverseMap[estadoES] || String(estadoES).toLowerCase();
}

function pickLatestRejectionReason(comments = []) {
  const list = Array.isArray(comments) ? comments : [];
  const last = [...list]
    .filter(c => c && c.type === "rejection" && typeof c.text === "string")
    .sort((a,b) => (toMillis(b.createdAt)||0) - (toMillis(a.createdAt)||0))[0];
  return last?.text || null;
}

// ---- Routes

app.get("/", (_req, res) => {
  res.send("Backend funcionando 🚀 - Conectado a Backoffice (usuarios) y Parqueame (parkings)");
});

app.post("/api/register", async (req, res) => {
  try {
    const { email, password, name, role } = req.body;
    if (!email || !password) return res.status(400).json({ message: "Email y password requeridos" });

    const emailNorm = String(email).trim().toLowerCase();
    const exists = await User.findOne({ email: emailNorm });
    if (exists) return res.status(409).json({ message: "El email ya está registrado" });

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({ email: emailNorm, passwordHash, name, role, active: true });

    res.status(201).json({ id: user._id, email: user.email, name: user.name, role: user.role });
  } catch (err) {
    res.status(500).json({ message: "Error registrando usuario", error: err.message });
  }
});

// LOGIN con compatibilidad legacy
app.post("/api/login", async (req, res) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) {
      return res.status(400).json({ message: "Email y password requeridos" });
    }

    const emailNorm = String(email).trim().toLowerCase();
    const user = await User.findOne({ email: emailNorm });

    if (!user) return res.status(401).json({ message: "Credenciales inválidas" });
    if (typeof user.active === "boolean" && !user.active) {
      return res.status(403).json({ message: "Usuario desactivado" });
    }

    const { passwordHash, password: legacyPassword } = user;
    let valid = false;

    if (passwordHash) {
      valid = await bcrypt.compare(password, passwordHash);
    } else if (legacyPassword) {
      try {
        valid = await bcrypt.compare(password, legacyPassword);
      } catch {
        valid = false;
      }
      if (!valid && process.env.ALLOW_PLAINTEXT_LOGIN === "true") {
        valid = legacyPassword === password;
      }
      if (valid && !passwordHash) {
        const newHash = await bcrypt.hash(password, 10);
        user.passwordHash = newHash;
        await user.save().catch(() => {});
      }
    }

    if (!valid) return res.status(401).json({ message: "Credenciales inválidas" });

    res.json({
      message: "Login exitoso",
      user: { id: user._id, email: user.email, name: user.name, role: user.role || "user" },
    });
  } catch (err) {
    console.error("❌ Error en login:", err);
    res.status(500).json({ message: "Error en login", error: err.message });
  }
});

// ---- PARKING ROUTES ----

// GET /api/parkings — devuelve formato nuevo + legacy para la tabla actual
app.get("/api/parkings", async (req, res) => {
  try {
    const { estado = "Todos", q = "", fecha = "", type = "", tipo = "", solicitudTipo = "" } = req.query;
    console.log("📋 Listing parkings:", { estado, q, fecha, type, tipo, solicitudTipo });

    const andConditions = [];

    if (estado !== "Todos") {
      const statusValue = mapSpanishToStatus(estado);
      andConditions.push({
        $or: [{ status: statusValue }, { estado: estado }],
      });
    }

    if (q) {
      andConditions.push({
        $or: [
          { localName: { $regex: q, $options: "i" } },
          { address:   { $regex: q, $options: "i" } },
          { nombreLocal: { $regex: q, $options: "i" } },
          { direccion:   { $regex: q, $options: "i" } },
        ],
      });
    }

    if (fecha) {
      const start = new Date(`${fecha}T00:00:00.000Z`);
      const end   = new Date(`${fecha}T23:59:59.999Z`);
      const startMs = start.getTime();
      const endMs   = end.getTime();
      andConditions.push({
        $or: [
          { createdAt:     { $gte: start,  $lte: end } },
          { createdAt:     { $gte: startMs, $lte: endMs } },
          { fechaCreacion: { $gte: start,  $lte: end } },
          { fechaCreacion: { $gte: startMs, $lte: endMs } },
        ],
      });
    }

    // Filtro por tipo "operativo" (RESIDENCIAL/COMERCIAL...) - compat
    const typeValue = (type || tipo || "").trim();
    if (typeValue) {
      andConditions.push({
        $or: [{ type: typeValue }, { tipo: typeValue }],
      });
    }

    // NUEVO: filtro por solicitudTipo (Apertura/Cierre/Editar)
    const solicitudTipoValue = String(solicitudTipo || "").trim();
    if (solicitudTipoValue) {
      andConditions.push({ solicitudTipo: solicitudTipoValue });
    }

    const mongoQuery = andConditions.length ? { $and: andConditions } : {};
    const parkings = await Parking.find(mongoQuery).sort({ createdAt: -1, fechaCreacion: -1 });

    const items = parkings.map((pDoc) => {
      const p = pDoc.toObject ? pDoc.toObject() : pDoc;

      const localName = p.localName ?? p.nombreLocal ?? p.nombre ?? p.name ?? null;
      const address   = p.address   ?? p.direccion   ?? p.addressLine ?? null;
      const capacity  = (p.capacity ?? p.capacidad) ?? null;
      const priceHour = (p.priceHour ?? p.precioPorHora ?? p.precioHora) ?? null;

      const statusEN  = p.status ?? mapSpanishToStatus(p.estado) ?? null;
      const estadoES  = statusEN ? mapStatusToSpanish(statusEN) : (p.estado ?? null);

      const createdAtMs = toMillis(p.createdAt ?? p.fechaCreacion ?? p.created_at) ?? null;

      const typeOut         = p.type ?? p.tipo ?? null;               // operativo
      const solicitudTipoOut= p.solicitudTipo ?? null;                // Apertura/Cierre/Editar

      return {
        // —— Claves nuevas/crudas ——
        id: String(p._id),
        type: typeOut,
        solicitudTipo: solicitudTipoOut,
        status: statusEN,
        localName,
        address,
        capacity,
        priceHour,
        daysOfWeek: p.daysOfWeek ?? p.diasSemana ?? null,
        schedules:  p.schedules  ?? p.horarios   ?? null,
        characteristics: p.characteristics ?? p.caracteristicas ?? null,
        photos: p.photos ?? p.fotos ?? null,
        infraDocUrl: p.infraDocUrl ?? p.documentoInfraestructuraUrl ?? null,
        location: p.location ?? p.ubicacion ?? null,
        createdAt: createdAtMs,
        updatedAt: toMillis(p.updatedAt ?? p.fechaActualizacion ?? p.updated_at) ?? null,
        rejectionReason: pickLatestRejectionReason(p.comments) || null,

        // —— Claves legacy (compat con tu UI actual) ——
        estado: estadoES,
        local: localName,
        direccion: address,
        capacidad: capacity,
        precio: priceHour,
        fecha: createdAtMs ? formatDateForDisplay(createdAtMs) : null,

        // Alias que usa tu UI en la columna "Tipo":
        // prioriza solicitudTipo; si no existe, usa type/tipo.
        tipo: solicitudTipoOut ?? typeOut ?? null
      };
    });

    res.json({
      items,
      total: items.length,
      filters: { estado, q, fecha, type: typeValue, solicitudTipo: solicitudTipoValue },
      debug: {
        mongoQuery,
        totalFound: parkings.length,
        source: "MongoDB Direct (Parqueame)",
      },
    });
  } catch (err) {
    console.error("❌ Error listing parkings:", err);
    res.status(500).json({
      message: "Error listando parqueos",
      error: err.message,
      timestamp: new Date().toISOString(),
    });
  }
});

// GET /api/parkings/:id — sin defaults; expone type/pendingChanges si existen
app.get("/api/parkings/:id", async (req, res) => {
  try {
    const { id } = req.params;
    console.log(`📝 Getting parking: ${id}`);

    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "ID de parqueo inválido" });
    }

    const p = await Parking.findById(id);
    if (!p) return res.status(404).json({ message: "Parqueo no encontrado" });

    const obj = p.toObject ? p.toObject() : p;

    const localName = obj.localName ?? obj.nombreLocal ?? obj.nombre ?? obj.name ?? null;
    const address   = obj.address   ?? obj.direccion   ?? obj.addressLine ?? null;
    const capacity  = (obj.capacity ?? obj.capacidad) ?? null;
    const priceHour = (obj.priceHour ?? obj.precioPorHora ?? obj.precioHora) ?? null;

    const statusEN  = obj.status ?? mapSpanishToStatus(obj.estado) ?? null;

    const typeOut          = obj.type ?? obj.tipo ?? null;
    const solicitudTipoOut = obj.solicitudTipo ?? null;

    const formatted = {
      id: String(obj._id),

      type: typeOut,
      solicitudTipo: solicitudTipoOut,
      pendingChanges: obj.pendingChanges ?? null,

      localName,
      address,
      capacity,
      priceHour,
      status: statusEN,

      daysOfWeek: obj.daysOfWeek ?? obj.diasSemana ?? null,
      schedules:  obj.schedules  ?? obj.horarios   ?? null,
      characteristics: obj.characteristics ?? obj.caracteristicas ?? null,
      photos: obj.photos ?? obj.fotos ?? null,
      infraDocUrl: obj.infraDocUrl ?? obj.documentoInfraestructuraUrl ?? null,
      location: obj.location ?? obj.ubicacion ?? null,

      createdBy: obj.createdBy ?? null,
      createdByDocumento: obj.createdByDocumento ?? null,
      createdByTipoDocumento: obj.createdByTipoDocumento ?? null,

      createdAt: toMillis(obj.createdAt ?? obj.fechaCreacion ?? obj.created_at),
      updatedAt: toMillis(obj.updatedAt ?? obj.fechaActualizacion ?? obj.updated_at),

      rejectionReason: pickLatestRejectionReason(obj.comments) || null,

      // LEGACY alias por compatibilidad en UI:
      tipo: solicitudTipoOut ?? typeOut ?? null
    };

    res.json(formatted);
  } catch (err) {
    console.error("❌ Error getting parking:", err);
    res.status(500).json({ message: "Error obteniendo parqueo", error: err.message });
  }
});

// PUT /api/parkings/:id/status/:status
app.put("/api/parkings/:id/status/:status", async (req, res) => {
  try {
    const { id, status } = req.params;

    if (!["pending", "approved", "rejected"].includes(status)) {
      return res.status(400).json({ message: "Estado inválido. Use: pending, approved, rejected" });
    }
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "ID de parqueo inválido" });
    }

    const now = Date.now();
    const update = { status, updatedAt: now };

    // Guardar razón como comentario si viene y el estado es rejected
    const { reason, authorId, authorEmail } = req.body || {};
    if (status === "rejected" && typeof reason === "string" && reason.trim() !== "") {
      const parking = await Parking.findByIdAndUpdate(
        id,
        {
          $set: update,
          $push: {
            comments: {
              type: "rejection",
              text: reason.trim(),
              authorId: authorId || null,
              authorEmail: authorEmail || null,
              createdAt: now,
            }
          }
        },
        { new: true }
      );
      if (!parking) return res.status(404).json({ message: "Parqueo no encontrado" });

      return res.json({
        id: String(parking._id),
        localName: parking.localName ?? parking.nombreLocal ?? "—",
        status: parking.status,
        updatedAt: parking.updatedAt,
        rejectionReason: pickLatestRejectionReason(parking.comments),
        message: `Estado actualizado a ${mapStatusToSpanish(status)}`
      });
    }

    const parking = await Parking.findByIdAndUpdate(id, update, { new: true });
    if (!parking) return res.status(404).json({ message: "Parqueo no encontrado" });

    res.json({
      id: String(parking._id),
      localName: parking.localName ?? parking.nombreLocal ?? "—",
      status: parking.status,
      updatedAt: parking.updatedAt,
      message: `Estado actualizado a ${mapStatusToSpanish(status)}`
    });
  } catch (err) {
    console.error("❌ Error updating parking status:", err);
    res.status(500).json({ message: "Error actualizando estado", error: err.message });
  }
});

// POST /api/parkings
app.post("/api/parkings", async (req, res) => {
  try {
    console.log("📝 Creating new parking:", req.body);

    const {
      localName, nombreLocal,
      address, direccion,
      capacity, capacidad,
      priceHour, precioPorHora, precioHora,
      daysOfWeek = [], diasSemana = [],
      schedules = [], horarios = [],
      characteristics = [], caracteristicas = [],
      photos = [], fotos = [],
      infraDocUrl, documentoInfraestructuraUrl,
      location, ubicacion,
      createdBy, createdByDocumento, createdByTipoDocumento,
      type = null,
      pendingChanges = null,
      // Alias de entrada
      tipo = null,
      solicitudTipo = null
    } = req.body;

    const _localName = localName ?? nombreLocal;
    const _address = address ?? direccion;
    const _capacity = (capacity ?? capacidad);
    const _priceHour = (priceHour ?? precioPorHora ?? precioHora);

    if (!_localName || !_address || _capacity == null || _priceHour == null) {
      return res.status(400).json({
        message: "Campos requeridos: localName/nombreLocal, address/direccion, capacity/capacidad, priceHour/precioPorHora"
      });
    }

    const parking = new Parking({
      localName: _localName,
      address: _address,
      capacity: Number(_capacity),
      priceHour: Number(_priceHour),
      daysOfWeek: (daysOfWeek.length ? daysOfWeek : diasSemana),
      schedules: (schedules.length ? schedules : horarios),
      characteristics: (characteristics.length ? characteristics : caracteristicas),
      photos: (photos.length ? photos : fotos),
      infraDocUrl: infraDocUrl ?? documentoInfraestructuraUrl,
      location: location ?? ubicacion,
      status: "pending",
      createdBy,
      createdByDocumento,
      createdByTipoDocumento,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      // Normaliza ambos:
      type: type ?? tipo ?? null,
      solicitudTipo: solicitudTipo ?? null,
      pendingChanges,
    });

    await parking.save();
    console.log("✅ Parking created:", parking._id);

    res.status(201).json({
      id: String(parking._id),
      localName: parking.localName,
      status: parking.status,
      message: "Parqueo creado exitosamente",
    });
  } catch (err) {
    console.error("❌ Error creando parqueo:", err);
    res.status(500).json({ message: "Error creando parqueo", error: err.message });
  }
});

// ============================================================
// COMENTARIOS
// ============================================================

app.post("/api/parkings/:id/comments", async (req, res) => {
  try {
    const { id } = req.params;
    const { text, authorId, authorEmail, type = "note" } = req.body || {};
    
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "ID de parqueo inválido" });
    }
    if (typeof text !== "string" || text.trim() === "") {
      return res.status(400).json({ message: "El texto del comentario es requerido" });
    }

    const now = Date.now();
    const comment = {
      type: type === "rejection" ? "rejection" : "note",
      text: text.trim(),
      authorId: authorId || null,
      authorEmail: authorEmail || null,
      createdAt: now,
    };

    const parking = await Parking.findByIdAndUpdate(
      id,
      { $set: { updatedAt: now }, $push: { comments: comment } },
      { new: true }
    );

    if (!parking) {
      return res.status(404).json({ message: "Parqueo no encontrado" });
    }

    res.json({
      id: String(parking._id),
      message: "Comentario agregado exitosamente",
      comment: {
        id: comment._id,
        type: comment.type,
        text: comment.text,
        authorId: comment.authorId,
        authorEmail: comment.authorEmail,
        createdAt: comment.createdAt,
      },
    });
  } catch (err) {
    console.error("❌ Error agregando comentario:", err);
    res.status(500).json({ message: "Error agregando comentario", error: err.message });
  }
});

app.get("/api/parkings/:id/comments", async (req, res) => {
  try {
    const { id } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({ message: "ID de parqueo inválido" });
    }

    const parking = await Parking.findById(id);
    if (!parking) {
      return res.status(404).json({ message: "Parqueo no encontrado" });
    }

    const comments = (parking.comments || []).map(c => ({
      id: String(c._id),
      type: c.type,
      text: c.text,
      authorId: c.authorId,
      authorEmail: c.authorEmail,
      createdAt: toMillis(c.createdAt),
    })).sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

    res.json({ parkingId: String(parking._id), comments, total: comments.length });
  } catch (err) {
    console.error("❌ Error obteniendo comentarios:", err);
    res.status(500).json({ message: "Error obteniendo comentarios", error: err.message });
  }
});

app.delete("/api/parkings/:id/comments/:commentId", async (req, res) => {
  try {
    const { id, commentId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(id) || !mongoose.Types.ObjectId.isValid(commentId)) {
      return res.status(400).json({ message: "ID inválido" });
    }

    const now = Date.now();
    const parking = await Parking.findByIdAndUpdate(
      id,
      { $set: { updatedAt: now }, $pull: { comments: { _id: commentId } } },
      { new: true }
    );

    if (!parking) {
      return res.status(404).json({ message: "Parqueo no encontrado" });
    }

    res.json({ id: String(parking._id), message: "Comentario eliminado exitosamente" });
  } catch (err) {
    console.error("❌ Error eliminando comentario:", err);
    res.status(500).json({ message: "Error eliminando comentario", error: err.message });
  }
});

// ---- DEBUG ENDPOINTS ----
app.get("/api/debug", async (_req, res) => {
  try {
    const totalUsers    = await User.countDocuments();
    const totalParkings = await Parking.countDocuments();
    const statusCounts  = await Parking.aggregate([{ $group: { _id: "$status", count: { $sum: 1 } } }]);
    const sampleParking = await Parking.findOne().limit(1);
    const sampleUser    = await User.findOne().limit(1);

    res.json({
      message: "Debug information",
      timestamp: new Date().toISOString(),
      databases: {
        backoffice: {
          connected: connBackoffice?.readyState === 1,
          name: DB_BACKOFFICE,
          users: totalUsers,
          sampleUser: sampleUser ? {
            id: sampleUser._id,
            email: sampleUser.email,
            active: sampleUser.active,
            role: sampleUser.role
          } : null
        },
        parqueame: {
          connected: connParqueame?.readyState === 1,
          name: DB_PARQUEAME,
          parkings: totalParkings,
          statusDistribution: statusCounts.reduce((acc, item) => {
            acc[item._id || "unknown"] = item.count;
            return acc;
          }, {}),
          sampleParking: sampleParking ? {
            id: sampleParking._id,
            localName: sampleParking.localName ?? sampleParking.nombreLocal,
            status: sampleParking.status ?? sampleParking.estado,
            createdAt: formatDateForDisplay(sampleParking.createdAt ?? sampleParking.fechaCreacion),
          } : null
        }
      },
      environment: {
        NODE_ENV: process.env.NODE_ENV || "development",
        PORT: PORT,
      },
    });
  } catch (err) {
    console.error("Error in debug endpoint:", err);
    res.status(500).json({ error: "Error interno en debug", message: err.message });
  }
});

// ---- Error Handling ----
app.use((err, _req, res, _next) => {
  console.error("💥 Unhandled error:", err);
  res.status(500).json({ message: "Error interno del servidor", error: err.message });
});

// ---- Boot
(async () => {
  await connectDbs();

  // Inicializa modelos una vez conectados
  User = connBackoffice.model("User", userSchema);
  Parking = connParqueame.model("Parking", parkingSchema);

  // Seed opcional
  (async function seedAdminIfNeeded() {
    try {
      const seedEmail = (process.env.SEED_ADMIN_EMAIL || "").trim().toLowerCase();
      const seedPass  = process.env.SEED_ADMIN_PASSWORD || "";
      if (!seedEmail || !seedPass) return;

      const exists = await User.findOne({ email: seedEmail });
      if (exists) {
        console.log(`🔐 Seed admin ya existe: ${seedEmail}`);
        return;
      }

      const hash = await bcrypt.hash(seedPass, 10);
      await User.create({
        email: seedEmail,
        passwordHash: hash,
        name: "Admin",
        role: "admin",
        active: true,
      });
      console.log(`✅ Seed admin creado: ${seedEmail}`);
    } catch (e) {
      console.warn("⚠️ No se pudo crear seed admin:", e.message);
    }
  })();

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 Backend running on http://localhost:${PORT}`);
    console.log(`📚 Users → DB: ${DB_BACKOFFICE}, Collection: usuarios`);
    console.log(`🅿️  Parkings → DB: ${DB_PARQUEAME}, Collection: Parkings`);
    console.log(`📝 Endpoints:`);
    console.log(`   - POST /api/register`);
    console.log(`   - POST /api/login`);
    console.log(`   - GET  /api/parkings`);
    console.log(`   - GET  /api/parkings/:id`);
    console.log(`   - PUT  /api/parkings/:id/status/:status`);
    console.log(`   - POST /api/parkings`);
    console.log(`   - POST /api/parkings/:id/comments`);
    console.log(`   - GET  /api/parkings/:id/comments`);
    console.log(`   - DELETE /api/parkings/:id/comments/:commentId`);
    console.log(`   - GET  /api/debug`);
  });
})();