import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import CommonHeader from '../CommonHeader/CommonHeader';
import './SolicitudAprobadaDetail.css';

const BACKEND_BASE_URL =
  (process.env.REACT_APP_BACKEND_BASE_URL && process.env.REACT_APP_BACKEND_BASE_URL.trim()) ||
  "https://backend-production-d482.up.railway.app";

const mapStatusToES = (status) => {
  if (!status) return '—';
  const s = String(status).toLowerCase();
  if (s === 'approved') return 'Aprobado';
  if (s === 'rejected') return 'Rechazado';
  if (s === 'pending')  return 'Pendiente';
  return status;
};

const fileNameFromUrl = (url) => {
  try {
    if (!url) return '—';
    const u = new URL(url);
    return decodeURIComponent(u.pathname.split('/').pop() || '—');
  } catch {
    return '—';
  }
};

function SolicitudAprobadaDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let abort = false;
    async function load() {
      if (!id) return;
      try {
        const res = await fetch(`${BACKEND_BASE_URL}/api/parkings/${id}`);
        if (!res.ok) throw new Error('No se pudo cargar');
        const p = await res.json();
        if (!abort) setData(p);
      } catch (e) {
        console.error(e);
        if (!abort) navigate('/home');
      } finally {
        if (!abort) setLoading(false);
      }
    }
    load();
    return () => { abort = true; };
  }, [id, navigate]);

  const handleClose = () => navigate('/home');

  if (loading) {
    return (
      <div className="solicitud-detail-container">
        <CommonHeader />
        <main className="solicitud-detail-main-content">
          <p>Cargando detalles de la solicitud aprobada...</p>
        </main>
      </div>
    );
  }

  if (!data) return null;

  const docName = fileNameFromUrl(data.infraDocUrl);

  const openDoc = () => {
    const url = data.infraDocUrl;
    if (url && /^https?:\/\//.test(url)) window.open(url, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="solicitud-detail-container">
      <CommonHeader />
      <main className="solicitud-detail-main-content">
        <h2 className="solicitud-detail-heading">
          Solicitud <span className="solicitud-type-blue">{data.solicitudTipo || data.type || ''}</span> -{" "}
          <span className="status-approved">{mapStatusToES(data.status)}</span>
        </h2>

        <div className="detail-grid">
          <div className="detail-group">
            <label>Nombre del Local:</label>
            <input type="text" value={data.localName || '—'} readOnly />
          </div>

          <div className="detail-group">
            <label>Documentos:</label>
            {data.infraDocUrl ? (
              <button type="button" className="document-link" onClick={openDoc}>
                {docName}
              </button>
            ) : (
              <input type="text" value="—" readOnly />
            )}
          </div>

          <div className="detail-group">
            <label>Dirección:</label>
            <input type="text" value={data.address || '—'} readOnly />
          </div>

          <div className="detail-group">
            <label>Precio:</label>
            <div className="price-input-group">
              <input type="text" value={data.priceHour != null ? `DOP ${data.priceHour}` : '—'} readOnly />
              <button className="price-unit-button" disabled>Por hora</button>
            </div>
          </div>

          <div className="detail-group">
            <label>Capacidad:</label>
            <input type="text" value={data.capacity != null ? String(data.capacity) : '—'} readOnly />
          </div>

          <div className="detail-group">
            <label>Horario:</label>
            <div className="horario-input-group">
              <input type="text" value={data.schedules?.[0]?.start ? `Desde ${data.schedules[0].start}` : '—'} readOnly />
              <input type="text" value={data.schedules?.[0]?.end   ? `Hasta ${data.schedules[0].end}`   : '—'} readOnly />
            </div>
          </div>
        </div>

        <div className="action-buttons">
          <button className="close-button" onClick={handleClose}>Cerrar</button>
        </div>
      </main>
    </div>
  );
}

export default SolicitudAprobadaDetail;