import React, { useState } from 'react';
import './LoginScreen.css';
import { useNavigate } from 'react-router-dom';
import logo from '../../assets/Parquéame.svg';

// 👇 URL fija del backend (Railway)
const API_BASE = 'https://backend-production-d482.up.railway.app';

function LoginScreen() {
  const [username, setUsername] = useState('');   // aquí escribes el correo
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();

    const email = String(username || '').trim();
    const pwd = String(password || '').trim();

    if (!email || !pwd) {
      alert('Por favor ingresa usuario y contraseña.');
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/api/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // 👇 el backend espera "email" y "password"
        body: JSON.stringify({ email, password: pwd }),
      });

      let data;
      try {
        data = await response.json();
      } catch {
        data = { message: await response.text() };
      }

      if (response.ok) {
        // alert('Login exitoso'); // Esta línea ha sido eliminada
        // Puedes guardar data.user si lo necesitas
        navigate('/home');
      } else {
        alert(`Error de login: ${data?.message || 'Error desconocido'}`);
      }
    } catch (error) {
      console.error('Error al conectar con el servidor:', error);
      alert('No se pudo conectar con el servidor. Verifica la URL del backend.');
    }
  };

  const togglePasswordVisibility = () => setShowPassword((v) => !v);

  return (
    <div className="login-container">
      <div className="login-card">
        <img src={logo} className="app-logo" alt="Parquéame Logo" />

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Usuario:</label>
            <input
              type="text"
              id="username"
              placeholder="Correo electrónico"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña:</label>
            <div className="password-input-wrapper">
              <input
                type={showPassword ? 'text' : 'password'}
                id="password"
                placeholder="Contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <span className="password-toggle" onClick={togglePasswordVisibility}>
                {showPassword ? (
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 16 16">
                    <path d="M10.5 8a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0z"/>
                    <path d="M0 8s3-5.5 8-5.5S16 8 16 8s-3 5.5-8 5.5S0 8 0 8zm8 3.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z"/>
                  </svg>
                ) : (
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 16 16">
                    <path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7.028 7.028 0 0 0-2.79.588l.77.771A6.594 6.594 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457l-1.823 1.823-.018.019a3.5 3.5 0 0 0-4.474-4.474l-.019-.018L3.5 7.118a6.595 6.595 0 0 1-1.918-2.992l-1.617-.96L0 8c1.5 3 4.5 5.5 8 5.5 1.61 0 3.031-.317 4.386-1.042l.76-.761z"/>
                    <path d="M.293 13.536 13.536.293l1.414 1.414L1.707 14.95 0 13.238l.293.298zm12.912-1.552a3.5 3.5 0 0 1-4.474-4.474l.019-.018 4.455 4.455z"/>
                  </svg>
                )}
              </span>
            </div>
          </div>

          <button type="submit" className="login-button">Ingresar</button>
        </form>
      </div>
    </div>
  );
}

export default LoginScreen;
