#!/bin/bash
# quad-services PROD deployment
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/../scripts/deploy.sh" prod "$@"
