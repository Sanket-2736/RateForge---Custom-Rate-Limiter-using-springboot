import { useState, useEffect } from 'react';
import { localStorage, AppSettings } from '../utils/localStorage';

/**
 * Hook for managing application settings
 */
export const useSettings = () => {
  const [settings, setSettings] = useState<AppSettings>(localStorage.getSettings());

  const updateSetting = <K extends keyof AppSettings>(key: K, value: AppSettings[K]) => {
    localStorage.updateSetting(key, value);
    setSettings((prev) => ({ ...prev, [key]: value }));
  };

  const updateSettings = (updates: Partial<AppSettings>) => {
    localStorage.updateSettings(updates);
    setSettings((prev) => ({ ...prev, ...updates }));
  };

  const resetSettings = () => {
    localStorage.resetSettings();
    setSettings(localStorage.getSettings());
  };

  return {
    settings,
    updateSetting,
    updateSettings,
    resetSettings,
  };
};
