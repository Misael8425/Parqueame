import React from 'react';
import { useNavigate } from 'react-router-dom';
import './SolicitudDenegadaScreen.css';
import CommonHeader from '../CommonHeader/CommonHeader';

function SolicitudDenegadaScreen() {
  const navigate = useNavigate();

  const handleClose = () => {
    navigate('/home'); // Redirige a la pantalla de inicio
  };

  return (
    <div className="solicitud-denegada-container">
      <CommonHeader />
      <main className="solicitud-denegada-content">
        <h2 className="solicitud-denegada-heading">Solicitud <span className="status-denied">Denegada</span></h2>
        <div className="cross-circle">
          <svg className="cross-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 52 52">
            <circle className="cross-circle-path" cx="26" cy="26" r="25" fill="none"/>
            <path className="cross-check" fill="none" d="M16 16 36 36 M36 16 16 36"/>
          </svg>
        </div>
        <button className="close-button" onClick={handleClose}>Cerrar</button>
      </main>
    </div>
  );
}

export default SolicitudDenegadaScreen;