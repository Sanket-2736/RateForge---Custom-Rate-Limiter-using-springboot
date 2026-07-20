import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { TrendingUp, TrendingDown, Users, Zap, AlertCircle } from 'lucide-react';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { formatting } from '../utils/formatting';
import { COLORS } from '../config/api';

const Dashboard: React.FC = () => {
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    setIsAnimating(true);
  }, []);

  // Mock data - In production, this would come from your backend
  const metrics = {
    totalRequests: 125432,
    allowedRequests: 119234,
    blockedRequests: 6198,
    redisLatency: 2.3,
    avgResponseTime: 45.2,
    activeClients: 342,
  };

  const requestsPerMinute = [
    { time: '00:00', requests: 234 },
    { time: '05:00', requests: 421 },
    { time: '10:00', requests: 389 },
    { time: '15:00', requests: 512 },
    { time: '20:00', requests: 478 },
    { time: '25:00', requests: 645 },
    { time: '30:00', requests: 523 },
    { time: '35:00', requests: 712 },
    { time: '40:00', requests: 634 },
    { time: '45:00', requests: 891 },
    { time: '50:00', requests: 756 },
    { time: '55:00', requests: 823 },
  ];

  const allowedVsBlocked = [
    { name: 'Allowed', value: metrics.allowedRequests, fill: COLORS.ALLOWED },
    { name: 'Blocked', value: metrics.blockedRequests, fill: COLORS.BLOCKED },
  ];

  const tierDistribution = [
    { name: 'FREE', value: 45000, fill: '#8b5cf6' },
    { name: 'PRO', value: 60000, fill: '#3b82f6' },
    { name: 'ENTERPRISE', value: 20432, fill: '#10b981' },
  ];

  const redisOperations = [
    { operation: 'GET', count: 45230 },
    { operation: 'SET', count: 38902 },
    { operation: 'EVAL', count: 28045 },
    { operation: 'ZREM', count: 12254 },
    { operation: 'ZADD', count: 10203 },
  ];

  const recentActivity = [
    { id: 1, type: 'REQUEST', status: 'allowed', tier: 'PRO', time: '2024-01-15 14:32:21' },
    { id: 2, type: 'REQUEST', status: 'blocked', tier: 'FREE', time: '2024-01-15 14:32:19' },
    { id: 3, type: 'REQUEST', status: 'allowed', tier: 'ENTERPRISE', time: '2024-01-15 14:32:18' },
    { id: 4, type: 'HEALTH', status: 'healthy', tier: 'SYSTEM', time: '2024-01-15 14:32:15' },
    { id: 5, type: 'REQUEST', status: 'allowed', tier: 'PRO', time: '2024-01-15 14:32:12' },
  ];

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1,
        delayChildren: 0.2,
      },
    },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { type: 'spring', stiffness: 100, damping: 15 },
    },
  };

  return (
    <motion.div
      className="p-8 space-y-8"
      variants={containerVariants}
      initial="hidden"
      animate={isAnimating ? 'visible' : 'hidden'}
    >
      {/* Header */}
      <motion.div variants={itemVariants}>
        <h1 className="text-4xl font-bold text-white mb-2">Dashboard</h1>
        <p className="text-slate-400">Real-time analytics and monitoring</p>
      </motion.div>

      {/* Metrics Grid */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4">
        {/* Total Requests */}
        <Card variant="glass" className="lg:col-span-2">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-slate-400 text-sm mb-2">Total Requests</p>
              <p className="text-3xl font-bold text-white">{formatting.formatNumber(metrics.totalRequests)}</p>
            </div>
            <Zap className="w-8 h-8 text-blue-400" />
          </div>
          <div className="flex items-center gap-2 mt-4 text-sm">
            <TrendingUp className="w-4 h-4 text-emerald-400" />
            <span className="text-emerald-400">+12.5% today</span>
          </div>
        </Card>

        {/* Allowed Requests */}
        <Card variant="glass" className="lg:col-span-2">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-slate-400 text-sm mb-2">Allowed</p>
              <p className="text-3xl font-bold text-emerald-400">{formatting.formatNumber(metrics.allowedRequests)}</p>
            </div>
            <TrendingUp className="w-8 h-8 text-emerald-400" />
          </div>
          <div className="flex items-center gap-2 mt-4 text-sm">
            <Badge variant="success">95.1% success rate</Badge>
          </div>
        </Card>

        {/* Blocked Requests */}
        <Card variant="glass" className="lg:col-span-2">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-slate-400 text-sm mb-2">Blocked</p>
              <p className="text-3xl font-bold text-red-400">{formatting.formatNumber(metrics.blockedRequests)}</p>
            </div>
            <AlertCircle className="w-8 h-8 text-red-400" />
          </div>
          <div className="flex items-center gap-2 mt-4 text-sm">
            <Badge variant="error">4.9% blocked</Badge>
          </div>
        </Card>

        {/* Redis Latency */}
        <Card variant="glass" className="lg:col-span-2">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-slate-400 text-sm mb-2">Redis Latency</p>
              <p className="text-3xl font-bold text-white">{metrics.redisLatency}ms</p>
            </div>
            <Zap className="w-8 h-8 text-purple-400" />
          </div>
          <div className="flex items-center gap-2 mt-4 text-sm">
            <Badge variant="success">Optimal</Badge>
          </div>
        </Card>

        {/* Avg Response Time */}
        <Card variant="glass" className="lg:col-span-2">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-slate-400 text-sm mb-2">Avg Response Time</p>
              <p className="text-3xl font-bold text-white">{metrics.avgResponseTime}ms</p>
            </div>
            <TrendingDown className="w-8 h-8 text-blue-400" />
          </div>
          <div className="flex items-center gap-2 mt-4 text-sm">
            <TrendingDown className="w-4 h-4 text-emerald-400" />
            <span className="text-emerald-400">-2.1% vs last hour</span>
          </div>
        </Card>

        {/* Active Clients */}
        <Card variant="glass" className="lg:col-span-2">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-slate-400 text-sm mb-2">Active Clients</p>
              <p className="text-3xl font-bold text-white">{formatting.formatNumber(metrics.activeClients)}</p>
            </div>
            <Users className="w-8 h-8 text-cyan-400" />
          </div>
          <div className="flex items-center gap-2 mt-4 text-sm">
            <TrendingUp className="w-4 h-4 text-orange-400" />
            <span className="text-orange-400">+8 this minute</span>
          </div>
        </Card>
      </motion.div>

      {/* Charts Grid */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Requests per Minute */}
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Requests per Minute</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={requestsPerMinute}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis stroke="#94a3b8" />
              <YAxis stroke="#94a3b8" />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1e293b',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                }}
              />
              <Line
                type="monotone"
                dataKey="requests"
                stroke={COLORS.PRIMARY}
                dot={{ fill: COLORS.PRIMARY, r: 4 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Card>

        {/* Allowed vs Blocked */}
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Requests Distribution</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie data={allowedVsBlocked} cx="50%" cy="50%" labelLine={false} label={({ name, value }) => `${name}: ${formatting.formatNumber(value)}`} outerRadius={80} fill="#8884d8" dataKey="value">
                {allowedVsBlocked.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.fill} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1e293b',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </Card>
      </motion.div>

      {/* More Charts */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Tier Distribution */}
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Tier Distribution</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={tierDistribution}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, value }) => `${name}: ${formatting.formatNumber(value)}`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {tierDistribution.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.fill} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1e293b',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </Card>

        {/* Redis Operations */}
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Redis Operations</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={redisOperations}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis stroke="#94a3b8" />
              <YAxis stroke="#94a3b8" />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#1e293b',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                }}
              />
              <Bar dataKey="count" fill={COLORS.SECONDARY} radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </motion.div>

      {/* Recent Activity */}
      <motion.div variants={itemVariants}>
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Recent Activity</h2>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-700">
                  <th className="text-left py-3 px-4 text-sm font-medium text-slate-400">Type</th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-slate-400">Status</th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-slate-400">Tier</th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-slate-400">Time</th>
                </tr>
              </thead>
              <tbody>
                {recentActivity.map((activity) => (
                  <tr key={activity.id} className="border-b border-slate-800 hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 px-4 text-sm text-slate-300">{activity.type}</td>
                    <td className="py-3 px-4 text-sm">
                      <Badge
                        variant={activity.status === 'allowed' || activity.status === 'healthy' ? 'success' : 'error'}
                      >
                        {activity.status.charAt(0).toUpperCase() + activity.status.slice(1)}
                      </Badge>
                    </td>
                    <td className="py-3 px-4 text-sm text-slate-300">{activity.tier}</td>
                    <td className="py-3 px-4 text-sm text-slate-400">{activity.time}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default Dashboard;
