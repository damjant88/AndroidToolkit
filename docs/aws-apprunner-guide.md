# AWS App Runner Deployment — Step by Step

## What You Get

- Public HTTPS URL (e.g., `https://abc123.us-east-1.awsapprunner.com`)
- WebSocket support for Agent connections
- Auto-scaling (scales to zero when idle)
- No load balancer, no cluster, no nginx to manage
- ~$5-20/month for light usage

## Prerequisites

1. AWS CLI installed: `winget install Amazon.AWSCLI`
2. AWS account configured: `aws configure` (enter your access key, secret, region `us-east-1`)
3. Docker Desktop running

## Step 1: Create S3 Bucket

```bash
aws s3 mb s3://androidtoolkit-<your-name> --region us-east-1
```

## Step 2: Create RDS PostgreSQL

```bash
# Create a DB subnet group (use your default VPC subnets)
aws rds create-db-instance ^
  --db-instance-identifier androidtoolkit-db ^
  --db-instance-class db.t3.micro ^
  --engine postgres ^
  --engine-version 16.4 ^
  --master-username androidtoolkit ^
  --master-user-password YourSecurePassword123 ^
  --allocated-storage 20 ^
  --publicly-accessible ^
  --db-name androidtoolkit ^
  --region us-east-1
```

Wait (~5 min):
```bash
aws rds wait db-instance-available --db-instance-identifier androidtoolkit-db
```

Get the endpoint:
```bash
aws rds describe-db-instances --db-instance-identifier androidtoolkit-db --query "DBInstances[0].Endpoint.Address" --output text
```

Save this — you'll need it as `<RDS_ENDPOINT>` below.

## Step 3: Create ECR Repository

```bash
aws ecr create-repository --repository-name androidtoolkit-server --region us-east-1
```

Get your account ID:
```bash
aws sts get-caller-identity --query Account --output text
```

## Step 4: Build and Push Docker Image

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Build (from project root)
docker build -t androidtoolkit-server -f backend/Dockerfile .

# Tag
docker tag androidtoolkit-server:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest

# Push
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest
```

## Step 5: Create IAM Role for App Runner

App Runner needs permission to pull from ECR:

```bash
aws iam create-role ^
  --role-name AppRunnerECRAccess ^
  --assume-role-policy-document "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"build.apprunner.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"}]}"

aws iam attach-role-policy ^
  --role-name AppRunnerECRAccess ^
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess
```

## Step 6: Create App Runner Service

Create `apprunner.json`:

```json
{
  "ServiceName": "androidtoolkit",
  "SourceConfiguration": {
    "ImageRepository": {
      "ImageIdentifier": "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest",
      "ImageRepositoryType": "ECR",
      "ImageConfiguration": {
        "Port": "8080",
        "RuntimeEnvironmentVariables": {
          "SPRING_PROFILES_ACTIVE": "saas",
          "DEPLOYMENT_MODE": "saas",
          "DATABASE_URL": "jdbc:postgresql://<RDS_ENDPOINT>:5432/androidtoolkit",
          "DATABASE_USERNAME": "androidtoolkit",
          "DATABASE_PASSWORD": "YourSecurePassword123",
          "DATABASE_DRIVER": "org.postgresql.Driver",
          "STORAGE_ENDPOINT": "https://s3.us-east-1.amazonaws.com",
          "STORAGE_BUCKET": "androidtoolkit-<your-name>",
          "STORAGE_ACCESS_KEY": "<YOUR_AWS_ACCESS_KEY>",
          "STORAGE_SECRET_KEY": "<YOUR_AWS_SECRET_KEY>",
          "STORAGE_PATH_STYLE": "false",
          "JWT_SECRET": "GenerateASecure64CharStringHereForProductionUseOpenSSLRandBase64",
          "CORS_ALLOWED_ORIGINS": "*"
        }
      }
    },
    "AutoDeploymentsEnabled": false,
    "AuthenticationConfiguration": {
      "AccessRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/AppRunnerECRAccess"
    }
  },
  "InstanceConfiguration": {
    "Cpu": "0.5 vCPU",
    "Memory": "1 GB"
  },
  "HealthCheckConfiguration": {
    "Protocol": "HTTP",
    "Path": "/v3/api-docs",
    "Interval": 20,
    "Timeout": 5,
    "HealthyThreshold": 1,
    "UnhealthyThreshold": 5
  }
}
```

Deploy:
```bash
aws apprunner create-service --cli-input-json file://apprunner.json --region us-east-1
```

Wait for it (~3-5 min):
```bash
aws apprunner list-services --query "ServiceSummaryList[?ServiceName=='androidtoolkit'].{Status:Status,URL:ServiceUrl}" --output table
```

## Step 7: Get Your URL

```bash
aws apprunner describe-service --service-arn <SERVICE_ARN> --query "Service.ServiceUrl" --output text
```

Your app is now live at `https://<generated-id>.us-east-1.awsapprunner.com`

## Step 8: Configure Agents

On each developer machine, update `agent/resources/application.properties`:

```properties
agent.server-url=wss://<generated-id>.us-east-1.awsapprunner.com/ws/agent
agent.token=<get-a-jwt-from-the-login-endpoint>
```

## Step 9: Fix S3 Path Style (One-Time Code Change)

The current code uses `forcePathStyle(true)` for MinIO compatibility. For real AWS S3, update `S3ObjectStorageService.java`:

```java
// Change this:
.forcePathStyle(true)

// To this (reads from env var):
.forcePathStyle(Boolean.parseBoolean(
    System.getenv().getOrDefault("STORAGE_PATH_STYLE", "true")))
```

Set `STORAGE_PATH_STYLE=false` in the App Runner env vars (already included above).

## Updating the Deployment

After code changes:
```bash
# Rebuild and push
docker build -t androidtoolkit-server -f backend/Dockerfile .
docker tag androidtoolkit-server:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest

# Trigger redeployment
aws apprunner start-deployment --service-arn <SERVICE_ARN>
```

## Tear Down (When Done Testing)

```bash
# Delete App Runner service
aws apprunner delete-service --service-arn <SERVICE_ARN>

# Delete RDS
aws rds delete-db-instance --db-instance-identifier androidtoolkit-db --skip-final-snapshot

# Delete S3 bucket
aws s3 rb s3://androidtoolkit-<your-name> --force

# Delete ECR repository
aws ecr delete-repository --repository-name androidtoolkit-server --force
```

## Troubleshooting

**App Runner shows "Create failed":**
- Check CloudWatch logs: `/aws/apprunner/androidtoolkit/...`
- Common issue: RDS security group doesn't allow inbound from App Runner

**Can't connect to RDS from App Runner:**
- App Runner runs in AWS-managed VPC by default
- For RDS access, you need a VPC Connector:
  ```bash
  aws apprunner create-vpc-connector \
    --vpc-connector-name androidtoolkit-vpc \
    --subnets <SUBNET_1> <SUBNET_2> \
    --security-groups <SG_ALLOWING_5432>
  ```
  Then add `"NetworkConfiguration": {"EgressConfiguration": {"EgressType": "VPC", "VpcConnectorArn": "<ARN>"}}` to your service config.

**WebSocket not connecting:**
- App Runner supports WebSocket natively on HTTPS (wss://)
- Make sure Agent uses `wss://` not `ws://`

## Cost Summary

| Service | Monthly (light usage) |
|---|---|
| App Runner (0.5 vCPU, 1GB, auto-pause) | ~$5-15 |
| RDS PostgreSQL (db.t3.micro) | ~$15 |
| S3 | ~$1 |
| **Total** | **~$21-31/month** |

App Runner pauses when there's no traffic, so you only pay for active compute time.
