# Installation Instructions

## Prerequisites

- Node.js 18+ ([Download](https://nodejs.org/))
- npm 9+ (comes with Node.js) or yarn

## Quick Installation

### Step 1: Navigate to Frontend Directory
```bash
cd frontend
```

### Step 2: Install Dependencies
```bash
npm install
```

This will install all required packages listed in `package.json`:
- React 19
- TypeScript
- Vite
- TanStack Query
- Framer Motion
- Chart.js
- Tailwind CSS
- And more...

Expected time: 2-5 minutes depending on internet speed

### Step 3: Verify Installation
```bash
npm list --depth=0
```

You should see all dependencies installed without "UNMET DEPENDENCY" errors.

## Starting the Development Server

```bash
npm run dev
```

Output should show:
```
  VITE v8.1.1  ready in XXX ms

  ➜  Local:   http://localhost:5173/
  ➜  press h + enter to show help
```

Open your browser to `http://localhost:5173`

## Build for Production

```bash
npm run build
```

Output will be in `dist/` directory.

## Troubleshooting

### Missing Dependencies
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Port Already in Use
```bash
# Use different port
npm run dev -- --port 5174
```

### Node Version Issue
```bash
# Check your Node version
node --version

# Should be 18.0.0 or higher
# If not, download from https://nodejs.org/
```

### Permission Denied (Windows)
```bash
# Run PowerShell as Administrator
# Then run: npm install
```

## Next Steps

After installation:
1. ✅ Configure backend URL in Settings or `.env`
2. ✅ Explore the Dashboard
3. ✅ Test endpoints in API Tester
4. ✅ Review documentation in About page

---

**Questions?** Check `README.md` or `FRONTEND_SETUP.md` for more details.
