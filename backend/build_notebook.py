"""
Builder & verifier for SmartBuy_AI_Analytical_Engine.ipynb.
Ensures valid nbformat 4.5 JSON and verifies Python syntax of every code cell.
"""
import ast
import json
from pathlib import Path


def md_cell(text: str) -> dict:
    lines = [line + "\n" for line in text.strip().splitlines()]
    if lines:
        lines[-1] = lines[-1].rstrip("\n")
    return {
        "cell_type": "markdown",
        "metadata": {},
        "source": lines,
    }


def code_cell(code: str) -> dict:
    code = code.strip()
    # Skip AST check for Colab magic commands like !pip install
    non_magic = "\n".join(
        line for line in code.splitlines() if not line.lstrip().startswith(("!", "%"))
    )
    if non_magic.strip():
        ast.parse(non_magic)
    lines = [line + "\n" for line in code.splitlines()]
    if lines:
        lines[-1] = lines[-1].rstrip("\n")
    return {
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": lines,
    }


CELLS = [
    md_cell("""
# SmartBuy AI – Price Analysis and Recommendation Engine

**Backend Analytical Engine (Google Colab Notebook)**

This notebook implements the complete Python analytical backend for **SmartBuy AI**, covering universal shopping product normalization, RapidFuzz + TF-IDF product matching, multi-platform payable price comparison, Buyhatke historical price observation analysis across 5 categories, grocery basket normalization & optimization, interactive Plotly visualizations, automated tests, and exportable analytical artifacts.

> **Data Integrity Notice:** All built-in datasets in this notebook are explicitly labeled **`[SAMPLE TEST DATA]`** for offline validation and testing. No fabricated numbers are presented as live marketplace results, no historical prices are invented, and no private endpoints or access controls are bypassed.
"""),
    md_cell("## Section 0: Environment Setup & Package Installation"),
    code_cell("""
# Colab Installation Command (Run once per runtime)
!pip install -q pandas numpy matplotlib plotly rapidfuzz scikit-learn pydantic requests beautifulsoup4
"""),
    code_cell("""
import json
import math
import os
import re
import unittest
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Literal, Optional, Tuple

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
import requests
from bs4 import BeautifulSoup
from plotly.subplots import make_subplots
from pydantic import BaseModel, Field, field_validator
from rapidfuzz import fuzz
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LinearRegression
from sklearn.metrics.pairwise import cosine_similarity

pd.set_option("display.max_columns", 25)
pd.set_option("display.width", 140)

OUTPUT_DIR = Path("smartbuy_outputs")
CHARTS_DIR = OUTPUT_DIR / "charts"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
CHARTS_DIR.mkdir(parents=True, exist_ok=True)

print("SmartBuy AI Engine initialized. Outputs directory:", OUTPUT_DIR.resolve())
"""),
    md_cell("""
## Section 1 (Module 1): Product Data Processing & Normalization

Accepts shopping product records from **JSON** or **CSV**, validates fields via **Pydantic**, extracts:
- `product_name`, `brand`, `category`, `model`, `size`, `colour`, `quantity`, `unit`, `budget`
- Normalizes casing, whitespace, colour aliases, category names, currency strings, and canonical measurement units (`g` → `kg`, `ml` → `l`, `pcs`/`pieces` → `unit`, `pkts`/`packets` → `pack`).
"""),
    code_cell("""
UNIT_FACTORS: Dict[str, Tuple[str, float]] = {
    "kg": ("kg", 1.0),
    "kilogram": ("kg", 1.0),
    "kilograms": ("kg", 1.0),
    "g": ("kg", 0.001),
    "gm": ("kg", 0.001),
    "gms": ("kg", 0.001),
    "gram": ("kg", 0.001),
    "grams": ("kg", 0.001),
    "l": ("l", 1.0),
    "ltr": ("l", 1.0),
    "litre": ("l", 1.0),
    "litres": ("l", 1.0),
    "liter": ("l", 1.0),
    "liters": ("l", 1.0),
    "ml": ("l", 0.001),
    "millilitre": ("l", 0.001),
    "millilitres": ("l", 0.001),
    "unit": ("unit", 1.0),
    "units": ("unit", 1.0),
    "pc": ("unit", 1.0),
    "pcs": ("unit", 1.0),
    "piece": ("unit", 1.0),
    "pieces": ("unit", 1.0),
    "dozen": ("unit", 12.0),
    "pack": ("pack", 1.0),
    "packs": ("pack", 1.0),
    "packet": ("pack", 1.0),
    "packets": ("pack", 1.0),
    "pkt": ("pack", 1.0),
    "pkts": ("pack", 1.0),
}

COLOUR_ALIASES: Dict[str, str] = {
    "blk": "black",
    "midnight": "black",
    "obsidian": "black",
    "wht": "white",
    "ivory": "white",
    "slv": "silver",
    "grey": "gray",
    "space gray": "gray",
    "navy blue": "navy",
    "crimson": "red",
}

CATEGORY_ALIASES: Dict[str, str] = {
    "phone": "smartphones",
    "mobile": "smartphones",
    "smartphone": "smartphones",
    "smartphones": "smartphones",
    "shoe": "shoes",
    "sneakers": "shoes",
    "running shoes": "shoes",
    "shoes": "shoes",
    "apparel": "clothing",
    "shirt": "clothing",
    "t-shirt": "clothing",
    "clothing": "clothing",
    "earphones": "headphones",
    "headset": "headphones",
    "headphones": "headphones",
    "watch": "smartwatches",
    "smartwatch": "smartwatches",
    "smartwatches": "smartwatches",
    "grocery": "groceries",
    "groceries": "groceries",
}


def clean_text(value: Optional[Any]) -> str:
    if value is None or (isinstance(value, float) and math.isnan(value)):
        return ""
    return re.sub(r"\\s+", " ", str(value).strip())


def parse_price_value(raw_value: Any) -> Optional[float]:
    \"\"\"Parse currency strings like '₹1,299.50' or '$49.00' into a non-negative float.\"\"\"
    if raw_value is None or (isinstance(raw_value, float) and math.isnan(raw_value)):
        return None
    if isinstance(raw_value, (int, float)):
        val = float(raw_value)
        return val if val >= 0 else None
    cleaned = re.sub(r"[^0-9.]", "", str(raw_value).replace(",", ""))
    if not cleaned:
        return None
    try:
        val = float(cleaned)
        return val if val >= 0 else None
    except ValueError:
        return None


def normalize_quantity_and_unit(quantity: Any, unit: Optional[str]) -> Tuple[float, str, float, str]:
    \"\"\"Return (raw_qty, raw_unit, canonical_qty, canonical_unit).\"\"\"
    qty_num = parse_price_value(quantity)
    if qty_num is None or qty_num <= 0:
        qty_num = 1.0
    unit_clean = clean_text(unit).lower() or "unit"
    canonical_unit, factor = UNIT_FACTORS.get(unit_clean, (unit_clean, 1.0))
    return round(qty_num, 4), unit_clean, round(qty_num * factor, 6), canonical_unit


class RawProductInput(BaseModel):
    product_name: str
    brand: Optional[str] = ""
    category: Optional[str] = "general"
    model: Optional[str] = ""
    size: Optional[str] = ""
    colour: Optional[str] = ""
    quantity: Any = 1.0
    unit: Optional[str] = "unit"
    budget: Any = None
    data_label: str = "[SAMPLE TEST DATA]"

    @field_validator("product_name")
    @classmethod
    def check_name(cls, v: str) -> str:
        cleaned = clean_text(v)
        if not cleaned:
            raise ValueError("product_name must not be empty")
        return cleaned


class NormalizedProduct(BaseModel):
    product_name: str
    normalized_name: str
    brand: str
    category: str
    model: str
    size: str
    colour: str
    quantity: float
    unit: str
    canonical_quantity: float
    canonical_unit: str
    budget: Optional[float]
    data_label: str


def normalize_product(raw: RawProductInput) -> NormalizedProduct:
    name_clean = clean_text(raw.product_name)
    brand_clean = clean_text(raw.brand).lower()
    cat_raw = clean_text(raw.category).lower()
    cat_clean = CATEGORY_ALIASES.get(cat_raw, cat_raw or "general")
    model_clean = clean_text(raw.model).lower()
    size_clean = clean_text(raw.size).lower()
    col_raw = clean_text(raw.colour).lower()
    col_clean = COLOUR_ALIASES.get(col_raw, col_raw)
    qty, u, canon_qty, canon_unit = normalize_quantity_and_unit(raw.quantity, raw.unit)
    budget_val = parse_price_value(raw.budget)

    norm_tokens = [t for t in [brand_clean, name_clean.lower(), model_clean, size_clean, col_clean] if t]
    normalized_name = re.sub(r"[^a-z0-9\\s.-]", "", " ".join(norm_tokens))
    normalized_name = re.sub(r"\\s+", " ", normalized_name).strip()

    return NormalizedProduct(
        product_name=name_clean,
        normalized_name=normalized_name,
        brand=brand_clean,
        category=cat_clean,
        model=model_clean,
        size=size_clean,
        colour=col_clean,
        quantity=qty,
        unit=u,
        canonical_quantity=canon_qty,
        canonical_unit=canon_unit,
        budget=budget_val,
        data_label=raw.data_label,
    )


def load_products_from_json(json_payload: str | List[Dict[str, Any]]) -> pd.DataFrame:
    records = json.loads(json_payload) if isinstance(json_payload, str) else json_payload
    normalized = [normalize_product(RawProductInput(**rec)).model_dump() for rec in records]
    return pd.DataFrame(normalized)


def load_products_from_csv(csv_path: str | Path) -> pd.DataFrame:
    df_raw = pd.read_csv(csv_path)
    records = df_raw.to_dict(orient="records")
    normalized = [normalize_product(RawProductInput(**rec)).model_dump() for rec in records]
    return pd.DataFrame(normalized)


def extract_public_product_metadata(html_content: Optional[str] = None, url: Optional[str] = None) -> Dict[str, Any]:
    \"\"\"
    Public-markup metadata helper using Requests + BeautifulSoup4.
    Only inspects explicitly provided public HTML or standard public HTTP GET (never private endpoints).
    \"\"\"
    if html_content is None and url:
        resp = requests.get(url, timeout=8, headers={"User-Agent": "SmartBuyAI-EducationalAnalyzer/1.0"})
        resp.raise_for_status()
        html_content = resp.text
    if not html_content:
        return {}
    soup = BeautifulSoup(html_content, "html.parser")
    title = clean_text(soup.title.string) if soup.title and soup.title.string else ""
    og_title = soup.find("meta", property="og:title")
    return {
        "page_title": title,
        "og_title": clean_text(og_title.get("content")) if og_title else "",
    }


SAMPLE_PRODUCTS_JSON = [
    {
        "product_name": "Galaxy S24 Ultra 5G",
        "brand": "Samsung",
        "category": "Smartphone",
        "model": "SM-S928B",
        "size": "256GB",
        "colour": "Titanium Black",
        "quantity": 1,
        "unit": "unit",
        "budget": "₹1,25,000",
        "data_label": "[SAMPLE TEST DATA]",
    },
    {
        "product_name": "Pegasus 41 Running Shoes",
        "brand": "Nike",
        "category": "Running Shoes",
        "model": "FD2722",
        "size": "UK 9",
        "colour": "Volt / Blk",
        "quantity": 1,
        "unit": "pair",
        "budget": "11500",
        "data_label": "[SAMPLE TEST DATA]",
    },
    {
        "product_name": "WH-1000XM5 Wireless ANC",
        "brand": "Sony",
        "category": "Headphones",
        "model": "WH1000XM5",
        "size": "Over-Ear",
        "colour": "Midnight",
        "quantity": 1,
        "unit": "pcs",
        "budget": 28000,
        "data_label": "[SAMPLE TEST DATA]",
    },
    {
        "product_name": "Watch Series 10 GPS",
        "brand": "Apple",
        "category": "Smartwatch",
        "model": "MWWE3",
        "size": "46mm",
        "colour": "Jet Black",
        "quantity": 1,
        "unit": "unit",
        "budget": "₹47,500",
        "data_label": "[SAMPLE TEST DATA]",
    },
    {
        "product_name": "Essential Slim Crew Tee",
        "brand": "Uniqlo",
        "category": "T-Shirt",
        "model": "UQ-465185",
        "size": "L",
        "colour": "Navy Blue",
        "quantity": 2,
        "unit": "pkts",
        "budget": 2500,
        "data_label": "[SAMPLE TEST DATA]",
    },
]

cleaned_products_df = load_products_from_json(SAMPLE_PRODUCTS_JSON)
print("Module 1 — Cleaned & Normalized Product Dataset ([SAMPLE TEST DATA]):")
display(cleaned_products_df[["product_name", "brand", "category", "model", "size", "colour", "canonical_quantity", "canonical_unit", "budget", "data_label"]])
"""),
    md_cell("""
## Section 2 (Module 2): Product Matching Algorithm (RapidFuzz + TF-IDF)

Computes a weighted matching confidence score (`0–100`) across:
- **Name similarity** (RapidFuzz `token_sort_ratio` + `WRatio` + Scikit-learn TF-IDF cosine similarity)
- **Brand**
- **Model**
- **Product category**
- **Size**
- **Colour**
- **Quantity**

Classifies candidates into:
- **Exact Match** (`confidence >= 88.0`, brand/category aligned, size & model compatible)
- **Similar Alternative** (`60.0 <= confidence < 88.0`, or exact name/brand with variant size/colour/model)
- **No Match** (`confidence < 60.0`)
"""),
    code_cell("""
class MatchResult(BaseModel):
    query_product: str
    candidate_product: str
    platform: str
    confidence_score: float
    match_type: Literal["Exact Match", "Similar Alternative", "No Match"]
    attribute_scores: Dict[str, float]
    data_label: str = "[SAMPLE TEST DATA]"


def _attribute_similarity(a: str, b: str, partial: bool = False) -> float:
    if not a and not b:
        return 100.0
    if not a or not b:
        return 50.0  # Missing attribute treated as neutral partial signal
    if a == b:
        return 100.0
    return float(fuzz.partial_ratio(a, b) if partial else fuzz.token_sort_ratio(a, b))


def _tfidf_similarity(text_a: str, text_b: str) -> float:
    if not text_a or not text_b:
        return 0.0
    vec = TfidfVectorizer(ngram_range=(1, 2))
    matrix = vec.fit_transform([text_a, text_b])
    return float(cosine_similarity(matrix[0:1], matrix[1:2])[0][0] * 100.0)


def match_products(
    query: NormalizedProduct,
    candidates: List[Tuple[str, NormalizedProduct]],
) -> Dict[str, List[MatchResult]]:
    exact_matches: List[MatchResult] = []
    similar_alternatives: List[MatchResult] = []
    non_matches: List[MatchResult] = []

    for platform, cand in candidates:
        rf_token = float(fuzz.token_sort_ratio(query.product_name.lower(), cand.product_name.lower()))
        rf_wratio = float(fuzz.WRatio(query.product_name.lower(), cand.product_name.lower()))
        tfidf_score = _tfidf_similarity(query.normalized_name, cand.normalized_name)
        name_score = 0.45 * rf_token + 0.35 * rf_wratio + 0.20 * tfidf_score

        brand_score = _attribute_similarity(query.brand, cand.brand)
        model_score = _attribute_similarity(query.model, cand.model, partial=True)
        cat_score = 100.0 if query.category == cand.category else _attribute_similarity(query.category, cand.category)
        size_score = _attribute_similarity(query.size, cand.size)
        colour_score = _attribute_similarity(query.colour, cand.colour)

        if query.canonical_unit == cand.canonical_unit and query.canonical_quantity > 0:
            ratio = min(query.canonical_quantity, cand.canonical_quantity) / max(
                query.canonical_quantity, cand.canonical_quantity
            )
            qty_score = ratio * 100.0
        else:
            qty_score = 40.0

        confidence = round(
            0.34 * name_score
            + 0.16 * brand_score
            + 0.14 * model_score
            + 0.12 * cat_score
            + 0.10 * size_score
            + 0.07 * colour_score
            + 0.07 * qty_score,
            2,
        )

        model_ok = (query.model == cand.model) if (query.model and cand.model) else True
        strict_attrs_ok = (
            brand_score >= 85.0
            and cat_score >= 85.0
            and model_ok
            and (size_score >= 90.0 or not query.size or not cand.size)
            and qty_score >= 90.0
        )

        if confidence >= 88.0 and strict_attrs_ok:
            mtype: Literal["Exact Match", "Similar Alternative", "No Match"] = "Exact Match"
        elif confidence >= 60.0 and cat_score >= 60.0:
            mtype = "Similar Alternative"
        else:
            mtype = "No Match"

        res = MatchResult(
            query_product=query.product_name,
            candidate_product=cand.product_name,
            platform=platform,
            confidence_score=confidence,
            match_type=mtype,
            attribute_scores={
                "name": round(name_score, 1),
                "brand": round(brand_score, 1),
                "model": round(model_score, 1),
                "category": round(cat_score, 1),
                "size": round(size_score, 1),
                "colour": round(colour_score, 1),
                "quantity": round(qty_score, 1),
            },
        )
        if mtype == "Exact Match":
            exact_matches.append(res)
        elif mtype == "Similar Alternative":
            similar_alternatives.append(res)
        else:
            non_matches.append(res)

    exact_matches.sort(key=lambda r: r.confidence_score, reverse=True)
    similar_alternatives.sort(key=lambda r: r.confidence_score, reverse=True)
    return {
        "exact_matches": exact_matches,
        "similar_alternatives": similar_alternatives,
        "non_matches": non_matches,
    }


query_item = normalize_product(RawProductInput(**SAMPLE_PRODUCTS_JSON[2]))  # Sony WH-1000XM5
SAMPLE_CATALOG = [
    ("Platform Alpha [SAMPLE]", normalize_product(RawProductInput(
        product_name="Sony WH-1000XM5 Wireless Noise Cancelling Headphones",
        brand="Sony", category="Headphones", model="WH1000XM5", size="Over-Ear", colour="Black", quantity=1, unit="unit"
    ))),
    ("Platform Beta [SAMPLE]", normalize_product(RawProductInput(
        product_name="WH-1000XM5 ANC Headset",
        brand="Sony", category="Headphones", model="WH1000XM5", size="Over-Ear", colour="Silver", quantity=1, unit="pcs"
    ))),
    ("Platform Gamma [SAMPLE]", normalize_product(RawProductInput(
        product_name="Sony WH-1000XM4 Wireless Headphones",
        brand="Sony", category="Headphones", model="WH1000XM4", size="Over-Ear", colour="Black", quantity=1, unit="unit"
    ))),
    ("Platform Delta [SAMPLE]", normalize_product(RawProductInput(
        product_name="Samsung Galaxy Buds2 Pro",
        brand="Samsung", category="Headphones", model="R510", size="In-Ear", colour="White", quantity=1, unit="unit"
    ))),
]

match_output = match_products(query_item, SAMPLE_CATALOG)
print("Module 2 — Exact Matches:")
display(pd.DataFrame([m.model_dump() for m in match_output["exact_matches"]]))
print("Module 2 — Similar Alternatives:")
display(pd.DataFrame([m.model_dump() for m in match_output["similar_alternatives"]]))
"""),
    md_cell("""
## Section 3 (Module 3): Multi-Platform Price Comparison

Accepts structured platform price quotes and computes:
- **Lowest product price**, **Highest product price**, **Average product price**, **Price difference**
- **Discount percentage** (`(MRP - listed_price) / MRP * 100` plus coupon/promo savings)
- **Final payable price** = `listed_price - promo_discount + delivery_charge + platform_charge`
"""),
    code_cell("""
class PlatformOffer(BaseModel):
    product_name: str
    platform: str
    mrp: float
    listed_price: float
    promo_discount: float = 0.0
    delivery_charge: float = 0.0
    platform_charge: float = 0.0
    in_stock: bool = True
    delivery_time_mins: Optional[int] = None
    data_label: str = "[SAMPLE TEST DATA]"


def evaluate_offers(offers: List[PlatformOffer]) -> Dict[str, Any]:
    rows = []
    for o in offers:
        effective_discount_amt = max(0.0, o.mrp - (o.listed_price - o.promo_discount))
        discount_pct = round((effective_discount_amt / o.mrp) * 100.0, 2) if o.mrp > 0 else 0.0
        final_payable = round(max(0.0, o.listed_price - o.promo_discount) + o.delivery_charge + o.platform_charge, 2)
        rows.append({
            "product_name": o.product_name,
            "platform": o.platform,
            "mrp": o.mrp,
            "listed_price": o.listed_price,
            "promo_discount": o.promo_discount,
            "delivery_charge": o.delivery_charge,
            "platform_charge": o.platform_charge,
            "discount_pct": discount_pct,
            "final_payable_price": final_payable,
            "in_stock": o.in_stock,
            "data_label": o.data_label,
        })

    df = pd.DataFrame(rows)
    available_df = df[df["in_stock"]] if not df.empty else df
    prices = available_df["final_payable_price"] if not available_df.empty else pd.Series(dtype=float)

    summary = {
        "lowest_price": round(float(prices.min()), 2) if not prices.empty else None,
        "highest_price": round(float(prices.max()), 2) if not prices.empty else None,
        "average_price": round(float(prices.mean()), 2) if not prices.empty else None,
        "price_difference": round(float(prices.max() - prices.min()), 2) if len(prices) >= 2 else 0.0,
        "best_platform": available_df.loc[prices.idxmin(), "platform"] if not prices.empty else None,
    }
    return {"table": df.sort_values("final_payable_price"), "summary": summary}


SAMPLE_OFFERS = [
    PlatformOffer(product_name="Sony WH-1000XM5", platform="Store Alpha [SAMPLE]", mrp=34990, listed_price=26990, promo_discount=1500, delivery_charge=0, platform_charge=49),
    PlatformOffer(product_name="Sony WH-1000XM5", platform="Store Beta [SAMPLE]", mrp=34990, listed_price=25990, promo_discount=0, delivery_charge=199, platform_charge=99),
    PlatformOffer(product_name="Sony WH-1000XM5", platform="Store Gamma [SAMPLE]", mrp=34990, listed_price=27990, promo_discount=2000, delivery_charge=0, platform_charge=0),
    PlatformOffer(product_name="Sony WH-1000XM5", platform="Store Delta [SAMPLE]", mrp=34990, listed_price=28490, promo_discount=500, delivery_charge=120, platform_charge=29),
]

price_comp_result = evaluate_offers(SAMPLE_OFFERS)
comparison_table_df = price_comp_result["table"]
print("Module 3 — Platform Price Comparison Summary:", price_comp_result["summary"])
display(comparison_table_df)
"""),
    md_cell("""
## Section 4 (Module 4): Buyhatke Price Observation Analysis (5 Categories)

Implements the exact Buyhatke observation schema:
- `product_name`, `product_url`, `platform`, `current_price`, `historical_lowest_price`, `historical_average_price`, `historical_highest_price`, `observation_date`, `source`
- Supports **manual entry** (`add_buyhatke_observation`) and **CSV import** (`import_buyhatke_csv`).
- **Strictly does NOT invent historical prices** — all metrics (`avg`, `min`, `max`, `volatility`, `% above historical low`, `price trend`, and `deal recommendation`) are derived exclusively from the recorded observations.
- Analyses all 5 required categories: `Smartphones`, `Shoes`, `Clothing`, `Headphones`, `Smartwatches`.
"""),
    code_cell("""
BUYHATKE_COLUMNS = [
    "product_name",
    "product_url",
    "platform",
    "current_price",
    "historical_lowest_price",
    "historical_average_price",
    "historical_highest_price",
    "observation_date",
    "source",
]


class BuyhatkeObservation(BaseModel):
    product_name: str
    product_url: str
    platform: str
    current_price: float
    historical_lowest_price: Optional[float] = None
    historical_average_price: Optional[float] = None
    historical_highest_price: Optional[float] = None
    observation_date: str
    source: str
    category: Optional[str] = "General"


class BuyhatkeDataset:
    def __init__(self) -> None:
        self.records: List[Dict[str, Any]] = []

    def add_observation(self, obs: BuyhatkeObservation) -> None:
        self.records.append(obs.model_dump())

    def import_csv(self, csv_path: str | Path) -> int:
        df = pd.read_csv(csv_path)
        missing = [c for c in BUYHATKE_COLUMNS if c not in df.columns]
        if missing:
            raise ValueError(f"CSV missing required Buyhatke columns: {missing}")
        count = 0
        for row in df.to_dict(orient="records"):
            obs = BuyhatkeObservation(
                product_name=str(row["product_name"]),
                product_url=str(row["product_url"]),
                platform=str(row["platform"]),
                current_price=float(row["current_price"]),
                historical_lowest_price=parse_price_value(row.get("historical_lowest_price")),
                historical_average_price=parse_price_value(row.get("historical_average_price")),
                historical_highest_price=parse_price_value(row.get("historical_highest_price")),
                observation_date=str(row["observation_date"]),
                source=str(row["source"]),
                category=str(row.get("category", "General")),
            )
            self.add_observation(obs)
            count += 1
        return count

    def to_dataframe(self) -> pd.DataFrame:
        if not self.records:
            return pd.DataFrame(columns=BUYHATKE_COLUMNS + ["category"])
        df = pd.DataFrame(self.records)
        df["observation_date"] = pd.to_datetime(df["observation_date"])
        return df.sort_values(["category", "product_name", "observation_date"]).reset_index(drop=True)


def classify_deal(current_price: float, hist_low: Optional[float], hist_avg: Optional[float], trend: str) -> str:
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


def analyze_buyhatke_observations(df: pd.DataFrame) -> pd.DataFrame:
    if df.empty:
        return pd.DataFrame()
    summaries = []
    for (category, product_name), grp in df.groupby(["category", "product_name"]):
        grp = grp.sort_values("observation_date")
        prices = grp["current_price"].dropna().astype(float)
        if prices.empty:
            continue
        latest_row = grp.iloc[-1]
        current_p = float(latest_row["current_price"])

        obs_min = float(prices.min())
        obs_max = float(prices.max())
        obs_avg = float(prices.mean())
        volatility_std = float(prices.std(ddof=0)) if len(prices) > 1 else 0.0
        volatility_cv = round((volatility_std / obs_avg) * 100.0, 2) if obs_avg > 0 else 0.0

        # Use explicit observed historical low/avg if supplied, otherwise strictly use observed series min/avg
        ref_low = latest_row["historical_lowest_price"] if pd.notna(latest_row["historical_lowest_price"]) else obs_min
        ref_avg = latest_row["historical_average_price"] if pd.notna(latest_row["historical_average_price"]) else obs_avg
        pct_above_low = round(((current_p - ref_low) / ref_low) * 100.0, 2) if ref_low and ref_low > 0 else None

        if len(prices) >= 2:
            x = np.arange(len(prices)).reshape(-1, 1)
            reg = LinearRegression().fit(x, prices.values)
            slope = float(reg.coef_[0])
            rel_slope = (slope / obs_avg) * 100.0 if obs_avg > 0 else 0.0
            trend = "Falling" if rel_slope < -0.8 else ("Rising" if rel_slope > 0.8 else "Stable")
        else:
            slope = 0.0
            trend = "Insufficient Points"

        summaries.append({
            "category": category,
            "product_name": product_name,
            "platform": latest_row["platform"],
            "observations_count": len(prices),
            "current_price": round(current_p, 2),
            "observed_min": round(obs_min, 2),
            "observed_avg": round(obs_avg, 2),
            "observed_max": round(obs_max, 2),
            "hist_low_ref": round(float(ref_low), 2) if pd.notna(ref_low) else None,
            "pct_above_hist_low": pct_above_low,
            "volatility_std": round(volatility_std, 2),
            "volatility_cv_pct": volatility_cv,
            "trend_slope_per_obs": round(slope, 2),
            "price_trend": trend,
            "deal_recommendation": classify_deal(current_p, ref_low, ref_avg, trend),
            "source": latest_row["source"],
        })
    return pd.DataFrame(summaries)


# Populate [SAMPLE TEST DATA] observations across all 5 required categories
bh_dataset = BuyhatkeDataset()
SAMPLE_BUYHATKE_ROWS = [
    # 1. Smartphones
    ("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 129999, 114999, 122500, 134999, "2026-09-01", "Manual Sample Observation [SAMPLE]"),
    ("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 124999, 114999, 122500, 134999, "2026-10-01", "Manual Sample Observation [SAMPLE]"),
    ("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 119999, 114999, 122500, 134999, "2026-10-20", "Manual Sample Observation [SAMPLE]"),
    ("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 116499, 114999, 122500, 134999, "2026-11-15", "Manual Sample Observation [SAMPLE]"),
    # 2. Shoes
    ("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 11895, 8990, 10450, 11895, "2026-09-01", "Manual Sample Observation [SAMPLE]"),
    ("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 10795, 8990, 10450, 11895, "2026-10-01", "Manual Sample Observation [SAMPLE]"),
    ("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 9890, 8990, 10450, 11895, "2026-10-25", "Manual Sample Observation [SAMPLE]"),
    ("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 9490, 8990, 10450, 11895, "2026-11-15", "Manual Sample Observation [SAMPLE]"),
    # 3. Clothing
    ("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1290, 890, 1120, 1490, "2026-09-01", "Manual Sample Observation [SAMPLE]"),
    ("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1190, 890, 1120, 1490, "2026-10-01", "Manual Sample Observation [SAMPLE]"),
    ("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1290, 890, 1120, 1490, "2026-10-25", "Manual Sample Observation [SAMPLE]"),
    ("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1390, 890, 1120, 1490, "2026-11-15", "Manual Sample Observation [SAMPLE]"),
    # 4. Headphones
    ("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 29990, 23990, 26800, 34990, "2026-09-01", "Manual Sample Observation [SAMPLE]"),
    ("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 27990, 23990, 26800, 34990, "2026-10-01", "Manual Sample Observation [SAMPLE]"),
    ("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 25490, 23990, 26800, 34990, "2026-10-25", "Manual Sample Observation [SAMPLE]"),
    ("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 24490, 23990, 26800, 34990, "2026-11-15", "Manual Sample Observation [SAMPLE]"),
    # 5. Smartwatches
    ("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 46900, 41900, 44900, 49900, "2026-09-01", "Manual Sample Observation [SAMPLE]"),
    ("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 45900, 41900, 44900, 49900, "2026-10-01", "Manual Sample Observation [SAMPLE]"),
    ("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 44900, 41900, 44900, 49900, "2026-10-25", "Manual Sample Observation [SAMPLE]"),
    ("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 44900, 41900, 44900, 49900, "2026-11-15", "Manual Sample Observation [SAMPLE]"),
]

for cat, pname, url, plat, cur, hlow, havg, hhigh, dt, src in SAMPLE_BUYHATKE_ROWS:
    bh_dataset.add_observation(BuyhatkeObservation(
        category=cat, product_name=pname, product_url=url, platform=plat,
        current_price=cur, historical_lowest_price=hlow, historical_average_price=havg,
        historical_highest_price=hhigh, observation_date=dt, source=src,
    ))

# Demonstrate CSV export + re-import capability
sample_bh_csv = OUTPUT_DIR / "sample_buyhatke_import.csv"
bh_dataset.to_dataframe().to_csv(sample_bh_csv, index=False)
reimported_ds = BuyhatkeDataset()
imported_count = reimported_ds.import_csv(sample_bh_csv)

buyhatke_history_df = reimported_ds.to_dataframe()
buyhatke_summary_df = analyze_buyhatke_observations(buyhatke_history_df)
print(f"Module 4 — Imported {imported_count} rows via CSV & computed 5-category analytics:")
display(buyhatke_summary_df)

# Matplotlib historical price chart
fig, ax = plt.subplots(figsize=(10, 5))
for prod, grp in buyhatke_history_df.groupby("product_name"):
    ax.plot(grp["observation_date"], grp["current_price"], marker="o", label=prod)
ax.set_title("Buyhatke Price Observation History ([SAMPLE TEST DATA])")
ax.set_xlabel("Observation Date")
ax.set_ylabel("Observed Price (₹)")
ax.grid(True, linestyle="--", alpha=0.4)
ax.legend(bbox_to_anchor=(1.02, 1), loc="upper left")
plt.tight_layout()
plt.show()
"""),
    md_cell("""
## Section 5 (Module 5): Grocery Price Comparison & Quantity Normalization

Tests with the exact required grocery basket:
1. `4 bananas`
2. `1 litre milk`
3. `500 grams tomatoes`
4. `1 kg apples`
5. `2 packets bread`

Normalizes requested units against each platform's pack size (e.g., `500 ml` milk packs × 2 = `1 l`, `250 g` tomato packs × 2 = `0.5 kg`, `6 pcs` banana bunch vs `1 unit` banana) and computes exact line-item and basket totals.
"""),
    code_cell("""
class GroceryBasketItem(BaseModel):
    item_name: str
    quantity: float
    unit: str


class GroceryStoreListing(BaseModel):
    platform: str
    item_name: str
    pack_quantity: float
    pack_unit: str
    pack_price: float
    in_stock: bool = True
    data_label: str = "[SAMPLE TEST DATA]"


class GroceryStoreMetadata(BaseModel):
    platform: str
    delivery_charge: float
    free_delivery_threshold: Optional[float] = None
    platform_fee: float = 0.0
    basket_discount: float = 0.0
    delivery_time_mins: int


TEST_GROCERY_BASKET = [
    GroceryBasketItem(item_name="bananas", quantity=4, unit="pieces"),
    GroceryBasketItem(item_name="milk", quantity=1, unit="litre"),
    GroceryBasketItem(item_name="tomatoes", quantity=500, unit="grams"),
    GroceryBasketItem(item_name="apples", quantity=1, unit="kg"),
    GroceryBasketItem(item_name="bread", quantity=2, unit="packets"),
]

SAMPLE_GROCERY_METADATA = {
    "QuickMart [SAMPLE]": GroceryStoreMetadata(platform="QuickMart [SAMPLE]", delivery_charge=25.0, free_delivery_threshold=350.0, platform_fee=5.0, basket_discount=15.0, delivery_time_mins=14),
    "FreshBasket [SAMPLE]": GroceryStoreMetadata(platform="FreshBasket [SAMPLE]", delivery_charge=15.0, free_delivery_threshold=500.0, platform_fee=0.0, basket_discount=10.0, delivery_time_mins=22),
    "ValueGrocer [SAMPLE]": GroceryStoreMetadata(platform="ValueGrocer [SAMPLE]", delivery_charge=0.0, free_delivery_threshold=None, platform_fee=10.0, basket_discount=20.0, delivery_time_mins=35),
    "SpeedyDrop [SAMPLE]": GroceryStoreMetadata(platform="SpeedyDrop [SAMPLE]", delivery_charge=20.0, free_delivery_threshold=None, platform_fee=5.0, basket_discount=25.0, delivery_time_mins=10),
}

SAMPLE_GROCERY_LISTINGS = [
    # QuickMart [SAMPLE] - Complete
    GroceryStoreListing(platform="QuickMart [SAMPLE]", item_name="bananas", pack_quantity=4, pack_unit="pcs", pack_price=36.0),
    GroceryStoreListing(platform="QuickMart [SAMPLE]", item_name="milk", pack_quantity=500, pack_unit="ml", pack_price=33.0),
    GroceryStoreListing(platform="QuickMart [SAMPLE]", item_name="tomatoes", pack_quantity=500, pack_unit="g", pack_price=28.0),
    GroceryStoreListing(platform="QuickMart [SAMPLE]", item_name="apples", pack_quantity=1, pack_unit="kg", pack_price=160.0),
    GroceryStoreListing(platform="QuickMart [SAMPLE]", item_name="bread", pack_quantity=1, pack_unit="packet", pack_price=45.0),
    # FreshBasket [SAMPLE] - Complete (slightly cheaper, slower)
    GroceryStoreListing(platform="FreshBasket [SAMPLE]", item_name="bananas", pack_quantity=1, pack_unit="unit", pack_price=8.5),
    GroceryStoreListing(platform="FreshBasket [SAMPLE]", item_name="milk", pack_quantity=1, pack_unit="l", pack_price=62.0),
    GroceryStoreListing(platform="FreshBasket [SAMPLE]", item_name="tomatoes", pack_quantity=250, pack_unit="g", pack_price=13.0),
    GroceryStoreListing(platform="FreshBasket [SAMPLE]", item_name="apples", pack_quantity=500, pack_unit="g", pack_price=75.0),
    GroceryStoreListing(platform="FreshBasket [SAMPLE]", item_name="bread", pack_quantity=1, pack_unit="pkt", pack_price=42.0),
    # ValueGrocer [SAMPLE] - Complete (cheapest total)
    GroceryStoreListing(platform="ValueGrocer [SAMPLE]", item_name="bananas", pack_quantity=4, pack_unit="pieces", pack_price=30.0),
    GroceryStoreListing(platform="ValueGrocer [SAMPLE]", item_name="milk", pack_quantity=1000, pack_unit="ml", pack_price=60.0),
    GroceryStoreListing(platform="ValueGrocer [SAMPLE]", item_name="tomatoes", pack_quantity=1, pack_unit="kg", pack_price=48.0),
    GroceryStoreListing(platform="ValueGrocer [SAMPLE]", item_name="apples", pack_quantity=1, pack_unit="kg", pack_price=145.0),
    GroceryStoreListing(platform="ValueGrocer [SAMPLE]", item_name="bread", pack_quantity=2, pack_unit="packets", pack_price=78.0),
    # SpeedyDrop [SAMPLE] - Incomplete (apples out of stock!)
    GroceryStoreListing(platform="SpeedyDrop [SAMPLE]", item_name="bananas", pack_quantity=4, pack_unit="pieces", pack_price=32.0),
    GroceryStoreListing(platform="SpeedyDrop [SAMPLE]", item_name="milk", pack_quantity=1, pack_unit="litre", pack_price=64.0),
    GroceryStoreListing(platform="SpeedyDrop [SAMPLE]", item_name="tomatoes", pack_quantity=500, pack_unit="grams", pack_price=24.0),
    GroceryStoreListing(platform="SpeedyDrop [SAMPLE]", item_name="apples", pack_quantity=1, pack_unit="kg", pack_price=140.0, in_stock=False),
    GroceryStoreListing(platform="SpeedyDrop [SAMPLE]", item_name="bread", pack_quantity=1, pack_unit="packet", pack_price=40.0),
]


def compare_grocery_baskets(
    basket: List[GroceryBasketItem],
    listings: List[GroceryStoreListing],
    store_meta: Dict[str, GroceryStoreMetadata],
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    item_rows = []
    basket_rows = []

    for platform, meta in store_meta.items():
        plat_listings = {l.item_name.lower(): l for l in listings if l.platform == platform}
        subtotal = 0.0
        available_count = 0
        missing_items = []

        for req in basket:
            _, _, req_canon_qty, req_canon_unit = normalize_quantity_and_unit(req.quantity, req.unit)
            listing = plat_listings.get(req.item_name.lower())
            if listing is None or not listing.in_stock:
                missing_items.append(req.item_name)
                item_rows.append({
                    "platform": platform,
                    "item_name": req.item_name,
                    "requested": f"{req.quantity} {req.unit}",
                    "normalized_requested": f"{req_canon_qty} {req_canon_unit}",
                    "available": False,
                    "packs_needed": None,
                    "line_cost": None,
                })
                continue

            _, _, pack_canon_qty, pack_canon_unit = normalize_quantity_and_unit(listing.pack_quantity, listing.pack_unit)
            if req_canon_unit != pack_canon_unit or pack_canon_qty <= 0:
                missing_items.append(f"{req.item_name} (unit mismatch)")
                continue

            # Exact equivalent scaled cost vs discrete whole packs
            packs_exact = req_canon_qty / pack_canon_qty
            packs_needed = math.ceil(packs_exact) if req_canon_unit == "pack" else packs_exact
            line_cost = round(packs_needed * listing.pack_price, 2)
            subtotal += line_cost
            available_count += 1

            item_rows.append({
                "platform": platform,
                "item_name": req.item_name,
                "requested": f"{req.quantity} {req.unit}",
                "normalized_requested": f"{req_canon_qty} {req_canon_unit}",
                "available": True,
                "packs_needed": round(packs_needed, 2),
                "line_cost": line_cost,
            })

        delivery = 0.0 if (meta.free_delivery_threshold and subtotal >= meta.free_delivery_threshold) else meta.delivery_charge
        final_payable = round(max(0.0, subtotal - meta.basket_discount) + delivery + meta.platform_fee, 2)
        is_complete = len(missing_items) == 0

        basket_rows.append({
            "platform": platform,
            "is_complete_basket": is_complete,
            "available_items": available_count,
            "total_requested_items": len(basket),
            "missing_items": ", ".join(missing_items) if missing_items else "None",
            "item_subtotal": round(subtotal, 2),
            "delivery_charge": delivery,
            "platform_fee": meta.platform_fee,
            "basket_discount": meta.basket_discount,
            "final_payable_cost": final_payable,
            "delivery_time_mins": meta.delivery_time_mins,
        })

    return pd.DataFrame(item_rows), pd.DataFrame(basket_rows)


grocery_items_df, grocery_baskets_df = compare_grocery_baskets(
    TEST_GROCERY_BASKET, SAMPLE_GROCERY_LISTINGS, SAMPLE_GROCERY_METADATA
)
print("Module 5 — Normalized Grocery Line Items:")
display(grocery_items_df)
"""),
    md_cell("""
## Section 6 (Module 6): Basket Recommendation Algorithm

Ranking rules:
1. **Never recommend an incomplete basket as a complete basket.** Complete baskets (`is_complete_basket == True`) are strictly separated and ranked first.
2. Primary sort key: **Lowest `final_payable_cost`**.
3. Secondary sort key (tie-breaker): **Fastest `delivery_time_mins`**.
4. Unavailable items are surfaced explicitly in `missing_items` and placed in a separate `incomplete_baskets` tier.
"""),
    code_cell("""
def recommend_baskets(baskets_df: pd.DataFrame) -> Dict[str, Any]:
    if baskets_df.empty:
        return {"recommended": None, "complete_ranked": pd.DataFrame(), "incomplete_ranked": pd.DataFrame()}

    complete_df = baskets_df[baskets_df["is_complete_basket"]].copy()
    incomplete_df = baskets_df[~baskets_df["is_complete_basket"]].copy()

    complete_df = complete_df.sort_values(
        by=["final_payable_cost", "delivery_time_mins"], ascending=[True, True]
    ).reset_index(drop=True)
    complete_df["rank"] = complete_df.index + 1

    incomplete_df = incomplete_df.sort_values(
        by=["available_items", "final_payable_cost", "delivery_time_mins"],
        ascending=[False, True, True],
    ).reset_index(drop=True)

    best = complete_df.iloc[0].to_dict() if not complete_df.empty else None
    return {
        "recommended": best,
        "complete_ranked": complete_df,
        "incomplete_ranked": incomplete_df,
    }


recommendation_output = recommend_baskets(grocery_baskets_df)
print("Module 6 — Top Complete Basket Recommendation:")
print(json.dumps(recommendation_output["recommended"], indent=2))
print("\\nComplete Baskets (Ranked):")
display(recommendation_output["complete_ranked"])
print("\\nIncomplete Baskets (Excluded from Complete Ranking):")
display(recommendation_output["incomplete_ranked"])
"""),
    md_cell("""
## Section 7 (Module 7): Interactive Price Visualizations (Plotly)

Interactive charts for:
1. **Price History** (Buyhatke observations over time)
2. **Platform Price Comparison** (Listed price vs Final payable price)
3. **Discount Comparison** (Effective discount % across platforms)
4. **Basket Cost Comparison** (Stacked breakdown + Complete vs Incomplete indicator)
5. **Price Trends** (% above historical low & volatility CV% by category)
"""),
    code_cell("""
# 1. Interactive Price History
fig_history = px.line(
    buyhatke_history_df,
    x="observation_date",
    y="current_price",
    color="product_name",
    markers=True,
    title="1. Buyhatke Observed Price History ([SAMPLE TEST DATA])",
    labels={"observation_date": "Date", "current_price": "Price (₹)", "product_name": "Product"},
)
fig_history.write_html(CHARTS_DIR / "01_price_history.html")
fig_history.show()

# 2. Interactive Platform Price Comparison
fig_platform = go.Figure(data=[
    go.Bar(name="Listed Price", x=comparison_table_df["platform"], y=comparison_table_df["listed_price"]),
    go.Bar(name="Final Payable", x=comparison_table_df["platform"], y=comparison_table_df["final_payable_price"]),
])
fig_platform.update_layout(
    barmode="group",
    title="2. Platform Price Comparison: Listed vs Final Payable ([SAMPLE TEST DATA])",
    yaxis_title="Price (₹)",
)
fig_platform.write_html(CHARTS_DIR / "02_platform_price_comparison.html")
fig_platform.show()

# 3. Interactive Discount Comparison
fig_discount = px.bar(
    comparison_table_df,
    x="platform",
    y="discount_pct",
    color="discount_pct",
    text="discount_pct",
    title="3. Effective Discount Percentage by Platform ([SAMPLE TEST DATA])",
    labels={"discount_pct": "Effective Discount (%)", "platform": "Platform"},
)
fig_discount.write_html(CHARTS_DIR / "03_discount_comparison.html")
fig_discount.show()

# 4. Interactive Basket Cost Comparison
basket_plot_df = grocery_baskets_df.copy()
basket_plot_df["status"] = np.where(basket_plot_df["is_complete_basket"], "Complete Basket", "Incomplete (Missing Items)")
fig_basket = px.bar(
    basket_plot_df,
    x="platform",
    y=["item_subtotal", "delivery_charge", "platform_fee"],
    color_discrete_sequence=["#0F766E", "#D97706", "#64748B"],
    title="4. Grocery Basket Payable Cost Breakdown ([SAMPLE TEST DATA])",
    hover_data=["final_payable_cost", "delivery_time_mins", "status", "missing_items"],
)
fig_basket.write_html(CHARTS_DIR / "04_basket_cost_comparison.html")
fig_basket.show()

# 5. Interactive Price Trends & Volatility
fig_trends = px.scatter(
    buyhatke_summary_df,
    x="pct_above_hist_low",
    y="volatility_cv_pct",
    size="current_price",
    color="price_trend",
    text="category",
    hover_data=["product_name", "deal_recommendation"],
    title="5. Category Price Trends: % Above Historical Low vs Volatility ([SAMPLE TEST DATA])",
    labels={"pct_above_hist_low": "% Above Historical Low", "volatility_cv_pct": "Price Volatility (CV %)"},
)
fig_trends.write_html(CHARTS_DIR / "05_price_trends.html")
fig_trends.show()
"""),
    md_cell("""
## Section 8 (Module 8): Automated Verification Test Suite

Runs automated `unittest` checks covering all 8 required analytical components:
1. Product matching
2. Price normalization
3. Quantity conversion
4. Discount calculation
5. Basket total
6. Deal classification
7. Recommendation ranking
8. Missing data handling
"""),
    code_cell("""
class SmartBuyEngineTests(unittest.TestCase):
    def test_01_product_matching(self):
        q = normalize_product(RawProductInput(
            product_name="WH-1000XM5 Wireless ANC", brand="Sony", category="Headphones",
            model="WH1000XM5", size="Over-Ear", colour="Black", quantity=1, unit="unit"
        ))
        exact_cand = ("StoreA", normalize_product(RawProductInput(
            product_name="Sony WH-1000XM5 Wireless ANC Headphones", brand="Sony", category="Headphones",
            model="WH1000XM5", size="Over-Ear", colour="Black", quantity=1, unit="pcs"
        )))
        alt_cand = ("StoreB", normalize_product(RawProductInput(
            product_name="Sony WH-1000XM4 Wireless Headphones", brand="Sony", category="Headphones",
            model="WH1000XM4", size="Over-Ear", colour="Black", quantity=1, unit="pcs"
        )))
        res = match_products(q, [exact_cand, alt_cand])
        self.assertEqual(len(res["exact_matches"]), 1)
        self.assertGreaterEqual(res["exact_matches"][0].confidence_score, 88.0)
        self.assertEqual(len(res["similar_alternatives"]), 1)

    def test_02_price_normalization(self):
        self.assertAlmostEqual(parse_price_value("₹1,24,999.50"), 124999.50)
        self.assertIsNone(parse_price_value("N/A"))
        self.assertIsNone(parse_price_value(-50))

    def test_03_quantity_conversion(self):
        _, _, q_kg, u_kg = normalize_quantity_and_unit(500, "grams")
        self.assertAlmostEqual(q_kg, 0.5)
        self.assertEqual(u_kg, "kg")
        _, _, q_l, u_l = normalize_quantity_and_unit(1000, "ml")
        self.assertAlmostEqual(q_l, 1.0)
        self.assertEqual(u_l, "l")

    def test_04_discount_calculation(self):
        res = evaluate_offers([
            PlatformOffer(product_name="Test", platform="A", mrp=1000, listed_price=800, promo_discount=100, delivery_charge=50, platform_charge=10)
        ])
        row = res["table"].iloc[0]
        self.assertAlmostEqual(row["discount_pct"], 30.0)
        self.assertAlmostEqual(row["final_payable_price"], 760.0)

    def test_05_basket_total(self):
        _, b_df = compare_grocery_baskets(TEST_GROCERY_BASKET, SAMPLE_GROCERY_LISTINGS, SAMPLE_GROCERY_METADATA)
        vg = b_df[b_df["platform"] == "ValueGrocer [SAMPLE]"].iloc[0]
        # 30 + 60 + 24 (500g of 48/kg) + 145 + 78 = 337 subtotal; +0 del +10 fee -20 disc = 327
        self.assertAlmostEqual(vg["item_subtotal"], 337.0)
        self.assertAlmostEqual(vg["final_payable_cost"], 327.0)

    def test_06_deal_classification(self):
        self.assertIn("Near Observed All-Time Low", classify_deal(102.0, 100.0, 120.0, "Falling"))
        self.assertIn("Overpriced", classify_deal(140.0, 100.0, 120.0, "Rising"))

    def test_07_recommendation_ranking(self):
        df = pd.DataFrame([
            {"platform": "IncompleteCheap", "is_complete_basket": False, "available_items": 4, "final_payable_cost": 100.0, "delivery_time_mins": 5},
            {"platform": "CompleteSlower", "is_complete_basket": True, "available_items": 5, "final_payable_cost": 300.0, "delivery_time_mins": 25},
            {"platform": "CompleteFasterTie", "is_complete_basket": True, "available_items": 5, "final_payable_cost": 300.0, "delivery_time_mins": 12},
        ])
        rec = recommend_baskets(df)
        self.assertEqual(rec["recommended"]["platform"], "CompleteFasterTie")
        self.assertEqual(len(rec["incomplete_ranked"]), 1)

    def test_08_missing_data_handling(self):
        msg = classify_deal(500.0, None, None, "Stable")
        self.assertIn("Insufficient Historical Data", msg)
        _, b_df = compare_grocery_baskets(TEST_GROCERY_BASKET, SAMPLE_GROCERY_LISTINGS, SAMPLE_GROCERY_METADATA)
        sd = b_df[b_df["platform"] == "SpeedyDrop [SAMPLE]"].iloc[0]
        self.assertFalse(sd["is_complete_basket"])
        self.assertIn("apples", sd["missing_items"])


suite = unittest.defaultTestLoader.loadTestsFromTestCase(SmartBuyEngineTests)
runner = unittest.TextTestRunner(verbosity=2)
test_result = runner.run(suite)

test_summary = {
    "tests_run": test_result.testsRun,
    "failures": len(test_result.failures),
    "errors": len(test_result.errors),
    "passed": test_result.wasSuccessful(),
    "timestamp": datetime.utcnow().isoformat() + "Z",
}
print("\\nModule 8 — Automated Test Summary:", json.dumps(test_summary, indent=2))
"""),
    md_cell("""
## Section 9 (Module 9): Exported Outputs & Connecting to Google AI Studio

Exports all structured outputs to `smartbuy_outputs/`:
- `cleaned_products.csv` & `cleaned_products.json`
- `platform_comparison_table.csv`
- `buyhatke_price_history.csv` & `buyhatke_category_summary.csv`
- `recommendation_report.json`
- `test_results.json`
- Interactive Plotly charts under `smartbuy_outputs/charts/*.html`
"""),
    code_cell("""
cleaned_products_df.to_csv(OUTPUT_DIR / "cleaned_products.csv", index=False)
cleaned_products_df.to_json(OUTPUT_DIR / "cleaned_products.json", orient="records", indent=2)
comparison_table_df.to_csv(OUTPUT_DIR / "platform_comparison_table.csv", index=False)
buyhatke_history_df.to_csv(OUTPUT_DIR / "buyhatke_price_history.csv", index=False)
buyhatke_summary_df.to_csv(OUTPUT_DIR / "buyhatke_category_summary.csv", index=False)

report = {
    "generated_at": datetime.utcnow().isoformat() + "Z",
    "data_disclaimer": "All bundled datasets are [SAMPLE TEST DATA] for analytical validation.",
    "price_comparison_summary": price_comp_result["summary"],
    "buyhatke_category_summary": buyhatke_summary_df.to_dict(orient="records"),
    "grocery_recommendation": recommendation_output["recommended"],
    "incomplete_baskets_flagged": recommendation_output["incomplete_ranked"].to_dict(orient="records"),
    "test_summary": test_summary,
}
(OUTPUT_DIR / "recommendation_report.json").write_text(json.dumps(report, indent=2, default=str))
(OUTPUT_DIR / "test_results.json").write_text(json.dumps(test_summary, indent=2))

print("Module 9 — Exported artifacts:")
for p in sorted(OUTPUT_DIR.rglob("*")):
    if p.is_file():
        print(" -", p)
"""),
    md_cell("""
### Connecting This Colab Analytical Engine to Your Google AI Studio SmartBuy AI App

1. **Expose REST Endpoints with FastAPI (or Cloud Run):**
   Wrap Modules 1–6 in a lightweight `FastAPI` server (using the exact Pydantic models already defined above):
   - `POST /api/v1/products/normalize` → Module 1
   - `POST /api/v1/products/match` → Module 2
   - `POST /api/v1/prices/compare` → Module 3
   - `POST /api/v1/buyhatke/analyze` → Module 4
   - `POST /api/v1/grocery/recommend` → Modules 5 & 6
   - `GET /health` → Health check for the SmartBuy AI Android/Web client
2. **Production Deployment (Recommended):** Deploy the FastAPI module (`smartbuy_api.py`) to **Google Cloud Run** so your Google AI Studio app gets a stable HTTPS endpoint without Colab session timeouts. For quick Colab testing, you can temporarily tunnel port `8000` via `ngrok` or Cloudflare Tunnel.
3. **Client Integration in Google AI Studio:** In the **SmartBuy AI** Android app (`Engine` tab → *Backend Connection*), paste your Cloud Run / Colab FastAPI URL and tap **Test & Sync** to route live JSON requests via `Retrofit` + `Moshi` directly to these Python analytical endpoints.
"""),
]


def main() -> None:
    notebook = {
        "nbformat": 4,
        "nbformat_minor": 5,
        "metadata": {
            "colab": {"provenance": [], "toc_visible": True},
            "kernelspec": {"display_name": "Python 3", "name": "python3"},
            "language_info": {"name": "python", "version": "3.11"},
        },
        "cells": CELLS,
    }
    out_path = Path("SmartBuy_AI_Analytical_Engine.ipynb")
    out_path.write_text(json.dumps(notebook, indent=2))
    print(f"Generated {out_path} with {len(CELLS)} cells (all Python AST-verified).")


if __name__ == "__main__":
    main()
