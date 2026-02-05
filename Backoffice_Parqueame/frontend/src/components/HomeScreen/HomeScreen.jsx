import React, { useState, useEffect } from 'react';
import './HomeScreen.css';
import { useNavigate } from 'react-router-dom';
import CommonHeader from '../CommonHeader/CommonHeader';

// Backend URL - Updated to use your BFF
const BACKEND_BASE_URL =
  (process.env.REACT_APP_BACKEND_BASE_URL && process.env.REACT_APP_BACKEND_BASE_URL.trim()) ||
  "https://backend-production-d482.up.railway.app";

function toDisplayDate(timestamp) {
  if (!timestamp) return "";
  const d = new Date(timestamp);
  if (Number.isNaN(d.getTime())) return "";
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const yy = String(d.getFullYear()).slice(2);
  return `${dd}/${mm}/${yy}`;
}

export default function HomeScreen() {
  const navigate = useNavigate();

  const [searchTerm, setSearchTerm] = useState('');
  const [estadoFilter, setEstadoFilter] = useState('Todos'); // Todos|Pendiente|Aprobado|Rechazado
  const [fechaFilter, setFechaFilter] = useState('');        // YYYY-MM-DD
  const [tipoFilter, setTipoFilter]   = useState('Todos');   // Tipo/Type

  const [displayedSolicitudes, setDisplayedSolicitudes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Statistics
  const [stats, setStats] = useState({
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0
  });

  // Function to load data from MongoDB via BFF
  const loadData = async (search, estado, fecha, tipo) => {
    try {
      setLoading(true);
      setError('');

      const params = new URLSearchParams();
      if (search) params.set("q", search);
      if (estado && estado !== 'Todos') params.set("estado", estado);
      if (fecha) params.set("fecha", fecha);
      if (tipo && tipo !== 'Todos') params.set("type", tipo);

      const url = `${BACKEND_BASE_URL}/api/parkings?${params.toString()}`;
      console.log('Fetching from:', url);
      
      const response = await fetch(url, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      });

      if (!response.ok) {
        if (response.status === 404) {
          setDisplayedSolicitudes([]);
          return;
        }
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody?.message || `Error HTTP ${response.status}`);
      }
      
      const data = await response.json();
      console.log('Response data:', data);

      // Normaliza 'tipo' (soporta tipo, type y solicitudTipo)
      const rows = (data?.items || []).map((s) => ({
        ...s,
        tipo: s.tipo ?? s.type ?? s.solicitudTipo ?? '—',
        fecha: s.fecha || toDisplayDate(s._raw?.createdAt || s.createdAt || Date.now()),
      }));

      setDisplayedSolicitudes(rows);

      const newStats = {
        total: rows.length,
        pending: rows.filter(r => r.estado === 'Pendiente').length,
        approved: rows.filter(r => r.estado === 'Aprobado').length,
        rejected: rows.filter(r => r.estado === 'Rechazado').length
      };
      setStats(newStats);

    } catch (e) {
      console.error('Error loading data:', e);
      setError(e.message || "No se pudieron cargar las solicitudes.");
      setDisplayedSolicitudes([]);
      setStats({ total: 0, pending: 0, approved: 0, rejected: 0 });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData(searchTerm, estadoFilter, fechaFilter, tipoFilter);
  }, [searchTerm, estadoFilter, fechaFilter, tipoFilter]);

  const reload = () => loadData(searchTerm, estadoFilter, fechaFilter, tipoFilter);

  const handleFilterClick = () => reload();

  // 👇 Ruta correcta según TIPO de solicitud
  const handleSolicitudClick = (solicitud) => {
    const tipoRaw =
      solicitud?.tipo ||
      solicitud?.type ||
      solicitud?.solicitudTipo ||
      '';

    const tipo = String(tipoRaw).toUpperCase();

    if (!solicitud?.id) return;

    if (tipo === 'EDITAR') {
      navigate(`/solicitud/${solicitud.id}/editar`, { state: { solicitud } });
    } else if (tipo === 'APERTURA') {
      navigate(`/solicitud/${solicitud.id}/apertura`, { state: { solicitud } });
    } else if (tipo === 'CIERRE') {
      navigate(`/solicitud/${solicitud.id}/cierre`, { state: { solicitud } });
    } else {
      // Fallback: si no tenemos tipo, intenta ir al detalle de apertura
      navigate(`/solicitud/${solicitud.id}/apertura`, { state: { solicitud } });
    }
  };

  // APPROVE: hace PUT y navega a la pantalla de aprobada
  async function approveAndGo(solicitud) {
    if (!solicitud?.id) return;
    try {
      setLoading(true);
      const response = await fetch(`${BACKEND_BASE_URL}/api/parkings/${solicitud.id}/status/approved`, {
        method: "PUT",
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody?.message || `Error actualizando estado (approved)`);
      }
      const updated = { ...solicitud, estado: 'Aprobado' };
      navigate(`/solicitud/${solicitud.id}/aprobada`, { state: { solicitud: updated } });
    } catch (e) {
      console.error('Error approving:', e);
      setError(e.message || "No se pudo aprobar la solicitud.");
    } finally {
      setLoading(false);
    }
  }

  // REJECT: ir a pantalla para escribir razón (ahí se hace el PUT)
  function goToRejectionReason(solicitud) {
    if (!solicitud?.id) return;
    const solicitudTipo =
      solicitud.tipo ||
      solicitud.type ||
      solicitud.solicitudTipo ||
      'Apertura';
    navigate('/solicitud/razon-rechazo', {
      state: { solicitudId: solicitud.id, solicitudTipo }
    });
  }

  const getStatusColor = (estado) => {
    switch (estado) {
      case 'Aprobado': return '#28a745';
      case 'Rechazado': return '#dc3545';
      case 'Pendiente':
      default: return '#6c757d';
    }
  };

  return (
    <div className="solicitudes-container">
      <CommonHeader activeTab="solicitudes" />

      <main className="solicitudes-main-content">
        <h2 className="solicitudes-heading">Solicitudes de Parqueos</h2>

        {/* Statistics Cards */}
        <div className="stats-section" style={{ 
          display: 'flex', 
          gap: '16px', 
          marginBottom: '24px',
          flexWrap: 'wrap'
        }}>
          <div className="stat-card" style={{
            padding: '16px',
            backgroundColor: '#f8f9fa',
            borderRadius: '8px',
            flex: '1',
            minWidth: '120px',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '24px', color: '#333' }}>{stats.total}</h3>
            <p style={{ margin: 0, color: '#6c757d', fontSize: '14px' }}>Total</p>
          </div>
          <div className="stat-card" style={{
            padding: '16px',
            backgroundColor: '#fff3cd',
            borderRadius: '8px',
            flex: '1',
            minWidth: '120px',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '24px', color: '#856404' }}>{stats.pending}</h3>
            <p style={{ margin: 0, color: '#856404', fontSize: '14px' }}>Pendientes</p>
          </div>
          <div className="stat-card" style={{
            padding: '16px',
            backgroundColor: '#d4edda',
            borderRadius: '8px',
            flex: '1',
            minWidth: '120px',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '24px', color: '#155724' }}>{stats.approved}</h3>
            <p style={{ margin: 0, color: '#155724', fontSize: '14px' }}>Aprobados</p>
          </div>
          <div className="stat-card" style={{
            padding: '16px',
            backgroundColor: '#f8d7da',
            borderRadius: '8px',
            flex: '1',
            minWidth: '120px',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '24px', color: '#721c24' }}>{stats.rejected}</h3>
            <p style={{ margin: 0, color: '#721c24', fontSize: '14px' }}>Rechazados</p>
          </div>
        </div>

        <div className="filter-section">
          <div className="search-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
            </svg>
            <input
              type="text"
              placeholder="Buscar por nombre o dirección"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="filters">
            <span>Filtrar por:</span>

            {/* Estado */}
            <label htmlFor="estadoFilter">Estado</label>
            <select
              id="estadoFilter"
              value={estadoFilter}
              onChange={(e) => setEstadoFilter(e.target.value)}
            >
              <option value="Todos">Todos</option>
              <option value="Pendiente">Pendiente</option>
              <option value="Aprobado">Aprobado</option>
              <option value="Rechazado">Rechazado</option>
            </select>

            {/* Tipo */}
            <label htmlFor="tipoFilter">Tipo</label>
            <select
              id="tipoFilter"
              value={tipoFilter}
              onChange={(e) => setTipoFilter(e.target.value)}
            >
              <option value="Todos">Todos</option>
              <option value="APERTURA">APERTURA</option>
              <option value="EDITAR">EDITAR</option>
              <option value="CIERRE">CIERRE</option>
              <option value="RESIDENCIAL">RESIDENCIAL</option>
              <option value="COMERCIAL">COMERCIAL</option>
              <option value="CALLE">CALLE</option>
              <option value="VALET">VALET</option>
            </select>

            <label htmlFor="fechaFilter">Fecha</label>
            <input
              type="date"
              id="fechaFilter"
              value={fechaFilter}
              onChange={(e) => setFechaFilter(e.target.value)}
            />

            <button
              type="button"
              className="filter-button"
              onClick={() => setFechaFilter('')}
              title="Quitar filtro de fecha"
              style={{ marginLeft: 8 }}
            >
              Limpiar fecha
            </button>

            <button className="filter-button" onClick={handleFilterClick}>Actualizar</button>
          </div>
        </div>

        {loading && (
          <div style={{ 
            padding: '16px', 
            color: '#6c757d',
            textAlign: 'center',
            backgroundColor: '#f8f9fa',
            borderRadius: '4px',
            margin: '16px 0'
          }}>
            🔄 Cargando solicitudes...
          </div>
        )}
        
        {error && (
          <div style={{ 
            padding: '16px', 
            color: '#721c24', 
            backgroundColor: '#f8d7da', 
            borderRadius: '4px', 
            margin: '16px 0',
            border: '1px solid #f5c6cb'   // <-- FIX DEL TYPO
          }}>
            ⚠️ {error}
          </div>
        )}

        <div className="solicitudes-table-container">
          <table className="solicitudes-table">
            <thead>
              <tr>
                <th></th>
                <th>Tipo</th>
                <th>Estado</th>
                <th>Local</th>
                <th>Dirección</th>
                <th>Capacidad</th>
                <th style={{ textAlign: 'left' }}>Precio/Hora</th>
                <th>Fecha</th>
                <th style={{ width: 200 }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {displayedSolicitudes.map((solicitud) => (
                <tr key={solicitud.id}>
                  <td></td>

                  {/* Click en TIPO → abre el detalle correcto según el tipo */}
                  <td
                    onClick={() => handleSolicitudClick(solicitud)}
                    style={{ 
                      cursor: 'pointer', 
                      color: '#007bff', 
                      textDecoration: 'underline',
                      fontWeight: '500'
                    }}
                  >
                    {solicitud.tipo ?? '—'}
                  </td>

                  {/* Click en ESTADO → si ya está aprobado/rechazado, lleva a su pantalla */}
                  <td
                    onClick={
                      solicitud.estado === 'Aprobado'
                        ? () => navigate(`/solicitud/${solicitud.id}/aprobada`, { state: { solicitud } })
                        : solicitud.estado === 'Rechazado'
                          ? () => navigate(`/solicitud/${solicitud.id}/rechazada`, { state: { solicitud } })
                          : undefined
                    }
                    style={{
                      cursor: solicitud.estado !== 'Pendiente' ? 'pointer' : 'default',
                      color: getStatusColor(solicitud.estado),
                      textDecoration: solicitud.estado !== 'Pendiente' ? 'underline' : 'none',
                      fontWeight: 'bold'
                    }}
                  >
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '12px',
                      fontSize: '12px',
                      fontWeight: 'bold',
                      backgroundColor: solicitud.estado === 'Aprobado' ? '#d4edda' :
                                      solicitud.estado === 'Rechazado' ? '#f8d7da' :
                                      '#fff3cd',
                      color: getStatusColor(solicitud.estado)
                    }}>
                      {solicitud.estado}
                    </span>
                  </td>

                  <td style={{ fontWeight: '500' }}>{solicitud.local}</td>
                  <td style={{ 
                    maxWidth: '200px', 
                    overflow: 'hidden', 
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }} title={solicitud.direccion}>
                    {solicitud.direccion}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    {solicitud.capacidad || 'N/A'}
                  </td>
                  <td style={{ textAlign: 'left', fontWeight: '500' }}>
                    RD$ {solicitud.precio || 0}
                  </td>
                  <td>{solicitud.fecha}</td>

                  <td>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        onClick={() => approveAndGo(solicitud)}
                        disabled={solicitud.estado === "Aprobado" || loading}
                        style={{ 
                          backgroundColor: solicitud.estado === "Aprobado" ? '#28a745' : '#007bff',
                          color: 'white',
                          border: 'none',
                          padding: '6px 12px',
                          borderRadius: '4px',
                          cursor: solicitud.estado === "Aprobado" || loading ? 'not-allowed' : 'pointer',
                          opacity: solicitud.estado === "Aprobado" || loading ? 0.6 : 1,
                          fontSize: '12px',
                          fontWeight: '500',
                          transition: 'all 0.2s',
                          width: '100px',
                          textAlign: 'center'
                        }}
                        title={solicitud.estado === "Aprobado" ? "Ya está aprobado" : "Aprobar solicitud"}
                      >
                        {solicitud.estado === "Aprobado" ? "✓ Aprobado" : "Aprobar"}
                      </button>

                      <button
                        onClick={() => goToRejectionReason(solicitud)}
                        disabled={solicitud.estado === "Rechazado" || loading}
                        style={{
                          backgroundColor: solicitud.estado === "Rechazado" ? '#dc3545' : '#ffc107',
                          color: solicitud.estado === "Rechazado" ? 'white' : 'black',
                          border: 'none',
                          padding: '6px 12px',
                          borderRadius: '4px',
                          cursor: solicitud.estado === "Rechazado" || loading ? 'not-allowed' : 'pointer',
                          opacity: solicitud.estado === "Rechazado" || loading ? 0.6 : 1,
                          fontSize: '12px',
                          fontWeight: '500',
                          transition: 'all 0.2s',
                          width: '100px',
                          textAlign: 'center'
                        }}
                        title={solicitud.estado === "Rechazado" ? "Ya está rechazado" : "Rechazar solicitud"}
                      >
                        {solicitud.estado === "Rechazado" ? "✗ Rechazado" : "Rechazar"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}

              {!loading && !error && displayedSolicitudes.length === 0 && (
                <tr>
                  <td colSpan="9" style={{ 
                    textAlign: 'center', 
                    padding: '40px 20px', 
                    color: '#6c757d',
                    fontStyle: 'italic'
                  }}>
                    {estadoFilter === 'Todos' ? 
                      "📋 No hay solicitudes de parqueos registradas." :
                      `📋 No hay solicitudes con estado "${estadoFilter}".`
                    }
                    <br />
                    <small style={{ fontSize: '12px', marginTop: '8px', display: 'block' }}>
                      Los datos se cargan directamente desde MongoDB Atlas
                    </small>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

      </main>
    </div>
  );
}