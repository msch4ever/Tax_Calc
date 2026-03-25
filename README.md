# 🧮 Czech Stock Tax Calculator

A **local, privacy-focused** web application for calculating Czech tax liability on RSU (Restricted Stock Units) and ESPP (Employee Stock Purchase Plan) compensation from **E-Trade** and **Fidelity NetBenefits** exports.

**🔒 Privacy First:** All data is processed locally on your machine. No uploads to external servers, no storage, no tracking.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Multi--arch-blue.svg)](https://hub.docker.com/r/constantinelos/stock-tax-calculator)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [Philosophy](#-philosophy)
- [Features](#-features)
- [Roadmap](#-roadmap)
- [Quick Start](#-quick-start)
- [How It Works](#-how-it-works)
- [Architecture](#-architecture)
- [Technologies](#-technologies)
- [Project Structure](#-project-structure)
- [Development](#-development)
- [Contributing](#-contributing)

## 🎯 Philosophy

This is a **personal, local-first tool** designed with privacy and simplicity in mind:

### Core Principles
- **🔒 Privacy First** - All data processing happens locally on your machine
- **📁 No Storage** - No databases, no file storage, no data persistence
- **🚫 No Cloud** - No deployment to external servers, no API endpoints
- **⚡ One-Shot Calculator** - Upload files, get results, done
- **🎯 Single Purpose** - Calculate Czech taxes for stock compensation, nothing more

### What This Tool Is NOT
- ❌ Not a cloud service or SaaS platform
- ❌ Not a data storage or historical tracking system
- ❌ Not a mobile app or responsive web service
- ❌ Not an API for programmatic access
- ❌ Not a production-grade enterprise application

### Why Local?
You control what data you upload. Your financial information never leaves your computer. Run it locally via Docker or Gradle, process your E-Trade or Fidelity exports, and get your tax calculations - all without sending sensitive data anywhere.

## ✨ Features

### Supported Brokers
- ✅ **E-Trade** - Full support (RSU + ESPP) via Excel exports
- ✅ **Fidelity NetBenefits** - RSU support via PDF "Custom Transaction Summary"

### Tax Calculations
- ✅ **RSU Vesting Income** - Employment income from vested shares
- ✅ **ESPP Purchase Discount** - 15% discount benefit as employment income (E-Trade)
- ✅ **Capital Gains/Losses** - Profit/loss from selling shares (RSU & ESPP)
- ✅ **3-Year Holding Exemption** - Automatic detection of tax-exempt sales
- ✅ **CZK 100k Exemption** - Total proceeds threshold check

### Exchange Rates
- ✅ **Historical GFŘ Rates** - Hardcoded official yearly rates (2019-2025)
- ✅ **CNB Daily Rates** - Real-time daily rates from Czech National Bank API
- ✅ **Multi-Year Support** - Correctly applies different rates for different years
- ✅ **Rate Comparison** - Shows both daily and yearly calculations side-by-side
- ✅ **Tax Optimization** - Recommends which rate method saves more money

### Internationalization (i18n)
- ✅ **Multi-Language Support** - English (EN), Czech (CZ), Russian (RU), Ukrainian (UA)
- ✅ **URL-Based Switching** - Change language dynamically via toggle or `?lang=XX`

### User Experience
- ✅ **Broker Selection** - Landing page to choose between E-Trade and Fidelity
- ✅ **Excel + PDF Support** - E-Trade .xlsx files and Fidelity PDF reports
- ✅ **Dark/Light Mode** - Theme toggle with persistent preference
- ✅ **Detailed Reports** - Per-transaction breakdown with totals and recommendations
- ✅ **Tax Guide** - Built-in Czech tax rules documentation
- ✅ **Fun Savings** - See what your tax savings can buy (lunches, trips, apartments)

## 🗺️ Roadmap

### 🔴 Priority 1 — High Impact

| # | Feature | Why It Matters |
|---|---------|----------------|
| 1 | **📄 Tax-Attachable PDF Report** | Generate a professional PDF attachable to your DPFO so Finanční úřad sees exactly how you calculated |
| 2 | **🔢 POSP Employment Income Input** | Enter income & taxes from your employer's POSP form to get exact 15%/23% progressive tax calculation instead of guessing |
| 3 | **👁️ Rate Display Toggle** | Choose to show daily, yearly, or both rate columns — declutters tables when you've already decided which to use |

### 🟡 Priority 2 — Valuable Additions

| # | Feature | Why It Matters |
|---|---------|----------------|
| 4 | **💸 Transaction Costs Support** | Include broker fees as deductible expenses in capital gains — makes the calculation more accurate |
| 5 | **🏦 Fidelity ESPP Support** | Extend Fidelity from RSU-only to full ESPP parity — blocked until we have real-world Fidelity ESPP data |
| 6 | **📊 Improved Calculation Log** | Upgrade raw console output to a structured, color-coded, collapsible detail view — easier to audit and debug |

### 🟢 Priority 3 — Nice to Have

| # | Feature | Why It Matters |
|---|---------|----------------|
| 7 | **🧪 Test Data Generator** | Generate sample E-Trade/Fidelity files for testing — makes it easier for contributors to develop and test without real data |

### ❌ Out of Scope

These will **not** be implemented — they contradict the local-first, privacy-first philosophy:

- ❌ Historical Data Storage — no databases, no persistence
- ❌ Tax Form Pre-Fill — no integration with external systems
- ❌ API Endpoint — no programmatic access
- ❌ Mobile-Responsive UI — desktop tool
- ❌ Email Reports / Caching / Telemetry — not needed for local use

## 🚀 Quick Start

### Option 1: Docker (Recommended)

```bash
# Pull and run from Docker Hub
docker pull constantinelos/stock-tax-calculator:latest
docker run -p 8080:8080 constantinelos/stock-tax-calculator:latest

# Open http://localhost:8080
```

### Option 2: Local Development

```bash
# Prerequisites: Java 21, Gradle 8.10+
git clone <repository-url>
cd Tax_Calc

# Run the web application
./gradlew bootRun

# Open http://localhost:8080
```

### Option 3: Docker Compose

```bash
docker-compose up
# Open http://localhost:8080
```

## 📊 How It Works

### 1. Export from Your Broker

**E-Trade** — Export two Excel files:
- **Benefit History** (BenefitHistory.xlsx): `At Work → My Account → Benefit History → Download Expanded`
- **Gains & Losses** (GainsLosses.xlsx): `At Work → My Account → Gains & Losses → Pick Year → Download Expanded`

**Fidelity NetBenefits** — Export one PDF:
- **Custom Transaction Summary**: `Activity & Orders → History → Custom Transaction Summary → Download PDF`

### 2. Upload to Calculator

1. Open http://localhost:8080
2. Choose your broker (E-Trade or Fidelity)
3. Upload your files
4. Select tax year (e.g., 2025)
5. Click "Calculate"

### 3. Review Results

The calculator provides:
- **Exchange Rates Table** - Daily and yearly rates used
- **RSU Vesting Report** - Employment income from vested shares
- **ESPP Purchase Report** - Discount benefit as employment income
- **RSU Sell Report** - Capital gains/losses from RSU sales
- **ESPP Sell Report** - Capital gains/losses from ESPP sales
- **Tax Summary** - Total taxable amounts with rate comparison
- **Recommendation** - Which rate method (daily vs yearly) saves more CZK

## 🏗️ Architecture

### Design Principles

The application follows clean architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      Web Layer                              │
│  (TaxCalculatorController, Thymeleaf Templates)             │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│         (TaxCalculationService - orchestration)             │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼──────┐  ┌────────▼────────┐
│    Parsers     │  │ Calculators │  │ Exchange Rates  │
│                │  │             │  │                 │
│ - Excel→CSV    │  │ - Vesting   │  │ - CNB API       │
│ - CSV→Model    │  │ - ESPP      │  │ - GFŘ Rates     │
│ - PDF→Model    │  │ - Capital   │  │ - Combined      │
│   (Fidelity)   │  │   Gains     │  │                 │
└────────────────┘  └─────────────┘  └─────────────────┘
```




### Key Components

#### 1. Parsers
- **ExcelToCsvConverter** - Converts XLSX to CSV on-the-fly using Apache POI
- **RestrictedStockCsvParser** - Parses RSU vesting events from Benefit History
- **EsppPurchaseParser** - Parses ESPP purchase events from Benefit History
- **GainsLossesCsvParser** - Parses sell transactions from Gains & Losses
- **FidelityPdfParser** - Extracts RSU stock sale data from Fidelity PDF using Apache PDFBox

#### 2. Calculators
- **VestingCalculator** - Calculates RSU vesting income (employment income)
- **EsppPurchaseCalculator** - Calculates ESPP discount benefit (employment income)
- **RsuTaxCalculator** - Calculates capital gains/losses from stock sales
- **HoldingPeriodCalculator** - Determines 3-year exemption eligibility

#### 3. Exchange Rate Providers
- **CnbExchangeRateProvider** - Fetches daily rates from Czech National Bank API
- **CombinedExchangeRateProvider** - Combines CNB daily rates with hardcoded GFŘ yearly rates
  - Historical rates: 2019=22.93, 2020=23.14, 2021=21.72, 2022=23.41, 2023=22.14, 2024=23.28, 2025=21.84

#### 4. Web Layer
- **TaxCalculatorController** - Handles file uploads and form submissions
- **TaxCalculationService** - Orchestrates E-Trade parsing, calculation, and reporting
- **FidelityCalculationService** - Orchestrates Fidelity PDF parsing, vesting derivation, and reporting
- **Thymeleaf Templates** - Landing page, E-Trade/Fidelity forms, results pages, tax guide

## 🛠️ Technologies

### Core Stack
- **Java 21** - Modern Java with records, pattern matching, and text blocks
- **Spring Boot 3.2.3** - Web framework and dependency injection
- **Gradle 8.10.2** - Build automation and dependency management

### Libraries
- **Apache POI 5.2.5** - Excel file parsing (XLSX)
- **Apache PDFBox 3.0.4** - PDF parsing (Fidelity)
- **OpenCSV 5.9** - CSV parsing and writing
- **Thymeleaf** - Server-side HTML templating
- **Gson 2.11.0** - JSON parsing for CNB API responses

### Testing
- **JUnit 5.11** - Unit testing framework
- **AssertJ 3.26** - Fluent assertions
- **Spring Boot Test** - Integration testing support

### Infrastructure
- **Docker** - Multi-stage builds for optimized images
- **Docker Compose** - Local development orchestration
- **Eclipse Temurin 21** - OpenJDK distribution (Alpine Linux)

## 📁 Project Structure

```
Tax_Calc/
├── src/
│   ├── main/
│   │   ├── java/com/klos/automation/taxable/
│   │   │   ├── TaxCalculatorApplication.java      # Spring Boot entry point
│   │   │   ├── TaxableAmountCalculator.java       # CLI entry point (legacy)
│   │   │   ├── calculator/
│   │   │   │   ├── VestingCalculator.java         # RSU vesting income
│   │   │   │   ├── EsppPurchaseCalculator.java    # ESPP discount benefit
│   │   │   │   ├── RsuTaxCalculator.java          # Capital gains/losses
│   │   │   │   └── HoldingPeriodCalculator.java   # 3-year exemption logic
│   │   │   ├── exchange/
│   │   │   │   ├── ExchangeRateProvider.java      # Interface
│   │   │   │   ├── CnbExchangeRateProvider.java   # CNB API client
│   │   │   │   ├── CombinedExchangeRateProvider.java  # CNB + GFŘ rates
│   │   │   │   └── ManualExchangeRateProvider.java    # Testing only
│   │   │   ├── filter/
│   │   │   │   └── TransactionFilter.java         # RSU/ESPP/year filtering
│   │   │   ├── model/
│   │   │   │   ├── PlanType.java                  # RS, ESPP enum
│   │   │   │   ├── StockTransaction.java          # Sell transaction
│   │   │   │   ├── VestEvent.java                 # RSU vest event
│   │   │   │   ├── EsppPurchaseEvent.java         # ESPP purchase event
│   │   │   │   ├── TaxableTransaction.java        # Processed sell with CZK
│   │   │   │   ├── TaxableVestEvent.java          # Processed vest with CZK
│   │   │   │   ├── TaxableEsppPurchase.java       # Processed ESPP with CZK
│   │   │   │   ├── TaxReport.java                 # Capital gains report
│   │   │   │   ├── VestingReport.java             # Vesting income report
│   │   │   │   └── EsppPurchaseReport.java        # ESPP purchase report
│   │   │   ├── output/
│   │   │   │   ├── ReportFormatter.java           # Interface
│   │   │   │   └── ConsoleTableReportFormatter.java  # ASCII table output
│   │   │   ├── parser/
│   │   │   │   ├── ExcelToCsvConverter.java       # XLSX → CSV conversion
│   │   │   │   ├── RestrictedStockCsvParser.java  # RSU vest parser
│   │   │   │   ├── EsppPurchaseParser.java        # ESPP purchase parser
│   │   │   │   └── GainsLossesCsvParser.java      # Sell transaction parser
│   │   │   └── web/
│   │   │       ├── TaxCalculatorController.java   # Web controller
│   │   │       ├── TaxCalculationService.java     # Service layer
│   │   │       └── TaxCalculationResult.java      # Result record
│   │   └── resources/
│   │       ├── application.properties             # Spring Boot config
│   │       └── templates/
│   │           ├── index.html                     # Upload form
│   │           ├── results.html                   # Results page
│   │           └── tax-guide.html                 # Tax rules documentation
│   └── test/
│       └── java/com/klos/automation/taxable/
│           ├── calculator/
│           │   ├── HoldingPeriodCalculatorTest.java
│           │   ├── RsuTaxCalculatorTest.java
│           │   ├── VestingCalculatorTest.java
│           │   └── EsppPurchaseCalculatorTest.java
│           ├── exchange/
│           │   └── CombinedExchangeRateProviderTest.java
│           └── parser/
│               ├── GainsLossesCsvParserTest.java
│               ├── RestrictedStockCsvParserTest.java
│               └── EsppPurchaseParserTest.java
├── build.gradle                                   # Gradle build configuration
├── settings.gradle                                # Gradle settings
├── Dockerfile                                     # Multi-stage Docker build
├── docker-compose.yml                             # Docker Compose config
├── DOCKER_INSTRUCTIONS.md                         # Docker usage guide
└── README.md                                      # This file
```


## 💻 Development

### Prerequisites

- **Java 21** - [Download OpenJDK 21](https://adoptium.net/)
- **Gradle 8.10+** - Included via Gradle Wrapper (`./gradlew`)
- **Docker** (optional) - For containerized deployment

### Building the Project

```bash
# Clone the repository
git clone <repository-url>
cd Tax_Calc

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the web application
./gradlew bootRun
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests HoldingPeriodCalculatorTest

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Docker Development

```bash
# Build Docker image
docker build -t stock-tax-calculator .

# Run container
docker run -p 8080:8080 stock-tax-calculator

# Or use docker-compose
docker-compose up --build
```

### Multi-Architecture Build

```bash
# Build for both AMD64 and ARM64
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag constantinelos/stock-tax-calculator:latest \
  --push \
  .
```

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### Getting Started

1. **Request Collaborator Access** - Contact **klos** on Slack to be added as a collaborator
2. **Clone the repository** (`git clone <repository-url>`)
3. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
4. **Make your changes**
5. **Run tests** (`./gradlew test`)
6. **Commit your changes** (`git commit -m 'Add amazing feature'`)
7. **Push to the branch** (`git push origin feature/amazing-feature`)
8. **Open a Pull Request** and wait for review

**Note:** We don't accept contributions via forks. Please request collaborator access first.

### Code Style

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods focused and small
- Write tests for new features

### Testing Guidelines

- Write unit tests for calculators and parsers
- Use AssertJ for fluent assertions
- Test edge cases (empty files, invalid data, etc.)
- Aim for >80% code coverage

### Commit Messages

Use clear, descriptive commit messages:
```
Add ESPP purchase discount calculation

- Implement EsppPurchaseCalculator
- Add tests for 15% discount scenarios
- Update web UI to show ESPP income
```

## 🚧 Changelog — Recently Completed

- [x] **Optional Gains & Losses File** - New employees who haven't sold shares can now skip the G&L upload and still get vesting/ESPP reports
- [x] **Tax Summary Section** - New summary card with totals aligned to Czech tax form (Příloha č.2, ř.207/208) — added to both E-Trade and Fidelity results (thanks @JanVerner for the initial PR!)
- [x] **Fidelity NetBenefits Support** - RSU vesting income + capital gains from PDF exports
- [x] **Broker Selection Landing Page** - Choose between E-Trade and Fidelity at `/`
- [x] **Dark/Light Mode** - Theme toggle with persistent preference
- [x] **Localization** - English, Czech, Russian, and Ukrainian language support

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Czech National Bank (CNB)** - For providing free exchange rate API
- **General Financial Directorate (GFŘ)** - For official tax guidance
- **E-Trade** - For standardized export formats
- **Contributors** - Thank you to all who have contributed!

## 💰 Support

If this tool saved you time or money, consider buying me a coffee via my Revolut tag!

**Revolut:** [@los](https://revolut.me/los)

---

**Built with ❤️ for Czech tax season**

*Disclaimer: This tool is for informational purposes only. Always consult with a tax professional for official tax advice.*
