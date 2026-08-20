import { Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/LoadingSpinner';
import LoginLandingPage from '../pages/LoginLandingPage';
import Layout from '../components/Layout';

export default function ProtectedRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!user) {
    return <LoginLandingPage />;
  }

  return (
    <Layout>
      <Outlet />
    </Layout>
  );
}
