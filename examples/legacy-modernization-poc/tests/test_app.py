import json
import sys
import unittest
from pathlib import Path

TEST_DIR = Path(__file__).resolve().parent
SRC_DIR = TEST_DIR.parent / "src"
sys.path.insert(0, str(SRC_DIR))

from app import evaluate_policy, handler  # noqa: E402


class PolicyCalculatorTest(unittest.TestCase):
    def test_golden_dataset(self) -> None:
        dataset = json.loads((TEST_DIR / "golden_dataset.json").read_text(encoding="utf-8"))
        for case in dataset:
            result = evaluate_policy(case["input"], validate_parity=True)
            self.assertEqual(result["premium"], case["expected"]["premium"])
            self.assertEqual(result["decision"], case["expected"]["decision"])
            self.assertEqual(result["riskBand"], case["expected"]["riskBand"])
            self.assertTrue(result["parityValidated"])

    def test_handler_single_payload(self) -> None:
        event = {
            "body": json.dumps(
                {
                    "policyId": "POL-5005",
                    "basePremium": 1000,
                    "riskFactor": 1.1,
                    "loyaltyYears": 5,
                    "claimsLast5Years": 1,
                    "region": "AR",
                    "validateParity": True,
                }
            )
        }
        response = handler(event, None)
        payload = json.loads(response["body"])
        self.assertEqual(response["statusCode"], 200)
        self.assertEqual(payload["count"], 1)
        self.assertTrue(payload["parityValidated"])
        self.assertEqual(payload["results"][0]["policyId"], "POL-5005")

    def test_handler_batch_payload(self) -> None:
        event = {
            "records": [
                {
                    "policyId": "POL-6006",
                    "basePremium": 1400,
                    "riskFactor": 1.2,
                    "loyaltyYears": 1,
                    "claimsLast5Years": 0,
                    "region": "US",
                },
                {
                    "policyId": "POL-7007",
                    "basePremium": 1700,
                    "riskFactor": 1.25,
                    "loyaltyYears": 0,
                    "claimsLast5Years": 4,
                    "region": "EU",
                },
            ],
            "validateParity": True,
        }
        response = handler(event, None)
        payload = json.loads(response["body"])
        self.assertEqual(response["statusCode"], 200)
        self.assertEqual(payload["count"], 2)
        self.assertEqual(payload["results"][0]["decision"], "approve")
        self.assertEqual(payload["results"][1]["decision"], "review")


if __name__ == "__main__":
    unittest.main()
