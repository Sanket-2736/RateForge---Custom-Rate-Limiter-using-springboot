import React from 'react';
import { motion } from 'framer-motion';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { CheckCircle, Zap, TrendingUp, Users, Database } from 'lucide-react';
import { TIERS } from '../config/api';

const TierLimits: React.FC = () => {
  const tierDetails = [
    {
      ...TIERS.FREE,
      description: 'For testing and personal projects',
      features: [
        'Up to 100 requests per hour',
        'Basic rate limiting',
        'Standard support',
        'Single instance',
      ],
    },
    {
      ...TIERS.PRO,
      description: 'For production applications',
      features: [
        'Up to 1,000 requests per hour',
        'Advanced rate limiting',
        'Priority support',
        'Multiple instances',
        'API analytics',
      ],
    },
    {
      ...TIERS.ENTERPRISE,
      description: 'For large-scale operations',
      features: [
        'Up to 1,000,000 requests per hour',
        'Custom rate limiting',
        'Dedicated support',
        'Unlimited instances',
        'Advanced analytics',
        'Custom SLAs',
      ],
    },
  ];

  return (
    <motion.div
      className="p-8 space-y-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <div>
        <h1 className="text-4xl font-bold text-white mb-2">Tier Configuration</h1>
        <p className="text-slate-400">Rate limit tiers for different user levels</p>
      </div>

      <motion.div
        className="grid grid-cols-1 md:grid-cols-3 gap-6"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ staggerChildren: 0.1 }}
      >
        {tierDetails.map((tier, idx) => (
          <motion.div
            key={tier.name}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.1 }}
          >
            <Card
              variant={tier.name === 'PRO' ? 'gradient' : 'glass'}
              className={tier.name === 'PRO' ? 'ring-2 ring-purple-500' : ''}
            >
              <div className="mb-6">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h2 className="text-2xl font-bold text-white mb-1">{tier.name}</h2>
                    <p className="text-sm text-slate-400">{tier.description}</p>
                  </div>
                  {tier.name === 'PRO' && (
                    <Badge variant="info">Recommended</Badge>
                  )}
                </div>

                <p className="text-3xl font-bold text-white mb-2">{tier.limit}</p>
              </div>

              <div className="space-y-3 mb-6 pb-6 border-b border-slate-700">
                {tier.features.map((feature) => (
                  <div key={feature} className="flex items-start gap-3">
                    <CheckCircle className="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" />
                    <span className="text-sm text-slate-300">{feature}</span>
                  </div>
                ))}
              </div>

              <div className="space-y-3">
                <div className="flex items-center gap-2 text-sm">
                  <Zap className="w-4 h-4 text-orange-400" />
                  <span className="text-slate-300">Token Bucket compatible</span>
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <TrendingUp className="w-4 h-4 text-blue-400" />
                  <span className="text-slate-300">Auto-scaling</span>
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <Users className="w-4 h-4 text-purple-400" />
                  <span className="text-slate-300">Unlimited clients</span>
                </div>
              </div>
            </Card>
          </motion.div>
        ))}
      </motion.div>

      {/* Current Usage */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
      >
        <Card variant="glass">
          <h2 className="text-lg font-semibold text-white mb-6">Current Usage (Today)</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              { tier: 'FREE', current: 85, total: 100 },
              { tier: 'PRO', current: 742, total: 1000 },
              { tier: 'ENTERPRISE', current: 523421, total: 1000000 },
            ].map((usage) => (
              <div key={usage.tier}>
                <div className="flex justify-between mb-3">
                  <span className="text-sm font-medium text-slate-300">{usage.tier} Tier</span>
                  <span className="text-sm font-bold text-white">
                    {usage.current} / {usage.total}
                  </span>
                </div>
                <div className="w-full bg-slate-700 rounded-full h-3">
                  <div
                    className={`h-3 rounded-full transition-all duration-300 ${
                      usage.tier === 'FREE'
                        ? 'bg-red-500'
                        : usage.tier === 'PRO'
                          ? 'bg-orange-500'
                          : 'bg-emerald-500'
                    }`}
                    style={{ width: `${(usage.current / usage.total) * 100}%` }}
                  />
                </div>
                <p className="text-xs text-slate-400 mt-2">
                  {Math.round((usage.current / usage.total) * 100)}% used
                </p>
              </div>
            ))}
          </div>
        </Card>
      </motion.div>

      {/* Algorithm Configuration */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
      >
        <Card variant="glass">
          <h2 className="text-lg font-semibold text-white mb-6">Algorithm Configuration</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              {
                name: 'Token Bucket',
                description: 'Accumulates tokens, allows bursts',
                config: 'Capacity: 100-1M, Rate: 0.01-277.8 tokens/sec',
              },
              {
                name: 'Sliding Window',
                description: 'Precise per-window request counting',
                config: 'Window: 1s-1h, Max Requests: 1-1M',
              },
              {
                name: 'Leaky Bucket',
                description: 'Smooths traffic at constant rate',
                config: 'Capacity: 10-1M, Rate: 0.1-277.8 items/sec',
              },
            ].map((algo) => (
              <div key={algo.name} className="bg-slate-800 border border-slate-700 rounded-lg p-4">
                <h3 className="font-semibold text-white mb-2">{algo.name}</h3>
                <p className="text-sm text-slate-400 mb-3">{algo.description}</p>
                <p className="text-xs text-slate-500 bg-slate-900 p-2 rounded">{algo.config}</p>
              </div>
            ))}
          </div>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default TierLimits;
