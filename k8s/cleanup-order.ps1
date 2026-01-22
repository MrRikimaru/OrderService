Write-Host "Cleaning up order service in namespace microservice-network..." -ForegroundColor Yellow

kubectl delete deployment order-service -n microservice-network --ignore-not-found
kubectl delete deployment order-postgres-db -n microservice-network --ignore-not-found

kubectl delete service order-service -n microservice-network --ignore-not-found
kubectl delete service order-postgres-db -n microservice-network --ignore-not-found

kubectl delete pvc order-postgres-pvc -n microservice-network --ignore-not-found

kubectl delete configmap order-service-config -n microservice-network --ignore-not-found

kubectl delete secret order-postgres-secret -n microservice-network --ignore-not-found

Write-Host "Cleanup complete!" -ForegroundColor Green