# AWS Deployment Guide — Android Toolkit SaaS

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    AWS Cloud                         │
│                                                     │
│  ┌──────────┐    ┌──────────────┐    ┌──────────┐  │
│  │   ALB    │───▶│  ECS Fargate │───▶│ RDS      │  │
│  │ (HTTPS)  │    │  (Server)    │    │ PostgreSQL│  │
│  └──────────┘    └──────┬───────┘    └──────────┘  │
│       ▲                 │                           │
│       │                 ▼                           │
│  ┌────┴─────┐    ┌──────────────┐                  │
│  │ Route 53 │    │     S3       │                  │
│  │  (DNS)   │    │   (Storage)  │                  │
│  └──────────┘    └──────────────┘                  │
│                                                     │
└─────────────────────────────────────────────────────┘
        ▲                    ▲
        │ HTTPS + WS         │ WebSocket
   ┌────┴────┐          ┌───┴────┐
   │ Browser │          │ Agent  │
   └─────────┘          └────────┘
```

## Prerequisites

- AWS CLI installed and configured (`aws configure`)
- Docker installed locally
- An AWS account with permissions for ECR, ECS, RDS, S3, ALB

## Step 1: Create the S3 Bucket

```bash
aws s3 mb s3://androidtoolkit-storage --region us-east-1
```

## Step 2: Create RDS PostgreSQL

```bash
aws rds create-db-instance \
  --db-instance-identifier androidtoolkit-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16.4 \
  --master-username androidtoolkit \
  --master-user-password <YOUR_DB_PASSWORD> \
  --allocated-storage 20 \
  --publicly-accessible \
  --db-name androidtoolkit \
  --region us-east-1
```

Wait for it to become available:
```bash
aws rds wait db-instance-available --db-instance-identifier androidtoolkit-db
```

Get the endpoint:
```bash
aws rds describe-db-instances --db-instance-identifier androidtoolkit-db \
  --query 'DBInstances[0].Endpoint.Address' --output text
```

## Step 3: Create ECR Repository and Push Docker Image

```bash
# Create repository
aws ecr create-repository --repository-name androidtoolkit-server --region us-east-1

# Get login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Build and push
docker build -t androidtoolkit-server -f backend/Dockerfile .
docker tag androidtoolkit-server:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest
```

## Step 4: Create ECS Cluster and Task Definition

### Create cluster:
```bash
aws ecs create-cluster --cluster-name androidtoolkit --region us-east-1
```

### Create task definition (`ecs-task-definition.json`):

```json
{
  "family": "androidtoolkit-server",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "server",
      "image": "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest",
      "portMappings": [
        { "containerPort": 8080, "protocol": "tcp" }
      ],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "saas" },
        { "name": "DEPLOYMENT_MODE", "value": "saas" },
        { "name": "DATABASE_URL", "value": "jdbc:postgresql://<RDS_ENDPOINT>:5432/androidtoolkit" },
        { "name": "DATABASE_USERNAME", "value": "androidtoolkit" },
        { "name": "DATABASE_PASSWORD", "value": "<YOUR_DB_PASSWORD>" },
        { "name": "DATABASE_DRIVER", "value": "org.postgresql.Driver" },
        { "name": "STORAGE_ENDPOINT", "value": "https://s3.us-east-1.amazonaws.com" },
        { "name": "STORAGE_BUCKET", "value": "androidtoolkit-storage" },
        { "name": "STORAGE_ACCESS_KEY", "value": "<AWS_ACCESS_KEY>" },
        { "name": "STORAGE_SECRET_KEY", "value": "<AWS_SECRET_KEY>" },
        { "name": "JWT_SECRET", "value": "<GENERATE_A_SECURE_SECRET_HERE>" },
        { "name": "CORS_ALLOWED_ORIGINS", "value": "*" },
        { "name": "STORAGE_PATH_STYLE", "value": "false" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/androidtoolkit",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "server"
        }
      }
    }
  ]
}
```

Register it:
```bash
aws ecs register-task-definition --cli-input-json file://ecs-task-definition.json
```

## Step 5: Create ALB (Application Load Balancer)

This handles HTTPS termination and WebSocket support:

```bash
# Create ALB
aws elbv2 create-load-balancer \
  --name androidtoolkit-alb \
  --subnets <SUBNET_1> <SUBNET_2> \
  --security-groups <SG_ID> \
  --scheme internet-facing \
  --type application

# Create target group (health check on /api/auth/register won't work, use a simple endpoint)
aws elbv2 create-target-group \
  --name androidtoolkit-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id <VPC_ID> \
  --target-type ip \
  --health-check-path /swagger-ui/index.html \
  --health-check-interval-seconds 30

# Create HTTPS listener (requires ACM certificate)
aws elbv2 create-listener \
  --load-balancer-arn <ALB_ARN> \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=<ACM_CERT_ARN> \
  --default-actions Type=forward,TargetGroupArn=<TG_ARN>
```

## Step 6: Create ECS Service

```bash
aws ecs create-service \
  --cluster androidtoolkit \
  --service-name androidtoolkit-server \
  --task-definition androidtoolkit-server \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[<SUBNET_1>,<SUBNET_2>],securityGroups=[<SG_ID>],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=<TG_ARN>,containerName=server,containerPort=8080"
```

## Step 7: Configure DNS (Optional)

Point your domain to the ALB:
```bash
aws route53 change-resource-record-sets --hosted-zone-id <ZONE_ID> --change-batch '{
  "Changes": [{"Action": "CREATE", "ResourceRecordSet": {
    "Name": "toolkit.yourdomain.com",
    "Type": "A",
    "AliasTarget": {
      "HostedZoneId": "<ALB_HOSTED_ZONE>",
      "DNSName": "<ALB_DNS_NAME>",
      "EvaluateTargetHealth": true
    }
  }}]
}'
```

## Step 8: Fix S3 Path Style for AWS

The current `S3ObjectStorageService` uses `forcePathStyle(true)` which is needed for MinIO but not for real AWS S3. Add this config option:

In `application-saas.properties`:
```properties
storage.path-style=${STORAGE_PATH_STYLE:true}
```

Then update `S3ObjectStorageService` to use it:
```java
.forcePathStyle(Boolean.parseBoolean(pathStyle))
```

For AWS deployment, set `STORAGE_PATH_STYLE=false`.

## Step 9: Connect Agents

On each developer machine, configure the Agent to point to your AWS deployment:

```properties
# agent/resources/application.properties
agent.server-url=wss://toolkit.yourdomain.com/ws/agent
agent.token=<device-scoped-jwt>
```

## Security Checklist

- [ ] RDS is in a private subnet (not publicly accessible in production)
- [ ] Security groups restrict access (ALB → ECS only on 8080, ECS → RDS only on 5432)
- [ ] S3 bucket has no public access
- [ ] JWT_SECRET is stored in AWS Secrets Manager (not plain env var)
- [ ] CORS_ALLOWED_ORIGINS is set to your actual domain (not `*`)
- [ ] HTTPS only (redirect HTTP → HTTPS on ALB)
- [ ] Database password in Secrets Manager

## Cost Estimate (minimal setup)

| Service | Monthly Cost |
|---|---|
| ECS Fargate (0.5 vCPU, 1GB) | ~$15 |
| RDS PostgreSQL (db.t3.micro) | ~$15 |
| ALB | ~$16 + data |
| S3 | ~$1-5 (depends on storage) |
| **Total** | **~$47/month** |

## Alternative: AWS App Runner (Even Simpler)

If you want zero infrastructure management:

```bash
aws apprunner create-service \
  --service-name androidtoolkit \
  --source-configuration '{
    "ImageRepository": {
      "ImageIdentifier": "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/androidtoolkit-server:latest",
      "ImageRepositoryType": "ECR",
      "ImageConfiguration": {
        "Port": "8080",
        "RuntimeEnvironmentVariables": {
          "SPRING_PROFILES_ACTIVE": "saas",
          "DATABASE_URL": "jdbc:postgresql://<RDS_ENDPOINT>:5432/androidtoolkit",
          "STORAGE_ENDPOINT": "https://s3.us-east-1.amazonaws.com",
          "STORAGE_BUCKET": "androidtoolkit-storage"
        }
      }
    },
    "AutoDeploymentsEnabled": true,
    "AuthenticationConfiguration": {
      "AccessRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/AppRunnerECRAccessRole"
    }
  }'
```

App Runner gives you a public HTTPS URL immediately, handles scaling, and supports WebSocket. No ALB, no ECS cluster to manage.
