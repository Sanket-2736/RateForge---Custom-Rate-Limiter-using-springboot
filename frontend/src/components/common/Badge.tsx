import React from 'react';
import clsx from 'clsx';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'success' | 'error' | 'warning' | 'info' | 'default';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({ children, variant = 'default', className }) => {
  const variantClasses = {
    success: 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30',
    error: 'bg-red-500/20 text-red-300 border border-red-500/30',
    warning: 'bg-orange-500/20 text-orange-300 border border-orange-500/30',
    info: 'bg-blue-500/20 text-blue-300 border border-blue-500/30',
    default: 'bg-slate-700 text-slate-200 border border-slate-600',
  };

  return (
    <span
      className={clsx(
        'inline-flex items-center px-3 py-1 rounded-full text-sm font-medium',
        variantClasses[variant],
        className
      )}
    >
      {children}
    </span>
  );
};
