package com.labgarcias.ordenes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.Generated;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * T-16: las entidades tienen que reflejar el esquema congelado (01_labgarcias_schema.sql).
 * Se verifica por reflexión, sin base de datos: el objetivo es que un cambio accidental
 * en el mapeo (por ejemplo, hacer escribible una columna generada) rompa la build.
 */
class OrdenMapeoTest {

    private static Field campo(Class<?> entidad, String nombre) {
        try {
            return entidad.getDeclaredField(nombre);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Falta el campo " + nombre + " en " + entidad.getSimpleName(), e);
        }
    }

    private static Set<String> columnasMapeadas(Class<?> entidad) {
        return Arrays.stream(entidad.getDeclaredFields())
                .map(campo -> {
                    Column columna = campo.getAnnotation(Column.class);
                    if (columna != null) {
                        return columna.name();
                    }
                    JoinColumn join = campo.getAnnotation(JoinColumn.class);
                    return join != null ? join.name() : null;
                })
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .collect(Collectors.toSet());
    }

    private static boolean tieneMetodo(Class<?> entidad, String nombre) {
        return Arrays.stream(entidad.getDeclaredMethods()).anyMatch(metodo -> metodo.getName().equals(nombre));
    }

    @Test
    void lasEntidadesApuntanALasTablasDelEsquema() {
        assertThat(Orden.class.getAnnotation(Table.class).name()).isEqualTo("orden");
        assertThat(OrdenHistorialEstado.class.getAnnotation(Table.class).name()).isEqualTo("orden_historial_estado");
        assertThat(OrdenArchivo.class.getAnnotation(Table.class).name()).isEqualTo("orden_archivo");
    }

    @Test
    void precioTotalEsColumnaGeneradaDeSoloLectura() {
        Field precioTotal = campo(Orden.class, "precioTotal");
        Column columna = precioTotal.getAnnotation(Column.class);

        assertThat(columna.insertable()).isFalse();
        assertThat(columna.updatable()).isFalse();
        assertThat(precioTotal.isAnnotationPresent(Generated.class)).isTrue();
        assertThat(tieneMetodo(Orden.class, "setPrecioTotal")).isFalse();
    }

    @Test
    void codigoYPacienteCodigoLosGeneraLaBase() {
        for (String nombre : new String[] { "codigo", "pacienteCodigo" }) {
            Field generado = campo(Orden.class, nombre);
            Column columna = generado.getAnnotation(Column.class);

            assertThat(columna.insertable()).as(nombre + ".insertable").isFalse();
            assertThat(columna.updatable()).as(nombre + ".updatable").isFalse();
            assertThat(generado.isAnnotationPresent(Generated.class)).as(nombre + " @Generated").isTrue();
        }
        assertThat(tieneMetodo(Orden.class, "setCodigo")).isFalse();
        assertThat(tieneMetodo(Orden.class, "setPacienteCodigo")).isFalse();
    }

    @Test
    void ordenMapeaExactamenteLasColumnasDeLaTabla() {
        assertThat(columnasMapeadas(Orden.class)).containsExactlyInAnyOrder(
                "codigo", "odontologo_id", "paciente_nombre", "paciente_iniciales", "paciente_codigo",
                "tipo_trabajo_id", "tipo_orden_id", "estado_id", "descripcion", "fecha_ingreso",
                "fecha_estimada_entrega", "dias_estimados_aplicados", "precio_base", "recargo_urgencia",
                "precio_total", "cargo_cancelacion", "fecha_creacion", "fecha_actualizacion", "fecha_cancelacion");
    }

    @Test
    void historialMapeaExactamenteLasColumnasDeLaTabla() {
        assertThat(columnasMapeadas(OrdenHistorialEstado.class)).containsExactlyInAnyOrder(
                "orden_id", "estado_id", "usuario_id", "fecha_hora", "observacion");
    }

    @Test
    void archivoMapeaExactamenteLasColumnasDeLaTabla() {
        assertThat(columnasMapeadas(OrdenArchivo.class)).containsExactlyInAnyOrder(
                "orden_id", "categoria", "nombre_original", "ruta_almacenamiento", "tipo_mime",
                "tamano_bytes", "subido_por", "fecha_carga");
    }

    @Test
    void cargoCancelacionNoTieneSetter() {
        // P-14: la columna existe pero no se le aplica lógica de cálculo ni cobro; queda en null.
        assertThat(campo(Orden.class, "cargoCancelacion")).isNotNull();
        assertThat(tieneMetodo(Orden.class, "setCargoCancelacion")).isFalse();
    }

    @Test
    void categoriaArchivoTieneSoloLosValoresDelCheckConstraint() {
        assertThat(CategoriaArchivo.values())
                .containsExactlyInAnyOrder(CategoriaArchivo.IMAGEN, CategoriaArchivo.DOCUMENTO);
    }

    @Test
    void laOrdenNoExponeUnSetterDeIdentidadNiDeFechasDeAuditoria() {
        assertThat(tieneMetodo(Orden.class, "setId")).isFalse();
        assertThat(tieneMetodo(Orden.class, "setFechaCreacion")).isFalse();
        assertThat(tieneMetodo(Orden.class, "setFechaActualizacion")).isFalse();
    }
}
