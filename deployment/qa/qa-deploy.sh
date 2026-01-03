#!/bin/bash
# quad-services QA deployment
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/../scripts/deploy.sh" qa "$@"
