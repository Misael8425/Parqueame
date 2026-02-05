import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import CommonHeader from '../CommonHeader/CommonHeader';
import './SolicitudEditarDetail.css';

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

function SolicitudEditarDetail() {
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
    // Usa solicitudTipo del backend para navegación
    navigate('/solicitud/razon-rechazo', { state: { solicitudId: data.id, solicitudTipo: data.solicitudTipo || 'Editar' } });
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

  const pending = data.pendingChanges || {}; // <-- si tu backend lo agrega

  const currentName     = data.localName || '—';
  const newName         = pending.localName ?? '—';

  const currentAddress  = data.address || '—';
  const newAddress      = pending.address ?? '—';

  const currentCap      = (data.capacity != null) ? String(data.capacity) : '—';
  const newCap          = (pending.capacity != null) ? String(pending.capacity) : '—';

  const currentPrice    = (data.priceHour != null) ? `DOP ${data.priceHour}` : '—';
  const newPrice        = (pending.priceHour != null) ? `DOP ${pending.priceHour}` : '—';

  const currentFrom     = data.schedules?.[0]?.start ? `Desde ${data.schedules[0].start}` : '—';
  const currentTo       = data.schedules?.[0]?.end   ? `Hasta ${data.schedules[0].end}`   : '—';

  const newFrom         = pending.schedules?.[0]?.start ? `Desde ${pending.schedules[0].start}` : '—';
  const newTo           = pending.schedules?.[0]?.end   ? `Hasta ${pending.schedules[0].end}`   : '—';

  return (
    <div className="solicitud-detail-container">
      <CommonHeader />
      <main className="solicitud-detail-main-content">
        <h2 className="solicitud-detail-heading">
          Solicitud de: <span className="solicitud-type">{data.solicitudTipo || 'Editar'}</span> · Estado: {mapStatusToES(data.status)}
        </h2>

        <div className="detail-comparison-grid">
          <div className="column">
            <h3>Actual</h3>
            <div className="detail-group">
              <label>Nombre del Local:</label>
              <input type="text" value={currentName} readOnly />
            </div>
            <div className="detail-group">
              <label>Dirección:</label>
              <input type="text" value={currentAddress} readOnly />
            </div>
            <div className="detail-group">
              <label>Capacidad:</label>
              <input type="text" value={currentCap} readOnly />
            </div>
            <div className="detail-group">
              <label>Precio:</label>
              <div className="price-input-group">
                <input type="text" value={currentPrice} readOnly />
                <button className="price-unit-button">Por hora</button>
              </div>
            </div>
            <div className="detail-group">
              <label>Horario:</label>
              <div className="horario-input-group">
                <input type="text" value={currentFrom} readOnly />
                <input type="text" value={currentTo} readOnly />
              </div>
            </div>
          </div>

          <div className="column">
            <h3>Nuevo</h3>
            <div className="detail-group">
              <label>Nombre del Local:</label>
              <input type="text" value={newName} readOnly />
            </div>
            <div className="detail-group">
              <label>Dirección:</label>
              <input type="text" value={newAddress} readOnly />
            </div>
            <div className="detail-group">
              <label>Capacidad:</label>
              <input type="text" value={newCap} readOnly />
            </div>
            <div className="detail-group">
              <label>Precio:</label>
              <div className="price-input-group">
                <input type="text" value={newPrice} readOnly />
                <button className="price-unit-button changed">Por hora</button>
              </div>
            </div>
            <div className="detail-group">
              <label>Horario:</label>
              <div className="horario-input-group">
                <input type="text" value={newFrom} readOnly />
                <input type="text" value={newTo} readOnly />
              </div>
            </div>
          </div>
        </div>

        <div className="action-buttons">
          <button className="accept-button" onClick={accept} disabled={busy}>
            {busy ? 'Procesando...' : 'Aceptar solicitud'}
          </button>
          <button className="deny-button" onClick={deny} disabled={busy}>Denegar solicitud</button>
        </div>
      </main>
    </div>
  );
}

export default SolicitudEditarDetail;