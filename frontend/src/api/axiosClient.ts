import axios, { AxiosInstance, AxiosError, AxiosResponse } from 'axios';
import { API_ENDPOINTS } from '../config/api';

/**
 * Custom Axios instance with interceptors
 */
const createAxiosClient = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: API_ENDPOINTS.HEALTH.split('/health')[0],
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // Request interceptor
  instance.interceptors.request.use(
    (config) => {
      // Add request logging
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`);
      return config;
    },
    (error) => {
      console.error('[API Request Error]', error);
      return Promise.reject(error);
    }
  );

  // Response interceptor
  instance.interceptors.response.use(
    (response) => {
      console.log(`[API Response] ${response.status} ${response.config.url}`);
      return response;
    },
    (error: AxiosError) => {
      console.error('[API Error]', {
        status: error.response?.status,
        message: error.message,
        url: error.config?.url,
      });
      return Promise.reject(error);
    }
  );

  return instance;
};

export const axiosClient = createAxiosClient();

/**
 * Type definitions for API responses
 */
export interface HealthResponse {
  status: string;
}

export interface RedisHealthResponse {
  status: string;
  message: string;
  redisStatus: string;
}

export interface DemoResponse {
  endpoint: string;
  algorithm: string;
  description: string;
  use_case: string;
  message: string;
  timestamp: number;
}

export interface RateLimitHeaders {
  'x-ratelimit-limit'?: string;
  'x-ratelimit-remaining'?: string;
  'x-ratelimit-reset'?: string;
  'retry-after'?: string;
}

export interface ApiResponse<T = any> {
  data: T;
  headers: RateLimitHeaders;
  status: number;
  timestamp: number;
}
