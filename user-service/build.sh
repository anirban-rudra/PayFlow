#!/bin/bash

echo "🏢 Building on Office Laptop with SSL Bypass..."

# Clean previous builds
echo "1. Cleaning..."
mvn clean -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true

# Package with all SSL bypass flags
echo "2. Packaging..."
mvn package -DskipTests -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true

# Verify
echo "3. Verifying JAR..."
if [ -f "target/user-service-0.0.1-SNAPSHOT.jar" ]; then
    echo "✅ JAR created successfully!"
    echo "=== JAR Details ==="
    ls -lh target/user-service-0.0.1-SNAPSHOT.jar
    echo "=== Manifest Check ==="
    unzip -p target/user-service-0.0.1-SNAPSHOT.jar META-INF/MANIFEST.MF | head -10
else
    echo "❌ JAR not found!"
    echo "=== Target Contents ==="
    ls -la target/
    exit 1
fi