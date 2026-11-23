import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import HomePage from './pages/HomePage.jsx'

// Rutas de la aplicación. Cada área (cuentas, perfiles, catálogo, reproducción,
// administración, informes) agrega aquí sus páginas.
const router = createBrowserRouter([
  { path: '/', element: <HomePage /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
