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
});
