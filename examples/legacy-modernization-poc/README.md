# Legacy Modernization PoC Sample (AWS Marketplace Week 3)

This example provides a small but concrete modernization slice aligned to the ModernAsh Marketplace PoC offering.

## Scenario

Legacy policy premium calculation is exposed as a modern API workload:

- Input: policy pricing attributes.
- Processing: deterministic premium calculation with optional parity check.
- Output: quote result and decision (`approve` or `review`).
- Persistence: quote records stored in DynamoDB.

## Folder structure

```
legacy-modernization-poc/
├── src/app.py
├── template.yaml
├── tests/golden_dataset.json
└── tests/test_app.py
```

## Local test command

From `renovatio/`:

```bash
python3 -m unittest discover -s examples/legacy-modernization-poc/tests -p "test_*.py"
```

## Local SAM build

```bash
sam build --template-file examples/legacy-modernization-poc/template.yaml
```

## Deploy with provided automation

From `renovatio/`:

```bash
ENVIRONMENT=dev AWS_REGION=us-east-1 ./scripts/deploy-marketplace-poc.sh
```
