# 🧮 Czech Stock Tax Calculator - Docker Setup

Calculate your Czech taxes for E-Trade RSU and ESPP stock plans.

## Quick Start

### Option 1: Load Pre-built Image (Recommended)

If you received the `tax-calculator.tar` file:

```bash
# Load the Docker image
docker load -i tax-calculator.tar

# Run the container
docker run -d -p 8080:8080 --name tax-calc czech-stock-tax-calculator:latest

# Open in browser
open http://localhost:8080
```

### Option 2: Build from Source

```bash
# Clone or extract the source code, then:
docker-compose up -d

# Or build manually:
docker build -t czech-stock-tax-calculator:latest .
docker run -d -p 8080:8080 --name tax-calc czech-stock-tax-calculator:latest
```

## Usage

1. Open http://localhost:8080 in your browser
2. Upload your E-Trade files:
   - **BenefitHistory.xlsx** - Contains vesting and ESPP purchase data
   - **GainsLosses.xlsx** - Contains sell transaction data
3. Select tax year and plan types (RSU / ESPP)
4. Click "Calculate" to see your tax report

## Features

- ✅ RSU vesting income calculation
- ✅ RSU capital gains/losses
- ✅ ESPP purchase discount income (15% benefit)
- ✅ ESPP capital gains/losses
- ✅ CNB daily rates vs GFŘ yearly average comparison
- ✅ 3-year holding period exemption detection
- ✅ Rate recommendation to minimize taxes

## Stop the Container

```bash
docker stop tax-calc
docker rm tax-calc
```

## Troubleshooting

**Port already in use?**
```bash
docker run -d -p 9090:8080 --name tax-calc czech-stock-tax-calculator:latest
# Then open http://localhost:9090
```

**Check logs:**
```bash
docker logs tax-calc
```

---
Built with ❤️ for Czech tax season

