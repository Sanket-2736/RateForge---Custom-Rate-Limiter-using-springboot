import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Menu, X, BarChart3, Code2, Network, TrendingUp, Settings, Info, Zap } from 'lucide-react';
import clsx from 'clsx';

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/', icon: <BarChart3 className="w-5 h-5" /> },
  { label: 'Algorithms', path: '/algorithms', icon: <Code2 className="w-5 h-5" /> },
  { label: 'API Tester', path: '/api-tester', icon: <Network className="w-5 h-5" /> },
  { label: 'Metrics', path: '/metrics', icon: <TrendingUp className="w-5 h-5" /> },
  { label: 'Tier Limits', path: '/tier-limits', icon: <Zap className="w-5 h-5" /> },
  { label: 'Settings', path: '/settings', icon: <Settings className="w-5 h-5" /> },
  { label: 'About', path: '/about', icon: <Info className="w-5 h-5" /> },
];

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen = true, onClose }) => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const location = useLocation();

  return (
    <>
      {/* Mobile toggle */}
      <button
        onClick={() => setIsCollapsed(!isCollapsed)}
        className="md:hidden fixed top-4 left-4 z-50 p-2 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white"
      >
        {isCollapsed ? <Menu className="w-5 h-5" /> : <X className="w-5 h-5" />}
      </button>

      {/* Sidebar */}
      <motion.aside
        className={clsx(
          'fixed left-0 top-0 h-screen bg-slate-950 border-r border-slate-800 flex flex-col',
          'md:relative md:w-64 md:translate-x-0 md:rounded-none',
          'rounded-r-2xl shadow-xl',
          isCollapsed ? 'w-20' : 'w-64',
          !isOpen && 'hidden'
        )}
        animate={{ x: 0 }}
        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
      >
        {/* Logo */}
        <div className="p-6 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
              <Zap className="w-6 h-6 text-white" />
            </div>
            {!isCollapsed && (
              <div>
                <div className="font-bold text-white text-lg">RateForge</div>
                <div className="text-xs text-slate-400">Rate Limiting</div>
              </div>
            )}
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto p-4 space-y-2">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={onClose}
                className={clsx(
                  'flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200',
                  'hover:bg-slate-800',
                  isActive ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30' : 'text-slate-400'
                )}
              >
                <div className="flex-shrink-0">{item.icon}</div>
                {!isCollapsed && <span className="text-sm font-medium">{item.label}</span>}
                {isActive && !isCollapsed && (
                  <motion.div
                    layoutId="sidebarIndicator"
                    className="absolute right-0 w-1 h-6 bg-blue-500 rounded-l-full"
                  />
                )}
              </Link>
            );
          })}
        </nav>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 text-xs text-slate-400">
          {!isCollapsed && <div>© 2024 RateForge</div>}
        </div>
      </motion.aside>
    </>
  );
};
