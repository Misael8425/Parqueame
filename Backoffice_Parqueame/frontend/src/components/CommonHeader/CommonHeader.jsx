import React from 'react';
import { useNavigate } from 'react-router-dom';
import logo from '../../assets/Parquéame.svg'; // Asegúrate de que la ruta al logo sea correcta
import './CommonHeader.css';

function CommonHeader({ activeTab }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    // alert('Cerrando sesión...'); // Esta línea ha sido eliminada
    navigate('/');
  };

  return (
    <header className="common-header">
      <img src={logo} className="app-logo" alt="Parquéame Logo" />
      <nav className="common-nav">
        <button
          className={`nav-button ${activeTab === 'solicitudes' ? 'active' : ''}`}
          onClick={() => navigate('/home')}
        >
          Solicitudes
        </button>
        <button className="nav-button" onClick={handleLogout}>
          Cerrar sesión
        </button>
      </nav>
    </header>
  );
}

export default CommonHeader;