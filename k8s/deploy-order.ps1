Write-Host "1. Creating namespace..." -ForegroundColor Green
kubectl apply -f namespace.yaml

Write-Host "2. Creating secrets..." -ForegroundColor Green
kubectl apply -f order-service-secrets.yaml

Write-Host "3. Creating storage..." -ForegroundColor Green
kubectl apply -f order-postgres-pvc.yaml

Write-Host "4. Creating database..." -ForegroundColor Green
kubectl apply -f order-postgres-deployment.yaml

Write-Host "5. Waiting for database to be ready..." -ForegroundColor Yellow
kubectl wait --namespace=microservice-network --for=condition=ready pod -l app=order-postgres-db --timeout=180s

Write-Host "6. Creating configmap..." -ForegroundColor Green
kubectl apply -f order-configmap.yaml

Write-Host "7. Waiting 30 seconds for database initialization..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "8. Deploying order service..." -ForegroundColor Green
kubectl apply -f order-service-deployment.yaml

Write-Host "9. Waiting for order service to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10
kubectl wait --namespace=microservice-network --for=condition=ready pod -l app=order-service --timeout=300s

Write-Host "DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "To access the service:" -ForegroundColor Yellow
Write-Host "  Use port-forward: kubectl port-forward svc/order-service 8083:8083 -n microservice-network" -ForegroundColor Cyan
Write-Host "  Then access via: http://localhost:8083" -ForegroundColor Cyan