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

# Source vault configuration (for all environments)
VAULT_CONFIG="/Users/semostudio/git/a2vibes/QUAD/quad-web/deployment/vault.config.sh"
if [ -f "$VAULT_CONFIG" ]; then
    source "$VAULT_CONFIG"
fi

# Smart BW_SESSION handling - Auto unlock vault if needed (for all environments)
setup_vault_session() {
    if [ -z "$BW_SESSION" ]; then
        print_warning "BW_SESSION not set. Checking vault status..."

        # Check if vault is already unlocked
        VAULT_STATUS=$(bw status 2>/dev/null | jq -r '.status' 2>/dev/null || echo "locked")

        if [ "$VAULT_STATUS" = "unlocked" ]; then
            print_status "Vault already unlocked in another session."
            print_warning "Please enter your Vaultwarden master password to get session token:"
            export BW_SESSION=$(bw unlock --raw)
        elif [ "$VAULT_STATUS" = "locked" ]; then
            print_warning "Vault is locked. Please enter your Vaultwarden master password:"
            export BW_SESSION=$(bw unlock --raw)
        else
            print_error "Not logged into Vaultwarden. Please run:"
            echo "  bw config server https://vault.a2vibes.tech"
            echo "  bw login madhuri.recherla@gmail.com"
            return 1
        fi

        if [ -z "$BW_SESSION" ]; then
            print_error "Failed to unlock vault. Deployment cancelled."
            return 1
        fi

        print_status "✓ Vault unlocked successfully (session valid for 15 minutes)"
    else
        print_status "✓ Using existing BW_SESSION from environment"
    fi
    return 0
}

# Environment-specific configs
case "$ENV" in
    dev)
        PORT=14101
        DB_PORT=5432
        DB_HOST="postgres-quad-dev"
        DB_NAME="quad_dev_db"
        DB_PASS="quad_dev_pass"
        PROFILE="dev"
        CONTAINER="quad-services-dev"
        ;;
    qa)
        PORT=15101
        DB_PORT=5432
        DB_HOST="postgres-quad-qa"
        DB_NAME="quad_qa_db"
        DB_PASS="quad_qa_pass"
        PROFILE="qa"
        CONTAINER="quad-services-qa"
        ;;
    prod)
        print_status "PROD uses GCP Cloud Run"

        # GCP Configuration
        GCP_PROJECT="nutrinine-prod"
        GCP_REGION="us-east1"
        GCP_SERVICE="quad-services-prod"
        GCP_IMAGE="gcr.io/${GCP_PROJECT}/quad-services:latest"
        CLOUD_SQL_INSTANCE="${GCP_PROJECT}:${GCP_REGION}:nutrinine-db"

        # Build and deploy to Cloud Run
        cd "$SERVICES_DIR"

        print_status "Building with Maven..."
        JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn clean package -DskipTests

        print_status "Building Docker image for Cloud Run (amd64)..."
        docker build --platform linux/amd64 -t $GCP_IMAGE .

        print_status "Configuring Docker for GCR..."
        gcloud config set project $GCP_PROJECT
        gcloud auth configure-docker --quiet

        print_status "Pushing image to GCR..."
        docker push $GCP_IMAGE

        print_status "Fetching secrets from Vaultwarden..."

        # Setup vault session (interactive password prompt if needed)
        if ! setup_vault_session; then
            exit 1
        fi

        # Fetch JWT Secret from Vaultwarden (using NextAuth Secret)
        print_status "Fetching JWT Secret (NextAuth Secret)..."
        JWT_ITEM=$(bw list items --organizationid "$VAULT_QUAD_ORG_ID" 2>/dev/null | \
            jq -r ".[] | select(.name==\"NextAuth Secret\" and (.collectionIds | contains([\"$VAULT_COLLECTION_PROD\"])))")

        if [ -z "$JWT_ITEM" ] || [ "$JWT_ITEM" = "null" ]; then
            print_error "NextAuth Secret not found in Vaultwarden (QUAD org → prod collection)"
            exit 1
        fi

        JWT_SECRET=$(echo "$JWT_ITEM" | jq -r '.login.password')

        if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "null" ]; then
            print_error "JWT Secret password is empty"
            exit 1
        fi

        print_status "JWT Secret loaded: ${JWT_SECRET:0:10}..."

        # Fetch Database credentials from Vaultwarden
        print_status "Fetching Database credentials..."
        DB_ITEM=$(bw list items --organizationid "$VAULT_QUAD_ORG_ID" 2>/dev/null | \
            jq -r ".[] | select(.name==\"Database\" and (.collectionIds | contains([\"$VAULT_COLLECTION_PROD\"])))")

        if [ -z "$DB_ITEM" ] || [ "$DB_ITEM" = "null" ]; then
            print_error "Database credentials not found in Vaultwarden (QUAD org → prod collection)"
            exit 1
        fi

        DB_PASSWORD=$(echo "$DB_ITEM" | jq -r '.login.password')
        DB_USER="quad_user"
        DB_NAME="quad_prod_db"

        # Build DATABASE_URL for Cloud SQL
        DATABASE_URL="jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${CLOUD_SQL_INSTANCE}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"

        print_status "Deploying to Cloud Run..."
        gcloud run deploy $GCP_SERVICE \
            --image=$GCP_IMAGE \
            --region=$GCP_REGION \
            --platform=managed \
            --allow-unauthenticated \
            --memory=1Gi \
            --cpu=1 \
            --max-instances=10 \
            --min-instances=0 \
            --timeout=300 \
            --add-cloudsql-instances=$CLOUD_SQL_INSTANCE \
            --set-env-vars="SPRING_PROFILES_ACTIVE=prod,JWT_SECRET=${JWT_SECRET},DATABASE_URL=${DATABASE_URL},DB_USER=${DB_USER},DB_PASSWORD=${DB_PASSWORD}"

        SERVICE_URL=$(gcloud run services describe $GCP_SERVICE --region=$GCP_REGION --format='value(status.url)')

        print_status "╔════════════════════════════════════════════════╗"
        print_status "║  PROD Deployment Complete! ✓                  ║"
        print_status "╚════════════════════════════════════════════════╝"
        print_status "Service URL: $SERVICE_URL"
        print_status "Custom Domain: https://api.quadframe.work"

        exit 0
        ;;
    *)
        echo "Usage: $0 {dev|qa|prod}"
        exit 1
        ;;
esac

# Fetch secrets from Vaultwarden (for DEV and QA)
if [ "$ENV" = "dev" ] || [ "$ENV" = "qa" ]; then
    print_status "Fetching secrets from Vaultwarden..."

    # Setup vault session (interactive password prompt if needed)
    if ! setup_vault_session; then
        exit 1
    fi

    # Determine collection based on environment
    if [ "$ENV" = "dev" ]; then
        COLLECTION_ID="$VAULT_COLLECTION_DEV"
    else
        COLLECTION_ID="$VAULT_COLLECTION_QA"
    fi

    # Fetch JWT Secret from Vaultwarden (using NextAuth Secret)
    print_status "Fetching JWT Secret (NextAuth Secret)..."
    JWT_ITEM=$(bw list items --organizationid "$VAULT_QUAD_ORG_ID" 2>/dev/null | \
        jq -r ".[] | select(.name==\"NextAuth Secret\" and (.collectionIds | contains([\"$COLLECTION_ID\"])))")

    if [ -z "$JWT_ITEM" ] || [ "$JWT_ITEM" = "null" ]; then
        print_warning "NextAuth Secret not found in Vaultwarden ($ENV collection) - using default"
        JWT_SECRET=""
    else
        JWT_SECRET=$(echo "$JWT_ITEM" | jq -r '.login.password')
        if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "null" ]; then
            print_warning "JWT Secret password is empty - using default"
            JWT_SECRET=""
        else
            print_status "JWT Secret loaded: ${JWT_SECRET:0:10}..."
        fi
    fi
fi

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
    docker build -t quad-services:${ENV} .

    docker stop $CONTAINER 2>/dev/null || true
    docker rm $CONTAINER 2>/dev/null || true

    print_status "Starting container on port ${PORT}..."

    # Build DATABASE_URL
    DATABASE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"

    # Prepare environment variables
    ENV_VARS="-e SPRING_PROFILES_ACTIVE=${PROFILE}"
    ENV_VARS="$ENV_VARS -e DATABASE_URL=${DATABASE_URL}"
    ENV_VARS="$ENV_VARS -e DB_USER=quad_user"
    ENV_VARS="$ENV_VARS -e DB_PASSWORD=${DB_PASS}"

    # Add JWT_SECRET if available from vault
    if [ -n "$JWT_SECRET" ]; then
        ENV_VARS="$ENV_VARS -e JWT_SECRET=${JWT_SECRET}"
        print_status "Using JWT_SECRET from Vaultwarden"
    fi

    docker run -d \
        --name $CONTAINER \
        --network ${ENV}-network \
        -p ${PORT}:8080 \
        --restart unless-stopped \
        $ENV_VARS \
        quad-services:${ENV}

    print_status "Container started: $CONTAINER on port ${PORT}"
else
    print_warning "No Dockerfile found. Run manually:"
    echo ""
    echo "  SPRING_PROFILES_ACTIVE=${PROFILE} java -jar $JAR_FILE"
    echo ""
fi

print_status "API: http://localhost:${PORT}/api"
