# Remote DroidGuard Server

Reference implementation of a Remote DroidGuard server for Play Integrity API support.

## Features

- ✅ Multi-step DroidGuard protocol support
- ✅ VM bytecode caching with automatic cleanup
- ✅ Rate limiting and access control
- ✅ Health monitoring and statistics
- ✅ Docker and standalone deployment
- ✅ Compatible with microG Remote DroidGuard

## Quick Start

### Docker (Recommended)

```bash
# Clone repository
git clone https://github.com/microg/GmsCore.git
cd GmsCore/play-services-droidguard/server

# Configure
cp config.yaml.example config.yaml
# Edit config.yaml with your settings

# Start server
export GOOGLE_API_KEY="your_key_here"
docker-compose up -d

# Verify
curl http://localhost:8080/health
```

### Standalone

```bash
# Install dependencies
pip install -r requirements.txt

# Run server
python3 droidguard-server.py --config config.yaml --port 8080
```

## Documentation

- [Setup Guide](docs/REMOTE_SETUP_GUIDE.md) - Complete setup instructions
- [API Reference](docs/REMOTE_SETUP_GUIDE.md#api-reference) - Endpoint documentation
- [Troubleshooting](docs/REMOTE_SETUP_GUIDE.md#troubleshooting) - Common issues

## Architecture

```
Client Device (microG)
       │
       │ HTTP/HTTPS
       ▼
┌─────────────────────┐
│  DroidGuard Server  │
│  - Multi-step DG    │
│  - VM Cache         │
│  - Rate Limiting    │
└─────────────────────┘
       │
       │ HTTPS
       ▼
┌─────────────────────┐
│   Google Servers    │
│   (Play Integrity)  │
└─────────────────────┘
```

## Configuration

See [config.yaml.example](config.yaml.example) for all options:

```yaml
port: 8080
cache_dir: ./cache
rate_limit: 60  # requests/minute
api_key: YOUR_GOOGLE_API_KEY
use_google_fallback: true
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/stats` | GET | Cache statistics |
| `/init` | POST | Initialize DroidGuard session |
| `/snapshot` | POST | Execute VM snapshot |

## Development

### Running Tests

```bash
# Run test suite
python3 -m pytest tests/

# Run with coverage
python3 -m pytest --cov=droidguard_server tests/
```

### Building Docker Image

```bash
docker build -t droidguard-server:latest .
```

## Security Considerations

- **Use HTTPS** in production environments
- **Restrict access** with firewall rules
- **Monitor logs** for unusual activity
- **Run on trusted network** - don't expose to public internet without authentication

See [Security section](docs/REMOTE_SETUP_GUIDE.md#security-considerations) in setup guide.

## Requirements

- Python 3.11+
- Docker (for containerized deployment)
- Google API key (optional, for Google fallback)

## License

Apache 2.0 - See [LICENSE](../../LICENSE) for details.

## Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

- **Issues:** https://github.com/microg/GmsCore/issues
- **Discussions:** https://github.com/microg/GmsCore/discussions

---

**Version:** 1.0.0  
**Last Updated:** March 16, 2026
