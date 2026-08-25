import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import Bloqueado from '../Bloqueado';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { guardarSesion } from '../../../shared/api/token';

function renderizar(usuario) {
  if (usuario) {
    guardarSesion('token-de-prueba', usuario);
  }
  render(
    <SesionProvider>
      <MemoryRouter>
        <Bloqueado />
      </MemoryRouter>
    </SesionProvider>,
  );
}

describe('Bloqueado', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('muestra el mensaje de sistema bloqueado por licencia vencida', () => {
    renderizar();

    expect(screen.getByText('Sistema bloqueado')).toBeInTheDocument();
    expect(screen.getByText(/licencia/i)).toBeInTheDocument();
  });

  /**
   * §3.6, criterio crítico de T-35: con la licencia vencida el frontend manda todo acá, así que
   * esta pantalla es la única salida del SuperAdmin hacia la que resuelve el bloqueo.
   */
  it('ofrece al SUPERADMIN el acceso a /admin/licencias', () => {
    renderizar({ id: 11, nombreCompleto: 'Ernesto Carlos', rol: 'SUPERADMIN' });

    expect(screen.getByRole('link', { name: /licencia/i })).toHaveAttribute('href', '/admin/licencias');
  });

  /** A los demás roles el endpoint les responde 403: ofrecerles el enlace sería una promesa falsa. */
  it('no ofrece el acceso a los demás roles', () => {
    renderizar({ id: 10, nombreCompleto: 'Mona', rol: 'ADMIN' });

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('tampoco al odontólogo ni sin sesión', () => {
    renderizar({ id: 3, nombreCompleto: 'Dr. Ernesto Pérez', rol: 'ODONTOLOGO' });

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
