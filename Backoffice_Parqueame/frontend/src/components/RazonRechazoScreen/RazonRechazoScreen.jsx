import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import CommonHeader from '../CommonHeader/CommonHeader';
import './RazonRechazoScreen.css';

// URL del servidor que conecta DIRECTO a MongoDB (documento 3)
const BACKEND_BASE_URL =
  (process.env.REACT_APP_BACKEND_BASE_URL && process.env.REACT_APP_BACKEND_BASE_URL.trim()) ||
  "https://backend-production-d482.up.railway.app";

function RazonRechazoScreen() {
  const navigate = useNavigate();
  const location = useLocation();
  const [razon, setRazon] = useState('');
  const [loading, setLoading] = useState(false);
  const { solicitudId, solicitudTipo } = location.state || {};

  const handleSubmit = async () => {
    if (razon.trim() === '') {
      alert('Por favor, ingresa una razón para el rechazo.');
      return;
    }
    if (!solicitudId) {
      alert('No se recibió el ID de la solicitud.');
      return;
    }

    setLoading(true);
    try {
      // Llamada DIRECTA al servidor MongoDB con reason en el body
      const response = await fetch(
        `${BACKEND_BASE_URL}/api/parkings/${solicitudId}/status/rejected`,
        {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ 
            reason: razon,
            authorEmail: sessionStorage.getItem('userEmail') || 'sistema'
          }),
        }
      );

      if (!response.ok) {
        throw new Error('Error al rechazar la solicitud');
      }

      const data = await response.json();
      console.log('Rechazo guardado:', data);

      // Redirige a la vista de rechazado
      navigate(`/solicitud/${solicitudId}/rechazada`, {
        state: {
          solicitud: {
            id: solicitudId,
            solicitud: solicitudTipo || 'Apertura',
            estado: 'Rechazado',
            detalles: { razonRechazo: razon }
          }
        }
      });
    } catch (e) {
      console.error('Error al rechazar:', e);
      alert('No se pudo enviar la razón de rechazo. Intenta nuevamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <CommonHeader activeTab="solicitudes" />
      <div className="razon-rechazo-container">
        <div className="razon-rechazo-content">
          <h2>Razón de rechazo</h2>
          <div className="razon-group">
            <label htmlFor="razon">Razón:</label>
            <textarea
              id="razon"
              value={razon}
              onChange={(e) => setRazon(e.target.value)}
              placeholder="Escribe aquí la razón del rechazo..."
              rows="8"
              disabled={loading}
            ></textarea>
          </div>
          <button 
            className="close-button" 
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? 'Enviando...' : 'Enviar'}
          </button>
        </div>
      </div>
    </>
  );
}

export default RazonRechazoScreen;