

# 📲 Yape Notificaciones → SQLite → Excel  

Aplicación Android personal desarrollada en **Kotlin + Jetpack Compose + Room + Apache POI**.  
Su objetivo es **registrar notificaciones de Yape automáticamente**, almacenarlas en SQLite y permitir exportarlas a **Excel (.xlsx)** para análisis diario o mensual.  

## 🚀 Funcionalidades principales  

- 🔔 **Escucha notificaciones de Yape** mediante un `NotificationListenerService`.  
- 💾 **Almacena los Yapes recibidos** (monto, contraparte, fecha, texto original) en **SQLite** usando Room.  
- 📊 **Exporta a Excel** con dos opciones:  
  - **Por día** → detalle de todas las operaciones y un resumen.  
  - **Por mes** → detalle agrupado + totales por día.  
- 📂 Archivos guardados directamente en **Descargas (MediaStore)**.  
- ❌ Botón para **borrar todos los registros** en la base de datos.  
- 🎨 Interfaz moderna con **Jetpack Compose (Material 3)**, responsive (probado en Samsung A54).  

---

## 🛠️ Tecnologías usadas  

- **Lenguaje**: Kotlin (1.9.x)  
- **UI**: Jetpack Compose + Material 3  
- **Persistencia**: Room (SQLite)  
- **Exportación Excel**: Apache POI (`poi-ooxml`)  
- **Compatibilidad**: minSdk 26, targetSdk 34  
- **Gradle Plugin**: AGP 8.3.x  

---

## 📷 Flujo de negocio  

1. El usuario habilita el acceso a notificaciones desde la app.  
2. Cada vez que llega una notificación de **Yape**, la app la procesa y guarda en SQLite.  
3. Desde la pantalla principal se puede:  
   - Ver el historial de Yapes.  
   - Exportar un Excel de **día** o de **mes**.  
   - Eliminar todos los registros.  

---

## 🛠️ Proximo a Integrarse 

🔐 Seguridad & Privacidad
- PIN o biometría (huella / FaceID) para abrir la app.
- Cifrado de la base de datos (SQLCipher o Room + EncryptedFile).
- Bloqueo selectivo: que los Excel solo se puedan abrir desde la app (no en cualquier visor).

🎨 Interfaz y Experiencia de Usuario
- Tema oscuro / claro dinámico.
- Selector de fechas avanzado (calendario para elegir rango).
- Etiquetas a los Yapes (ejemplo: “Trabajo”, “Amigos”, “Gastos”).
- Widget en el home con resumen del día.

