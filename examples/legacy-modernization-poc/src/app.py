import json
import os
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Dict, List

REGION_RISK_MULTIPLIER = {
    "AR": 1.02,
    "US": 1.08,
    "EU": 1.04,
}


def _normalize_record(record: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "policyId": str(record.get("policyId", "")).strip(),
        "basePremium": float(record.get("basePremium", 0)),
        "riskFactor": float(record.get("riskFactor", 1.0)),
        "loyaltyYears": int(record.get("loyaltyYears", 0)),
        "claimsLast5Years": int(record.get("claimsLast5Years", 0)),
        "region": str(record.get("region", "GLOBAL")).upper(),
    }


def _legacy_premium(record: Dict[str, Any]) -> float:
    premium = record["basePremium"] * record["riskFactor"]
    if record["loyaltyYears"] >= 5:
        premium = premium - (premium * 0.05)
    if record["claimsLast5Years"] > 2:
        premium = premium + (premium * 0.10)
    multiplier = REGION_RISK_MULTIPLIER.get(record["region"], 1.0)
    premium = premium * multiplier
    return round(premium, 2)


def _modern_premium(record: Dict[str, Any]) -> float:
    premium = record["basePremium"] * record["riskFactor"]
    loyalty_discount = 0.95 if record["loyaltyYears"] >= 5 else 1.0
    claims_penalty = 1.10 if record["claimsLast5Years"] > 2 else 1.0
    regional_factor = REGION_RISK_MULTIPLIER.get(record["region"], 1.0)
    premium = premium * loyalty_discount * claims_penalty * regional_factor
    return round(premium, 2)


def _risk_band(record: Dict[str, Any], premium: float) -> str:
    if premium >= 2400 or record["claimsLast5Years"] >= 5:
        return "high"
    if premium >= 1600 or record["claimsLast5Years"] >= 2:
        return "medium"
    return "low"


def evaluate_policy(record: Dict[str, Any], validate_parity: bool = True) -> Dict[str, Any]:
    normalized = _normalize_record(record)
    if not normalized["policyId"]:
        raise ValueError("policyId is required")

    legacy_premium = _legacy_premium(normalized)
    modern_premium = _modern_premium(normalized)
    if validate_parity and legacy_premium != modern_premium:
        raise ValueError(
            f"Parity check failed for policyId={normalized['policyId']}: "
            f"legacy={legacy_premium} modern={modern_premium}"
        )

    decision = "review" if modern_premium >= 2000 else "approve"
    return {
        "policyId": normalized["policyId"],
        "premium": modern_premium,
        "decision": decision,
        "riskBand": _risk_band(normalized, modern_premium),
        "parityValidated": validate_parity,
    }


def _persist_quote(item: Dict[str, Any]) -> None:
    table_name = os.getenv("QUOTES_TABLE_NAME", "").strip()
    if not table_name:
        return
    try:
        import boto3  # pylint: disable=import-outside-toplevel
    except ImportError:
        return

    table = boto3.resource("dynamodb").Table(table_name)
    table.put_item(
        Item={
            "policyId": item["policyId"],
            "processedAt": datetime.now(timezone.utc).isoformat(),
            # DynamoDB's serializer rejects Python floats. Converting through
            # str preserves the decimal representation returned by the API.
            "premium": Decimal(str(item["premium"])),
            "decision": item["decision"],
            "riskBand": item["riskBand"],
            "environment": os.getenv("ENVIRONMENT", "dev"),
        }
    )


def _parse_event_payload(event: Dict[str, Any]) -> Dict[str, Any]:
    if "body" not in event:
        return event
    body = event["body"]
    if isinstance(body, str):
        return json.loads(body)
    if isinstance(body, dict):
        return body
    raise ValueError("Unsupported body payload")


def handler(event: Dict[str, Any], _context: Any) -> Dict[str, Any]:
    payload = _parse_event_payload(event)
    validate_parity = bool(payload.get("validateParity", True))
    records: List[Dict[str, Any]]
    if isinstance(payload.get("records"), list):
        records = payload["records"]
    else:
        records = [payload]

    results = []
    for record in records:
        result = evaluate_policy(record, validate_parity=validate_parity)
        _persist_quote(result)
        results.append(result)

    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(
            {
                "results": results,
                "count": len(results),
                "parityValidated": validate_parity,
            }
        ),
    }
