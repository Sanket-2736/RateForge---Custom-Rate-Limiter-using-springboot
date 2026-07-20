import React from 'react';
import { motion } from 'framer-motion';
import { Github, Moon, Sun, Bell, Search } from 'lucide-react';
import { StatusIndicator } from '../common/StatusIndicator';
import { useBackendStatus } from '../../hooks/useBackendStatus';
import { useSettings } from '../../hooks/useSettings';
import clsx from 'clsx';

interface NavbarProps {
  onMenuClick?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onMenuClick }) => {
  const { appStatus, redisStatus, isHealthy } = useBackendStatus();
  const { settings, updateSetting } = useSettings();
  const [showNotifications, setShowNotifications] = React.useState(false);

  return (
    <motion.nav
      className="sticky top-0 z-40 bg-slate-950/80 backdrop-blur-md border-b border-slate-800 shadow-lg"
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      transition={{ type: 'spring', stiffness: 300, damping: 30 }}
    >
      <div className="px-6 py-4 flex items-center justify-between">
        {/* Left section */}
        <div className="flex items-center gap-6 flex-1">
          <div className="hidden md:flex items-center gap-3 text-sm">
            <StatusIndicator
              status={appStatus === 'ok' ? 'online' : 'offline'}
              label="Backend"
            />
            <span className="text-slate-600">•</span>
            <StatusIndicator
              status={redisStatus === 'connected' ? 'online' : 'offline'}
              label="Redis"
            />
          </div>

          {/* Search */}
          <div className="hidden lg:flex items-center gap-2 bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 flex-1 max-w-xs">
            <Search className="w-4 h-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search..."
              className="bg-transparent outline-none text-sm text-slate-300 placeholder-slate-500 w-full"
            />
          </div>
        </div>

        {/* Right section */}
        <div className="flex items-center gap-4">
          {/* Theme toggle */}
          <motion.button
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
            onClick={() => updateSetting('darkMode', !settings.darkMode)}
            className="p-2 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white transition-colors"
          >
            {settings.darkMode ? (
              <Moon className="w-5 h-5" />
            ) : (
              <Sun className="w-5 h-5" />
            )}
          </motion.button>

          {/* Notifications */}
          <motion.div className="relative">
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setShowNotifications(!showNotifications)}
              className="p-2 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white transition-colors relative"
            >
              <Bell className="w-5 h-5" />
              <div className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
            </motion.button>

            {showNotifications && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="absolute right-0 mt-2 w-64 bg-slate-900 border border-slate-700 rounded-lg shadow-xl p-4"
              >
                <div className="text-sm text-slate-300">
                  <p className="font-medium mb-2">Notifications</p>
                  <p className="text-xs text-slate-400">No new notifications</p>
                </div>
              </motion.div>
            )}
          </motion.div>

          {/* GitHub button */}
          <motion.a
            href="https://github.com/yourusername/rateforge"
            target="_blank"
            rel="noopener noreferrer"
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
            className="p-2 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white transition-colors"
          >
            <Github className="w-5 h-5" />
          </motion.a>

          {/* Profile */}
          <motion.div
            whileHover={{ scale: 1.05 }}
            className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center cursor-pointer font-semibold text-white text-sm"
          >
            RF
          </motion.div>
        </div>
      </div>
    </motion.nav>
  );
};
