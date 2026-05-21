import mysql.connector

# ── Conexión ──────────────────────────────────────────────────────────────────
conexion = mysql.connector.connect(
    host="switchyard.proxy.rlwy.net",
    port=25919,
    user="root",
    password="icaHycoMzIDfrUSshmCuiTzqJeyYgocl",
    database="railway"
)

cursor = conexion.cursor()

# ── Crear tablas ──────────────────────────────────────────────────────────────

cursor.execute("""
    CREATE TABLE IF NOT EXISTS generos (
        id          BIGINT       NOT NULL AUTO_INCREMENT,
        nombre      VARCHAR(60)  NOT NULL,
        descripcion VARCHAR(200) NULL,
        activo      TINYINT(1)   NOT NULL DEFAULT 1,
        PRIMARY KEY (id),
        UNIQUE KEY uk_genero_nombre (nombre)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
""")

cursor.execute("""
    CREATE TABLE IF NOT EXISTS tipos_documento (
        id          BIGINT       NOT NULL AUTO_INCREMENT,
        codigo      VARCHAR(10)  NOT NULL,
        nombre      VARCHAR(100) NOT NULL,
        descripcion VARCHAR(250) NULL,
        activo      TINYINT(1)   NOT NULL DEFAULT 1,
        PRIMARY KEY (id),
        UNIQUE KEY uk_tipo_doc_codigo (codigo)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
""")

# ── Insertar datos base ───────────────────────────────────────────────────────

cursor.execute("SELECT COUNT(*) FROM generos")
if cursor.fetchone()[0] == 0:
    cursor.executemany(
        "INSERT INTO generos (nombre, descripcion, activo) VALUES (%s, %s, %s)",
        [
            ("Masculino", None, 1),
            ("Femenino", None, 1),
            ("No binario", "Identidad fuera del binario hombre/mujer", 1),
            ("Prefiero no decirlo", "El usuario elige no especificar", 1),
        ]
    )

cursor.execute("SELECT COUNT(*) FROM tipos_documento")
if cursor.fetchone()[0] == 0:
    cursor.executemany(
        "INSERT INTO tipos_documento (codigo, nombre, descripcion, activo) VALUES (%s, %s, %s, %s)",
        [
            ("CC", "Cédula de Ciudadanía", None, 1),
            ("CE", "Cédula de Extranjería", None, 1),
            ("TI", "Tarjeta de Identidad", None, 1),
            ("PP", "Pasaporte", None, 1),
            ("PEP", "Permiso Especial de Permanencia", None, 1),
        ]
    )

conexion.commit()
cursor.close()
conexion.close()

print("Tablas y datos creados correctamente")