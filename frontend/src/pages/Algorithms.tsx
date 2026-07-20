import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/Tabs';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { Play, Pause, RotateCcw, Plus, Minus } from 'lucide-react';
import { COLORS } from '../config/api';
import clsx from 'clsx';

const Algorithms: React.FC = () => {
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    setIsAnimating(true);
  }, []);

  return (
    <motion.div
      className="p-8 space-y-8"
      initial={{ opacity: 0 }}
      animate={isAnimating ? { opacity: 1 } : {}}
      transition={{ duration: 0.5 }}
    >
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold text-white mb-2">Algorithm Simulators</h1>
        <p className="text-slate-400">Visual representation of rate limiting algorithms</p>
      </div>

      <Tabs defaultValue="token-bucket" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3 bg-slate-900 border border-slate-800 rounded-lg p-1">
          <TabsTrigger value="token-bucket" className="text-slate-300 data-[state=active]:text-white">
            Token Bucket
          </TabsTrigger>
          <TabsTrigger value="sliding-window" className="text-slate-300 data-[state=active]:text-white">
            Sliding Window
          </TabsTrigger>
          <TabsTrigger value="leaky-bucket" className="text-slate-300 data-[state=active]:text-white">
            Leaky Bucket
          </TabsTrigger>
        </TabsList>

        {/* Token Bucket */}
        <TabsContent value="token-bucket" className="space-y-6">
          <TokenBucketSimulator />
        </TabsContent>

        {/* Sliding Window */}
        <TabsContent value="sliding-window" className="space-y-6">
          <SlidingWindowSimulator />
        </TabsContent>

        {/* Leaky Bucket */}
        <TabsContent value="leaky-bucket" className="space-y-6">
          <LeakyBucketSimulator />
        </TabsContent>
      </Tabs>
    </motion.div>
  );
};

/**
 * Token Bucket Simulator
 */
const TokenBucketSimulator: React.FC = () => {
  const [tokens, setTokens] = useState(100);
  const [capacity, setCapacity] = useState(100);
  const [refillRate, setRefillRate] = useState(10);
  const [isRunning, setIsRunning] = useState(false);
  const [history, setHistory] = useState<{ type: 'added' | 'consumed'; count: number; time: number }[]>([]);

  useEffect(() => {
    if (!isRunning) return;

    const interval = setInterval(() => {
      setTokens((prev) => {
        const refilled = Math.min(prev + refillRate, capacity);
        if (refilled > prev) {
          setHistory((h) => [...h.slice(-20), { type: 'added', count: refillRate, time: Date.now() }]);
        }
        return refilled;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isRunning, refillRate, capacity]);

  const consumeToken = () => {
    if (tokens > 0) {
      setTokens((prev) => prev - 1);
      setHistory((h) => [...h.slice(-20), { type: 'consumed', count: 1, time: Date.now() }]);
    }
  };

  return (
    <motion.div
      className="grid grid-cols-1 lg:grid-cols-2 gap-6"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      {/* Visualization */}
      <Card variant="gradient">
        <h2 className="text-lg font-semibold text-white mb-6">Bucket Visualization</h2>

        {/* Bucket */}
        <div className="relative h-64 bg-slate-800 rounded-xl border border-slate-700 mb-6 p-4 flex items-end justify-center">
          <div className="relative w-32 h-48 bg-slate-700 rounded-b-2xl border-4 border-slate-600 flex items-end justify-center">
            {/* Tokens inside bucket */}
            <motion.div
              className={clsx(
                'absolute bottom-0 left-0 right-0 rounded-b-xl transition-all duration-300',
                tokens > capacity * 0.75
                  ? 'bg-emerald-500'
                  : tokens > capacity * 0.5
                    ? 'bg-blue-500'
                    : tokens > capacity * 0.25
                      ? 'bg-orange-500'
                      : 'bg-red-500'
              )}
              style={{ height: `${(tokens / capacity) * 100}%` }}
            />

            {/* Token count */}
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center">
              <div className="text-2xl font-bold text-white">{Math.round(tokens)}</div>
              <div className="text-xs text-slate-300">/{capacity}</div>
            </div>
          </div>

          {/* Refill indicator */}
          {isRunning && (
            <motion.div
              className="absolute top-4 right-4 flex items-center gap-2 text-sm text-emerald-400"
              animate={{ opacity: [0.5, 1, 0.5] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              <div className="w-2 h-2 bg-emerald-400 rounded-full" />
              Refilling
            </motion.div>
          )}
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Current</p>
            <p className="text-2xl font-bold text-white">{Math.round(tokens)}</p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Capacity</p>
            <p className="text-2xl font-bold text-white">{capacity}</p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Rate</p>
            <p className="text-2xl font-bold text-white">{refillRate}/s</p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex gap-2">
          <Button
            onClick={() => setIsRunning(!isRunning)}
            variant={isRunning ? 'secondary' : 'primary'}
            size="sm"
          >
            {isRunning ? (
              <>
                <Pause className="w-4 h-4" /> Pause
              </>
            ) : (
              <>
                <Play className="w-4 h-4" /> Start
              </>
            )}
          </Button>
          <Button onClick={() => { setTokens(capacity); setHistory([]); }} variant="outline" size="sm">
            <RotateCcw className="w-4 h-4" /> Reset
          </Button>
          <Button onClick={consumeToken} variant="ghost" size="sm" disabled={tokens === 0}>
            <Minus className="w-4 h-4" /> Consume
          </Button>
        </div>
      </Card>

      {/* Controls */}
      <Card variant="gradient">
        <h2 className="text-lg font-semibold text-white mb-6">Configuration</h2>

        {/* Capacity Slider */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <label className="text-sm font-medium text-slate-300">Capacity</label>
            <span className="text-lg font-bold text-blue-400">{capacity}</span>
          </div>
          <input
            type="range"
            min="10"
            max="500"
            value={capacity}
            onChange={(e) => {
              const newCapacity = parseInt(e.target.value);
              setCapacity(newCapacity);
              setTokens(Math.min(tokens, newCapacity));
            }}
            className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-500"
          />
          <div className="flex justify-between text-xs text-slate-500 mt-2">
            <span>10</span>
            <span>500</span>
          </div>
        </div>

        {/* Refill Rate Slider */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-3">
            <label className="text-sm font-medium text-slate-300">Refill Rate (tokens/sec)</label>
            <span className="text-lg font-bold text-emerald-400">{refillRate}</span>
          </div>
          <input
            type="range"
            min="1"
            max="50"
            value={refillRate}
            onChange={(e) => setRefillRate(parseInt(e.target.value))}
            className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-emerald-500"
          />
          <div className="flex justify-between text-xs text-slate-500 mt-2">
            <span>1</span>
            <span>50</span>
          </div>
        </div>

        {/* History */}
        <div>
          <h3 className="text-sm font-medium text-slate-300 mb-3">Recent Activity</h3>
          <div className="space-y-2 max-h-40 overflow-y-auto">
            {history.length === 0 ? (
              <p className="text-sm text-slate-500">No activity yet</p>
            ) : (
              history.map((event, idx) => (
                <div key={idx} className="flex items-center gap-2 text-sm">
                  <Badge variant={event.type === 'added' ? 'success' : 'error'}>
                    {event.type === 'added' ? '+' : '-'}{event.count} tokens
                  </Badge>
                </div>
              ))
            )}
          </div>
        </div>
      </Card>
    </motion.div>
  );
};

/**
 * Sliding Window Simulator
 */
const SlidingWindowSimulator: React.FC = () => {
  const [requests, setRequests] = useState<number[]>([]);
  const [maxRequests, setMaxRequests] = useState(10);
  const [windowSize, setWindowSize] = useState(60000);
  const [isRunning, setIsRunning] = useState(false);

  const currentCount = requests.filter((time) => Date.now() - time < windowSize).length;
  const canRequest = currentCount < maxRequests;

  const addRequest = () => {
    if (canRequest) {
      setRequests((prev) => [...prev, Date.now()]);
    }
  };

  return (
    <motion.div
      className="grid grid-cols-1 lg:grid-cols-2 gap-6"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      {/* Timeline Visualization */}
      <Card variant="gradient">
        <h2 className="text-lg font-semibold text-white mb-6">Window Timeline</h2>

        {/* Timeline */}
        <div className="relative h-48 bg-slate-800 rounded-xl border border-slate-700 p-4 mb-6">
          {/* Window background */}
          <div className="absolute inset-4 bg-gradient-to-r from-slate-700/30 via-blue-500/10 to-slate-700/30 rounded-lg" />

          {/* Current time marker */}
          <div className="absolute inset-y-4 right-4 w-0.5 bg-red-500" />

          {/* Requests */}
          <div className="relative h-full flex items-center">
            {requests.map((time, idx) => {
              const isInWindow = Date.now() - time < windowSize;
              const position = ((Date.now() - time) / windowSize) * 100;

              return (
                <motion.div
                  key={idx}
                  className={clsx(
                    'absolute w-3 h-3 rounded-full cursor-pointer',
                    isInWindow ? 'bg-emerald-500 shadow-lg shadow-emerald-500/50' : 'bg-slate-600 opacity-30'
                  )}
                  style={{ left: `${Math.max(5, Math.min(95, position))}%` }}
                  animate={isInWindow ? { scale: [1, 1.2, 1] } : {}}
                  transition={{ duration: 1, repeat: Infinity }}
                />
              );
            })}
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 gap-4 mb-6">
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Requests in window</p>
            <p className="text-3xl font-bold text-white">{currentCount}</p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Limit</p>
            <p className="text-3xl font-bold text-white">{maxRequests}</p>
          </div>
        </div>

        {/* Status */}
        <Badge variant={canRequest ? 'success' : 'error'}>
          {canRequest ? '✓ Can process' : '✗ Rate limited'}
        </Badge>
      </Card>

      {/* Controls */}
      <Card variant="gradient">
        <h2 className="text-lg font-semibold text-white mb-6">Configuration</h2>

        {/* Max Requests */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <label className="text-sm font-medium text-slate-300">Max Requests</label>
            <span className="text-lg font-bold text-blue-400">{maxRequests}</span>
          </div>
          <input
            type="range"
            min="1"
            max="50"
            value={maxRequests}
            onChange={(e) => setMaxRequests(parseInt(e.target.value))}
            className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-500"
          />
        </div>

        {/* Window Size */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-3">
            <label className="text-sm font-medium text-slate-300">Window Size (ms)</label>
            <span className="text-lg font-bold text-emerald-400">{windowSize}</span>
          </div>
          <input
            type="range"
            min="5000"
            max="120000"
            step="5000"
            value={windowSize}
            onChange={(e) => setWindowSize(parseInt(e.target.value))}
            className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-emerald-500"
          />
        </div>

        {/* Request Buttons */}
        <div className="space-y-2">
          <Button onClick={addRequest} disabled={!canRequest} variant="primary" size="sm" className="w-full">
            <Plus className="w-4 h-4" /> Add Request
          </Button>
          <Button onClick={() => setRequests([])} variant="outline" size="sm" className="w-full">
            <RotateCcw className="w-4 h-4" /> Clear
          </Button>
        </div>

        {/* Request History */}
        <div className="mt-6">
          <h3 className="text-sm font-medium text-slate-300 mb-3">All Requests</h3>
          <div className="space-y-2 max-h-32 overflow-y-auto">
            {requests.map((time, idx) => {
              const isInWindow = Date.now() - time < windowSize;
              return (
                <div key={idx} className="text-xs text-slate-400">
                  Request {idx + 1}: {isInWindow ? '✓ In window' : '✗ Expired'}
                </div>
              );
            })}
          </div>
        </div>
      </Card>
    </motion.div>
  );
};

/**
 * Leaky Bucket Simulator
 */
const LeakyBucketSimulator: React.FC = () => {
  const [queue, setQueue] = useState(0);
  const [capacity, setCapacity] = useState(10);
  const [drainRate, setDrainRate] = useState(2);
  const [isRunning, setIsRunning] = useState(false);

  useEffect(() => {
    if (!isRunning) return;

    const interval = setInterval(() => {
      setQueue((prev) => Math.max(0, prev - drainRate));
    }, 1000);

    return () => clearInterval(interval);
  }, [isRunning, drainRate]);

  const addRequest = () => {
    if (queue < capacity) {
      setQueue((prev) => prev + 1);
    }
  };

  return (
    <motion.div
      className="grid grid-cols-1 lg:grid-cols-2 gap-6"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      {/* Bucket Visualization */}
      <Card variant="gradient">
        <h2 className="text-lg font-semibold text-white mb-6">Leaky Bucket</h2>

        <div className="relative h-64 bg-slate-800 rounded-xl border border-slate-700 mb-6 p-4 flex items-end justify-center">
          {/* Bucket */}
          <div className="relative w-32 h-40 bg-slate-700 rounded-t-2xl border-4 border-slate-600 flex items-end justify-center">
            {/* Water inside */}
            <motion.div
              className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-blue-500 to-blue-400 rounded-t-xl"
              style={{ height: `${(queue / capacity) * 100}%` }}
            />
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center z-10">
              <div className="text-2xl font-bold text-white">{queue}</div>
              <div className="text-xs text-slate-300">/{capacity}</div>
            </div>
          </div>

          {/* Leak */}
          <div className="absolute bottom-0 left-1/2 -translate-x-1/2 translate-y-full">
            {isRunning && (
              <motion.div
                className="flex flex-col items-center"
                animate={{ y: [0, 8, 0] }}
                transition={{ duration: 1, repeat: Infinity }}
              >
                <div className="text-2xl">💧</div>
              </motion.div>
            )}
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">In Queue</p>
            <p className="text-2xl font-bold text-white">{queue}</p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Capacity</p>
            <p className="text-2xl font-bold text-white">{capacity}</p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Drain</p>
            <p className="text-2xl font-bold text-white">{drainRate}/s</p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex gap-2">
          <Button
            onClick={() => setIsRunning(!isRunning)}
            variant={isRunning ? 'secondary' : 'primary'}
            size="sm"
          >
            {isRunning ? (
              <>
                <Pause className="w-4 h-4" /> Pause
              </>
            ) : (
              <>
                <Play className="w-4 h-4" /> Start
              </>
            )}
          </Button>
          <Button onClick={() => { setQueue(0); }} variant="outline" size="sm">
            <RotateCcw className="w-4 h-4" /> Reset
          </Button>
        </div>
      </Card>

      {/* Configuration */}
      <Card variant="gradient">
        <h2 className="text-lg font-semibold text-white mb-6">Configuration</h2>

        {/* Capacity */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <label className="text-sm font-medium text-slate-300">Capacity</label>
            <span className="text-lg font-bold text-blue-400">{capacity}</span>
          </div>
          <input
            type="range"
            min="5"
            max="50"
            value={capacity}
            onChange={(e) => {
              const newCapacity = parseInt(e.target.value);
              setCapacity(newCapacity);
              setQueue(Math.min(queue, newCapacity));
            }}
            className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-500"
          />
        </div>

        {/* Drain Rate */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-3">
            <label className="text-sm font-medium text-slate-300">Drain Rate (items/sec)</label>
            <span className="text-lg font-bold text-emerald-400">{drainRate}</span>
          </div>
          <input
            type="range"
            min="0.5"
            max="10"
            step="0.5"
            value={drainRate}
            onChange={(e) => setDrainRate(parseFloat(e.target.value))}
            className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-emerald-500"
          />
        </div>

        {/* Actions */}
        <Button
          onClick={addRequest}
          disabled={queue >= capacity}
          variant="primary"
          size="sm"
          className="w-full"
        >
          <Plus className="w-4 h-4" /> Add Request
        </Button>

        {/* Info */}
        <div className="mt-6 p-4 bg-slate-800 rounded-lg border border-slate-700">
          <p className="text-sm text-slate-300">
            <span className="font-medium text-blue-400">Leaky Bucket</span> smooths traffic by processing requests at a constant rate, regardless of input variation.
          </p>
        </div>
      </Card>
    </motion.div>
  );
};

export default Algorithms;
