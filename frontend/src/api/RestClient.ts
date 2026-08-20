import axios from 'axios';
import { API_BASE_URL } from '../config';

/**
 * All backend calls go through this instance. `withCredentials` sends the
 * HttpOnly JWT cookie automatically — that's the only credential this API
 * requires; CSRF protection is disabled on the backend (see SecurityConfig).
 */
const RestClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

export default RestClient;
