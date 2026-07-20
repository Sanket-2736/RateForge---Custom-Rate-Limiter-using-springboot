/**
 * LocalStorage utilities for persisting settings
 */

export interface AppSettings {
  backendUrl: string;
  pollingInterval: number;
  darkMode: boolean;
  animationSpeed: number;
  liveUpdatesEnabled: boolean;
}

const DEFAULT_SETTINGS: AppSettings = {
  backendUrl: 'http://localhost:3000',
  pollingInterval: 5000,
  darkMode: true,
  animationSpeed: 1,
  liveUpdatesEnabled: true,
};

const STORAGE_KEY = 'rateforge-settings';

export const localStorage = {
  /**
   * Get all settings from localStorage
   */
  getSettings(): AppSettings {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      return stored ? { ...DEFAULT_SETTINGS, ...JSON.parse(stored) } : DEFAULT_SETTINGS;
    } catch (error) {
      console.error('Failed to get settings:', error);
      return DEFAULT_SETTINGS;
    }
  },

  /**
   * Update a single setting
   */
  updateSetting<K extends keyof AppSettings>(key: K, value: AppSettings[K]): void {
    try {
      const settings = this.getSettings();
      settings[key] = value;
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch (error) {
      console.error('Failed to update setting:', error);
    }
  },

  /**
   * Update multiple settings
   */
  updateSettings(updates: Partial<AppSettings>): void {
    try {
      const settings = this.getSettings();
      Object.assign(settings, updates);
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch (error) {
      console.error('Failed to update settings:', error);
    }
  },

  /**
   * Reset to default settings
   */
  resetSettings(): void {
    try {
      window.localStorage.removeItem(STORAGE_KEY);
    } catch (error) {
      console.error('Failed to reset settings:', error);
    }
  },
};
