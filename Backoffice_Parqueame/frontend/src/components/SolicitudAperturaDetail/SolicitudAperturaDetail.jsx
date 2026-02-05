import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import CommonHeader from '../CommonHeader/CommonHeader';
import './SolicitudAperturaDetail.css';

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

function SolicitudAperturaDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

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

  const accept = async () => {
    if (!data?.id) return;
    try {
      setBusy(true);
      const res = await fetch(`${BACKEND_BASE_URL}/api/parkings/${data.id}/status/approved`, { method: 'PUT' });
      if (!res.ok) throw new Error('No se pudo aprobar');
      const updated = { ...data, status: 'approved' };
      navigate(`/solicitud/${updated.id}/aprobada`, { state: { solicitud: updated } });
    } catch (e) {
      console.error(e);
      alert('No se pudo aprobar la solicitud.');
    } finally {
      setBusy(false);
    }
  };

  const deny = () => {
    if (!data?.id) return;
    // Usa solicitudTipo del backend (Apertura/Cierre/Editar)
    navigate('/solicitud/razon-rechazo', { state: { solicitudId: data.id, solicitudTipo: data.solicitudTipo || 'Apertura' } });
  };

  const openDoc = () => {
    const url = data?.infraDocUrl;
    if (url && /^https?:\/\//.test(url)) window.open(url, '_blank', 'noopener,noreferrer');
  };

  if (loading) {
    return (
      <div className="solicitud-detail-container">
        <CommonHeader />
        <main className="solicitud-detail-main-content">
          <p>Cargando detalles de la solicitud...</p>
        </main>
      </div>
    );
  }

  if (!data) return null;

  const docName = fileNameFromUrl(data.infraDocUrl);

  return (
    <div className="solicitud-detail-container">
      <CommonHeader />
      <main className="solicitud-detail-main-content">
        <h2 className="solicitud-detail-heading">
          Solicitud <span className="solicitud-type">{data.solicitudTipo || data.type || ''}</span> · Estado: {mapStatusToES(data.status)}
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
              <button className="price-unit-button">Por hora</button>
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
          <button className="accept-button" onClick={accept} disabled={busy}>
            {busy ? 'Procesando...' : 'Aceptar solicitud'}
          </button>
          <button className="deny-button" onClick={deny} disabled={busy}>
            Denegar solicitud
          </button>
        </div>
      </main>
    </div>
  );
}

export default SolicitudAperturaDetail;