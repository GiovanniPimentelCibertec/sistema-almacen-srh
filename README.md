# Sistema de Gestión de Almacén – SRH S.A.C.

Aplicación web para controlar el almacén central de SRH S.A.C. (Planta Callao) y el consumo de sus sedes hospitalarias (Hospital Barton y Hospital Kaelin).

Proyecto del curso Lenguaje de Programación II (Cibertec, T4MO, 2026-II).
Java 17 · Spring Boot 3.3.4 · Maven · Spring MVC + Thymeleaf · Spring Data JPA (Hibernate) · Spring Security (BCrypt) · JasperReports 6.21.3 · MySQL 8 · JUnit 5 + H2

## Equipo

| Integrante | Rol | Usuario de GitHub |
|---|---|---|
| Pimentel Lugo, Jan Giovanni | Product Owner | GiovanniPimentelCibertec |
| Zelaya Rivera, Guillermo | Scrum Master y documentación | zrguillen21-a11y |
| Blas Durand, Julio Antonio | Equipo de desarrollo: analista | julioblass |
| Callañaupa Yauri, Esteban | Equipo de desarrollo: investigador | stebancya-blip |
| Laguna Mamani, Michael Piero | Equipo de desarrollo: diseñador de base de datos | MichaelLaguna |
| Iquise Huamani, Giancarlo Emerson | Equipo de desarrollo: desarrollador | giangian20 |

El proyecto se organizó con Scrum en 6 sprints de una semana, con un tablero Kanban en Trello como herramienta. El aporte de cada integrante se ve en el historial de commits.

## Funcionalidades

| Rol | Qué puede hacer |
|---|---|
| ADMINISTRADOR | Usuarios, sedes y categorías; consultas de kardex y stock |
| JEFE | Productos y marcas, entradas, salidas, ajustes, hospitales (stock, envíos y diferencias por revisar), reportes PDF, kardex y stock |
| ASISTENTE | Ver productos, registrar entradas y salidas, kardex y stock |
| SEDE | Encargado de un hospital: su stock, confirmar los envíos del almacén y registrar el consumo de cada turno |

- **Salida a sedes (caso transaccional):** varios productos con lo pedido y lo despachado; si un producto no tiene stock, no se guarda nada (`@Transactional` y bloqueo `PESSIMISTIC_WRITE`). Si se despacha menos de lo pedido, queda como entrega incompleta.
- **Ajustes:** solo el jefe, con su motivo; un ajuste no puede dejar el stock en negativo.
- **Stock mínimo:** consumo promedio diario × días de reposición, recalculado con cada salida. El promedio diario son las salidas de los últimos 90 días divididas entre min(90, días desde la primera salida del producto), como mínimo 1. Sin salidas, se usa el mínimo inicial del producto.
- **Consultas:** kardex por producto y rango de fechas, y stock actual con su situación (Normal o Reponer) y filtros.
- **Reportes PDF (JasperReports):** despachos por sede y mes (lo que el almacén envió a cada sede), y productos por reponer.
- **Hospitales:** cada uno empieza con stock 0, suma lo que confirma de los envíos y resta su consumo por turno. Si un envío llega incompleto, la diferencia queda registrada para que el jefe la revise.
- Nada se borra: usuarios, sedes y productos se desactivan; categorías y marcas solo se eliminan si ningún producto las usa.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `src/main/java/pe/srh/almacen` | Código Java: `config`, `controller`, `dto`, `entity`, `repository`, `service` |
| `src/main/resources/templates` | Pantallas Thymeleaf |
| `src/main/resources/reportes` | Diseños de los reportes (`.jrxml`) |
| `src/test` | Pruebas automáticas (base de datos en memoria H2) |
| `database/` | Script `bd_almacen_srh.sql` con las 9 tablas y los datos de prueba |
| `docs/analisis/` | Diagramas BPMN para Bizagi Modeler (proceso actual, proceso propuesto y registro de consumo en la sede) |
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

Pruebas automáticas: `mvn test` (25 pruebas: 17 de movimientos y 8 de hospitales).

## Base de datos (9 tablas)

- Almacén central: usuario, sede, categoria, marca, producto, movimiento, movimiento_detalle.
- Sedes hospitalarias: consumo_sede, registro_error.

Los datos son simulados: 5 productos EPP y un historial de 30 días (del 21 de setiembre al 20 de octubre de 2026) con entradas, salidas, ajustes, confirmaciones de envío y consumo de los hospitales.

## Control de versiones

- `main`: versión estable.
- `avance-2`: rama del segundo avance; cada integrante subió su parte desde su cuenta y se unió a `main` con los Pull Requests #2 y #3.
- `Esteban`: rama integrada a `main` con el Pull Request #1.
