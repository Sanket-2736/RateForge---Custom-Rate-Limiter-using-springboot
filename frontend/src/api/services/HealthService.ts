import { axiosClient, HealthResponse, RedisHealthResponse, ApiResponse } from '../axiosClient';
import { API_ENDPOINTS } from '../../config/api';

export const HealthService = {
  /**
   * Check application health
   */
  async checkHealth(): Promise<ApiResponse<HealthResponse>> {
    const response = await axiosClient.get<HealthResponse>(API_ENDPOINTS.HEALTH);
    return {
      data: response.data,
      headers: response.headers as any,
      status: response.status,
      timestamp: Date.now(),
    };
  },

  /**
   * Check Redis connectivity health
   */
  async checkRedisHealth(): Promise<ApiResponse<RedisHealthResponse>> {
    const response = await axiosClient.get<RedisHealthResponse>(API_ENDPOINTS.HEALTH_REDIS);
    return {
      data: response.data,
      headers: response.headers as any,
      status: response.status,
      timestamp: Date.now(),
    };
  },
};
