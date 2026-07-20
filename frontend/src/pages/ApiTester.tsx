import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/Tabs';
import { Send, Copy, Download } from 'lucide-react';
import { RateLimitService, RateLimitRequest } from '../api/services/RateLimitService';
import { ALGORITHMS, TIERS, COLORS } from '../config/api';
import { formatting } from '../utils/formatting';
import clsx from 'clsx';

interface ApiResponse {
  status: number;
  data: any;
  headers: any;
  timestamp: number;
  latency: number;
}

const ApiTester: React.FC = () => {
  const [isAnimating, setIsAnimating] = React.useState(false);
  const [selectedAlgorithm, setSelectedAlgorithm] = React.useState<keyof typeof ALGORITHMS>('TOKEN_BUCKET');
  const [selectedTier, setSelectedTier] = React.useState<keyof typeof TIERS>('PRO');
  const [customMessage, setCustomMessage] = React.useState('Test request');
  const [response, setResponse] = React.useState<ApiResponse | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    setIsAnimating(true);
  }, []);

  const executeRequest = async () => {
    setLoading(true);
    setError(null);
    const startTime = Date.now();

    try {
      const result = await RateLimitService.makeRequest({
        algorithm: selectedAlgorithm,
        tier: selectedTier,
        message: customMessage,
      });

      const latency = Date.now() - startTime;

      setResponse({
        status: result.status,
        data: result.data,
        headers: result.headers,
        timestamp: result.timestamp,
        latency,
      });
    } catch (err: any) {
      setError(err.message || 'Request failed');
      setResponse(null);
    } finally {
      setLoading(false);
    }
  };

  const copyResponse = () => {
    if (response) {
      navigator.clipboard.writeText(JSON.stringify(response.data, null, 2));
    }
  };

  const downloadResponse = () => {
    if (response) {
      const dataStr = JSON.stringify(response, null, 2);
      const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
      const exportFileDefaultName = `response-${Date.now()}.json`;

      const linkElement = document.createElement('a');
      linkElement.setAttribute('href', dataUri);
      linkElement.setAttribute('download', exportFileDefaultName);
      linkElement.click();
    }
  };

  return (
    <motion.div
      className="p-8 space-y-8"
      initial={{ opacity: 0 }}
      animate={isAnimating ? { opacity: 1 } : {}}
      transition={{ duration: 0.5 }}
    >
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold text-white mb-2">API Tester</h1>
        <p className="text-slate-400">Test rate limiting endpoints and view responses</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Request Configuration */}
        <motion.div
          className="lg:col-span-1"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.1 }}
        >
          <Card variant="gradient">
            <h2 className="text-lg font-semibold text-white mb-6">Configuration</h2>

            {/* Algorithm Selection */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-slate-300 mb-3">Algorithm</label>
              <div className="space-y-2">
                {Object.entries(ALGORITHMS).map(([key]) => (
                  <button
                    key={key}
                    onClick={() => setSelectedAlgorithm(key as keyof typeof ALGORITHMS)}
                    className={clsx(
                      'w-full px-4 py-2 rounded-lg text-left text-sm font-medium transition-all duration-200',
                      selectedAlgorithm === key
                        ? 'bg-blue-600 text-white shadow-lg'
                        : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                    )}
                  >
                    {key === 'TOKEN_BUCKET'
                      ? '🪣 Token Bucket'
                      : key === 'SLIDING_WINDOW'
                        ? '📊 Sliding Window'
                        : '💧 Leaky Bucket'}
                  </button>
                ))}
              </div>
            </div>

            {/* Tier Selection */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-slate-300 mb-3">Tier</label>
              <div className="space-y-2">
                {Object.entries(TIERS).map(([key, tier]) => (
                  <button
                    key={key}
                    onClick={() => setSelectedTier(key as keyof typeof TIERS)}
                    className={clsx(
                      'w-full px-4 py-2 rounded-lg text-left text-sm font-medium transition-all duration-200',
                      selectedTier === key
                        ? 'bg-purple-600 text-white shadow-lg'
                        : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                    )}
                  >
                    <div>{tier.name}</div>
                    <div className="text-xs text-slate-400">{tier.limit}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Custom Message */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-slate-300 mb-2">Message (Optional)</label>
              <textarea
                value={customMessage}
                onChange={(e) => setCustomMessage(e.target.value)}
                className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                rows={3}
                placeholder="Custom request message..."
              />
            </div>

            {/* Execute Button */}
            <Button onClick={executeRequest} isLoading={loading} className="w-full" size="lg">
              <Send className="w-4 h-4" />
              Execute Request
            </Button>
          </Card>
        </motion.div>

        {/* Response Display */}
        <motion.div
          className="lg:col-span-2"
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.2 }}
        >
          <Card variant="gradient">
            {error ? (
              <div className="text-center py-12">
                <Badge variant="error" className="mb-4">
                  Error
                </Badge>
                <p className="text-sm text-slate-400">{error}</p>
              </div>
            ) : response ? (
              <>
                {/* Response Header */}
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h2 className="text-lg font-semibold text-white mb-2">Response</h2>
                    <div className="flex items-center gap-4">
                      <Badge variant={response.status === 200 ? 'success' : 'error'}>
                        {response.status} {response.status === 200 ? 'OK' : 'Error'}
                      </Badge>
                      <span className="text-sm text-slate-400">{response.latency}ms</span>
                      <span className="text-sm text-slate-400">{formatting.formatTime(response.timestamp)}</span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button onClick={copyResponse} variant="outline" size="sm">
                      <Copy className="w-4 h-4" />
                    </Button>
                    <Button onClick={downloadResponse} variant="outline" size="sm">
                      <Download className="w-4 h-4" />
                    </Button>
                  </div>
                </div>

                {/* Response Tabs */}
                <Tabs defaultValue="body" className="space-y-4">
                  <TabsList className="bg-slate-800 border border-slate-700 rounded-lg p-1 w-full">
                    <TabsTrigger value="body" className="flex-1">
                      Body
                    </TabsTrigger>
                    <TabsTrigger value="headers" className="flex-1">
                      Headers
                    </TabsTrigger>
                    <TabsTrigger value="rateLimitInfo" className="flex-1">
                      Rate Limit
                    </TabsTrigger>
                  </TabsList>

                  {/* Body */}
                  <TabsContent value="body">
                    <div className="bg-slate-800 border border-slate-700 rounded-lg p-4 max-h-96 overflow-y-auto font-mono text-xs text-slate-300">
                      <pre>{JSON.stringify(response.data, null, 2)}</pre>
                    </div>
                  </TabsContent>

                  {/* Headers */}
                  <TabsContent value="headers">
                    <div className="space-y-2 max-h-96 overflow-y-auto">
                      {Object.entries(response.headers).map(([key, value]) => (
                        <div key={key} className="bg-slate-800 border border-slate-700 rounded-lg p-3">
                          <div className="text-xs font-mono text-slate-400">{key}</div>
                          <div className="text-sm text-slate-200">{String(value)}</div>
                        </div>
                      ))}
                    </div>
                  </TabsContent>

                  {/* Rate Limit Info */}
                  <TabsContent value="rateLimitInfo">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="bg-slate-800 border border-slate-700 rounded-lg p-4">
                        <p className="text-slate-400 text-sm mb-1">Limit</p>
                        <p className="text-2xl font-bold text-white">
                          {response.headers['x-ratelimit-limit'] || 'N/A'}
                        </p>
                      </div>
                      <div className="bg-slate-800 border border-slate-700 rounded-lg p-4">
                        <p className="text-slate-400 text-sm mb-1">Remaining</p>
                        <p className="text-2xl font-bold text-emerald-400">
                          {response.headers['x-ratelimit-remaining'] || 'N/A'}
                        </p>
                      </div>
                      <div className="bg-slate-800 border border-slate-700 rounded-lg p-4">
                        <p className="text-slate-400 text-sm mb-1">Reset</p>
                        <p className="text-sm text-slate-200">
                          {response.headers['x-ratelimit-reset']
                            ? new Date(parseInt(response.headers['x-ratelimit-reset'])).toLocaleTimeString()
                            : 'N/A'}
                        </p>
                      </div>
                      <div className="bg-slate-800 border border-slate-700 rounded-lg p-4">
                        <p className="text-slate-400 text-sm mb-1">Retry After</p>
                        <p className="text-sm text-slate-200">
                          {response.headers['retry-after']
                            ? `${response.headers['retry-after']}s`
                            : 'N/A'}
                        </p>
                      </div>
                    </div>
                  </TabsContent>
                </Tabs>
              </>
            ) : (
              <div className="text-center py-12">
                <p className="text-slate-400">Execute a request to see the response</p>
              </div>
            )}
          </Card>
        </motion.div>
      </div>

      {/* Request Information */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <Card variant="glass">
          <h3 className="text-lg font-semibold text-white mb-4">Request Information</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <p className="text-slate-400 text-sm mb-2">Algorithm</p>
              <Badge variant="info">{selectedAlgorithm}</Badge>
            </div>
            <div>
              <p className="text-slate-400 text-sm mb-2">Tier</p>
              <Badge variant="info">{selectedTier}</Badge>
            </div>
            <div>
              <p className="text-slate-400 text-sm mb-2">API Key</p>
              <Badge variant="info">{TIERS[selectedTier].apiKey}</Badge>
            </div>
          </div>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default ApiTester;
