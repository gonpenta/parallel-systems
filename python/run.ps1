$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
pip install -r requirements.txt -q
$env:PYTHONPATH = $PSScriptRoot
python -m benchmark @args
