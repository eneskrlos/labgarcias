import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { RutaProtegida } from '../RutaProtegida';
import { SesionProvider } from '../../hooks/useSesion';
import { guardarUsuario } from '../../api/token';

function renderizarConRuteo() {
  render(
    <MemoryRouter initialEntries={['/protegida']}>
      <SesionProvider>
        <Routes>
          <Route
            path="/protegida"
            element={
              <RutaProtegida rolesPermitidos={['ADMIN']}>
                <div>Contenido protegido</div>
              </RutaProtegida>
            }
          />
          <Route path="/login" element={<div>Pantalla de login</div>} />
          <Route path="/cambiar-password" element={<div>Pantalla de cambio de contraseña</div>} />
        </Routes>
      </SesionProvider>
    </MemoryRouter>,
  );
}

function renderizarPantallaDeCambio() {
  render(
    <MemoryRouter initialEntries={['/cambiar-password']}>
      <SesionProvider>
        <Routes>
          <Route
            path="/cambiar-password"
            element={
              <RutaProtegida permitidaConCambioPendiente>
                <div>Formulario de cambio</div>
              </RutaProtegida>
            }
          />
          <Route path="/login" element={<div>Pantalla de login</div>} />
        </Routes>
      </SesionProvider>
    </MemoryRouter>,
  );
}

describe('RutaProtegida', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('redirige a /login si no hay sesión', () => {
    renderizarConRuteo();

    expect(screen.getByText('Pantalla de login')).toBeInTheDocument();
    expect(screen.queryByText('Contenido protegido')).not.toBeInTheDocument();
  });

  it('redirige a /login si el rol del usuario no está permitido', () => {
    guardarUsuario({ id: 1, nombreCompleto: 'Juan', rol: 'ODONTOLOGO' });

    renderizarConRuteo();

    expect(screen.getByText('Pantalla de login')).toBeInTheDocument();
  });

  it('muestra el contenido si el usuario tiene un rol permitido', () => {
    guardarUsuario({ id: 1, nombreCompleto: 'Ana', rol: 'ADMIN' });

    renderizarConRuteo();

    expect(screen.getByText('Contenido protegido')).toBeInTheDocument();
  });

  /** §3.1.b: con el cambio pendiente, toda ruta lleva a /cambiar-password. */
  it('redirige a /cambiar-password si la sesión tiene el cambio pendiente', () => {
    guardarUsuario({ id: 1, nombreCompleto: 'Ana', rol: 'ADMIN', debeCambiarPassword: true });

    renderizarConRuteo();

    expect(screen.getByText('Pantalla de cambio de contraseña')).toBeInTheDocument();
    expect(screen.queryByText('Contenido protegido')).not.toBeInTheDocument();
  });

  /** La propia pantalla de cambio es la excepción; si no, se redirigiría a sí misma. */
  it('la pantalla de cambio sí se muestra con el cambio pendiente', () => {
    guardarUsuario({ id: 1, nombreCompleto: 'Juan', rol: 'ODONTOLOGO', debeCambiarPassword: true });

    renderizarPantallaDeCambio();

    expect(screen.getByText('Formulario de cambio')).toBeInTheDocument();
  });

  /** Sin sesión, el cambio pendiente no aplica: primero hay que iniciar sesión. */
  it('sin sesión, la pantalla de cambio manda al login', () => {
    renderizarPantallaDeCambio();

    expect(screen.getByText('Pantalla de login')).toBeInTheDocument();
  });
});
