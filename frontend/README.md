# RateForge Frontend

A production-grade React dashboard for the RateForge rate limiting service. Built with modern web technologies and designed for real-time monitoring and testing of rate limiting algorithms.

## 🎯 Features

- **Real-time Dashboard**: Live analytics with TanStack Query polling
- **Algorithm Simulators**: Interactive visual demonstrations of Token Bucket, Sliding Window, and Leaky Bucket
- **API Tester**: Test rate limiting endpoints directly from the browser
- **Metrics & Analytics**: Performance monitoring with Chart.js
- **Tier Configuration**: Beautiful display of rate limit tiers
- **Dark Theme**: Modern glassmorphism design with Framer Motion animations
- **Responsive**: Mobile-first, works on all screen sizes
- **TypeScript**: Full type safety throughout the codebase

## 🛠️ Tech Stack

- **React 19** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool (sub-second HMR)
- **TanStack Query** - Data fetching & caching
- **Framer Motion** - Smooth animations
- **Chart.js** - Data visualization
- **Axios** - HTTP client
- **React Router** - Navigation
- **Tailwind CSS** - Styling
- **Lucide React** - Icons
- **Sonner** - Toast notifications

## 📦 Installation

### Prerequisites
- Node.js 18+ 
- npm or yarn

### Setup

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

## 🚀 Quick Start

1. **Start the development server**:
   ```bash
   npm run dev
   ```
   The frontend will be available at `http://localhost:5173`

2. **Connect to backend**:
   - By default, connects to `http://localhost:3000`
   - Configure in Settings → Backend URL
   - Or set `VITE_API_URL` environment variable

3. **View the dashboard**:
   - Navigate to `http://localhost:5173`
   - Explore different sections: Dashboard, Algorithms, API Tester, etc.

## 📁 Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   ├── axiosClient.ts           # Axios configuration with interceptors
│   │   └── services/                # API service layer
│   │       ├── HealthService.ts     # Health checks
│   │       └── RateLimitService.ts  # Rate limit endpoints
│   ├── components/
│   │   ├── common/                  # Reusable components
│   │   │   ├── Card.tsx
│   │   │   ├── Badge.tsx
│   │   │   ├── Button.tsx
│   │   │   └── StatusIndicator.tsx
│   │   ├── layout/                  # Layout components
│   │   │   ├── Sidebar.tsx          # Navigation sidebar
│   │   │   └── Navbar.tsx           # Top navigation bar
│   │   └── ui/                      # Custom UI components
│   │       └── Tabs.tsx
│   ├── config/
│   │   └── api.ts                   # API endpoints & constants
│   ├── hooks/
│   │   ├── useSettings.ts           # Settings management
│   │   └── useBackendStatus.ts      # Backend health checks
│   ├── pages/
│   │   ├── Dashboard.tsx            # Main dashboard
│   │   ├── Algorithms.tsx           # Algorithm simulators
│   │   ├── ApiTester.tsx            # API testing interface
│   │   ├── Metrics.tsx              # Performance metrics
│   │   ├── TierLimits.tsx           # Tier configuration display
│   │   ├── Settings.tsx             # Application settings
│   │   └── About.tsx                # Project information
│   ├── utils/
│   │   ├── formatting.ts            # Format utilities
│   │   └── localStorage.ts          # LocalStorage management
│   ├── App.tsx                      # Main app component
│   ├── App.css                      # Global styles
│   └── main.jsx                     # Entry point
├── package.json
├── vite.config.js
├── tailwind.config.js
└── README.md
```

## 🎨 Design Theme

- **Color Scheme**: Slate (primary), Blue (accent), Purple (secondary), Emerald (success), Red (danger), Orange (warning)
- **Style**: Glassmorphism with backdrop blur, soft shadows, and rounded corners
- **Animations**: Smooth Framer Motion transitions
- **Typography**: System fonts for optimal performance

## 🔌 API Integration

### Configuration

Edit `src/config/api.ts` to customize endpoints:

```typescript
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';

export const API_ENDPOINTS = {
  HEALTH: `${API_BASE_URL}/health`,
  // ... more endpoints
};
```

### Services

Services are located in `src/api/services/`:

- **HealthService**: Backend and Redis health checks
- **RateLimitService**: Test rate limiting endpoints

Example usage:
```typescript
import { RateLimitService } from '@/api/services/RateLimitService';

const response = await RateLimitService.makeRequest({
  algorithm: 'TOKEN_BUCKET',
  tier: 'PRO',
  message: 'Test request',
});
```

## ⚙️ Configuration

### Environment Variables

```env
VITE_API_URL=http://localhost:3000
```

### Settings

Access Settings page to configure:
- Backend URL
- Polling interval
- Theme (dark/light)
- Animation speed
- Live updates toggle

Settings are persisted in localStorage.

## 🎯 Pages Overview

### Dashboard
- Real-time metrics (total requests, allowed, blocked)
- Redis latency and response times
- Active clients count
- Charts: Requests per minute, allowed vs blocked, tier distribution
- Recent activity log

### Algorithms
- **Token Bucket**: Interactive bucket visualization with fill animation
- **Sliding Window**: Timeline view of requests in window
- **Leaky Bucket**: Queue visualization with drainage animation
- Each has configuration controls and live simulation

### API Tester
- Select algorithm, tier, and send custom requests
- View response status, headers, and body
- Rate limit headers display
- Copy/download response functionality

### Metrics
- Redis latency chart
- Error rate visualization
- CPU usage monitoring
- System health indicators

### Tier Limits
- Display all tier configurations (FREE, PRO, ENTERPRISE)
- Show features and limits for each tier
- Current usage display with progress bars
- Algorithm configuration details

### Settings
- Backend URL configuration
- Polling interval adjustment
- Theme toggle (dark/light)
- Animation speed control
- Live updates toggle
- Reset to defaults button

### About
- Project overview
- Algorithm explanations
- Architecture details
- Technology stack
- Feature highlights

## 🚀 Building for Production

```bash
# Build optimized bundle
npm run build

# Preview production build locally
npm run preview
```

The build output will be in the `dist/` directory, ready for deployment.

### Deployment Options

**Nginx**:
```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

**Vercel/Netlify**: Connect the repository and push to deploy automatically.

**Docker**:
```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 🔄 Data Fetching

Uses TanStack Query for:
- Automatic caching
- Automatic background refetching
- Stale-while-revalidate patterns
- Optimistic updates
- Retry logic

Example:
```typescript
const { data, isLoading, isError } = useQuery({
  queryKey: ['health'],
  queryFn: () => HealthService.checkHealth(),
  refetchInterval: 5000, // Poll every 5 seconds
});
```

## 🎬 Animations

Powered by Framer Motion for:
- Smooth page transitions
- Interactive element animations
- Staggered list animations
- Hover effects
- Gesture interactions

## 📱 Responsive Design

- Mobile-first approach
- Breakpoints: sm (640px), md (768px), lg (1024px), xl (1280px)
- Sidebar collapses on mobile
- Touch-friendly controls
- Optimized performance

## 🧪 Testing

Component testing with unit tests (to be added):
```bash
npm run test
```

## 🐛 Troubleshooting

### Backend not connecting
- Check `VITE_API_URL` environment variable
- Verify backend is running on port 3000
- Check CORS settings in backend

### Charts not showing
- Ensure Chart.js is properly installed
- Check console for JavaScript errors
- Verify data format matches expected structure

### Sidebar not working
- Clear browser cache
- Check if localStorage is available
- Verify JavaScript is enabled

## 📚 Additional Resources

- [React Documentation](https://react.dev)
- [Vite Guide](https://vitejs.dev)
- [TanStack Query Docs](https://tanstack.com/query)
- [Framer Motion](https://www.framer.com/motion/)
- [Tailwind CSS](https://tailwindcss.com)
- [Chart.js Documentation](https://www.chartjs.org)

## 🤝 Contributing

Contributions are welcome! Please:
1. Create a feature branch
2. Make your changes
3. Submit a pull request

## 📝 License

This project is part of RateForge - an educational rate limiting implementation.

## 📧 Support

For issues or questions:
1. Check the troubleshooting section
2. Review the code comments
3. Check backend logs
4. Open an issue on GitHub

---

**Built with ❤️ for rate limiting excellence**
