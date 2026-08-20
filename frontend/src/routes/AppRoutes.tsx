import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import RequireRole from './RequireRole';
import SystemsPage from '../pages/SystemsPage';
import SystemDetailPage from '../pages/SystemDetailPage';
import UsersAdminPage from '../pages/UsersAdminPage';
import UnauthorizedPage from '../pages/UnauthorizedPage';

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<SystemsPage />} />
        <Route path="/systems/:systemId" element={<SystemDetailPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route element={<RequireRole when={(u) => u.canManageUsers} />}>
          <Route path="/admin/users" element={<UsersAdminPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
