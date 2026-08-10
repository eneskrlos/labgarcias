import { createContext, useContext, useState } from 'react';
import { borrarSesion, guardarSesion, obtenerUsuario } from '../api/token';

const SesionContext = createContext(null);

export function SesionProvider({ children }) {
  const [usuario, setUsuario] = useState(() => obtenerUsuario());

  const iniciarSesion = (token, usuarioAutenticado) => {
    guardarSesion(token, usuarioAutenticado);
    setUsuario(usuarioAutenticado);
  };

  const cerrarSesion = () => {
    borrarSesion();
    setUsuario(null);
  };

  return (
    <SesionContext.Provider value={{ usuario, iniciarSesion, cerrarSesion }}>
      {children}
    </SesionContext.Provider>
  );
}

export function useSesion() {
  const contexto = useContext(SesionContext);
  if (!contexto) {
    throw new Error('useSesion debe usarse dentro de <SesionProvider>.');
  }
  return contexto;
}
