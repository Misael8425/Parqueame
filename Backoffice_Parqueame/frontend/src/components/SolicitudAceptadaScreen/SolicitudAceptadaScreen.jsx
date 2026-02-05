import React from 'react';
import { useNavigate } from 'react-router-dom';
import './SolicitudAceptadaScreen.css';
import CommonHeader from '../CommonHeader/CommonHeader';

function SolicitudAceptadaScreen() {
  const navigate = useNavigate();

  const handleClose = () => {
    navigate('/home');
  };

  return (
    <div className="solicitud-aceptada-container">
      <CommonHeader />
      <main className="solicitud-aceptada-content">
        <h2 className="solicitud-aceptada-heading">Solicitud <span className="status-accepted">Aceptada</span></h2>
        <div className="checkmark-circle">
          <svg className="checkmark-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 52 52">
            <circle className="checkmark-circle-path" cx="26" cy="26" r="25" fill="none"/>
            <path className="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
          </svg>
        </div>
        <button className="close-button" onClick={handleClose}>Cerrar</button>
      </main>
    </div>
  );
}

export default SolicitudAceptadaScreen;