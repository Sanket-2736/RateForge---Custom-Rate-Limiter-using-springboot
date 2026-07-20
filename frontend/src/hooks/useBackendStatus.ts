import { useQuery } from '@tanstack/react-query';
import { HealthService } from '../api/services/HealthService';
import { useSettings } from './useSettings';

/**
 * Hook for checking backend and Redis health status
 */
export const useBackendStatus = () => {
  const { settings } = useSettings();

  const health = useQuery({
    queryKey: ['health'],
    queryFn: () => HealthService.checkHealth(),
    refetchInterval: settings.liveUpdatesEnabled ? settings.pollingInterval : false,
    staleTime: 5000,
  });

  const redisHealth = useQuery({
    queryKey: ['redis-health'],
    queryFn: () => HealthService.checkRedisHealth(),
    refetchInterval: settings.liveUpdatesEnabled ? settings.pollingInterval : false,
    staleTime: 5000,
  });

  return {
    appStatus: health.data?.data?.status || 'unknown',
    redisStatus: redisHealth.data?.data?.redisStatus || 'unknown',
    isHealthy: health.data?.status === 200 && redisHealth.data?.status === 200,
    isLoading: health.isLoading || redisHealth.isLoading,
    isError: health.isError || redisHealth.isError,
  };
};
