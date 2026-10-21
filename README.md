# Sistema de Gestión de Almacén – SRH S.A.C.

Aplicación web para controlar el almacén central de SRH S.A.C. (Planta Callao) y el consumo de sus sedes hospitalarias (Hospital Barton y Hospital Kaelin).

Proyecto del curso Lenguaje de Programación II (Cibertec, T4MO, 2026-II).
Java 17 · Spring Boot 3 · Maven · Spring MVC + Thymeleaf · Spring Data JPA (Hibernate) · Spring Security (BCrypt) · JasperReports · MySQL 8

## Equipo

| Integrante | Rol | Usuario de GitHub |
|---|---|---|
| Pimentel Lugo, Jan Giovanni | Coordinador | GiovanniPimentelCibertec |
| Blas Durand, Julio Antonio | Analista | julioblass |
| Callañaupa Yauri, Esteban | Investigador | stebancya-blip |
| Laguna Mamani, Michael Piero | Diseñador de base de datos | MichaelLaguna |
| Iquise Huamani, Giancarlo Emerson | Desarrollador | giangian20 |
| Zelaya Rivera, Guillermo | Documentación | zrguillen21-a11y |

El aporte de cada integrante se ve en el historial de commits.

## Funcionalidades

| Rol | Qué puede hacer |
|---|---|
| ADMINISTRADOR | Usuarios, sedes y categorías; consultas de kardex y stock |
| JEFE | Productos y marcas, entradas, salidas, ajustes, consumo de los hospitales, reportes PDF, kardex y stock |
| ASISTENTE | Ver productos, registrar entradas y salidas, kardex y stock |
| SEDE | Encargado de un hospital: su stock, confirmar los envíos del almacén y registrar el consumo de cada turno |

- **Salida a sedes (caso transaccional):** varios productos con lo pedido y lo despachado; si un producto no tiene stock, no se guarda nada.
- **Stock mínimo:** consumo promedio diario × días de reposición, recalculado con cada salida. El promedio diario son las salidas de los últimos 90 días divididas entre min(90, días desde la primera salida del producto), como mínimo 1.
- **Consultas:** kardex por producto y rango de fechas, y stock actual con filtros.
- **Reportes PDF (JasperReports):** despachos por sede y mes (lo que el almacén envió a cada sede), y productos por reponer.
- **Hospitales:** cada uno empieza con stock 0, suma lo que confirma de los envíos y resta su consumo por turno.
- Nada se borra: usuarios, sedes y productos se desactivan; categorías y marcas solo se eliminan si ningún producto las usa.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `src/main/java/pe/srh/almacen` | Código Java: `config`, `controller`, `dto`, `entity`, `repository`, `service` |
| `src/main/resources/templates` | Pantallas Thymeleaf |
| `src/main/resources/reportes` | Diseños de los reportes (`.jrxml`) |
| `src/test` | Pruebas automáticas (base de datos en memoria H2) |
| `database/` | Script `bd_almacen_srh.sql` con las 9 tablas y los datos de prueba |
| `docs/analisis/` | Diagramas BPMN para Bizagi Modeler |
| `docs/pruebas/` | Capturas de las pruebas del sistema |

## Cómo ejecutarlo

1. **Base de datos:** abre `database/bd_almacen_srh.sql` en MySQL Workbench y ejecútalo completo (Ctrl+Shift+Enter). Crea las 9 tablas y los datos de prueba.
2. **Contraseña de MySQL:** crea el archivo `config/application.properties` en la carpeta del proyecto con esta línea:
   `spring.datasource.password=TU_CONTRASEÑA`
   Ese archivo es local y no se sube a GitHub.
3. **Importar:** en Eclipse: File → Import → Maven → Existing Maven Projects → elige la carpeta del proyecto.
4. **Ejecutar:** abre `SistemaAlmacenApplication.java` → Run As → Java Application.
5. Abre http://localhost:8080

Usuarios de prueba (contraseña `123456`): `admin` (ADMINISTRADOR), `jefe` (JEFE), `asistente` (ASISTENTE), `barton` y `kaelin` (SEDE).

Pruebas automáticas: `mvn test`.

## Base de datos (9 tablas)

- Almacén central: usuario, sede, categoria, marca, producto, movimiento, movimiento_detalle.
- Sedes hospitalarias: consumo_sede, registro_error.

Los datos (enero a setiembre de 2026) son simulados.
