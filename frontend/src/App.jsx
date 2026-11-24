import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import AdminCatalogPage from './catalogo/AdminCatalogPage.jsx'
import AdminContentPage from './catalogo/AdminContentPage.jsx'
import CatalogPage from './catalogo/CatalogPage.jsx'
import ContentPage from './catalogo/ContentPage.jsx'
import HomePage from './pages/HomePage.jsx'

// Rutas de la aplicación. Cada área (cuentas, perfiles, catálogo, reproducción,
// administración, informes) agrega aquí sus páginas.
const router = createBrowserRouter([
  { path: '/', element: <HomePage /> },

  // Catálogo
  { path: '/catalogo', element: <CatalogPage /> },
  { path: '/catalogo/:id', element: <ContentPage /> },
  { path: '/admin/catalogo', element: <AdminCatalogPage /> },
  { path: '/admin/catalogo/nuevo', element: <AdminContentPage /> },
  { path: '/admin/catalogo/:id', element: <AdminContentPage /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
