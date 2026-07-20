import React from 'react';
import { motion } from 'framer-motion';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { Code2, Database, Layers, Lock, Zap, Github } from 'lucide-react';

const About: React.FC = () => {
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
      animate="visible"
    >
      {/* Header */}
      <motion.div variants={itemVariants}>
        <h1 className="text-4xl font-bold text-white mb-2">About RateForge</h1>
        <p className="text-slate-400 max-w-2xl">
          An enterprise-grade rate limiting service demonstrating advanced distributed systems patterns,
          atomic operations, and real-time monitoring.
        </p>
      </motion.div>

      {/* Overview */}
      <motion.div variants={itemVariants}>
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Project Overview</h2>
          <p className="text-slate-300 mb-4">
            RateForge is a production-ready rate limiting service that demonstrates three sophisticated rate limiting
            algorithms implemented with atomic Lua scripts on Redis. The system prevents race conditions, supports
            multiple tier configurations, and provides real-time monitoring through a modern React dashboard.
          </p>
          <div className="flex gap-2">
            <Badge variant="success">Production Ready</Badge>
            <Badge variant="info">Open Source</Badge>
            <Badge variant="warning">v1.0.0</Badge>
          </div>
        </Card>
      </motion.div>

      {/* Algorithms */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          {
            name: 'Token Bucket',
            icon: <Zap className="w-6 h-6" />,
            description: 'Accumulates tokens at a fixed rate. Allows burst traffic up to capacity.',
            useCases: ['General API rate limiting', 'Traffic with occasional spikes'],
          },
          {
            name: 'Sliding Window',
            icon: <Layers className="w-6 h-6" />,
            description: 'Counts requests within a moving time window for precise request counting.',
            useCases: ['Billing APIs', 'Audit logs', 'Accurate per-minute limits'],
          },
          {
            name: 'Leaky Bucket',
            icon: <Database className="w-6 h-6" />,
            description: 'Queue drains at a constant rate, smoothing bursty traffic.',
            useCases: ['Protecting downstream systems', 'Steady stream enforcement'],
          },
        ].map((algo) => (
          <Card key={algo.name} variant="glass">
            <div className="flex items-center gap-3 mb-4">
              <div className="text-blue-400">{algo.icon}</div>
              <h3 className="text-lg font-semibold text-white">{algo.name}</h3>
            </div>
            <p className="text-sm text-slate-400 mb-4">{algo.description}</p>
            <div>
              <p className="text-xs font-medium text-slate-300 mb-2">Use Cases:</p>
              <ul className="space-y-1">
                {algo.useCases.map((useCase) => (
                  <li key={useCase} className="text-xs text-slate-500">• {useCase}</li>
                ))}
              </ul>
            </div>
          </Card>
        ))}
      </motion.div>

      {/* Technical Details */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Lock className="w-5 h-5" /> Atomicity & Race Conditions
          </h2>
          <p className="text-sm text-slate-300 mb-4">
            All algorithms use Lua scripts executed atomically on Redis. This eliminates race conditions where
            concurrent requests could both see the same state and consume duplicate resources.
          </p>
          <div className="bg-slate-800 border border-slate-700 rounded-lg p-3">
            <p className="text-xs font-mono text-slate-400 mb-2">Lua Script Benefit:</p>
            <p className="text-xs text-slate-300">
              GET → check → SET executed as single atomic operation on Redis server, preventing interleaving.
            </p>
          </div>
        </Card>

        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Database className="w-5 h-5" /> Redis Integration
          </h2>
          <p className="text-sm text-slate-300 mb-4">
            Leverages Redis for sub-millisecond state management. Supports both local and cloud Redis instances
            (Upstash, AWS ElastiCache, etc.).
          </p>
          <div className="space-y-2 text-xs text-slate-400">
            <div>✓ Sub-millisecond latency</div>
            <div>✓ Atomic Lua script execution</div>
            <div>✓ Distributed state management</div>
            <div>✓ Horizontal scaling support</div>
          </div>
        </Card>
      </motion.div>

      {/* Architecture */}
      <motion.div variants={itemVariants}>
        <Card variant="glass">
          <h2 className="text-lg font-semibold text-white mb-4">Architecture</h2>
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <h3 className="font-medium text-blue-400 mb-2">Backend</h3>
                <ul className="text-sm text-slate-400 space-y-1">
                  <li>• Java 17 with Spring Boot</li>
                  <li>• Redis client (Lettuce)</li>
                  <li>• Lua script evaluation</li>
                  <li>• HTTP servlet filter</li>
                </ul>
              </div>
              <div>
                <h3 className="font-medium text-purple-400 mb-2">Frontend</h3>
                <ul className="text-sm text-slate-400 space-y-1">
                  <li>• React 19 + TypeScript</li>
                  <li>• TanStack Query (caching)</li>
                  <li>• Framer Motion (animations)</li>
                  <li>• Chart.js (analytics)</li>
                </ul>
              </div>
            </div>
            <div className="bg-slate-800 border border-slate-700 rounded-lg p-4 mt-4">
              <p className="text-xs font-mono text-slate-400">
                [Client] → [HTTP] → [Spring Boot] → [Redis] ← [Lua Scripts]
              </p>
            </div>
          </div>
        </Card>
      </motion.div>

      {/* Key Features */}
      <motion.div variants={itemVariants}>
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-6">Key Features</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[
              { icon: '🔒', title: 'Atomic Operations', desc: 'Race-condition free with Lua scripts' },
              { icon: '⚡', title: 'Sub-ms Latency', desc: 'Redis-backed rate limiting checks' },
              { icon: '📊', title: 'Real-time Analytics', desc: 'Live dashboard with metrics' },
              { icon: '🎯', title: 'Multi-tier', desc: 'FREE, PRO, ENTERPRISE configurations' },
              { icon: '🔄', title: 'Horizontal Scaling', desc: 'Redis Cluster support' },
              { icon: '🐳', title: 'Containerized', desc: 'Docker with health checks' },
              { icon: '🧪', title: 'Fully Tested', desc: '16 unit tests, 100% pass rate' },
              { icon: '📱', title: 'Responsive UI', desc: 'Mobile-friendly dashboard' },
            ].map(({ icon, title, desc }) => (
              <div key={title} className="flex gap-3">
                <span className="text-2xl">{icon}</span>
                <div>
                  <p className="font-medium text-white text-sm">{title}</p>
                  <p className="text-xs text-slate-400">{desc}</p>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </motion.div>

      {/* Tier Limits */}
      <motion.div variants={itemVariants}>
        <Card variant="glass">
          <h2 className="text-lg font-semibold text-white mb-4">Tier Configuration</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {[
              { tier: 'FREE', limit: '100/hour', icon: '🎁' },
              { tier: 'PRO', limit: '1,000/hour', icon: '⭐' },
              { tier: 'ENTERPRISE', limit: '1,000,000/hour', icon: '👑' },
            ].map(({ tier, limit, icon }) => (
              <div key={tier} className="bg-slate-800 border border-slate-700 rounded-lg p-4 text-center">
                <p className="text-2xl mb-2">{icon}</p>
                <p className="font-semibold text-white text-lg">{tier}</p>
                <p className="text-sm text-slate-400">{limit}</p>
              </div>
            ))}
          </div>
        </Card>
      </motion.div>

      {/* Technology Stack */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card variant="glass">
          <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Code2 className="w-5 h-5" /> Backend Stack
          </h2>
          <div className="space-y-2">
            {[
              'Java 17',
              'Spring Boot 4.1.0',
              'Redis (Lettuce)',
              'Lua Scripts',
              'Maven',
              'Docker & Docker Compose',
            ].map((tech) => (
              <Badge key={tech} variant="info" className="inline-block">
                {tech}
              </Badge>
            ))}
          </div>
        </Card>

        <Card variant="glass">
          <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Code2 className="w-5 h-5" /> Frontend Stack
          </h2>
          <div className="space-y-2">
            {[
              'React 19',
              'TypeScript',
              'Vite',
              'TanStack Query',
              'Framer Motion',
              'Chart.js',
            ].map((tech) => (
              <Badge key={tech} variant="info" className="inline-block">
                {tech}
              </Badge>
            ))}
          </div>
        </Card>
      </motion.div>

      {/* Links & Resources */}
      <motion.div variants={itemVariants}>
        <Card variant="gradient">
          <h2 className="text-lg font-semibold text-white mb-4">Resources & Links</h2>
          <div className="space-y-3">
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 text-blue-400 hover:text-blue-300 transition-colors"
            >
              <Github className="w-5 h-5" />
              View on GitHub
            </a>
            <p className="text-sm text-slate-400">
              This project demonstrates advanced rate limiting patterns, atomic operations in distributed systems,
              and production-grade full-stack development.
            </p>
          </div>
        </Card>
      </motion.div>

      {/* Footer */}
      <motion.div variants={itemVariants} className="text-center py-8 border-t border-slate-800">
        <p className="text-sm text-slate-400">
          © 2024 RateForge. Built with ❤️ for rate limiting excellence.
        </p>
      </motion.div>
    </motion.div>
  );
};

export default About;
