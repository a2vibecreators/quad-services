#!/bin/bash
# =============================================================================
# quad-services - Deployment Script
# =============================================================================
# Deploys Java Spring Boot backend to specified environment
#
# Usage:
#   ./deploy.sh dev              # Deploy to DEV
#   ./deploy.sh qa               # Deploy to QA
#   ./deploy.sh prod             # Deploy to PROD (GCP)
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES_DIR="$(dirname "$SCRIPT_DIR")"
SERVICES_DIR="$(dirname "$SERVICES_DIR")"  # Go up from deployment/scripts to quad-services
ENV="${1:-dev}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() { echo -e "${GREEN}[quad-services]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[quad-services]${NC} $1"; }
print_error() { echo -e "${RED}[quad-services]${NC} $1"; }

# Environment-specific configs
case "$ENV" in
    dev)
        PORT=14101
        DB_PORT=14201
        DB_HOST="localhost"
        DB_NAME="quad_dev_db"
        DB_PASS="quad_dev_pass"
        PROFILE="dev"
        CONTAINER="quadframework-api-dev"
        ;;
    qa)
        PORT=15101
        DB_PORT=15201
        DB_HOST="localhost"
        DB_NAME="quad_qa_db"
        DB_PASS="quad_qa_pass"
        PROFILE="qa"
        CONTAINER="quadframework-api-qa"
        ;;
    prod)
        print_status "PROD uses GCP Cloud Run"
        # TODO: Add GCP deployment
        exit 0
        ;;
    *)
        echo "Usage: $0 {dev|qa|prod}"
        exit 1
        ;;
esac

# Build and deploy
cd "$SERVICES_DIR"

print_status "Building with Maven..."
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn clean package -DskipTests -q

JAR_FILE=$(ls target/quad-services-*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    print_error "JAR file not found"
    exit 1
fi

print_status "Built: $JAR_FILE"

# Check if running in Docker mode
if [ -f "Dockerfile" ]; then
    print_status "Building Docker image..."
    docker build -t quadframework-api:${ENV} .

    docker stop $CONTAINER 2>/dev/null || true
    docker rm $CONTAINER 2>/dev/null || true

    print_status "Starting container on port ${PORT}..."
    docker run -d \
        --name $CONTAINER \
        --network docker_${ENV}-network \
        -p ${PORT}:8080 \
        --restart unless-stopped \
        -e SPRING_PROFILES_ACTIVE=${PROFILE} \
        -e DB_HOST=${DB_HOST} \
        -e DB_PORT=${DB_PORT} \
        -e DB_NAME=${DB_NAME} \
        -e DB_PASSWORD=${DB_PASS} \
        quadframework-api:${ENV}

    print_status "Container started: $CONTAINER on port ${PORT}"
else
    print_warning "No Dockerfile found. Run manually:"
    echo ""
    echo "  SPRING_PROFILES_ACTIVE=${PROFILE} java -jar $JAR_FILE"
    echo ""
fi

print_status "API: http://localhost:${PORT}/api"
