# Sistema de Gestión de Almacén – SRH S.A.C.

Aplicación web para controlar el almacén central de SRH S.A.C. (Planta Callao) y el consumo de sus sedes hospitalarias (Hospital Barton y Hospital Kaelin).

Proyecto del curso Lenguaje de Programación II (Cibertec, T4MO, 2026-II).
Java 17 · Spring Boot 3 · Maven · Spring MVC + Thymeleaf · Spring Data JPA (Hibernate) · Spring Security (BCrypt) · MySQL 8

## Equipo

| Integrante | Rol | Aporte en el repositorio |
|---|---|---|
| Pimentel Lugo, Jan Giovanni | Coordinador | Estructura base del proyecto Maven |
| Laguna, Michael | Diseñador de base de datos | Script SQL, entidades JPA y repositorios |
| Iquise Huamani, Emerson | Desarrollador | Seguridad, login y pantalla de inicio |
| Blas Durand, Julio Antonio | Analista | Mantenimiento de usuarios y diagramas BPMN |
| Callañaupa Yauri, Esteban | Investigador | Mantenimientos de sedes y categorías, y registro simulado |
| Guillén | Tester y documentación | Mantenimientos de marcas y productos, y evidencias de pruebas |

## Cómo ejecutarlo

1. **Base de datos:** abre `database/bd_almacen_srh.sql` en MySQL Workbench y ejecútalo completo (Ctrl+Shift+Enter). Crea las 9 tablas y los datos de prueba.
2. **Contraseña de MySQL:** en `src/main/resources/application.properties` cambia `spring.datasource.password` por la de tu usuario root.
3. **Importar:** en Eclipse: File → Import → Maven → Existing Maven Projects → elige la carpeta del proyecto.
4. **Ejecutar:** clic derecho en `SistemaAlmacenApplication.java` → Run As → Java Application.
5. Abre http://localhost:8080

Usuarios de prueba (contraseña `123456`): `admin` (ADMINISTRADOR), `jefe` (JEFE), `asistente` (ASISTENTE), `barton` y `kaelin` (SEDE).

## Base de datos (9 tablas)

- Almacén central: usuario, sede, categoria, marca, producto, movimiento, movimiento_detalle.
- Sedes hospitalarias: consumo_sede, registro_error.

Los datos (enero a setiembre 2026) son simulados.
