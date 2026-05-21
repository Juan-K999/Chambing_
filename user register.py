import random

import requests as re
import faker

faker = faker.Faker('en_US')
n = int(input("¿Cuántos usuarios deseas registrar? "))

import asyncio
import aiohttp
from faker import Faker
import random

faker = Faker("es_ES")

headers = {
    'Accept': '*/*',
    'Accept-Language': 'es-US,es-419;q=0.9,es;q=0.8,en;q=0.7',
    'Connection': 'keep-alive',
    'Content-Type': 'application/json',
    'Origin': 'http://localhost:9999',
    'Referer': 'http://localhost:9999/register',
    'User-Agent': 'Mozilla/5.0',
}

URL = "http://localhost:9999/usuario"


async def create_user(session):

    name = faker.first_name()
    last_name = faker.last_name()

    json_data = {
        'tipoDocumento': 'CC',
        'documento': random.randint(100000000, 999999999),
        'nombre': name,
        'apellido': last_name,
        'edad': random.randint(18, 65),
        'genero': random.choice([
            'Masculino',
            'Femenino',
            'Prefiero no decirlo'
        ]),
        'email': f'{name.lower()}{last_name.lower()}{random.randint(1,99999)}@gmail.com',
        'contrasena': faker.password(
            length=10,
            special_chars=True,
            digits=True,
            upper_case=True,
            lower_case=True
        ),
        'roles': ['USER'],
        'direccion': {
            'calle': faker.street_name(),
            'ciudad': faker.city(),
            'departamento': faker.state(),
        },
    }

    try:

        async with session.post(
            URL,
            headers=headers,
            json=json_data,
            allow_redirects=False
        ) as response:

            data = await response.json()

            print(
                data.get(
                    'idUsuario',
                    f'Error -> {response.status}'
                )
            )

    except Exception as e:
        print(e)


async def users(n):

    connector = aiohttp.TCPConnector(limit=20)

    async with aiohttp.ClientSession(
        connector=connector
    ) as session:

        tasks = []

        for _ in range(n):
            tasks.append(
                asyncio.create_task(
                    create_user(session)
                )
            )

        await asyncio.gather(*tasks)


import asyncio
import aiohttp
import random
from faker import Faker

fake = Faker("es_ES")

BASE_URL = "http://localhost:9999"

categorias = [
    "Marketing Digital",
    "Desarrollo Web",
    "Diseño Gráfico",
    "Ciberseguridad",
    "Data Science",
    "Backend",
    "Frontend",
    "DevOps",
    "Soporte TI",
    "Machine Learning",
    "QA Testing",
    "UX/UI",
    "Administración",
    "Contabilidad",
    "Recursos Humanos",
    "Ventas",
    "Atención al Cliente",
    "Cloud Computing",
    "Redes",
    "Inteligencia Artificial"
]

estados = ["Activa", "Pausada"]

niveles = [
    "Junior",
    "Semi Senior",
    "Senior"
]

modalidades = [
    "Remoto",
    "Híbrido",
    "Presencial"
]

tecnologias = [
    "Python",
    "Java",
    "Spring Boot",
    "React",
    "Angular",
    "Node.js",
    "Docker",
    "Kubernetes",
    "MongoDB",
    "PostgreSQL",
    "AWS",
    "Azure",
    "Git",
    "Linux",
    "TensorFlow"
]


HEADERS_JSON = {
    "Accept": "*/*",
    "Content-Type": "application/json",
    "Origin": BASE_URL,
    "Referer": f"{BASE_URL}/register",
    "User-Agent": "Mozilla/5.0"
}

HEADERS_FORM = {
    "Accept": "text/html,application/xhtml+xml",
    "Content-Type": "application/x-www-form-urlencoded",
    "Origin": BASE_URL,
    "Referer": f"{BASE_URL}/login",
    "User-Agent": "Mozilla/5.0"
}


async def registrar_empresa(session):
    nombre = fake.company().replace(",", "")

    correo = f"""
    {fake.first_name()}
    {fake.last_name()}
    {random.randint(100,9999)}
    @gmail.com
    """.replace("\n", "").replace(" ", "").lower()

    password = fake.password(
        length=12,
        special_chars=True,
        digits=True,
        upper_case=True,
        lower_case=True
    )

    data = {
        "nit": random.randint(100000000, 999999999),
        "nombreEmpresa": nombre,
        "correo": correo,
        "contrasena": password,
        "roles": ["EMPRESA"],
        "enabled": True,
        "direccion": {
            "calle": fake.street_name(),
            "ciudad": random.choice([
                "Cartagena",
                "Barranquilla",
                "Bogotá",
                "Medellín",
                "Cali",
                "Bucaramanga"
            ]),
            "departamento": random.choice([
                "Bolivar",
                "Atlantico",
                "Antioquia",
                "Cundinamarca",
                "Valle del Cauca"
            ])
        }
    }

    async with session.post(
        f"{BASE_URL}/empresa",
        headers=HEADERS_JSON,
        json=data
    ) as response:

        try:
            result = await response.json()
        except:
            print("❌ Error registrando empresa")
            return None

        empresa_id = result.get("idEmpresa")

        if empresa_id:
            print(f"✅ Empresa creada -> {nombre}")
            return {
                "id": empresa_id,
                "correo": correo,
                "password": password
            }

        print("❌ No se pudo crear empresa")
        return None


async def login_empresa(session, correo, password):

    payload = {
        "username": correo,
        "password": password,
        "_csrf": ""
    }

    async with session.post(
        f"{BASE_URL}/login",
        headers=HEADERS_FORM,
        data=payload,
        allow_redirects=True
    ) as response:

        if response.status in [200, 302]:
            print(f"🔐 Login correcto -> {correo}")
            return True

        print(f"❌ Login falló -> {correo}")
        return False


def generar_propuesta():

    techs = random.sample(tecnologias, random.randint(2, 5))

    return {
        "titulo": f"{random.choice(niveles)} {random.choice(categorias)}",
        "salario": random.randint(1200000, 15000000),
        "descripcion": fake.text(max_nb_chars=300),
        "requisitos": (
            f"Experiencia con {', '.join(techs)}. "
            f"{fake.sentence()}"
        ),
        "categoria": random.choice(categorias),
        "estado": random.choice(estados),

        # extras opcionales por si tu backend los soporta
        "modalidad": random.choice(modalidades),
        "vacantes": random.randint(1, 10),
        "experiencia": f"{random.randint(0, 8)} años"
    }


async def crear_propuesta(session, empresa_id):

    data = generar_propuesta()

    async with session.post(
        f"{BASE_URL}/empresa/{empresa_id}/propuestas",
        headers=HEADERS_JSON,
        json=data
    ) as response:

        if response.status in [200, 201]:
            print(f"   📄 {data['titulo']}")
        else:
            text = await response.text()
            print(f"   ❌ Error propuesta {response.status}")
            print(text[:200])


async def flujo_empresa():

    timeout = aiohttp.ClientTimeout(total=60)

    async with aiohttp.ClientSession(
        timeout=timeout,
        cookie_jar=aiohttp.CookieJar()
    ) as session:

        empresa = await registrar_empresa(session)

        if not empresa:
            return

        login_ok = await login_empresa(
            session,
            empresa["correo"],
            empresa["password"]
        )

        if not login_ok:
            return

        cantidad = random.randint(1, 10)

        tareas = [
            crear_propuesta(session, empresa["id"])
            for _ in range(cantidad)
        ]

        await asyncio.gather(*tareas)

        print(f"🏁 Empresa completada -> {cantidad} propuestas\n")


async def empresas(n):

    tareas = [
        flujo_empresa()
        for _ in range(n)
    ]

    await asyncio.gather(*tareas)

import asyncio
import aiohttp
import random
from faker import Faker

fake = Faker("es_ES")

BASE_URL = "http://localhost:9999"

HEADERS_JSON = {
    "Accept": "*/*",
    "Content-Type": "application/json",
    "Origin": BASE_URL,
    "User-Agent": "Mozilla/5.0"
}

HEADERS_FORM = {
    "Accept": "text/html,application/xhtml+xml",
    "Content-Type": "application/x-www-form-urlencoded",
    "Origin": BASE_URL,
    "User-Agent": "Mozilla/5.0"
}


# =========================
# REGISTRAR USUARIO
# =========================
async def registrar_usuario(session):

    nombre = fake.first_name()
    apellido = fake.last_name()

    correo = f"""
    {nombre}
    {apellido}
    {random.randint(100,9999)}
    @gmail.com
    """.replace("\n", "").replace(" ", "").lower()

    password = fake.password(
        length=12,
        special_chars=True,
        digits=True,
        upper_case=True,
        lower_case=True
    )

    data = {
        "tipoDocumento": "CC",
        "documento": random.randint(100000000, 999999999),
        "nombre": nombre,
        "apellido": apellido,
        "edad": random.randint(18, 55),
        "genero": random.choice([
            "Masculino",
            "Femenino",
            "Prefiero no decirlo"
        ]),
        "email": correo,
        "contrasena": password,
        "roles": ["USER"],
        "direccion": {
            "calle": fake.street_name(),
            "ciudad": random.choice([
                "Cartagena",
                "Barranquilla",
                "Bogotá",
                "Medellín",
                "Cali"
            ]),
            "departamento": "Bolivar"
        }
    }

    async with session.post(
        f"{BASE_URL}/usuario",
        headers=HEADERS_JSON,
        json=data
    ) as response:

        try:
            result = await response.json()
        except:
            print("❌ Error registrando usuario")
            return None

        usuario_id = result.get("idUsuario")

        if usuario_id:
            print(f"✅ Usuario creado -> {correo}")

            return {
                "id": usuario_id,
                "correo": correo,
                "password": password
            }

        print("❌ No se pudo registrar usuario")
        return None


# =========================
# LOGIN
# =========================
async def login_usuario(session, correo, password):

    payload = {
        "username": correo,
        "password": password,
        "_csrf": ""
    }

    async with session.post(
        f"{BASE_URL}/login",
        headers=HEADERS_FORM,
        data=payload,
        allow_redirects=True
    ) as response:

        if response.status in [200, 302]:
            print(f"🔐 Login correcto -> {correo}")
            return True

        print(f"❌ Login falló -> {correo}")
        return False


# =========================
# OBTENER PROPUESTAS
# =========================
async def obtener_propuestas(session):

    async with session.get(
        f"{BASE_URL}/usuario/propuestas",
        headers=HEADERS_JSON
    ) as response:

        try:
            propuestas = await response.json()

            print(f"📄 Propuestas encontradas: {len(propuestas)}")

            return propuestas

        except Exception as e:
            print("❌ Error obteniendo propuestas")
            print(e)
            return []


# =========================
# APLICAR A PROPUESTA
# =========================
async def aplicar(session, usuario_id, propuesta_id):

    async with session.post(
        f"{BASE_URL}/usuario/aplicar/{usuario_id}/{propuesta_id}",
        headers=HEADERS_JSON
    ) as response:

        if response.status in [200, 201]:
            print(f"   ✅ Aplicado -> {propuesta_id}")
        else:
            text = await response.text()
            print(f"   ❌ Error aplicando -> {propuesta_id}")
            print(text[:150])


# =========================
# FLUJO COMPLETO
# =========================
async def flujo_usuario():

    timeout = aiohttp.ClientTimeout(total=60)

    async with aiohttp.ClientSession(
        timeout=timeout,
        cookie_jar=aiohttp.CookieJar()
    ) as session:

        # 1. Crear usuario
        usuario = await registrar_usuario(session)

        if not usuario:
            return

        # 2. Login
        login_ok = await login_usuario(
            session,
            usuario["correo"],
            usuario["password"]
        )

        if not login_ok:
            return

        # 3. Obtener propuestas
        propuestas = await obtener_propuestas(session)

        if not propuestas:
            print("⚠️ No hay propuestas")
            return

        # 4. Elegir propuestas aleatorias
        random.shuffle(propuestas)

        cantidad = min(
            random.randint(1, 20),
            len(propuestas)
        )

        seleccionadas = propuestas[:cantidad]

        print(f"🚀 Aplicando a {cantidad} propuestas...\n")

        # 5. Aplicar async
        tareas = []

        for propuesta in seleccionadas:

            propuesta_id = (
                propuesta.get("idPropuesta")
                or propuesta.get("id")
            )

            if propuesta_id:
                tareas.append(
                    aplicar(
                        session,
                        usuario["id"],
                        propuesta_id
                    )
                )

        await asyncio.gather(*tareas)

        print(f"\n🏁 Usuario completado -> {usuario['correo']}")


# =========================
# CREAR MUCHOS USUARIOS
# =========================
async def usuarios(n):

    tareas = [
        flujo_usuario()
        for _ in range(n)
    ]

    await asyncio.gather(*tareas)


# EJEMPLO

asyncio.run(empresas(n))
asyncio.run(usuarios(n))


# EJEMPLO



#asyncio.run(users(n))