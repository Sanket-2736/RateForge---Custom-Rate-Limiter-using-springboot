import React from 'react';
import { motion } from 'framer-motion';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { useSettings } from '../hooks/useSettings';
import { Moon, Sun, Settings as SettingsIcon, RotateCcw } from 'lucide-react';
import clsx from 'clsx';

const Settings: React.FC = () => {
  const { settings, updateSetting, updateSettings, resetSettings } = useSettings();

  const handleReset = () => {
    if (confirm('Are you sure you want to reset all settings to default?')) {
      resetSettings();
    }
  };

  return (
    <motion.div
      className="p-8 space-y-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <div>
        <h1 className="text-4xl font-bold text-white mb-2">Settings</h1>
        <p className="text-slate-400">Configure your RateForge experience</p>
      </div>

      <motion.div
        className="grid grid-cols-1 lg:grid-cols-2 gap-6"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ staggerChildren: 0.1 }}
      >
        {/* Backend URL */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card variant="glass">
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
              <SettingsIcon className="w-5 h-5" /> Backend Configuration
            </h2>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Backend URL</label>
              <input
                type="text"
                value={settings.backendUrl}
                onChange={(e) => updateSetting('backendUrl', e.target.value)}
                className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-sm focus:border-blue-500 focus:outline-none"
                placeholder="http://localhost:3000"
              />
              <p className="text-xs text-slate-500 mt-2">Current: {settings.backendUrl}</p>
            </div>
          </Card>
        </motion.div>

        {/* Polling Configuration */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card variant="glass">
            <h2 className="text-lg font-semibold text-white mb-4">Polling Configuration</h2>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Polling Interval (ms)
              </label>
              <div className="flex gap-2">
                <input
                  type="number"
                  value={settings.pollingInterval}
                  onChange={(e) => updateSetting('pollingInterval', parseInt(e.target.value))}
                  className="flex-1 px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-sm focus:border-blue-500 focus:outline-none"
                  min="1000"
                  step="1000"
                />
              </div>
              <p className="text-xs text-slate-500 mt-2">{settings.pollingInterval}ms (Updates every {(settings.pollingInterval / 1000).toFixed(1)}s)</p>
            </div>
          </Card>
        </motion.div>

        {/* Theme */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card variant="glass">
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
              {settings.darkMode ? (
                <Moon className="w-5 h-5" />
              ) : (
                <Sun className="w-5 h-5" />
              )}
              Theme
            </h2>

            <div className="flex gap-2">
              {[true, false].map((isDark) => (
                <button
                  key={String(isDark)}
                  onClick={() => updateSetting('darkMode', isDark)}
                  className={clsx(
                    'flex-1 px-4 py-2 rounded-lg font-medium transition-all duration-200 flex items-center justify-center gap-2',
                    settings.darkMode === isDark
                      ? 'bg-blue-600 text-white shadow-lg'
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                  )}
                >
                  {isDark ? (
                    <>
                      <Moon className="w-4 h-4" /> Dark
                    </>
                  ) : (
                    <>
                      <Sun className="w-4 h-4" /> Light
                    </>
                  )}
                </button>
              ))}
            </div>
          </Card>
        </motion.div>

        {/* Animation Speed */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card variant="glass">
            <h2 className="text-lg font-semibold text-white mb-4">Animation Speed</h2>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Multiplier: {settings.animationSpeed}x
              </label>
              <input
                type="range"
                min="0.5"
                max="2"
                step="0.25"
                value={settings.animationSpeed}
                onChange={(e) => updateSetting('animationSpeed', parseFloat(e.target.value))}
                className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-purple-500"
              />
              <div className="flex justify-between text-xs text-slate-500 mt-2">
                <span>0.5x</span>
                <span>2x</span>
              </div>
            </div>
          </Card>
        </motion.div>

        {/* Live Updates */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card variant="glass">
            <h2 className="text-lg font-semibold text-white mb-4">Live Updates</h2>

            <div className="space-y-3">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={settings.liveUpdatesEnabled}
                  onChange={(e) => updateSetting('liveUpdatesEnabled', e.target.checked)}
                  className="w-5 h-5 accent-blue-600 rounded"
                />
                <span className="text-sm text-slate-300">Enable live polling</span>
              </label>
              <p className="text-xs text-slate-500">
                {settings.liveUpdatesEnabled
                  ? 'Dashboard and metrics will auto-refresh'
                  : 'Manual refresh only'}
              </p>
            </div>
          </Card>
        </motion.div>
      </motion.div>

      {/* Information */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="grid grid-cols-1 lg:grid-cols-2 gap-6"
      >
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Current Settings</h2>
          <div className="space-y-3">
            <div className="flex justify-between items-center pb-3 border-b border-slate-700">
              <span className="text-sm text-slate-400">Backend URL</span>
              <Badge variant="info" className="text-xs">{settings.backendUrl}</Badge>
            </div>
            <div className="flex justify-between items-center pb-3 border-b border-slate-700">
              <span className="text-sm text-slate-400">Polling Interval</span>
              <Badge variant="info" className="text-xs">{settings.pollingInterval}ms</Badge>
            </div>
            <div className="flex justify-between items-center pb-3 border-b border-slate-700">
              <span className="text-sm text-slate-400">Theme</span>
              <Badge variant="info" className="text-xs">{settings.darkMode ? 'Dark' : 'Light'}</Badge>
            </div>
            <div className="flex justify-between items-center pb-3 border-b border-slate-700">
              <span className="text-sm text-slate-400">Animation Speed</span>
              <Badge variant="info" className="text-xs">{settings.animationSpeed}x</Badge>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-slate-400">Live Updates</span>
              <Badge variant={settings.liveUpdatesEnabled ? 'success' : 'error'} className="text-xs">
                {settings.liveUpdatesEnabled ? 'Enabled' : 'Disabled'}
              </Badge>
            </div>
          </div>
        </Card>

        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Actions</h2>
          <div className="space-y-3">
            <Button onClick={handleReset} variant="outline" className="w-full">
              <RotateCcw className="w-4 h-4" />
              Reset to Defaults
            </Button>
            <p className="text-xs text-slate-500">
              Resetting will restore all settings to their default values. This action cannot be undone.
            </p>
          </div>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default Settings;
