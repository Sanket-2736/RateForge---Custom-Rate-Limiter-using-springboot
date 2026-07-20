import React from 'react';
import { motion } from 'framer-motion';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { COLORS } from '../config/api';

const Metrics: React.FC = () => {
  const latencyData = [
    { time: '00:00', latency: 2.1 },
    { time: '04:00', latency: 2.3 },
    { time: '08:00', latency: 2.5 },
    { time: '12:00', latency: 2.2 },
    { time: '16:00', latency: 2.8 },
    { time: '20:00', latency: 2.4 },
  ];

  const cpuData = [
    { time: '00:00', usage: 35 },
    { time: '06:00', usage: 45 },
    { time: '12:00', usage: 52 },
    { time: '18:00', usage: 48 },
    { time: '23:59', usage: 42 },
  ];

  const errorRateData = [
    { time: '00:00', errors: 0.2 },
    { time: '06:00', errors: 0.3 },
    { time: '12:00', errors: 0.5 },
    { time: '18:00', errors: 0.4 },
    { time: '23:59', errors: 0.2 },
  ];

  return (
    <motion.div
      className="p-8 space-y-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <div>
        <h1 className="text-4xl font-bold text-white mb-2">Metrics</h1>
        <p className="text-slate-400">Performance and system statistics</p>
      </div>

      <motion.div
        className="grid grid-cols-1 md:grid-cols-4 gap-4"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <Card variant="glass">
          <p className="text-slate-400 text-sm mb-2">Avg Latency</p>
          <p className="text-3xl font-bold text-white">2.38ms</p>
          <Badge variant="success" className="mt-2">Optimal</Badge>
        </Card>
        <Card variant="glass">
          <p className="text-slate-400 text-sm mb-2">Error Rate</p>
          <p className="text-3xl font-bold text-white">0.31%</p>
          <Badge variant="success" className="mt-2">Excellent</Badge>
        </Card>
        <Card variant="glass">
          <p className="text-slate-400 text-sm mb-2">Uptime</p>
          <p className="text-3xl font-bold text-white">99.98%</p>
          <Badge variant="success" className="mt-2">Running</Badge>
        </Card>
        <Card variant="glass">
          <p className="text-slate-400 text-sm mb-2">Throughput</p>
          <p className="text-3xl font-bold text-white">45.2K req/s</p>
          <Badge variant="success" className="mt-2">Healthy</Badge>
        </Card>
      </motion.div>

      <motion.div
        className="grid grid-cols-1 lg:grid-cols-2 gap-6"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
      >
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Redis Latency</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={latencyData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis stroke="#94a3b8" />
              <YAxis stroke="#94a3b8" />
              <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }} />
              <Line
                type="monotone"
                dataKey="latency"
                stroke={COLORS.PRIMARY}
                dot={{ fill: COLORS.PRIMARY, r: 4 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Card>

        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Error Rate</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={errorRateData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis stroke="#94a3b8" />
              <YAxis stroke="#94a3b8" />
              <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }} />
              <Bar dataKey="errors" fill={COLORS.BLOCKED} radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </motion.div>

      <motion.div
        className="grid grid-cols-1 lg:grid-cols-2 gap-6"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">CPU Usage</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={cpuData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis stroke="#94a3b8" />
              <YAxis stroke="#94a3b8" />
              <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }} />
              <Bar dataKey="usage" fill={COLORS.SECONDARY} radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">System Health</h2>
          <div className="space-y-4">
            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm text-slate-300">Memory Usage</span>
                <span className="text-sm font-bold text-white">64%</span>
              </div>
              <div className="w-full bg-slate-700 rounded-full h-2">
                <div className="bg-blue-500 h-2 rounded-full" style={{ width: '64%' }} />
              </div>
            </div>
            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm text-slate-300">Disk Usage</span>
                <span className="text-sm font-bold text-white">42%</span>
              </div>
              <div className="w-full bg-slate-700 rounded-full h-2">
                <div className="bg-purple-500 h-2 rounded-full" style={{ width: '42%' }} />
              </div>
            </div>
            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm text-slate-300">Network I/O</span>
                <span className="text-sm font-bold text-white">78%</span>
              </div>
              <div className="w-full bg-slate-700 rounded-full h-2">
                <div className="bg-orange-500 h-2 rounded-full" style={{ width: '78%' }} />
              </div>
            </div>
          </div>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default Metrics;
