import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import AdminCatalogPage from './catalogo/AdminCatalogPage.jsx'
import AdminContentPage from './catalogo/AdminContentPage.jsx'
import CatalogPage from './catalogo/CatalogPage.jsx'
import ContentPage from './catalogo/ContentPage.jsx'
import AccountPage from './cuentas/AccountPage.jsx'
import LoginPage from './cuentas/LoginPage.jsx'
import RegisterPage from './cuentas/RegisterPage.jsx'
import ReportsPage from './informes/ReportsPage.jsx'
import HomePage from './pages/HomePage.jsx'
import ProfilesPage from './perfiles/ProfilesPage.jsx'
import HistoryPage from './reproduccion/HistoryPage.jsx'
import MyListPage from './reproduccion/MyListPage.jsx'
import PlayerPage from './reproduccion/PlayerPage.jsx'
import PlansPage from './suscripciones/PlansPage.jsx'

// Rutas de la aplicación. Cada área (cuentas, perfiles, catálogo, reproducción,
// administración, informes) agrega aquí sus páginas.
const router = createBrowserRouter([
  { path: '/', element: <HomePage /> },

  // Cuentas, perfiles y suscripciones
  { path: '/login', element: <LoginPage /> },
  { path: '/registro', element: <RegisterPage /> },
  { path: '/perfiles', element: <ProfilesPage /> },
  { path: '/planes', element: <PlansPage /> },
  { path: '/cuenta', element: <AccountPage /> },

  // Catálogo
  { path: '/catalogo', element: <CatalogPage /> },
  { path: '/catalogo/:id', element: <ContentPage /> },
  { path: '/admin/catalogo', element: <AdminCatalogPage /> },
  { path: '/admin/catalogo/nuevo', element: <AdminContentPage /> },
  { path: '/admin/catalogo/:id', element: <AdminContentPage /> },

  // Mi lista y reproducción
  { path: '/mi-lista', element: <MyListPage /> },
  { path: '/reproducir/:contentId', element: <PlayerPage /> },
  { path: '/historial', element: <HistoryPage /> },

  // Administración
  { path: '/admin', element: <Navigate to="/admin/catalogo" replace /> },
  { path: '/admin/login', element: <LoginPage admin /> },
  { path: '/admin/informes', element: <ReportsPage /> },

  // Cualquier otra ruta vuelve al inicio
  { path: '*', element: <Navigate to="/" replace /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
