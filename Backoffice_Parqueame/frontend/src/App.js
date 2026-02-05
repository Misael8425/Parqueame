import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import LoginScreen from './components/LoginScreen/LoginScreen';
import HomeScreen from './components/HomeScreen/HomeScreen';
import SolicitudAperturaDetail from './components/SolicitudAperturaDetail/SolicitudAperturaDetail';
import SolicitudEditarDetail from './components/SolicitudEditarDetail/SolicitudEditarDetail';
import SolicitudAceptadaScreen from './components/SolicitudAceptadaScreen/SolicitudAceptadaScreen';
import RazonRechazoScreen from './components/RazonRechazoScreen/RazonRechazoScreen'; // Vuelve a importar el componente original
import SolicitudDenegadaScreen from './components/SolicitudDenegadaScreen/SolicitudDenegadaScreen';
import SolicitudAprobadaDetail from './components/SolicitudAprobadaDetail/SolicitudAprobadaDetail';
import SolicitudRechazadaDetail from './components/SolicitudRechazadaDetail/SolicitudRechazadaDetail';
import RazonRechazoDetail from './components/RazonRechazoDetail/RazonRechazoDetail';
// import NewRazonRechazoScreen from './components/NewRazonRechazoScreen/NewRazonRechazoScreen'; // Eliminado el nuevo componente

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/" element={<LoginScreen />} />
          <Route path="/home" element={<HomeScreen />} />
          <Route path="/solicitud/:id/apertura" element={<SolicitudAperturaDetail />} />
          <Route path="/solicitud/:id/editar" element={<SolicitudEditarDetail />} />
          <Route path="/solicitud/aceptada" element={<SolicitudAceptadaScreen />} />
          <Route path="/solicitud/razon-rechazo" element={<RazonRechazoScreen />} /> {/* Vuelve a usar la ruta con el componente original */}
          <Route path="/solicitud/denegada" element={<SolicitudDenegadaScreen />} />
          <Route path="/solicitud/:id/aprobada" element={<SolicitudAprobadaDetail />} />
          <Route path="/solicitud/:id/rechazada" element={<SolicitudRechazadaDetail />} />
          <Route path="/solicitud/:id/razon-rechazo-detail" element={<RazonRechazoDetail />} />
          {/* La ruta para NewRazonRechazoScreen ha sido eliminada */}
        </Routes>
      </div>
    </Router>
  );
}

export default App;