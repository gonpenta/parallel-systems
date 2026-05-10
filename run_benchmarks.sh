#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Starting Sequential Language Benchmark Comparison...${NC}"

# 1. Build and Run Java
echo -e "\n${GREEN}[1/4] Building and Running Java Benchmarks...${NC}"
cd benchmark-framework-source
./build.sh
./run.sh
cd ..
mkdir -p benchmark-results
cp benchmark-framework-source/benchmark-results/benchmark-results-java.json benchmark-results/

# 2. Setup and Run Python
echo -e "\n${GREEN}[2/4] Setting up and Running Python Benchmarks...${NC}"
cd python
# Creating virtual environment to avoid "externally managed" error
if [ ! -d "venv" ]; then
    python3 -m venv venv
fi
source venv/bin/activate
pip install -r requirements.txt
python3 -m benchmark
deactivate
cd ..
cp python/benchmark-results/benchmark-results-python.json benchmark-results/

# 3. Generate Comparison Report
echo -e "\n${GREEN}[3/4] Generating Comparison Report...${NC}"
# Use the same venv for the comparison tool or system python
python3 compare_languages.py

# 4. Done
echo -e "\n${BLUE}======================================================${NC}"
echo -e "${BLUE}Benchmark suite complete!${NC}"
echo -e "${BLUE}Individual reports in benchmark-results/${NC}"
echo -e "${GREEN}Final Comparison: benchmark-results/comparison-report.html${NC}"
echo -e "${BLUE}======================================================${NC}"
