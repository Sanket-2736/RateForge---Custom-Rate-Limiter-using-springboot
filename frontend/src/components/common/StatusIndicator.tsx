import React from 'react';
import { motion } from 'framer-motion';
import clsx from 'clsx';

interface StatusIndicatorProps {
  status: 'online' | 'offline' | 'loading';
  label?: string;
  className?: string;
}

export const StatusIndicator: React.FC<StatusIndicatorProps> = ({
  status,
  label,
  className,
}) => {
  const statusColors = {
    online: 'bg-emerald-500',
    offline: 'bg-red-500',
    loading: 'bg-orange-500',
  };

  const dotVariants = {
    online: { scale: [1, 1.2, 1], opacity: [1, 0.7, 1] },
    loading: { scale: [1, 1.3, 1] },
    offline: { scale: 1, opacity: 1 },
  };

  return (
    <div className={clsx('flex items-center gap-2', className)}>
      <motion.div
        className={clsx('w-2.5 h-2.5 rounded-full', statusColors[status])}
        animate={status === 'offline' ? {} : dotVariants[status]}
        transition={{ duration: 2, repeat: Infinity }}
      />
      {label && <span className="text-sm text-slate-300">{label}</span>}
    </div>
  );
};
