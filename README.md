# 🧮 Czech Stock Tax Calculator

A **local, privacy-focused** web application for calculating Czech tax liability on RSU (Restricted Stock Units) and ESPP (Employee Stock Purchase Plan) compensation from E-Trade exports.

**🔒 Privacy First:** All data is processed locally on your machine. No uploads to external servers, no storage, no tracking.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Multi--arch-blue.svg)](https://hub.docker.com/r/constantinelos/stock-tax-calculator)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [Philosophy](#-philosophy)
- [Features](#-features)
- [Quick Start](#-quick-start)
- [How It Works](#-how-it-works)
- [Architecture](#-architecture)
- [Technologies](#-technologies)
- [Project Structure](#-project-structure)
- [Development](#-development)
- [Contributing](#-contributing)
- [Future Work](#-future-work)

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
You control what data you upload. Your financial information never leaves your computer. Run it locally via Docker or Gradle, process your E-Trade exports, and get your tax calculations - all without sending sensitive data anywhere.

## ✨ Features

### Tax Calculations
- ✅ **RSU Vesting Income** - Employment income from vested shares
- ✅ **ESPP Purchase Discount** - 15% discount benefit as employment income
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
- ✅ **Multi-Language Support** - Available in English (EN), Czech (CZ), Russian (RU), and Ukrainian (UK)
- ✅ **URL-Based Switching** - Change language dynamically using the toggle or `?lang=XX` URL parameters
- ✅ **Easy to Expand** - Easily add new languages via standard Spring Boot resource bundles (`messages_XX.properties`)

### User Experience
- ✅ **Web Interface** - Simple file upload form
- ✅ **Excel Support** - Direct upload of E-Trade .xlsx files
- ✅ **Multi-Year Data** - Handles unfiltered exports spanning multiple years
- ✅ **Detailed Reports** - Breakdown by transaction with totals
- ✅ **Tax Guide** - Built-in Czech tax rules documentation
- ✅ **Support Banners** - Revolut donation links (@los)

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

### 1. Export from E-Trade

**Benefit History** (BenefitHistory.xlsx):
```
At Work → My Account → Benefit History → Download Expanded
```
Downloads Excel with two sheets: "ESPP" and "Restricted Stock"

**Gains & Losses** (GainsLosses.xlsx):
```
At Work → My Account → Gains & Losses → Pick Year → Download Expanded
```
Contains both RSU and ESPP sell transactions

### 2. Upload to Calculator

1. Open http://localhost:8080
2. Upload both Excel files
3. Select tax year (e.g., 2025)
4. Choose plan types (RSU, ESPP, or both)
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
│                │  │ - Capital   │  │ - Combined      │
│                │  │   Gains     │  │                 │
└────────────────┘  └─────────────┘  └─────────────────┘
```




### Key Components

#### 1. Parsers
- **ExcelToCsvConverter** - Converts XLSX to CSV on-the-fly using Apache POI
- **RestrictedStockCsvParser** - Parses RSU vesting events from Benefit History
- **EsppPurchaseParser** - Parses ESPP purchase events from Benefit History
- **GainsLossesCsvParser** - Parses sell transactions from Gains & Losses

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
- **TaxCalculationService** - Orchestrates parsing, calculation, and reporting
- **Thymeleaf Templates** - index.html (upload form), results.html (reports), tax-guide.html (documentation)

## 🛠️ Technologies

### Core Stack
- **Java 21** - Modern Java with records, pattern matching, and text blocks
- **Spring Boot 3.2.3** - Web framework and dependency injection
- **Gradle 8.10.2** - Build automation and dependency management

### Libraries
- **Apache POI 5.2.5** - Excel file parsing (XLSX)
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

## 🚧 Future Work

### In Scope (Aligned with Local-First Philosophy)
- [ ] **PDF/Print-Friendly Reports** - Export results as PDF for tax filing
- [ ] **Transaction Costs Support** - Include broker fees in capital gains
- [ ] **Multi-Currency Support** - Handle EUR, GBP in addition to USD
- [ ] **Batch Processing** - Process multiple years at once
- [ ] **Test Data Generator** - Create sample Excel files for testing
- [ ] **Dark Mode** - UI theme toggle
- [x] **Localization** - English, Czech, Russian, and Ukrainian language support

### Out of Scope (Against Local-First Philosophy)
These features will **NOT** be implemented as they contradict the tool's privacy-first, local-only design:
- ❌ **Historical Data Storage** - No databases, no persistence
- ❌ **Tax Form Pre-Fill** - No integration with external systems
- ❌ **API Endpoint** - No programmatic access, local use only
- ❌ **Mobile-Responsive UI** - Desktop tool, not a mobile app
- ❌ **Email Reports** - No external communication
- ❌ **Caching** - Stateless, one-shot calculations
- ❌ **Rate Limiting** - Not needed for local use
- ❌ **Logging/Metrics** - No telemetry, no tracking
- ❌ **Health Checks** - Not a production service

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
