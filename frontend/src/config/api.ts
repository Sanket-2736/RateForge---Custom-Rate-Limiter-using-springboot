/**
 * API Configuration
 * Centralized API endpoint definitions
 */

// Backend URL - can be replaced with real backend values
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';

export const API_ENDPOINTS = {
  // Health checks
  HEALTH: `${API_BASE_URL}/health`,
  HEALTH_REDIS: `${API_BASE_URL}/health/redis`,

  // Demo endpoints
  DEMO: {
    TOKEN_BUCKET: `${API_BASE_URL}/demo/token-bucket`,
    SLIDING_WINDOW: `${API_BASE_URL}/demo/sliding-window`,
    LEAKY_BUCKET: `${API_BASE_URL}/demo/leaky-bucket`,
    INFO: `${API_BASE_URL}/demo/info`,
  },
} as const;

export const ALGORITHMS = {
  TOKEN_BUCKET: 'TOKEN_BUCKET',
  SLIDING_WINDOW: 'SLIDING_WINDOW',
  LEAKY_BUCKET: 'LEAKY_BUCKET',
} as const;

export const TIERS = {
  FREE: { name: 'FREE', limit: '100/hour', apiKey: 'demo-free', capacity: 100, rate: 0.0277 },
  PRO: { name: 'PRO', limit: '1000/hour', apiKey: 'demo-pro', capacity: 1000, rate: 0.2778 },
  ENTERPRISE: { name: 'ENTERPRISE', limit: '1000000/hour', apiKey: 'demo-enterprise', capacity: 1000000, rate: 277.8 },
} as const;

export const COLORS = {
  ALLOWED: '#10b981',      // Emerald
  BLOCKED: '#ef4444',       // Red
  PENDING: '#f59e0b',       // Orange
  PRIMARY: '#3b82f6',       // Slate Blue
  SECONDARY: '#8b5cf6',     // Purple
} as const;
