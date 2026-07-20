import { axiosClient, DemoResponse, ApiResponse } from '../axiosClient';
import { API_ENDPOINTS, ALGORITHMS, TIERS } from '../../config/api';

export interface RateLimitRequest {
  algorithm: keyof typeof ALGORITHMS;
  tier: keyof typeof TIERS;
  message?: string;
}

export const RateLimitService = {
  /**
   * Make a rate limited request
   */
  async makeRequest(request: RateLimitRequest): Promise<ApiResponse<DemoResponse>> {
    const tier = TIERS[request.tier];
    const algorithm = request.algorithm;

    let endpoint = '';
    switch (algorithm) {
      case 'TOKEN_BUCKET':
        endpoint = API_ENDPOINTS.DEMO.TOKEN_BUCKET;
        break;
      case 'SLIDING_WINDOW':
        endpoint = API_ENDPOINTS.DEMO.SLIDING_WINDOW;
        break;
      case 'LEAKY_BUCKET':
        endpoint = API_ENDPOINTS.DEMO.LEAKY_BUCKET;
        break;
      default:
        throw new Error(`Unknown algorithm: ${algorithm}`);
    }

    const response = await axiosClient.get<DemoResponse>(endpoint, {
      params: {
        message: request.message || `${algorithm} request`,
      },
      headers: {
        'X-API-Key': tier.apiKey,
      },
    });

    return {
      data: response.data,
      headers: {
        'x-ratelimit-limit': response.headers['x-ratelimit-limit'],
        'x-ratelimit-remaining': response.headers['x-ratelimit-remaining'],
        'x-ratelimit-reset': response.headers['x-ratelimit-reset'],
        'retry-after': response.headers['retry-after'],
      },
      status: response.status,
      timestamp: Date.now(),
    };
  },

  /**
   * Get demo endpoint information
   */
  async getDemoInfo(): Promise<ApiResponse<any>> {
    const response = await axiosClient.get(API_ENDPOINTS.DEMO.INFO);
    return {
      data: response.data,
      headers: response.headers as any,
      status: response.status,
      timestamp: Date.now(),
    };
  },
};
