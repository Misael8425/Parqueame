// routes/parkings.js
const express = require("express");
const router = express.Router();
const axios = require("axios");

// Apunta al backend directo (Mongo/Express)
const MONGO_API_BASE = (process.env.MONGO_API_BASE || "https://backend-production-d482.up.railway.app").trim();

// Helper axios común
const AXIOS_OPTS = {
  headers: { "Content-Type": "application/json" },
  timeout: 10000,
};

// GET /api/parkings  → pasa filtros tal cual (normalizando 'tipo' -> 'type')
router.get("/", async (req, res) => {
  try {
    const params = { ...req.query };
    if (params.tipo && !params.type) {
      params.type = params.tipo;
      delete params.tipo;
    }
    // solicitudTipo se reenvía tal cual si viene

    const r = await axios.get(`${MONGO_API_BASE}/api/parkings`, {
      params,
      timeout: AXIOS_OPTS.timeout,
    });
    res.json(r.data);
  } catch (err) {
    res
      .status(err.response?.status || 500)
      .json({ message: "Error listando parqueos", error: err.message });
  }
});

// GET /api/parkings/:id  → detalle tal cual lo devuelve el backend
router.get("/:id", async (req, res) => {
  try {
    const { id } = req.params;
    const r = await axios.get(`${MONGO_API_BASE}/api/parkings/${id}`, {
      timeout: AXIOS_OPTS.timeout,
    });
    res.json(r.data);
  } catch (err) {
    res
      .status(err.response?.status || 500)
      .json({ message: "Error obteniendo parqueo", error: err.message });
  }
});

// PUT /api/parkings/:id/status/:status  → forward del body (incluye reason si la envías)
router.put("/:id/status/:status", async (req, res) => {
  try {
    const { id, status } = req.params;
    const r = await axios.put(
      `${MONGO_API_BASE}/api/parkings/${id}/status/${status}`,
      req.body,
      AXIOS_OPTS
    );
    res.json(r.data);
  } catch (err) {
    res
      .status(err.response?.status || 500)
      .json({ message: "Error actualizando estado", error: err.message });
  }
});

// Comentarios (proxy directo)
router.post("/:id/comments", async (req, res) => {
  try {
    const { id } = req.params;
    const r = await axios.post(
      `${MONGO_API_BASE}/api/parkings/${id}/comments`,
      req.body,
      AXIOS_OPTS
    );
    res.json(r.data);
  } catch (err) {
    res
      .status(err.response?.status || 500)
      .json({ message: "Error agregando comentario", error: err.message });
  }
});

router.get("/:id/comments", async (req, res) => {
  try {
    const { id } = req.params;
    const r = await axios.get(
      `${MONGO_API_BASE}/api/parkings/${id}/comments`,
      { timeout: AXIOS_OPTS.timeout }
    );
    res.json(r.data);
  } catch (err) {
    res
      .status(err.response?.status || 500)
      .json({ message: "Error obteniendo comentarios", error: err.message });
  }
});

router.delete("/:id/comments/:commentId", async (req, res) => {
  try {
    const { id, commentId } = req.params;
    const r = await axios.delete(
      `${MONGO_API_BASE}/api/parkings/${id}/comments/${commentId}`,
      { timeout: AXIOS_OPTS.timeout }
    );
    res.json(r.data);
  } catch (err) {
    res
      .status(err.response?.status || 500)
      .json({ message: "Error eliminando comentario", error: err.message });
  }
});

module.exports = router;