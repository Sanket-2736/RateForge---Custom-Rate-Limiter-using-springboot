import React from 'react';
import { motion } from 'framer-motion';
import clsx from 'clsx';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
  variant?: 'default' | 'glass' | 'gradient';
  hover?: boolean;
}

export const Card: React.FC<CardProps> = ({
  children,
  className,
  onClick,
  variant = 'default',
  hover = true,
}) => {
  const baseClasses = 'rounded-2xl p-6 backdrop-blur-md transition-all duration-300';

  const variantClasses = {
    default: 'bg-slate-900 border border-slate-700 shadow-xl',
    glass: 'bg-slate-900/40 border border-slate-700/30 shadow-lg',
    gradient: 'bg-gradient-to-br from-slate-900 via-purple-900/20 to-slate-900 border border-purple-500/30 shadow-xl',
  };

  const hoverClasses = hover ? 'hover:shadow-2xl hover:border-purple-500/50' : '';

  return (
    <motion.div
      className={clsx(baseClasses, variantClasses[variant], hoverClasses, className)}
      onClick={onClick}
      whileHover={hover ? { y: -4 } : undefined}
      transition={{ type: 'spring', stiffness: 300, damping: 30 }}
    >
      {children}
    </motion.div>
  );
};
