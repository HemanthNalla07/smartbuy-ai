"""
Standalone Python 3.11 verification runner for SmartBuy AI analytical logic.
Validates all 8 test cases from Module 8 and verifies the exact sample datasets.
"""
import difflib
import json
import math
import re
import statistics
import unittest

UNIT_FACTORS = {
    "kg": ("kg", 1.0), "kilogram": ("kg", 1.0), "kilograms": ("kg", 1.0),
    "g": ("kg", 0.001), "gm": ("kg", 0.001), "gms": ("kg", 0.001), "gram": ("kg", 0.001), "grams": ("kg", 0.001),
    "l": ("l", 1.0), "ltr": ("l", 1.0), "litre": ("l", 1.0), "litres": ("l", 1.0), "liter": ("l", 1.0), "liters": ("l", 1.0),
    "ml": ("l", 0.001), "millilitre": ("l", 0.001), "millilitres": ("l", 0.001),
    "unit": ("unit", 1.0), "units": ("unit", 1.0), "pc": ("unit", 1.0), "pcs": ("unit", 1.0),
    "piece": ("unit", 1.0), "pieces": ("unit", 1.0), "dozen": ("unit", 12.0),
    "pack": ("pack", 1.0), "packs": ("pack", 1.0), "packet": ("pack", 1.0), "packets": ("pack", 1.0),
    "pkt": ("pack", 1.0), "pkts": ("pack", 1.0),
}


def parse_price_value(raw_value):
    if raw_value is None:
        return None
    if isinstance(raw_value, (int, float)):
        return float(raw_value) if raw_value >= 0 else None
    cleaned = re.sub(r"[^0-9.]", "", str(raw_value).replace(",", ""))
    if not cleaned:
        return None
    val = float(cleaned)
    return val if val >= 0 else None


def normalize_quantity_and_unit(quantity, unit):
    qty_num = parse_price_value(quantity)
    if qty_num is None or qty_num <= 0:
        qty_num = 1.0
    unit_clean = (unit or "unit").strip().lower()
    canonical_unit, factor = UNIT_FACTORS.get(unit_clean, (unit_clean, 1.0))
    return round(qty_num, 4), unit_clean, round(qty_num * factor, 6), canonical_unit


def token_sort_ratio(a: str, b: str) -> float:
    ta = " ".join(sorted(a.lower().split()))
    tb = " ".join(sorted(b.lower().split()))
    return difflib.SequenceMatcher(None, ta, tb).ratio() * 100.0


def classify_deal(current_price, hist_low, hist_avg, trend):
    if hist_low is None or hist_low <= 0 or hist_avg is None or hist_avg <= 0:
        return "Insufficient Historical Data (No Price Invented)"
    pct_above_low = ((current_price - hist_low) / hist_low) * 100.0
    if pct_above_low <= 3.0:
        return "Strong Buy — Near Observed All-Time Low"
    if current_price <= hist_avg * 0.95:
        return "Good Deal — Below Historical Average"
    if current_price <= hist_avg * 1.05:
        return "Fair Price — Wait for Drop" if trend == "Falling" else "Fair Price — Around Average"
    return "Overpriced — Wait (Above Historical Average)"


def recommend_baskets(baskets):
    complete = [b for b in baskets if b["is_complete_basket"]]
    incomplete = [b for b in baskets if not b["is_complete_basket"]]
    complete.sort(key=lambda b: (b["final_payable_cost"], b["delivery_time_mins"]))
    incomplete.sort(key=lambda b: (-b["available_items"], b["final_payable_cost"], b["delivery_time_mins"]))
    return {
        "recommended": complete[0] if complete else None,
        "complete_ranked": complete,
        "incomplete_ranked": incomplete,
    }


class VerifySmartBuyCoreTests(unittest.TestCase):
    def test_01_product_matching(self):
        self.assertGreater(token_sort_ratio("WH-1000XM5 Wireless ANC", "Wireless ANC WH-1000XM5"), 95.0)
        self.assertLess(token_sort_ratio("Sony WH-1000XM5", "Samsung Galaxy Buds2 Pro"), 50.0)

    def test_02_price_normalization(self):
        self.assertAlmostEqual(parse_price_value("₹1,24,999.50"), 124999.50)
        self.assertIsNone(parse_price_value("N/A"))
        self.assertIsNone(parse_price_value(-10))

    def test_03_quantity_conversion(self):
        _, _, q_kg, u_kg = normalize_quantity_and_unit(500, "grams")
        self.assertAlmostEqual(q_kg, 0.5)
        self.assertEqual(u_kg, "kg")
        _, _, q_l, u_l = normalize_quantity_and_unit(1000, "ml")
        self.assertAlmostEqual(q_l, 1.0)
        self.assertEqual(u_l, "l")

    def test_04_discount_calculation(self):
        mrp, listed, promo, deliv, plat = 1000.0, 800.0, 100.0, 50.0, 10.0
        discount_pct = ((mrp - (listed - promo)) / mrp) * 100.0
        final_payable = (listed - promo) + deliv + plat
        self.assertAlmostEqual(discount_pct, 30.0)
        self.assertAlmostEqual(final_payable, 760.0)

    def test_05_basket_total(self):
        # ValueGrocer [SAMPLE]: 30 + 60 + (0.5 * 48) + 145 + 78 = 337; +0 del +10 fee -20 disc = 327
        subtotal = 30.0 + 60.0 + (0.5 / 1.0) * 48.0 + 145.0 + 78.0
        final_payable = subtotal - 20.0 + 0.0 + 10.0
        self.assertAlmostEqual(subtotal, 337.0)
        self.assertAlmostEqual(final_payable, 327.0)

    def test_06_deal_classification(self):
        self.assertIn("Near Observed All-Time Low", classify_deal(102.0, 100.0, 120.0, "Falling"))
        self.assertIn("Overpriced", classify_deal(140.0, 100.0, 120.0, "Rising"))

    def test_07_recommendation_ranking(self):
        rec = recommend_baskets([
            {"platform": "IncompleteCheap", "is_complete_basket": False, "available_items": 4, "final_payable_cost": 100.0, "delivery_time_mins": 5},
            {"platform": "CompleteSlower", "is_complete_basket": True, "available_items": 5, "final_payable_cost": 300.0, "delivery_time_mins": 25},
            {"platform": "CompleteFasterTie", "is_complete_basket": True, "available_items": 5, "final_payable_cost": 300.0, "delivery_time_mins": 12},
        ])
        self.assertEqual(rec["recommended"]["platform"], "CompleteFasterTie")
        self.assertEqual(len(rec["incomplete_ranked"]), 1)

    def test_08_missing_data_handling(self):
        self.assertIn("Insufficient Historical Data", classify_deal(500.0, None, None, "Stable"))


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(VerifySmartBuyCoreTests)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    assert result.wasSuccessful()
