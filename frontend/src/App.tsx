import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { Sidebar } from './components/layout/Sidebar';
import { Navbar } from './components/layout/Navbar';
import Dashboard from './pages/Dashboard';
import Algorithms from './pages/Algorithms';
import ApiTester from './pages/ApiTester';
import Metrics from './pages/Metrics';
import TierLimits from './pages/TierLimits';
import Settings from './pages/Settings';
import About from './pages/About';
import './App.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 1,
    },
  },
});

function App() {
  const [sidebarOpen, setSidebarOpen] = useState(true);

  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <div className="flex h-screen bg-slate-950 text-slate-50">
          {/* Sidebar */}
          <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

          {/* Main Content */}
          <div className="flex-1 flex flex-col overflow-hidden">
            {/* Navbar */}
            <Navbar onMenuClick={() => setSidebarOpen(!sidebarOpen)} />

            {/* Page Content */}
            <main className="flex-1 overflow-y-auto">
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/algorithms" element={<Algorithms />} />
                <Route path="/api-tester" element={<ApiTester />} />
                <Route path="/metrics" element={<Metrics />} />
                <Route path="/tier-limits" element={<TierLimits />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="/about" element={<About />} />
              </Routes>
            </main>
          </div>
        </div>

        {/* Toast Notifications */}
        <Toaster
          position="top-right"
          theme="dark"
          toastOptions={{
            style: {
              background: '#1e293b',
              border: '1px solid #334155',
              color: '#f1f5f9',
            },
          }}
        />
      </Router>
    </QueryClientProvider>
  );
}

export default App;
