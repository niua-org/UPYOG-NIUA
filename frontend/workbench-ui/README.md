# Workbench UI

A React-based web application built on top of DIGIT UI Core for managing master data and localization in the DIGIT eGovernance platform.

## 📋 Table of Contents

- [About](#about)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Available Modules](#available-modules)
- [Development](#development)
- [Build & Deployment](#build--deployment)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## 📖 About

### DIGIT Platform

DIGIT (Digital Infrastructure for Governance, Impact & Transformation) is India's largest platform for governance services. It is a microservices-based API platform enabling quick rebundling of services as per specific needs.

Visit [DIGIT Core Documentation](https://core.digit.org/) for more details.

### Workbench UI

Workbench UI is an internal administrative tool designed for:

- **MDMS V2 Management**: Manage master data (MDMS V2 Service) used across DIGIT Services/Applications
- **Localization Management**: Manage localization data present in the system (Localization service)
- **Configuration Management**: Handle system-wide configurations and settings

## ✨ Features

- 🗂️ Master Data Management System (MDMS V2)
- 🌐 Multi-language Localization Support
- 👥 Role-Based Access Control (RBAC)
- 🎨 Modern UI with Tailwind CSS
- ⚡ Fast Development with Vite
- 🔄 State Management with React Query

## 🛠️ Tech Stack

**Frontend Framework:**
- [React 19](https://react.dev/) - UI Library
- [React Router DOM 6.28](https://reactrouter.com/) - Routing
- [Vite 6.4](https://vitejs.dev/) - Build Tool

**State & Form Management:**
- [React Query (TanStack Query) 5.0](https://tanstack.com/query/latest) - Server State Management
- [React Hook Form 7.51](https://www.react-hook-form.com/) - Form Handling

**Styling:**
- [Tailwind CSS](https://tailwindcss.com/) - Utility-first CSS Framework

**Internationalization:**
- [React i18next 14.0](https://react.i18next.com/) - Internationalization

**Build & Bundling:**
- [Webpack](https://webpack.js.org/) - Module Bundler (for packages)
- [Vite](https://vitejs.dev/) - Development Server & Build Tool

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js**: >= 22.x
- **Yarn**: Latest version (recommended) or npm
- **Git**: For version control

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/egovernments/DIGIT-Frontend.git
cd DIGIT-Frontend/frontend/workbench-ui
```

### 2. Install Dependencies

#### For Development (Micro UI Internals):

```bash
cd web/micro-ui-internals
yarn install
```

#### For Production Build:

```bash
cd web
yarn install
```

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the `web` directory with the following variables:

```env
# API Configuration
REACT_APP_PROXY_API=https://your-server-url.com
REACT_APP_GLOBAL=https://your-server-url.com
REACT_APP_PROXY_ASSETS=https://your-server-url.com

# User Type (EMPLOYEE or CITIZEN)
REACT_APP_USER_TYPE=EMPLOYEE

# Build Configuration
SKIP_PREFLIGHT_CHECK=true
```

### Sample Configuration Files

- [Sample .env file](https://github.com/egovernments/Digit-Core/blob/workbench/frontend/micro-ui/web/micro-ui-internals/example/.env-unifieddev)
- Location: `web/micro-ui-internals/example/.env`

## 🏃 Running the Application

### Development Mode

#### Option 1: Run from micro-ui-internals (For Package Development)

```bash
cd web/micro-ui-internals
yarn install
yarn start
```

#### Option 2: Run from web directory (For Application Development)

```bash
cd web
yarn install
yarn start
```

The application will start at `http://localhost:3000/workbench-ui`

### Production Build

```bash
cd web
yarn build
```

Build output will be in the `dist` directory.

### Preview Production Build

```bash
cd web
yarn preview
```

## 📁 Project Structure

```
workbench-ui/
├── web/
│   ├── micro-ui-internals/          # Core packages and modules
│   │   ├── packages/
│   │   │   ├── libraries/           # Shared libraries and services
│   │   │   ├── react-components/    # Reusable React components
│   │   │   ├── svg-components/      # SVG icon components
│   │   │   ├── css/                 # Tailwind CSS configuration
│   │   │   ├── config/              # Build configurations
│   │   │   └── modules/
│   │   │       ├── core/            # Core module
│   │   │       ├── workbench/       # Workbench module
│   │   │       └── utilities/       # Utility functions
│   │   ├── example/                 # Example configurations
│   │   ├── scripts/                 # Build and publish scripts
│   │   └── package.json
│   ├── src/
│   │   ├── Customisations/          # Custom configurations
│   │   ├── App.js                   # Main application component
│   │   ├── ComponentRegistry.js     # Component registration
│   │   ├── index.js                 # Application entry point
│   │   └── setupProxy.js            # Proxy configuration
│   ├── public/                      # Static assets
│   ├── docker/                      # Docker configuration
│   ├── .env                         # Environment variables
│   ├── vite.config.js               # Vite configuration
│   └── package.json
├── README.md
├── Jenkinsfile                      # CI/CD pipeline
└── package.json
```

## 📦 Available Modules

### 1. Core Module
- Authentication & Authorization
- User Management
- Common UI Components
- Routing & Navigation

### 2. Workbench Module
- MDMS V2 Data Management
- Schema Management
- Data Import/Export
- Version Control


### 4. Utilities Module
- Helper Functions
- Common Utilities
- Shared Services

## 💻 Development

### Package Development

To develop and test individual packages:

```bash
cd web/micro-ui-internals
yarn start
```

### Building Packages

```bash
cd web
yarn packages:build
```

### Publishing Packages

```bash
cd web
yarn packages:publish
```

### Build and Publish (Combined)

```bash
cd web
yarn packages:release
```

### Code Formatting

The project uses Prettier for code formatting:

```bash
cd web/micro-ui-internals
npx prettier --write "**/*.{js,jsx,json,css,md}"
```

### Workspaces

This project uses Yarn Workspaces for monorepo management:

- `@upyog/workbench-ui-libraries`
- `@upyog/workbench-ui-react-components`
- `@nudmcdgnpm/workbench-ui-svg-components`
- `@upyog/workbench-ui-module-core`
- `@upyog/workbench-ui-module-utilities`
- `@nudmcdgnpm/digit-ui-module-workbench`

## 🐳 Build & Deployment

### Docker Build

```bash
cd web/docker
docker build -t workbench-ui:latest .
```

### Docker Run

```bash
docker run -p 80:80 workbench-ui:latest
```

### CI/CD

The project includes a `Jenkinsfile` for automated CI/CD pipeline.

## 🤝 Contributing

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards

- Follow React best practices
- Use functional components and hooks
- Write clean, readable code
- Add comments for complex logic
- Ensure proper error handling
- Test your changes thoroughly

### Removed Features

- **Engagement Module**: Removed to focus on core workbench functionality (MDMS & Localization).
- **Payment Module**: Removed as workbench is an internal tool and doesn't require payment functionality. Role-based access control (RBAC) is handled at the backend level.

## 📄 License

[MIT License](https://choosealicense.com/licenses/mit/)

Copyright (c) 2024 eGovernments Foundation

## 👥 Authors

- [@jagankumar-egov](https://github.com/jagankumar-egov)

## 📚 Documentation

- [DIGIT UI Developer Guide](https://core.digit.org/guides/developer-guide/ui-developer-guide/digit-ui)
- [DIGIT Core Documentation](https://core.digit.org/)
- [API Documentation](https://core.digit.org/platform/core-services)

## 🆘 Support

For support and bug reports:

- Create an issue: [DIGIT Core Issues](https://github.com/egovernments/DIGIT-core/issues)
- Email: [support@egovernments.org](mailto:support@egovernments.org)
- Community Forum: [DIGIT Community](https://github.com/egovernments/DIGIT-core/discussions)

## 🔗 Related Links

- [DIGIT Platform](https://core.digit.org/)
- [eGovernments Foundation](https://egov.org.in/)
- [GitHub Repository](https://github.com/egovernments/DIGIT-Frontend)

---

**Note**: This is an internal administrative tool for DIGIT platform. For citizen-facing applications, please refer to the main DIGIT UI repository.

![DIGIT Logo](https://s3.ap-south-1.amazonaws.com/works-dev-asset/mseva-white-logo.png)
