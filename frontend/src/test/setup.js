import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

// Sin `globals: true` en vitest.config, @testing-library/react no detecta un
// afterEach global para su limpieza automática: se registra a mano acá, una
// sola vez, para que el DOM no quede montado entre tests de distintos archivos.
afterEach(() => {
  cleanup();
});
