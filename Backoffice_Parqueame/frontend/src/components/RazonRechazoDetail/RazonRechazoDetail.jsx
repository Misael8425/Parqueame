import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import CommonHeader from '../CommonHeader/CommonHeader';
import './RazonRechazoDetail.css';

const BACKEND_BASE_URL =
  (process.env.REACT_APP_BACKEND_BASE_URL && process.env.REACT_APP_BACKEND_BASE_URL.trim()) ||
  "https://backend-production-d482.up.railway.app";

function RazonRechazoDetail() {
  const location = useLocation();
  const navigate = useNavigate();
  const { id } = useParams();
  const [razon, setRazon] = useState(location.state?.razonRechazo || '');
  const [comentarios, setComentarios] = useState([]); // NUEVO

  useEffect(() => {
    async function fetchReason() {
      if (razon || !id) return;
      try {
        const res = await fetch(`${BACKEND_BASE_URL}/api/parkings/${id}`);
        if (!res.ok) return;
        const data = await res.json();
        const rejection = data?.rejectionReason || data?.detalles?.razonRechazo || '';
        if (rejection) setRazon(rejection);
      } catch (e) {
        // Silencioso
      }
    }
    fetchReason();
  }, [id, razon]);

  // NUEVO: Obtener comentarios adicionales
  useEffect(() => {
    async function fetchComments() {
      if (!id) return;
      try {
        const res = await fetch(`${BACKEND_BASE_URL}/api/parkings/${id}/comments`);
        if (!res.ok) return;
        const data = await res.json();
        const notes = data.comments.filter(c => c.type === 'note');
        setComentarios(notes);
      } catch (e) {
        console.error('Error obteniendo comentarios:', e);
      }
    }
    fetchComments();
  }, [id]);

  // NUEVO: Formatear fecha
  const formatDate = (timestamp) => {
    if (!timestamp) return '';
    const d = new Date(timestamp);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yy = d.getFullYear();
    const hh = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    return `${dd}/${mm}/${yy} ${hh}:${min}`;
  };

  const handleClose = () => navigate(-1);

  return (
    <>
      <CommonHeader />
      <div className="razon-rechazo-container">
        <div className="razon-rechazo-content">
          <h2>Razón de rechazo</h2>
          <div className="razon-group">
            <label>Razón:</label>
            <textarea
              readOnly
              value={razon}
              placeholder="No hay razón de rechazo detallada."
            ></textarea>
          </div>

          {/* NUEVO: Sección de comentarios */}
          {comentarios.length > 0 && (
            <div className="comentarios-section">
              <h3>Comentarios adicionales</h3>
              {comentarios.map((comentario, index) => (
                <div key={comentario.id || index} className="comentario-item">
                  <div className="comentario-header">
                    <span className="comentario-autor">
                      {comentario.authorEmail || 'Usuario'}
                    </span>
                    <span className="comentario-fecha">
                      {formatDate(comentario.createdAt)}
                    </span>
                  </div>
                  <div className="comentario-text">
                    {comentario.text}
                  </div>
                </div>
              ))}
            </div>
          )}

          <button className="close-button" onClick={handleClose}>Cerrar</button>
        </div>
      </div>
    </>
  );
}

export default RazonRechazoDetail;